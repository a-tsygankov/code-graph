export type NodeLevel = 'METHOD'|'CLASS'|'INTERFACE'|'PACKAGE';
export interface NodeDto { id:string; level:NodeLevel; displayName:string; packageName?:string|null; className?:string|null; methodName?:string|null; signature?:string|null; cost:number; complexity:number; hotspot:boolean; }
export interface EdgeDto { id:string; sourceId:string; targetId:string; type:string; weight:number; }
export interface GraphSliceDto { nodes:NodeDto[]; edges:EdgeDto[]; }
const BASE='http://localhost:8080/api/graph';
export const fetchPackages=async():Promise<GraphSliceDto>=>{const r=await fetch(BASE+'/packages');if(!r.ok)throw new Error('packages');return r.json();};
export function costLabel(cost: number): string {
  switch (cost) {
    case 0: return "LOWEST";
    case 1: return "LOW";
    case 2: return "MEDIUM";
    case 3: return "HIGH";
    case 4: return "CRITICAL";
    default: return "UNKNOWN";
  }
}
export function costColor(cost: number): string {
  switch (cost) {
    case 4: return "#d50000"; // CRITICAL
    case 3: return "#ff6d00"; // HIGH
    case 2: return "#ffab00"; // MEDIUM
    case 1: return "#64b5f6"; // LOW
    case 0: return "#90caf9"; // LOWEST
    default: return "#bdbdbd"; // UNKNOWN
  }
}
