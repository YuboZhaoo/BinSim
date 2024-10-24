package com.bai.graph;

import generic.stl.Pair;
import ghidra.program.model.lang.Register;
import ghidra.program.model.listing.Function;
import ghidra.program.model.pcode.*;
import ghidra.service.graph.AttributedEdge;
import ghidra.service.graph.AttributedVertex;
import ghidra.util.exception.CancelledException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;


// TODO not implemented
public class PPDG extends GraphBasic{
    public class PPDG_OP extends Graph_OP{
        private PpdgDisplay displayer;
        ProbCDG.CDG_INST cdg;
        DDG.DDG_OP ddg;
        HashMap<PcodeOp, Pair<Double, Double>> selectivity;
        HashMap<PcodeOp, HashSet<PcodeOp>> edges_o2o_tr;
        HashMap<PcodeOp, HashSet<PcodeOp>> edges_o2o_fls;
        public PPDG_OP(HighFunction highFunction) throws CancelledException {
            super();
            func = highFunction.getFunction();
            hfunc = highFunction;
            cdg = new ProbCDG().new CDG_INST(highFunction);
            ddg = new DDG().new DDG_OP(highFunction);
            selectivity = cdg.getSelectivity();
            buildGraph();
        }
        public void buildGraph(){
//            opnodes = cdg.getNodes();
//            opnodes.addAll(ddg.getOpnodes());
            opnodes = ddg.getOpnodes();
            varnodes = ddg.getVarnodes();

            edges_o2v = ddg.getEdges_o2v();
            edges_v2o = ddg.getEdges_v2o();

            edges_o2o_tr = cdg.getEdges_tr();
            edges_o2o_fls = cdg.getEdges_fls();
        }

        protected String getOpKey(PcodeOpAST op) { // get the id of the pcode inst
            SequenceNumber sq = op.getSeqnum();
            String id =
                    sq.getTarget().toString(true) + " o " + Integer.toString(op.getSeqnum().getTime());
            return id;
        }

        // output to json
        protected String var2String(VarnodeAST vn){
            String name = vn.toString();;
            if (vn.isRegister()) {
                if (vn.isInput()) {
                    Register reg = func.getProgram().getRegister(vn.getAddress(), vn.getSize());
                    if (reg != null) {
                        name = "arg_" + reg.getName();
                    }else {
                        name =  "arg_" + name;
                    }
                }
            }
            return "gg";
        }
        protected JSONObject dumpGraph() throws Exception {
            JSONObject funcOut = new JSONObject();

            HashMap<PcodeOp, Integer> op2index = new HashMap<>();
            HashMap<VarnodeAST, Integer> var2index = new HashMap<>();

            ArrayList<Integer> nodes = new ArrayList<Integer>();
            ArrayList<Integer[]> ddgedges = new ArrayList<Integer[]>();
            ArrayList<Integer[]> cdgedges = new ArrayList<Integer[]>();
            JSONObject nodesVerb = new JSONObject();
            int i = 0;
            for (PcodeOp node: opnodes){
                op2index.put(node, i);
                nodes.add(i);
                nodesVerb.put(String.format("%d", i), node.getMnemonic());
                i++;
            }
            for (VarnodeAST node: varnodes){
                var2index.put(node, i);
                nodes.add(i);
                nodesVerb.put(String.format("%d", i), node.toString());
                i++;
            }

            return funcOut;
        }
//        protected JSONObject dumpGraph_ex() throws Exception {
//            JSONObject funcOut = new JSONObject();
//            HashMap<String, Integer> id_map = new HashMap<>();
//            ArrayList<Integer> nodes = new ArrayList<Integer>();
//            ArrayList<Integer[]> ddgedges = new ArrayList<Integer[]>();
//            ArrayList<Integer[]> cdgedges = new ArrayList<Integer[]>();
//            JSONObject nodesVerb = new JSONObject();
//            int i = 0;
//            for (var node: graph.vertexSet()){
////            printf("\nnode id %s\n",node.getId());
////            printf("node name %s\n",node.getName());
//                id_map.put(node.getId(), i);
//                nodes.add(i);
//                nodesVerb.put(String.format("%d", i), node.getName());
//                i++;
//            }
//            for (AttributedEdge edge: graph.edgeSet()){
////            printf("\nedge src %s\n",graph.getEdgeSource(edge));
////            printf("edge target %s\n",graph.getEdgeTarget(edge));
//                var attr = edge.getAttribute(COLOR_ATTRIBUTE);
//                if(attr == null){
////                printf("null %d %d 0\n",id_map.get(graph.getEdgeSource(edge).getId()), id_map.get(graph.getEdgeTarget(edge).getId()));
//                    ddgedges.add(new Integer[] { id_map.get(graph.getEdgeSource(edge).getId()), id_map.get(graph.getEdgeTarget(edge).getId()), 1}); // 0 in past
//                }else if (attr.equals("Red")){
////                printf("red %d %d 1\n",id_map.get(graph.getEdgeSource(edge).getId()), id_map.get(graph.getEdgeTarget(edge).getId()));
//                    cdgedges.add(new Integer[] { id_map.get(graph.getEdgeSource(edge).getId()), id_map.get(graph.getEdgeTarget(edge).getId()), 2});
//                }
//            }
////        funcOut.put("fname", func.getName());
////        funcOut.put("faddr_ghidra", func.getEntryPoint());
//            funcOut.put("nodes", nodes);
//            funcOut.put("nverbs", nodesVerb);
//            funcOut.put("ddgedges", ddgedges);
//            funcOut.put("cdgedges", cdgedges);
//
////        printf(funcOut.toString());
//            return funcOut;
//            // idb_path, addr in txt, fun name
//
//        }

        // display pdg in ghidra
        public void display() throws Exception{
            displayer = new PpdgDisplay(func, hfunc);
            displayer.display();
        }
        class PpdgDisplay extends GraphDisplay {
            public PpdgDisplay(Function func, HighFunction hfunc){
                super(func, hfunc);
            }
//            @Override
//            protected AttributedVertex createOpVertex(PcodeOpAST op) { // create node of pcode operation
//                String name = op.toString();
//                String id = getOpKey(op);
//                AttributedVertex vert = graph.addVertex(id, name);
//                vert.setAttribute(ICON_ATTRIBUTE, "Square");
//                return vert;
//            }
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
                        AttributedEdge edge = createEdge(from, to, vertices);
                    }
                }
                for (VarnodeAST from: edges_v2o.keySet()){
                    for(PcodeOp to: edges_v2o.get(from)){
                        AttributedEdge edge = createEdge(from, to, vertices);
                    }
                }
                for (PcodeOp from: edges_o2o_tr.keySet()){
                    for(PcodeOp to: edges_o2o_tr.get(from)){
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
                for (PcodeOp from: edges_o2o_fls.keySet()){
                    for(PcodeOp to: edges_o2o_fls.get(from)){
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
}
