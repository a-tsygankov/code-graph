import Sigma from 'sigma';
import Graph from 'graphology';
import {fetchPackages, GraphSliceDto, NodeDto} from './api';
const container=document.getElementById('container') as HTMLElement;
const graph=new Graph();
const renderer=new Sigma(graph, container);
function costLabel(c:number){return c===4?'CRITICAL':c===3?'VERY_HIGH':c===2?'HIGH':c===1?'MEDIUM':'LOW';}
function color(n:NodeDto){return n.cost===4?'#d50000':n.cost===3?'#ff6d00':n.cost===2?'#ffab00':n.cost===1?'#9ccc65':'#64b5f6';}
async function init(){try{const slice:GraphSliceDto=await fetchPackages();for(const n of slice.nodes){graph.addNode(n.id,{label:n.displayName,color:color(n),size:5});}for(const e of slice.edges){if(graph.hasNode(e.sourceId)&&graph.hasNode(e.targetId))graph.addEdge(e.id,e.sourceId,e.targetId);} }catch(e){console.error(e);} }
init();
