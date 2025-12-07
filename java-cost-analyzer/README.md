# BT Merged Deep Analysis v4 (Aggressive)

This README describes the purpose, usage, and structure of the **BT Merged Deep Analysis v4** IntelliJ IDEA inspection profile.

## Overview
This inspection profile is designed for **large-scale Java codebases** and focuses on identifying:
- Performance issues  
- DB/Onshape operations inside loops  
- Expensive stream operations  
- Nested loops (O(n²) patterns)  
- Capability/event calls inside loops  
- Logging and string concatenation inside loops  
- Blocking calls inside async contexts  
- General Java dataflow, NPE, and code-quality issues  

The goal is to detect **performance bottlenecks, scalability risks, unnecessary allocations**, and misuse of important APIs in large backend systems.

## Key Changes in v4
- Eliminated all exclusion scripts  
- Ensures all SSR (Structural Search) rules run on all production files  
- Updated regex patterns for DB, Onshape, capability, and event APIs  
- Added deep-analysis rules: nested loops, stream-in-loop, async blocking detection  
- Deprecated inspection IDs removed or normalized  
- Designed to be used **with IntelliJ scopes**, not inline filters  

## Installation
1. Save the file `BT-Merged-DeepAnalysis-v4.xml` into your project or local system.
2. In IntelliJ IDEA:  
   `Settings → Editor → Inspections → ⚙ → Import Profile…`
3. Select:  
   **Scope: Production Code**
4. Run analysis:  
   `Analyze → Inspect Code…` and choose the imported profile.

## Recommended Usage
- Run on the entire codebase before performance refactoring.
- Run on modules individually for faster results.
- Review “ERROR” findings first (DB/Onshape in loops, blocking async calls).
- Then review “WARNING” and “WEAK WARNING” groups.

## Notes
This profile is intentionally aggressive.  
Expect high signal output, especially on:
- Loops  
- Streams  
- Complex service chains  
- Logging-heavy code  

Tune severities after first run if needed.
