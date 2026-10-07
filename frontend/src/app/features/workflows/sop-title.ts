export interface SopHierarchyNode {nodeKey:string;parentKey:string|null;name:string;code:string|null;}
export function formattedSopTitle(nodes:SopHierarchyNode[],nodeKey:string,title:string,version:string):string {
 const path:SopHierarchyNode[]=[];const seen=new Set<string>();let node=nodes.find(n=>n.nodeKey===nodeKey);
 while(node&&!seen.has(node.nodeKey)){seen.add(node.nodeKey);path.unshift(node);node=nodes.find(n=>n.nodeKey===node!.parentKey);}
 const codes=path.map(n=>n.code||'?').join('-');
 const process=path[path.length-1]?.name||'Process';const family=path[path.length-2]?.name||'';
 return `${codes ? codes+' · ' : ''}${family ? family+' | ' : ''}${process} | ${title} · ${version}`;
}
