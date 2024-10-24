package com.bai.graph;

import com.bai.util.GlobalState;
import generic.json.Json;
import ghidra.program.model.listing.Function;
import ghidra.program.model.pcode.*;
import ghidra.service.graph.AttributedEdge;
import ghidra.service.graph.AttributedVertex;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class DDG extends GraphBasic{
    public class DDG_OP extends Graph_OP{
        public DDG_OP(HighFunction highFunction){
            super();
            func = highFunction.getFunction();
            hfunc = highFunction;
            buildGraph();
        }

        protected void buildGraph() {
            Iterator<PcodeOpAST> opiter = hfunc.getPcodeOps();
            while (opiter.hasNext()) {
                PcodeOpAST op = opiter.next();
                opnodes.add(op);

                for (int i = 0; i < op.getNumInputs(); ++i) {
                    int opcode = op.getOpcode();
                    if ((i == 0) && ((opcode == PcodeOp.LOAD) || (opcode == PcodeOp.STORE))) {
                        continue;
                    }
                    if ((i == 1) && (opcode == PcodeOp.INDIRECT)) {
                        continue;
                    }
                    VarnodeAST vn = (VarnodeAST) op.getInput(i);

                    if (vn != null) {
                        varnodes.add(vn);
                        addEdge_v2o(vn, op);
                    }
                }
                VarnodeAST outvn = (VarnodeAST) op.getOutput();
                if (outvn != null) {
                    varnodes.add(outvn);
                    addEdge_o2v(op, outvn);
                }
            }
        }

        // dump to json
        public JSONObject dump(){
//            DDGJson ddg_json = new DDGJson(hfunc);
//            return ddg_json.dumpGraph();

            GraphJson json = new GraphJson(hfunc);
            return json.dumpGraph_OP(opnodes, varnodes, edges_o2v, edges_v2o);
        }
//        class DDGJson extends GraphJson{
//            public DDGJson(HighFunction hfunc){super(hfunc);}
//            public DDGJson(HighFunction hfunc, int edge_type){super(hfunc, edge_type);}
//            public JSONObject dumpGraph(){
//                return dumpGraph_OP(opnodes, varnodes, edges_o2v, edges_v2o);
//            }
////            @Override
////            protected JSONObject dumpGraph_OP(HashSet<PcodeOp> opnodes, HashSet<VarnodeAST> varnodes,
////                                              HashMap<PcodeOp, HashSet<VarnodeAST>> edges_o2v,
////                                              HashMap<VarnodeAST, HashSet<PcodeOp>> edges_v2o){
////                if(ifOrdered){
////                    ArrayList<Integer> nodes_seq = new ArrayList<Integer>(); // only for op_level graph, there will be duplicated nodes, such as 0,1,0
////                    HashMap<VarnodeAST, Integer> index_v = new HashMap<>();
////                    int id = 0;
////                    ArrayList<PcodeBlockBasic> bbs = hfunc.getBasicBlocks();
////                    for (PcodeBlockBasic bb: bbs){
////                        Iterator<PcodeOp> instIter = bb.getIterator();
////                        while (instIter.hasNext()) {
////                            PcodeOp op = instIter.next();
////                            // output varnode
////                            VarnodeAST outvn = (VarnodeAST) op.getOutput();
////                            if (outvn != null) {
////                                if(!index_v.containsKey(outvn)){
////                                    index_v.put(outvn, id);
////                                    nodes_seq.add(id);
////                                    nodesVerb.put(String.format("%d", id), outvn.toString());
////                                    id++;
////                                } else {
////                                    int id_old = index_v.get(outvn);
////                                    nodes_seq.add(id_old);
////                                }
////                            }
////                            // opcode
////                            index.put(op, id);
////                            nodes_seq.add(id);
////                            nodesVerb.put(String.format("%d", id),op.getMnemonic());
////                            id++;
////                            // input varnode
////                            for (int i = 0; i < op.getNumInputs(); ++i) {
////                                int opcode = op.getOpcode();
////                                if ((i == 0) && ((opcode == PcodeOp.LOAD) || (opcode == PcodeOp.STORE))) {
////                                    continue;
////                                }
////                                if ((i == 1) && (opcode == PcodeOp.INDIRECT)) {
////                                    continue;
////                                }
////                                VarnodeAST vn = (VarnodeAST) op.getInput(i);
////                                if (vn != null) {
////                                    if(!index_v.containsKey(vn)){
////                                        index_v.put(vn, id);
////                                        nodes_seq.add(id);
////                                        nodesVerb.put(String.format("%d", id), vn.toString());
////                                        id++;
////                                    } else {
////                                        int id_old = index_v.get(vn);
////                                        nodes_seq.add(id_old);
////                                    }
////                                }
////                            }
////                        }
////                    }
////                    for (int i = 0; i < id; i++) {
////                        nodes_dump.add(i);
////                    }
////                    for (PcodeOp from: edges_o2v.keySet()){
////                        for(VarnodeAST to: edges_o2v.get(from)){
////                            edges_dump.add(new Integer[] { index.get(from), index_v.get(to), edge_type});
////                        }
////                    }
////                    for (VarnodeAST from: edges_v2o.keySet()){
////                        for(PcodeOp to: edges_v2o.get(from)){
////                            edges_dump.add(new Integer[] { index_v.get(from), index.get(to), edge_type});
////                        }
////                    }
////                    funcOut.put("nodes_seq", nodes_seq);
////                }
////                else{ // original order in ddg graph, is out-of-order because of two node type and hashset
////                    HashMap<VarnodeAST, Integer> index_v = new HashMap<>();;
////                    int i = 0;
////                    for (PcodeOp op: opnodes){
////                        index.put(op, i);
////                        nodes_dump.add(i);
////                        nodesVerb.put(String.format("%d", i), op.toString());
////                        i++;
////                    }
////                    for (VarnodeAST vn: varnodes){
////                        index_v.put(vn, i);
////                        nodes_dump.add(i);
////                        nodesVerb.put(String.format("%d", i), vn.toString());
////                        i++;
////                    }
////                    for (PcodeOp from: edges_o2v.keySet()){
////                        for(VarnodeAST to: edges_o2v.get(from)){
////                            edges_dump.add(new Integer[] { index.get(from), index_v.get(to), edge_type});
////                        }
////                    }
////                    for (VarnodeAST from: edges_v2o.keySet()){
////                        for(PcodeOp to: edges_v2o.get(from)){
////                            edges_dump.add(new Integer[] { index_v.get(from), index.get(to), edge_type});
////                        }
////                    }
////                }
////
////
////                // output
////                funcOut.put("nodes", nodes_dump);
////                funcOut.put("nverbs", nodesVerb);
////                funcOut.put("edges", edges_dump);
////                return funcOut;
////            }
//
////            private void buildIndexGraph(){
//////                HashMap<Integer, PcodeOp> opIndex = new HashMap<>();
//////                HashMap<Integer, Object> vnIndex = new HashMap<>();
////
////                HashMap<PcodeOp, Integer> opIndex = new HashMap<>();
////                HashMap<VarnodeAST, Integer> vnIndex = new HashMap<>();
////                HashMap<Integer, String> nodeIndex = new HashMap<>();
////                HashMap<Integer, HashSet<Integer>> indexEdges = new HashMap<>();
////
////                Iterator<PcodeOpAST> opiter = hfunc.getPcodeOps();
////                int index = 0;
////                while (opiter.hasNext()) {
////                    PcodeOpAST op = opiter.next();
////                    VarnodeAST outvn = (VarnodeAST) op.getOutput();
////                    if (outvn != null) {
////                        vnIndex.getOrDefault(outvn, index);
//////                        opIndex.put(index++, op);
//////                        indexEdges.putIfAbsent()
////
////                        addEdge_o2v(op, outvn);
////                    }
//////                    opIndex.put(index, op);
////                    index++;
////
////                }
////            }
//        }

        // display in ghidra
        private DDGDisplay displayer;
        public void display() throws Exception{
            displayer = new DDGDisplay(func, hfunc);
            displayer.display();
        }
        class DDGDisplay extends GraphDisplay{
            public DDGDisplay(Function func, HighFunction hfunc){
                super(func, hfunc);
            }
            public void buildAttrGraph(){
                HashMap<Integer, AttributedVertex> vertices = new HashMap<>();
                for (PcodeOp op: opnodes){
                    AttributedVertex o = createOpVertex((PcodeOpAST) op);
                }
                for (VarnodeAST vn: varnodes){
                    AttributedVertex o = getVarnodeVertex(vertices, vn);
                }
                for (PcodeOp from: edges_o2v.keySet()){
                    for(VarnodeAST to: edges_o2v.get(from)){
                        createEdge(from, to, vertices);
                    }
                }
                for (VarnodeAST from: edges_v2o.keySet()){
                    for(PcodeOp to: edges_v2o.get(from)){
                        createEdge(from, to, vertices);
                    }
                }
            }
            public void display() throws Exception {
                buildAttrGraph();
//                for(var edge: graph.edgeSet()){
//                    GlobalState.ghidraScript.print(edge.toString()+"\n");
//                }
                displayGraph();
//                GlobalState.ghidraScript.printf("attr graph node num %d\n", graph.vertexSet().size());
//                GlobalState.ghidraScript.printf("opnodes node num %d\n",opnodes.size());
//                GlobalState.ghidraScript.printf("varnodes node num %d\n",varnodes.size());
//                for(VarnodeAST node: varnodes){
//                    GlobalState.ghidraScript.printf("%s\n", node.toString());
//                }
            }
        }
    }

    public class DDG_INST extends Graph_INST{
        public DDG_INST(HighFunction highFunction){
            super();
            func = highFunction.getFunction();
            hfunc = highFunction;
            buildGraph(hfunc);
        }
        public void buildGraph(HighFunction hfunc){
            Iterator<PcodeOpAST> opiter = hfunc.getPcodeOps();
            while (opiter.hasNext()) {
                PcodeOpAST op = opiter.next();
                nodes.add(op);
                for (int i = 0; i < op.getNumInputs(); ++i) {
                    int opcode = op.getOpcode();
                    if ((i == 0) && ((opcode == PcodeOp.LOAD) || (opcode == PcodeOp.STORE))) {
                        continue;
                    }
                    if ((i == 1) && (opcode == PcodeOp.INDIRECT)) {
                        continue;
                    }
                    VarnodeAST vn = (VarnodeAST) op.getInput(i);
                    if (vn != null) {
                        PcodeOp src = vn.getDef();
                        if(src!=null){
                            nodes.add(src);
                            addEdge(src, op);
                        }
                    }
                }
            }
        }

        // dump to json
        public JSONObject dump(){
            GraphJson json = new GraphJson(hfunc);
            return json.dumpGraph_INST(nodes, edges);
        }
//        public JSONObject dump(){
//            DDGJson ddg_json = new DDGJson(hfunc);
//            return ddg_json.dumpGraph();
//        }
//        public JSONObject dump(int edge_type){
//            DDGJson ddg_json = new DDGJson(hfunc, edge_type);
//            return ddg_json.dumpGraph();
//        }
//        class DDGJson extends GraphJson{
//            public DDGJson(HighFunction hfunc){super(hfunc);}
//            public DDGJson(HighFunction hfunc, int edge_type){super(hfunc, edge_type);}
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

        // display ddg in ghidra
        private DDGDisplay displayer;
        public void display() throws Exception{
            displayer = new DDGDisplay(func, hfunc);
            displayer.display();
        }
        class DDGDisplay extends GraphDisplay {
            public DDGDisplay(Function func, HighFunction hfunc){
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
                    AttributedVertex o = displayer.createOpVertex((PcodeOpAST) op);
                }
                for (PcodeOp from: edges.keySet()){
                    for(PcodeOp to: edges.get(from)){
                        createEdge(from, to);
                    }
                }
            }
            public void display() throws Exception {
                buildAttrGraph();
                displayGraph();
//                GlobalState.ghidraScript.printf("attr graph node num %d\n", graph.vertexSet().size());
//                for(PcodeOp node:nodes){
//                    GlobalState.ghidraScript.printf("%s\n", node.toString());
//                }
            }
        }

    }
}
