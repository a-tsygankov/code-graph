# Repository Overview

This repository brings together two siblings that grew up in different folders but share the same mission: **making large Java codebases slightly less scary**.  
The first one, **BelmontTech Code Graph**, builds huge structural graphs from source code—because sometimes the only way to understand a system is to draw a map so large that it makes cartographers blush. The second one, **Java Cost Analyzer**, uses those graphs (and other heuristics) to hunt for performance landmines, anti-patterns, and suspicious loops where someone thought *“one more database call won’t hurt”*.  

Together, these tools help identify hot spots, strange call patterns, questionable architectural decisions, and the occasional “I swear this looked fine during the sprint” code. Results may range from extremely useful to mildly alarming—but hey, that’s static analysis for you. For detailed documentation, see:  
- [belmonttech-code-graph/README.md](belmonttech-code-graph/README.md)  
- [java-cost-analyzer/README.md](java-cost-analyzer/README.md)  
