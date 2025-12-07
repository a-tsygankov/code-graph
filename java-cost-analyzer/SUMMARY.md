# Summary – BT Merged Deep Analysis v4 (Aggressive)

## What This Profile Does
The `BT Merged Deep Analysis v4` inspection profile identifies:
- Database calls inside loops  
- Onshape API interactions inside loops  
- Capability and event checks inside loops  
- Nested loops (both foreach and indexed)  
- Stream operations inside loops  
- Expensive stream terminal operations  
- Logging and string concatenation inside loops  
- Blocking calls inside async constructs  
- General dataflow issues and nullability defects  

## Why It Exists
This profile is tailored for large enterprise Java systems (e.g., Onshape/BelmontTech architecture) where:
- Backend logic is complex  
- Performance-sensitive operations are common  
- Loop nesting and repeated DB/HTTP/Capability checks can create massive slowdowns  

## Intended Workflow
1. Import the profile into IntelliJ IDEA  
2. Apply it using a **Production Code** scope  
3. Run **Inspect Code**  
4. Fix high-severity warnings first  
5. Review second-order findings (logging, string concatenation, streams)  

## Output Expectation
In large codebases, this profile will:
- Produce many findings (by design)  
- Highlight critical performance hotspots  
- Reveal expensive call patterns otherwise missed by default IntelliJ inspections  

Use results to guide:
- Refactoring  
- Performance fixes  
- API optimization  
- Domain-specific cleanup efforts  
