package com.bai.graph;

import com.bai.util.GlobalState;
import generic.stl.Pair;
import ghidra.graph.DefaultGEdge;
import ghidra.program.model.listing.Function;
import ghidra.program.model.pcode.HighFunction;
import ghidra.program.model.pcode.PcodeBlockBasic;
import ghidra.program.model.pcode.PcodeOp;
import ghidra.program.model.pcode.PcodeOpAST;
import ghidra.service.graph.AttributedEdge;
import ghidra.service.graph.AttributedGraph;
import ghidra.service.graph.AttributedVertex;
import ghidra.util.graph.*;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class CFG extends GraphBasic{

    // CFG in inst level
    public class CFG_INST extends Graph_INST{
        private CfgDisplay displayer;

        public CFG_INST(HighFunction highFunction){
            super();
            func = highFunction.getFunction();
            hfunc = highFunction;
            buildGraph(hfunc);
        }

        public void buildGraph(HighFunction hfunc){
            ArrayList<PcodeBlockBasic> bbs = hfunc.getBasicBlocks();
            // bb can be empty
            for (PcodeBlockBasic bb: bbs){
                Iterator<PcodeOp> instIter = bb.getIterator();
                PcodeOp prev = null;
                while (instIter.hasNext()) {
                    PcodeOp inst = instIter.next();
                    nodes.add(inst);
                    if(prev != null){
                        addEdge(prev, inst);
                    }
                    prev = inst;
                }
                for (int i = 0; i < bb.getOutSize(); i++) {
                    PcodeBlockBasic bb_succ =  (PcodeBlockBasic) bb.getOut(i);
                    Iterator<PcodeOp> iter_succ = bb_succ.getIterator();
                    // bb can be empty
                    if(prev == null){break;}
                    if(iter_succ.hasNext()){
                        addEdge(prev, iter_succ.next());
                    }
                }
//                for(var edge:edges.keySet()){
//                    GlobalState.ghidraScript.print(edge.toString()+"\n");
//                }
            }
        }



        // dump to json
        public JSONObject dump(){
            GraphJson json = new GraphJson(hfunc);
            return json.dumpGraph_INST(nodes, edges);
        }
//        public JSONObject dump(){
//            CFG_INST.CFGJson cfg_json = new CFG_INST.CFGJson(hfunc);
//            return cfg_json.dumpGraph();
//        }
//        public JSONObject dump(int edge_type){
//            CFG_INST.CFGJson cfg_json = new CFG_INST.CFGJson(hfunc, edge_type);
//            return cfg_json.dumpGraph();
//        }
//        class CFGJson extends GraphJson{
//            public CFGJson(HighFunction hfunc){super(hfunc);}
//            public CFGJson(HighFunction hfunc, int edge_type){super(hfunc, edge_type);}
//            public JSONObject dumpGraph(){
//                return dumpGraph_INST(nodes, edges);
//            }
////            public JSONObject dumpGraph(){
////                JSONObject funcOut = new JSONObject();
////                ArrayList<Integer> nodes_dump = new ArrayList<Integer>();
////                JSONObject nodesVerb = new JSONObject();
////                ArrayList<Integer[]> edges_dump = new ArrayList<Integer[]>();
////
////                HashMap<PcodeOp, Integer> index = new HashMap<>();
////                if(ifOrdered){
////                    int i = 0;
////                    Iterator<PcodeOpAST> opiter = hfunc.getPcodeOps();
////                    while (opiter.hasNext()) {
////                        PcodeOpAST op = opiter.next();
////                        index.put(op, i);
////                        nodes_dump.add(i);
////                        nodesVerb.put(String.format("%d", i), op.toString());
////                        i++;
////                    }
////                    for(PcodeOp from: edges.keySet()){
////                        for(PcodeOp to: edges.get(from)){
////                            edges_dump.add(new Integer[] { index.get(from), index.get(to), 1});
////                        }
////                    }
////                }
////                else{ // original order in ddg, may be out-of-order because of hashset
////                    int i = 0;
////                    for (PcodeOp op: nodes){
////                        index.put(op, i);
////                        nodes_dump.add(i);
////                        nodesVerb.put(String.format("%d", i), op.toString());
////                        i++;
////                    }
////                    for(PcodeOp from: edges.keySet()){
////                        for(PcodeOp to: edges.get(from)){
////                            edges_dump.add(new Integer[] { index.get(from), index.get(to), 1});
////                        }
////                    }
////                }
////                funcOut.put("nodes", nodes_dump);
////                funcOut.put("nverbs", nodesVerb);
////                funcOut.put("edges", edges_dump);
////                return funcOut;
////            }
//        }

        // display cfg in ghidra
        public void display() throws Exception{
            displayer = new CfgDisplay(func, hfunc);
            displayer.display();
        }
        class CfgDisplay extends GraphDisplay {
            public CfgDisplay(Function func, HighFunction hfunc){
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
            public void buildAttrGraph(){
                for (PcodeOp op: nodes){
                    AttributedVertex o = displayer.createOpVertex( (PcodeOpAST) op);
                }
                for (PcodeOp from: edges.keySet()){
                    for(PcodeOp to: edges.get(from)){
                        AttributedEdge edge = createEdge(from, to);
                        //TODO true or false by trueout
                        if(from.getParent() == to.getParent()){
                            edge.setAttribute(COLOR_ATTRIBUTE, "Black");
                        }else{
                            edge.setAttribute(COLOR_ATTRIBUTE, "Red");
                        }
                    }
                }
            }
            public void display() throws Exception {
                buildAttrGraph();
//                for(var edge: graph.edgeSet()){
//                    GlobalState.ghidraScript.print(edge.toString()+"\n");
//                }
                displayGraph();
            }
        }

    }

    // TODO dump graph
    public class CFG_BB extends Graph_BB{
        public CFG_BB(HighFunction highFunction){
            super();
            func = highFunction.getFunction();
            hfunc = highFunction;
            buildGraph(hfunc);
        }
        public void buildGraph(HighFunction hfunc){
            ArrayList<PcodeBlockBasic> bbs = hfunc.getBasicBlocks();
            // bb can be empty
            for (PcodeBlockBasic bb: bbs){
                Iterator<PcodeOp> instIter = bb.getIterator();
                nodes.add(bb);
                for (int i = 0; i < bb.getOutSize(); i++) {
                    PcodeBlockBasic bb_succ =  (PcodeBlockBasic) bb.getOut(i);
                    if(bb_succ!=null){
                        addEdge(bb, bb_succ);
                    }
                }
            }
        }
        // display cfg in ghidra
        private CFGDisplay displayer;
        public void display() throws Exception{
            displayer = new CFGDisplay(func, hfunc);
            displayer.display();
        }
        class CFGDisplay extends GraphDisplay {
            public CFGDisplay(Function func, HighFunction hfunc){
                super(func, hfunc);
            }
            protected AttributedVertex createOpVertex(PcodeBlockBasic bb) { // create node of pcode operation
//                StringBuilder name = new StringBuilder(bb.toString());
                StringBuilder name = new StringBuilder("bb@"+bb.getStart().toString());
//                Iterator<PcodeOp> instIter = bb.getIterator();
//                while (instIter.hasNext()) {
//                    PcodeOp inst = instIter.next();
//                    name.append(inst.toString()+" \n ");
//                }
                String id = getBBKey(bb);
                AttributedVertex vert = graph.addVertex(id, name.toString());
                vert.setAttribute(ICON_ATTRIBUTE, "Square");
                return vert;
            }
            protected AttributedVertex createExitVertex() { // create node of pcode operation
                String name = "exit";
                String id = "exit";
                AttributedVertex vert = graph.addVertex(id, name.toString());
                vert.setAttribute(ICON_ATTRIBUTE, "Circle");
                return vert;
            }
            public void buildAttrGraph(){
                for (PcodeBlockBasic op: nodes){
                    AttributedVertex o = displayer.createOpVertex(op);
                }
                AttributedVertex exit = createExitVertex();

                for (PcodeBlockBasic op: nodes){
                    AttributedVertex o = displayer.createOpVertex(op);
                    if(op.getOutSize()==0){
                        AttributedEdge exit_edge = createEdge(graph.getVertex(getBBKey(op)),exit);
                        exit_edge.setAttribute(COLOR_ATTRIBUTE, "Blue");
                    }
                    boolean self_loop = false;
                    for (int i = 0; i < op.getOutSize(); i++) {
                        PcodeBlockBasic to = (PcodeBlockBasic) op.getOut(i);
                        if(to == op) self_loop = true;
                    }
                    if(op.getOutSize()==1 && self_loop){ // give the dead loop an exit
                        AttributedEdge exit_edge = createEdge(graph.getVertex(getBBKey(op)),exit);
                        exit_edge.setAttribute(COLOR_ATTRIBUTE, "Blue");
                    }
                }

                for (PcodeBlockBasic from: edges.keySet()){
                    for(PcodeBlockBasic to: edges.get(from)){
                        AttributedEdge edge = createEdge(from, to);
                        if(from.getOutSize()>1){
                            if(to == from.getTrueOut()){
                                edge.setAttribute(COLOR_ATTRIBUTE, "Green");
                            } else if(to == from.getFalseOut()){
                                edge.setAttribute(COLOR_ATTRIBUTE, "Red");
                            } else {
                                edge.setAttribute(COLOR_ATTRIBUTE, "Purple");
                            }
                        } else {
                            edge.setAttribute(COLOR_ATTRIBUTE, "Black");
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

}
