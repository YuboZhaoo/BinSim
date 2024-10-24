package com.bai.graph;

import com.bai.analysis.SelectivityAnalysisSimple;
import com.bai.util.GlobalState;
import generic.stl.Pair;
import ghidra.graph.*;
import ghidra.program.model.listing.Function;
import ghidra.program.model.pcode.HighFunction;
import ghidra.program.model.pcode.PcodeBlockBasic;
import ghidra.program.model.pcode.PcodeOp;
import ghidra.program.model.pcode.PcodeOpAST;
import ghidra.service.graph.AttributedEdge;
import ghidra.service.graph.AttributedVertex;
import ghidra.util.exception.CancelledException;

import java.util.*;

// TODO not implemented
public class ProbCDG extends GraphBasic{
    public class CDG_INST extends Graph_INST{
        HashMap<PcodeOp, HashSet<PcodeOp>> edges_tr;
        HashMap<PcodeOp, HashSet<PcodeOp>> edges_fls;
        protected GDirectedGraph<PcodeBlockVtx, GEdge<PcodeBlockVtx>> rcfg;
        //        protected HashMap<PcodeOp, HashSet<PcodeOp>> ctrlDep;
        protected GDirectedGraph<PcodeBlockVtx, GEdge<PcodeBlockVtx>> postDominanceTree;
        Map<PcodeBlockBasic, PcodeBlockVtx> instanceMap;
        private ProbCdgDisplay displayer;

        private SelectivityAnalysisSimple analysis;
        HashMap<PcodeOp, Pair<Double, Double>> selectivity;

        public HashMap<PcodeOp, HashSet<PcodeOp>> getEdges_tr() {
            return edges_tr;
        }

        public HashMap<PcodeOp, HashSet<PcodeOp>> getEdges_fls() {
            return edges_fls;
        }

        public HashMap<PcodeOp, Pair<Double, Double>> getSelectivity() {
            return selectivity;
        }

        public CDG_INST(HighFunction highFunction) throws CancelledException {
            super();
            func = highFunction.getFunction();
            hfunc = highFunction;

            edges_fls = new HashMap<>();
            edges_tr = new HashMap<>();
            rcfg = GraphFactory.createDirectedGraph();
            analysis = new SelectivityAnalysisSimple();
            selectivity = analysis.solver_external(highFunction);

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
        protected void buildRevCFG(HighFunction hfunc) {
            instanceMap = new HashMap<PcodeBlockBasic, PcodeBlockVtx>();

            ArrayList<PcodeBlockBasic> blocks = hfunc.getBasicBlocks();
            PcodeBlockVtx exit_node = new PcodeBlockVtx("exit");
            rcfg.addVertex(exit_node);

//        printf("build rcfg: block num: %d\n", blocks.size());

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
                if(block.getOutSize()==0){
                    rcfg.addEdge(new DefaultGEdge(exit_node, fromVtx));
                }
            }

//        printf("build rcfg: block num: %d\n", rcfg.getVertices().size());
//        printf("build rcfg: edge num: %d\n", rcfg.getEdges().size());
            assert rcfg.getVertices().size()==blocks.size()+1;
        }
        public void addControlDepFromNodeToBB(PcodeOp srcOp, PcodeBlockBasic block, boolean isTrue) {
            if(isTrue){
                Iterator<PcodeOp> ins_iter = block.getIterator();
                if (!edges_tr.containsKey(srcOp)) {
                    edges_tr.put(srcOp, new HashSet<PcodeOp>());
                }
                HashSet<PcodeOp> control = edges_tr.get(srcOp);
                while (ins_iter.hasNext()) {
                    PcodeOp p = ins_iter.next();
//            AttributedEdge edge = createEdge(srcOp, p);
                    control.add(p);
                }
            } else {
                Iterator<PcodeOp> ins_iter = block.getIterator();
                if (!edges_fls.containsKey(srcOp)) {
                    edges_fls.put(srcOp, new HashSet<PcodeOp>());
                }
                HashSet<PcodeOp> control = edges_fls.get(srcOp);
                while (ins_iter.hasNext()) {
                    PcodeOp p = ins_iter.next();
//            AttributedEdge edge = createEdge(srcOp, p);
                    control.add(p);
                }
            }

        }
        public void addControlDepFromDominatedBlockToDominator(PcodeOp srcOp, PcodeBlockVtx srcBB, PcodeBlockVtx desBB,
                                                               GDirectedGraph<PcodeBlockVtx, GEdge<PcodeBlockVtx>> postDominanceTree, boolean isTrue) {
            Collection<PcodeBlockVtx> pdOfSrc = postDominanceTree.getPredecessors(srcBB);
            Collection<PcodeBlockVtx> dominatedBlock = new HashSet<PcodeBlockVtx>();
            dominatedBlock.add(desBB);
            PcodeBlockVtx nearestCommonDominator;
            // walk up along the Post Dominance Tree, start from desBB
            while (true) {
                Collection<PcodeBlockVtx> newDominatedBlock = new HashSet<PcodeBlockVtx>();
                for (PcodeBlockVtx pd : dominatedBlock) {
                    newDominatedBlock.addAll(postDominanceTree.getPredecessors(pd));
                    addControlDepFromNodeToBB(srcOp, pd.getCodeBlock(), isTrue);
                }
                nearestCommonDominator = this.containsAny(pdOfSrc, newDominatedBlock);
                if (nearestCommonDominator != null) {
                    break;
                }
                dominatedBlock = newDominatedBlock;
            }
            if (nearestCommonDominator.equals(srcBB)) {
                addControlDepFromNodeToBB(srcOp, srcBB.getCodeBlock(), isTrue);
            }
        }
        protected void getCtrlDep()throws CancelledException {
            for (GEdge edge : rcfg.getEdges()) {
                PcodeBlockVtx desBB = (PcodeBlockVtx)edge.getStart();
                PcodeBlockVtx srcBB = (PcodeBlockVtx)edge.getEnd();
                if (!GraphAlgorithms.findDominance(rcfg, desBB, GlobalState.monitor).contains(srcBB)) {
                    Iterator<PcodeOp> iter = srcBB.getCodeBlock().getIterator();
                    PcodeOp srcOp =  getLastElement(iter);
                    if(srcOp.getOpcode() != PcodeOp.CBRANCH){
                        GlobalState.ghidraScript.print("ctrl dep start with unknown pcodeop:     "+srcOp.getMnemonic()+"\n");
                    }
                    if(srcBB.pcodeBlock.getFalseOut()==desBB.pcodeBlock){
                        addControlDepFromDominatedBlockToDominator(srcOp, srcBB, desBB, postDominanceTree, false);
                    }else {
                        addControlDepFromDominatedBlockToDominator(srcOp, srcBB, desBB, postDominanceTree, true);
                    }

                }
            }
        }
        public void buildGraph(HighFunction hfunc) throws CancelledException {
            buildRevCFG(hfunc);
            postDominanceTree = GraphAlgorithms.findDominanceTree(rcfg, GlobalState.monitor);
            getCtrlDep();
            Iterator<PcodeOpAST> opiter = hfunc.getPcodeOps();
            while (opiter.hasNext()) {
                PcodeOp op = (PcodeOp) opiter.next();
                nodes.add(op);
            }

        }
        public void display() throws Exception {
            displayer = new ProbCdgDisplay(func, hfunc);
            displayer.display();
        }


        class ProbCdgDisplay extends GraphDisplay {
            public ProbCdgDisplay(Function func, HighFunction hfunc){
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
                for (PcodeOp from: edges_tr.keySet()){
                    for(PcodeOp to: edges_tr.get(from)){
                        AttributedEdge edge = createEdge(from, to);
                        double prob = selectivity.get(from).first;
                        if (prob==0.99d) {
                            edge.setAttribute(COLOR_ATTRIBUTE, "Green");
                        }  else if (prob==0.75d) {
                            edge.setAttribute(COLOR_ATTRIBUTE, "Purple");
                        }  else if(prob==0.5d){
                            edge.setAttribute(COLOR_ATTRIBUTE, "Blue");
                        } else if(prob==0.25d){
                            edge.setAttribute(COLOR_ATTRIBUTE, "Orange");
                        }
                        else if (prob==0.01d) {
                            edge.setAttribute(COLOR_ATTRIBUTE, "Red");
                        } else {
                            edge.setAttribute(COLOR_ATTRIBUTE, "Grey");
                        }
                    }
                }
                for (PcodeOp from: edges_fls.keySet()){
                    for(PcodeOp to: edges_fls.get(from)){
                        AttributedEdge edge = createEdge(from, to);
                        double prob = selectivity.get(from).second;
                        if (prob==0.99d) {
                            edge.setAttribute(COLOR_ATTRIBUTE, "Green");
                        }  else if (prob==0.75d) {
                            edge.setAttribute(COLOR_ATTRIBUTE, "Purple");
                        }  else if(prob==0.5d){
                            edge.setAttribute(COLOR_ATTRIBUTE, "Blue");
                        } else if(prob==0.25d){
                            edge.setAttribute(COLOR_ATTRIBUTE, "Orange");
                        }
                        else if (prob==0.01d) {
                            edge.setAttribute(COLOR_ATTRIBUTE, "Red");
                        } else {
                            edge.setAttribute(COLOR_ATTRIBUTE, "Grey");
                        }
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
