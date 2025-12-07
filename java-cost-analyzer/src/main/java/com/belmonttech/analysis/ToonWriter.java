package com.belmonttech.analysis;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import static com.belmonttech.analysis.TOONConstants.*;

/**
 * Encapsulates TOON output formatting.
 */
public final class ToonWriter {

    private ToonWriter() {
    }

    public static void writeHeader(Writer w) throws IOException {
        w.write("TOON\n");
        w.write("version: 1\n");
        w.write("\n");
    }

    public static void writeClassHeader(
            Writer w,
            String file,
            String className,
            String parent
    ) throws IOException {
        w.write(INDENT_CLASS + "File: " + file + "\n");
        w.write(INDENT_CLASS + "Class: " + className + "\n");
        w.write(INDENT_CLASS + "Parent: " + parent + "\n");
        w.write(INDENT_CLASS + "Methods:\n");
    }

    public static void writeMethod(
            Writer w,
            String methodName,
            String visibility,
            String signature,
            List<String> annotations,
            List<String> throwsTypes,
            int complexity,          // 0..4
            String costLabel,        // LOWEST/LOW/MEDIUM/HIGH/CRITICAL
            List<String> calls,
            List<String> calledBy
    ) throws IOException {

        w.write(INDENT_METHOD + "- method: " + methodName + "\n");
        w.write(INDENT_FIELD + "visibility: " + visibility + "\n");
        w.write(INDENT_FIELD + "signature: " + signature + "\n");

        writeList(w, "annotations", annotations);
        writeList(w, "throws", throwsTypes);

        w.write(INDENT_FIELD + "complexity: " + complexity + "\n");
        w.write(INDENT_FIELD + "cost: " + costLabel + "\n");

        writeList(w, "calls", calls);
        writeList(w, "calledBy", calledBy);
        w.write("\n");
    }

    private static void writeList(Writer w, String name, List<String> values) throws IOException {
        if (values == null || values.isEmpty()) {
            w.write(INDENT_FIELD + name + ": []\n");
            return;
        }
        w.write(INDENT_FIELD + name + ":\n");
        for (String v : values) {
            w.write(INDENT_LIST_ITEM + v + "\n");
        }
    }
}
