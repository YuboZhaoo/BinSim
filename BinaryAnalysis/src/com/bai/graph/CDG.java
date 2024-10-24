package com.bai.graph;

import com.bai.env.region.Global;
import com.bai.util.GlobalState;
import ghidra.graph.*;
import ghidra.graph.algo.ChkDominanceAlgorithm;
import ghidra.program.model.listing.Function;
import ghidra.program.model.pcode.HighFunction;
import ghidra.program.model.pcode.PcodeBlockBasic;
import ghidra.program.model.pcode.PcodeOp;
import ghidra.program.model.pcode.PcodeOpAST;
import ghidra.service.graph.AttributedEdge;
import ghidra.service.graph.AttributedVertex;
import ghidra.util.exception.CancelledException;
import org.json.JSONObject;

import java.util.*;



// control dependence graph
public class CDG extends GraphBasic{

    public class CDG_INST extends Graph_INST{
        protected GDirectedGraph<PcodeBlockVtx, GEdge<PcodeBlockVtx>> rcfg;
//        protected HashMap<PcodeOp, HashSet<PcodeOp>> ctrlDep;
        protected GDirectedGraph<PcodeBlockVtx, GEdge<PcodeBlockVtx>> postDominanceTree;
        Map<PcodeBlockBasic, PcodeBlockVtx> instanceMap;
        private CdgDisplay displayer;

        public CDG_INST(HighFunction highFunction) throws CancelledException {
            super();
            func = highFunction.getFunction();
            hfunc = highFunction;

//            ctrlDep = new HashMap<>();
            rcfg = GraphFactory.createDirectedGraph();
            buildGraph(hfunc);
        }
        private PcodeBlockVtx containsAny(Collection<PcodeBlockVtx> srcBB, Collection<PcodeBlockVtx> desBB) {
            for (PcodeBlockVtx des : desBB) {
                if (srcBB.contains(des)) {
                    return des;
                }
            }
            return null;
        }
        // v0 has problem with loop and dead loop entry point
        protected void buildRevCFG_v0(HighFunction hfunc) {
            instanceMap = new HashMap<PcodeBlockBasic, PcodeBlockVtx>();

            // TODO this should be deep copy, otherwise it will delete bb in hfunc
            ArrayList<PcodeBlockBasic> blocks = new ArrayList<>(hfunc.getBasicBlocks());

            PcodeBlockVtx exit_node = new PcodeBlockVtx("exit");
//            PcodeBlockVtx entry_node = new PcodeBlockVtx("entry");
            rcfg.addVertex(exit_node);
//            rcfg.addVertex(entry_node);

//        printf("build rcfg: block num: %d\n", blocks.size());
//            GlobalState.ghidraScript.print("blocksize "+blocks.size()+"\n");

            PcodeBlockBasic block = null;
            while (!blocks.isEmpty()) {
                block = blocks.remove(0);
                PcodeBlockVtx fromVtx = instanceMap.get(block);
                if (fromVtx == null) {
                    fromVtx = new PcodeBlockVtx(block, block.toString());
                    instanceMap.put(block, fromVtx);
                    rcfg.addVertex(fromVtx);
                }
                boolean self_loop = false;
                for(int i=0;i<block.getOutSize();i++){
                    PcodeBlockBasic succ_block = (PcodeBlockBasic)block.getOut(i);
                    if(succ_block == block) self_loop = true;
                    if (succ_block!=null){
                        PcodeBlockVtx toVtx = instanceMap.get(succ_block);
                        if (toVtx == null) {
                            toVtx = new PcodeBlockVtx(succ_block, succ_block.toString());
                            instanceMap.put(succ_block, toVtx);
                            rcfg.addVertex(toVtx);
                        }
                        int flag = 0;
                        for(int j=0;j<succ_block.getOutSize();j++){
                            PcodeBlockBasic srcbb = (PcodeBlockBasic)succ_block.getOut(j);
                            if(srcbb==block){
                                flag=1;
                            }
                        }
                        if(flag==0)
                            rcfg.addEdge(new DefaultGEdge(toVtx, fromVtx));
                    }
                }
                if(block.getOutSize()==0){
                    rcfg.addEdge(new DefaultGEdge(exit_node, fromVtx));
                }
                if(block.getOutSize()==1 && self_loop){ // give the dead loop an exit
//                    GlobalState.ghidraScript.print("dead loop\n");
                    rcfg.addEdge(new DefaultGEdge(exit_node, fromVtx));
                }
                // FIXME multi-nodes dead loop
                if(block.getOutSize()==1){ // give the two bb dead loop an exit
                    int flag = 0;
                    PcodeBlockBasic targetbb = (PcodeBlockBasic) block.getOut(0);
                    for(int i=0;i<targetbb.getOutSize();i++){
                        PcodeBlockBasic srcbb = (PcodeBlockBasic)targetbb.getOut(i);
                        if(srcbb==block)flag=1;
                    }
                    if(flag==1){
                        rcfg.addEdge(new DefaultGEdge(exit_node, fromVtx));
                    }
                }
            }
//        printf("build rcfg: block num: %d\n", rcfg.getVertices().size());
//        printf("build rcfg: edge num: %d\n", rcfg.getEdges().size());
//            GlobalState.ghidraScript.print("blocksize "+blocks.size()+"\n");
//            GlobalState.ghidraScript.printf("build rcfg: block num: %d\n", rcfg.getVertices().size());
//            assert rcfg.getVertices().size()==blocks.size()+1;
        }

        // v1 use getEntryPoints(rcfg)
        protected void buildRevCFG(HighFunction hfunc) {
            instanceMap = new HashMap<PcodeBlockBasic, PcodeBlockVtx>();

            // TODO this should be deep copy, otherwise it will delete bb in hfunc
            ArrayList<PcodeBlockBasic> blocks = new ArrayList<>(hfunc.getBasicBlocks());

            PcodeBlockBasic block = null;
            while (!blocks.isEmpty()) {
                block = blocks.remove(0);
                PcodeBlockVtx fromVtx = instanceMap.get(block);
                if (fromVtx == null) {
                    fromVtx = new PcodeBlockVtx(block, block.toString());
                    instanceMap.put(block, fromVtx);
                    rcfg.addVertex(fromVtx);
                }
                for(int i=0;i<block.getOutSize();i++){
                    PcodeBlockBasic succ_block = (PcodeBlockBasic)block.getOut(i);
                    if (succ_block!=null){
                        PcodeBlockVtx toVtx = instanceMap.get(succ_block);
                        if (toVtx == null) {
                            toVtx = new PcodeBlockVtx(succ_block, succ_block.toString());
                            instanceMap.put(succ_block, toVtx);
                            rcfg.addVertex(toVtx);
                        }
                        rcfg.addEdge(new DefaultGEdge(toVtx, fromVtx));
                    }
                }
            }

            PcodeBlockVtx exit_node = new PcodeBlockVtx("exit");
//            rcfg.addVertex(exit_node);
            for (PcodeBlockVtx vertex : GraphAlgorithms.getEntryPoints(rcfg)) {
                // will not create repetitive edges
                rcfg.addEdge(new DefaultGEdge(exit_node, vertex));
            }
        }
        public void addControlDepFromNodeToBB(PcodeOp srcOp, PcodeBlockBasic block) {
            Iterator<PcodeOp> ins_iter = block.getIterator();
            if (!edges.containsKey(srcOp)) {
                edges.put(srcOp, new HashSet<PcodeOp>());
            }
            HashSet<PcodeOp> control = edges.get(srcOp);
            while (ins_iter.hasNext()) {
                PcodeOp p = ins_iter.next();
//            AttributedEdge edge = createEdge(srcOp, p);
                control.add(p);
            }
        }
        public void addControlDepFromDominatedBlockToDominator(PcodeOp srcOp, PcodeBlockVtx srcBB, PcodeBlockVtx desBB,
                                                               GDirectedGraph<PcodeBlockVtx, GEdge<PcodeBlockVtx>> postDominanceTree) {
            Collection<PcodeBlockVtx> pdOfSrc = postDominanceTree.getPredecessors(srcBB);
            Collection<PcodeBlockVtx> dominatedBlock = new HashSet<PcodeBlockVtx>();
            dominatedBlock.add(desBB);
            PcodeBlockVtx nearestCommonDominator;
            // walk up along the Post Dominance Tree, start from desBB
            while (true) {
                Collection<PcodeBlockVtx> newDominatedBlock = new HashSet<PcodeBlockVtx>();
                for (PcodeBlockVtx pd : dominatedBlock) {
                    newDominatedBlock.addAll(postDominanceTree.getPredecessors(pd));
                    addControlDepFromNodeToBB(srcOp, pd.getCodeBlock());
                }
                nearestCommonDominator = this.containsAny(pdOfSrc, newDominatedBlock);
                if (nearestCommonDominator != null) {
                    break;
                }
                dominatedBlock = newDominatedBlock;
            }
            if (nearestCommonDominator.equals(srcBB)) {
                addControlDepFromNodeToBB(srcOp, srcBB.getCodeBlock());
            }
        }
        protected void getCtrlDep()throws CancelledException {
            for (GEdge edge : rcfg.getEdges()) {
                PcodeBlockVtx desBB = (PcodeBlockVtx)edge.getStart();
                PcodeBlockVtx srcBB = (PcodeBlockVtx)edge.getEnd();
                if (!GraphAlgorithms.findDominance(rcfg, desBB, GlobalState.monitor).contains(srcBB)) {
                    Iterator<PcodeOp> iter = srcBB.getCodeBlock().getIterator();
                    PcodeOp srcOp =  getLastElement(iter);
                    addControlDepFromDominatedBlockToDominator(srcOp, srcBB, desBB, postDominanceTree);
                }
            }
        }
        public void buildGraph(HighFunction hfunc) throws CancelledException {
            buildRevCFG(hfunc);
//            ChkDominanceAlgorithm<PcodeBlockVtx, GEdge<PcodeBlockVtx>> algorithm = new ChkDominanceAlgorithm<>(rcfg, GlobalState.monitor);
//            GlobalState.ghidraScript.print(algorithm+"\n");
            postDominanceTree = GraphAlgorithms.findDominanceTree(rcfg, GlobalState.monitor);
            getCtrlDep();
            Iterator<PcodeOpAST> opiter = hfunc.getPcodeOps();
            while (opiter.hasNext()) {
                PcodeOp op = (PcodeOp) opiter.next();
                nodes.add(op);
            }
        }

        // dump to json
        public JSONObject dump(){
            GraphJson json = new GraphJson(hfunc);
            return json.dumpGraph_INST(nodes, edges);
        }
//        public JSONObject dump(){
//            CDGJson cdg_json = new CDGJson(hfunc);
//            return cdg_json.dumpGraph();
//        }
//        public JSONObject dump(int edge_type){
//            CDGJson cdg_json = new CDGJson(hfunc, edge_type);
//            return cdg_json.dumpGraph();
//        }
//        class CDGJson extends GraphJson{
//            public CDGJson(HighFunction hfunc){super(hfunc);}
//            public CDGJson(HighFunction hfunc, int edge_type){super(hfunc, edge_type);}
//            public JSONObject dumpGraph(){
//                return dumpGraph_INST(nodes, edges);
//            }
//        }

        // display in ghidra
        public void display() throws Exception {
            displayer = new CdgDisplay(func, hfunc);
            displayer.display();
        }
        class CdgDisplay extends GraphDisplay {
            public CdgDisplay(Function func, HighFunction hfunc){
                super(func, hfunc);
            }
            @Override
            protected AttributedVertex createOpVertex(PcodeOpAST op) { // create node of pcode operation
                String name = op.toString();
                String id = getOpKey(op);
                AttributedVertex vert = graph.addVertex(id, name);
                vert.setAttribute(ICON_ATTRIBUTE, "Square");
                return vert;
            }
            protected AttributedEdge createEdge(PcodeOp in, PcodeOp out) {
                AttributedVertex from = graph.getVertex(getOpKey( (PcodeOpAST) in ));
                AttributedVertex to = graph.getVertex(getOpKey( (PcodeOpAST) out ));
                return graph.addEdge(from, to);
            }
            public void buildAttrGraph(){
                for (PcodeOp op: nodes){
                    AttributedVertex o = displayer.createOpVertex( (PcodeOpAST) op);
                }
                for (PcodeOp from: edges.keySet()){
                    for(PcodeOp to: edges.get(from)){
                        AttributedEdge edge = createEdge(from, to);
                        edge.setAttribute(COLOR_ATTRIBUTE, "Red");
                    }
                }
            }
            public void display() throws Exception {
                buildAttrGraph();
                displayGraph();
            }
        }
    }
    class PcodeBlockVtx {
        private final PcodeBlockBasic pcodeBlock;
        private final String name;
        public PcodeBlockVtx(PcodeBlockBasic codeBlock, String name) { // for normal block
            this.pcodeBlock = codeBlock;
            this.name = name;
        }
        public PcodeBlockVtx(String name) { // for entry and exit node
            this.pcodeBlock = null;
            this.name = name;
        }
        public PcodeBlockBasic getCodeBlock() {
            return pcodeBlock;
        }

        public String getName() {
            return name;
        }
    }
    public static <T> T getLastElement(Iterator<T> iterator) {
        T lastElement = null;
        while (iterator.hasNext()) {
            lastElement = iterator.next();
        }
        return lastElement;
    }

}
