package com.belmonttech.analysis;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe Java cost analyzer:
 *  - Java 21 language level (switch expressions, pattern instanceof, records, etc.)
 *  - Parses all Java files under a root directory using JavaParser + symbol solver
 *  - Computes method complexity (0..4)
 *  - Boosts complexity for DB/REST/RPC methods (CostSignals)
 *  - Builds a cross-class call graph
 *  - Propagates complexity along call graph and through inheritance
 *  - Writes a TOON-formatted report
 *
 * CLI:
 *   JavaCostAnalyzer <root> [output] [minComplexity]
 *
 *   minComplexity: 0..4 (0 = include everything, 4 = only most complex)
 */
public class JavaCostAnalyzer {

    private static final int DEFAULT_THREADS =
            Math.max(2, Runtime.getRuntime().availableProcessors());

    // ================================
    // Main
    // ================================
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: JavaCostAnalyzer <root> [output] [minComplexity]");
            System.exit(1);
        }

        Path rootDir = Paths.get(args[0]).toAbsolutePath().normalize();

        String defaultOut = "java_cost_report_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) +
                ".toon";
        Path out = args.length >= 2 ? Paths.get(args[1]) : Paths.get(defaultOut);

        final int minRequiredComplexity = args.length >= 3 ? Integer.parseInt(args[2]) : 0;
        if (minRequiredComplexity < 0 || minRequiredComplexity > 4) {
            System.err.println("Usage: JavaCostAnalyzer <root> [output] [minComplexity]");
            System.err.println("Error: minComplexity should be in [0..4] range");
            System.exit(1);
        }

        System.out.println("Root: " + rootDir);
        System.out.println("Using " + DEFAULT_THREADS + " threads for analysis");

        // --- Parser + TypeSolver configuration (single shared, immutable) ---
        ParserConfiguration parserConfig = new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21)
                .setAttributeComments(false);

        JavaParserTypeSolver javaParserTypeSolver =
                new JavaParserTypeSolver(rootDir.toFile(), parserConfig);

        CombinedTypeSolver solver = new CombinedTypeSolver(
                new ReflectionTypeSolver(false),
                javaParserTypeSolver
        );

        parserConfig.setSymbolResolver(new JavaSymbolSolver(solver));

        System.out.println("JavaParser language level   = " + parserConfig.getLanguageLevel());
        String jpVersion = JavaParser.class.getPackage().getImplementationVersion();
        System.out.println("JavaParser implementation   = " + jpVersion);

        AnalysisContext ctx = new AnalysisContext(solver, parserConfig);

        // --- Discover all Java files ---
        List<Path> javaFiles = new ArrayList<>();
        try (var walk = Files.walk(rootDir)) {
            walk.filter(p -> p.toString().endsWith(".java"))
                    .sorted()
                    .forEach(javaFiles::add);
        }

        System.out.println("Found " + javaFiles.size() + " Java files under " + rootDir);

        // --- Parse each file concurrently, with progress bar ---
        parseFilesConcurrently(javaFiles, ctx);

        // --- Build call graph (parallel with progress) ---
        buildCallGraphParallel(ctx);

        // --- Propagate complexity via call graph (single-threaded, iterative) ---
        propagateMethodCallComplexity(ctx);

        // --- Propagate complexity up inheritance (parallel with progress) ---
        propagateInheritanceComplexityParallel(ctx);

        // --- Write TOON report (parallel block build + sequential write) ---
        writeReportParallel(ctx, out, minRequiredComplexity);

        System.out.println("Done.");
    }

    // ================================
    // Context & models (thread-safe maps)
    // ================================

    private static class AnalysisContext {
        final Map<String, ClassInfo> classes = new ConcurrentHashMap<>();
        final Map<MethodKey, MethodInfo> methods = new ConcurrentHashMap<>();
        final CombinedTypeSolver solver;
        final ParserConfiguration parserConfig;

        AnalysisContext(CombinedTypeSolver solver, ParserConfiguration parserConfig) {
            this.solver = solver;
            this.parserConfig = parserConfig;
        }
    }

    private static class ClassInfo {
        String file;
        String fqName;
        String simpleName;
        String parent;                      // simple name of superclass or "null"
        List<String> interfaces = new ArrayList<>(); // simple names
        List<MethodInfo> methods = new ArrayList<>();
    }

    private record MethodKey(String classFq, String name, int paramCount) {
        String id() {
            return classFq + "#" + name + "/" + paramCount;
        }
    }

    private static class MethodInfo {
        MethodKey key;
        String visibility;
        String signature;
        List<String> annotations = new ArrayList<>();
        List<String> throwsTypes = new ArrayList<>();
        volatile int complexity; // 0..4, monotonic non-decreasing

        List<MethodKey> calls;
        List<MethodKey> calledBy;

        List<String> callsReadable() {
            return calls == null
                    ? List.of()
                    : calls.stream().map(MethodKey::id).toList();
        }

        List<String> calledByReadable() {
            return calledBy == null
                    ? List.of()
                    : calledBy.stream().map(MethodKey::id).toList();
        }
    }

    // ================================
    // Functional helper
    // ================================

    @FunctionalInterface
    private interface ThrowingConsumer<T> {
        void accept(T t) throws Exception;
    }

    private static <T> void parallelForEachWithProgress(
            String label,
            List<T> items,
            ThrowingConsumer<T> action
    ) throws InterruptedException {

        int total = items.size();
        if (total == 0) {
            return;
        }

        ExecutorService executor = Executors.newFixedThreadPool(DEFAULT_THREADS);
        AtomicInteger processed = new AtomicInteger(0);

        for (T item : items) {
            executor.submit(() -> {
                try {
                    action.accept(item);
                } catch (Exception e) {
                    System.err.println(label + " error: " + e.getMessage());
                } finally {
                    processed.incrementAndGet();
                }
            });
        }

        while (true) {
            int done = processed.get();
            printProgressBar(label, done, total);
            if (done >= total) {
                break;
            }
            Thread.sleep(150);
        }

        executor.shutdown();
        executor.awaitTermination(365, TimeUnit.DAYS);
        printProgressBar(label, total, total);
    }

    // ================================
    // Concurrent parsing
    // ================================

    private static void parseFilesConcurrently(List<Path> javaFiles, AnalysisContext ctx) throws InterruptedException {
        parallelForEachWithProgress(
                "Parsing Java files",
                javaFiles,
                file -> analyzeFile(file, ctx)
        );
    }

    // ================================
    // Parsing & method extraction
    // ================================

    private static void analyzeFile(Path file, AnalysisContext ctx) {
        try {
            // per-thread parser, shared immutable config
            JavaParser parser = new JavaParser(ctx.parserConfig);

            ParseResult<CompilationUnit> result = parser.parse(file);
            Optional<CompilationUnit> optCu = result.getResult();

            if (optCu.isEmpty()) {
                System.err.println("Parse error: " + file + " (no result)");
                result.getProblems().forEach(p ->
                        System.err.println("  Problem: " + p));
                return;
            }

            CompilationUnit cu = optCu.get();

            String pkg = cu.getPackageDeclaration()
                    .map(p -> p.getName().asString())
                    .orElse("");

            List<ClassOrInterfaceDeclaration> decls = cu.findAll(ClassOrInterfaceDeclaration.class);
            for (ClassOrInterfaceDeclaration cls : decls) {
                ClassInfo ci = new ClassInfo();
                ci.file = file.toString();
                ci.simpleName = cls.getNameAsString();
                ci.fqName = pkg.isEmpty()
                        ? ci.simpleName
                        : pkg + "." + ci.simpleName;

                ci.parent = "null";

                if (!cls.getExtendedTypes().isEmpty()) {
                    if (!cls.isInterface()) {
                        // class extends base class
                        ci.parent = cls.getExtendedTypes().get(0).getNameAsString();
                    } else {
                        // interface extends super-interfaces
                        cls.getExtendedTypes()
                                .forEach(t -> ci.interfaces.add(t.getNameAsString()));
                    }
                }

                // implemented interfaces for classes
                cls.getImplementedTypes()
                        .forEach(t -> ci.interfaces.add(t.getNameAsString()));

                // Build methods for this class
                for (MethodDeclaration m : cls.getMethods()) {
                    MethodInfo mi = extractMethod(ci, m);
                    ctx.methods.put(mi.key, mi);
                    ci.methods.add(mi);
                }

                // Register class AFTER methods are built
                ctx.classes.put(ci.fqName, ci);
            }
        } catch (Exception e) {
            System.err.println("Parse error: " + file + ": " + e.getMessage());
        }
    }

    private static MethodInfo extractMethod(ClassInfo ci, MethodDeclaration m) {
        MethodInfo mi = new MethodInfo();

        mi.key = new MethodKey(
                ci.fqName,
                m.getNameAsString(),
                m.getParameters().size()
        );

        if (m.isPublic()) mi.visibility = "public";
        else if (m.isProtected()) mi.visibility = "protected";
        else if (m.isPrivate()) mi.visibility = "private";
        else mi.visibility = "package-private";

        mi.signature = m.getDeclarationAsString(true, true, true)
                .replaceAll("\\s+", " ")
                .trim();

        for (AnnotationExpr a : m.getAnnotations()) {
            mi.annotations.add("@" + a.getNameAsString());
        }

        m.getThrownExceptions().forEach(te -> mi.throwsTypes.add(te.toString()));

        // base complexity
        mi.complexity = computeComplexity(m);

        // external IO (DB / REST / RPC) => always maximal complexity
        String methodText = m.toString();
        if (CostSignals.hasExternalIo(methodText)) {
            mi.complexity = 4;
        }

        // initialize call lists
        mi.calls = new ArrayList<>();
        mi.calledBy = Collections.synchronizedList(new ArrayList<>());

        // resolve outgoing calls
        m.getBody().ifPresent(body ->
                body.accept(new VoidVisitorAdapter<MethodInfo>() {
                    @Override
                    public void visit(MethodCallExpr mc, MethodInfo collector) {
                        super.visit(mc, collector);
                        try {
                            ResolvedMethodDeclaration resolved = mc.resolve();
                            collector.calls.add(new MethodKey(
                                    resolved.declaringType().getQualifiedName(),
                                    resolved.getName(),
                                    resolved.getNumberOfParams()
                            ));
                        } catch (Exception ignored) {
                        }
                    }
                }, mi)
        );

        return mi;
    }

    private static int computeComplexity(MethodDeclaration m) {
        if (m.getBody().isEmpty()) return 0;

        final int[] score = {0};

        m.getBody().get().accept(new VoidVisitorAdapter<Void>() {
            @Override public void visit(IfStmt n, Void a) { score[0]++;   super.visit(n, a); }
            @Override public void visit(ForStmt n, Void a) { score[0] += 2; super.visit(n, a); }
            @Override public void visit(ForEachStmt n, Void a) { score[0] += 2; super.visit(n, a); }
            @Override public void visit(WhileStmt n, Void a) { score[0] += 2; super.visit(n, a); }
            @Override public void visit(DoStmt n, Void a) { score[0] += 2; super.visit(n, a); }
            @Override public void visit(CatchClause n, Void a) { score[0]++; super.visit(n, a); }
            @Override public void visit(SwitchEntry n, Void a) {
                score[0] += n.getLabels().size();
                super.visit(n, a);
            }
        }, null);

        if (score[0] >= 10) return 4;
        if (score[0] >= 6)  return 3;
        if (score[0] >= 3)  return 2;
        if (score[0] >= 1)  return 1;
        return 0;
    }

    private static String complexityLabel(int c) {
        return switch (c) {
            case 0 -> "LOWEST";
            case 1 -> "LOW";
            case 2 -> "MEDIUM";
            case 3 -> "HIGH";
            case 4 -> "CRITICAL";
            default -> "UNKNOWN";
        };
    }

    // ================================
    // Call graph (parallel)
    // ================================

    private static void buildCallGraphParallel(AnalysisContext ctx) throws InterruptedException {
        List<MethodInfo> methodList = new ArrayList<>(ctx.methods.values());
        if (methodList.isEmpty()) return;

        parallelForEachWithProgress(
                "Building call graph",
                methodList,
                mi -> {
                    // filter unknown calls
                    List<MethodKey> filtered = new ArrayList<>();
                    for (MethodKey k : mi.calls) {
                        if (ctx.methods.containsKey(k)) {
                            filtered.add(k);
                        }
                    }
                    mi.calls = filtered;

                    // build reverse edges
                    for (MethodKey calleeKey : filtered) {
                        MethodInfo callee = ctx.methods.get(calleeKey);
                        if (callee != null && callee.calledBy != null) {
                            callee.calledBy.add(mi.key);
                        }
                    }
                }
        );
    }

    private static void propagateMethodCallComplexity(AnalysisContext ctx) {
        boolean changed;
        do {
            changed = false;
            for (MethodInfo mi : ctx.methods.values()) {
                int newC = mi.complexity;
                if (mi.calls != null) {
                    for (MethodKey k : mi.calls) {
                        MethodInfo callee = ctx.methods.get(k);
                        if (callee != null) {
                            newC = Math.max(newC, callee.complexity);
                        }
                    }
                }
                if (newC != mi.complexity) {
                    mi.complexity = newC;
                    changed = true;
                }
            }
        } while (changed);
    }

    // ================================
    // Inheritance propagation (parallel, upward-only)
    // ================================

    private static void propagateInheritanceComplexityParallel(AnalysisContext ctx) throws InterruptedException {

        // parentSimpleName -> children FQNs
        Map<String, Set<String>> parentSimpleToChildrenFq = new HashMap<>();

        for (ClassInfo ci : ctx.classes.values()) {
            // class inheritance
            if (!"null".equals(ci.parent)) {
                parentSimpleToChildrenFq
                        .computeIfAbsent(ci.parent, k -> new HashSet<>())
                        .add(ci.fqName);
            }

            // interfaces + super-interfaces
            for (String ifaceSimple : ci.interfaces) {
                parentSimpleToChildrenFq
                        .computeIfAbsent(ifaceSimple, k -> new HashSet<>())
                        .add(ci.fqName);
            }
        }

        // list of parents that actually have children
        List<ClassInfo> parentList = new ArrayList<>();
        for (ClassInfo ci : ctx.classes.values()) {
            if (parentSimpleToChildrenFq.containsKey(ci.simpleName)) {
                parentList.add(ci);
            }
        }

        if (parentList.isEmpty()) {
            return;
        }

        boolean changed;
        int iteration = 0;

        do {
            iteration++;
            AtomicBoolean iterationChanged = new AtomicBoolean(false);

            final String label = "Propagating inheritance (iter " + iteration + ")";

            parallelForEachWithProgress(
                    label,
                    parentList,
                    parentCi -> {
                        String parentSimple = parentCi.simpleName;
                        Set<String> childFqs = parentSimpleToChildrenFq.get(parentSimple);
                        if (childFqs == null || childFqs.isEmpty()) {
                            return;
                        }

                        for (MethodInfo parentMethod : ctx.methods.values()) {
                            if (!parentMethod.key.classFq.equals(parentCi.fqName)) continue;

                            int maxComplexity = parentMethod.complexity;

                            for (String childFq : childFqs) {
                                for (MethodInfo childMethod : ctx.methods.values()) {
                                    if (!childMethod.key.classFq.equals(childFq)) continue;

                                    if (childMethod.key.name.equals(parentMethod.key.name)
                                            && childMethod.key.paramCount == parentMethod.key.paramCount) {
                                        maxComplexity = Math.max(maxComplexity, childMethod.complexity);
                                    }
                                }
                            }

                            if (maxComplexity > parentMethod.complexity) {
                                parentMethod.complexity = maxComplexity;
                                iterationChanged.set(true);
                            }
                        }
                    }
            );

            changed = iterationChanged.get();
        } while (changed);
    }

    // ================================
    // Report writing (parallel block build, sequential write)
    // ================================

    private record IndexedClass(int index, ClassInfo ci) {}

    private static void writeReportParallel(AnalysisContext ctx, Path out, int minRequiredComplexity) throws Exception {
        List<ClassInfo> classList = new ArrayList<>(ctx.classes.values());
        int totalClasses = classList.size();
        if (totalClasses == 0) {
            try (Writer w = new BufferedWriter(
                    new OutputStreamWriter(Files.newOutputStream(out), StandardCharsets.UTF_8))) {
                ToonWriter.writeHeader(w);
            }
            System.out.println("Report written to: " + out + " (no classes)");
            return;
        }

        List<IndexedClass> indexed = new ArrayList<>(totalClasses);
        for (int i = 0; i < totalClasses; i++) {
            indexed.add(new IndexedClass(i, classList.get(i)));
        }

        String[] blocks = new String[totalClasses];

        parallelForEachWithProgress(
                "Writing report",
                indexed,
                item -> {
                    int i = item.index();
                    ClassInfo ci = item.ci();

                    List<MethodInfo> filtered = ci.methods.stream()
                            .filter(m -> m.complexity >= minRequiredComplexity)
                            .toList();

                    if (filtered.isEmpty()) {
                        blocks[i] = "";
                        return;
                    }

                    StringWriter sw = new StringWriter();
                    ToonWriter.writeClassHeader(sw, ci.file, ci.fqName, ci.parent);

                    for (MethodInfo mi : filtered) {
                        ToonWriter.writeMethod(
                                sw,
                                mi.key.name,
                                mi.visibility,
                                mi.signature,
                                mi.annotations,
                                mi.throwsTypes,
                                mi.complexity,
                                complexityLabel(mi.complexity),
                                mi.callsReadable(),
                                mi.calledByReadable()
                        );
                    }

                    sw.write("\n");
                    blocks[i] = sw.toString();
                }
        );

        try (Writer w = new BufferedWriter(
                new OutputStreamWriter(Files.newOutputStream(out), StandardCharsets.UTF_8))) {

            ToonWriter.writeHeader(w);

            for (String block : blocks) {
                if (block != null && !block.isEmpty()) {
                    w.write(block);
                }
            }
        }

        System.out.println("Report written to: " + out);
        System.out.println("Threshold minComplexity = " + minRequiredComplexity);
    }

    // ================================
    // Simple single-line progress bar
    // ================================

    private static void printProgressBar(String label, int current, int total) {
        if (total <= 0) {
            return;
        }
        if (current < 0) current = 0;
        if (current > total) current = total;

        int width = 30;
        double ratio = (double) current / (double) total;
        int filled = (int) Math.round(ratio * width);

        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < width; i++) {
            bar.append(i < filled ? '█' : '░');
        }

        int percent = (int) Math.round(ratio * 100.0);

        String line = String.format(
                "\r%-45s [%s] %3d%% (%d/%d)",
                label,
                bar,
                percent,
                current,
                total
        );

        System.out.print(line);
        if (current == total) {
            System.out.println();
        }
        System.out.flush();
    }
}