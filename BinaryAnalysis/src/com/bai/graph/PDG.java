package com.bai.graph;

import ghidra.program.model.listing.Function;
import ghidra.program.model.pcode.HighFunction;
import ghidra.program.model.pcode.PcodeOp;
import ghidra.program.model.pcode.PcodeOpAST;
import ghidra.program.model.pcode.VarnodeAST;
import ghidra.service.graph.AttributedEdge;
import ghidra.service.graph.AttributedVertex;
import ghidra.util.exception.CancelledException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class PDG extends GraphBasic{
    public class PDG_OP extends Graph_OP{
        CDG.CDG_INST cdg;
        DDG.DDG_OP ddg;
        public PDG_OP(HighFunction highFunction) throws CancelledException {
            super();
            func = highFunction.getFunction();
            hfunc = highFunction;
            cdg = new CDG().new CDG_INST(highFunction);
            ddg = new DDG().new DDG_OP(highFunction);
            buildGraph();
        }
        public void buildGraph(){
//            opnodes = cdg.getNodes();
//            opnodes.addAll(ddg.getOpnodes());
            opnodes = ddg.getOpnodes();
            varnodes = ddg.getVarnodes();

            edges_o2o = cdg.getEdges();
            edges_o2v = ddg.getEdges_o2v();
            edges_v2o = ddg.getEdges_v2o();
        }

        // dump into json
        public JSONObject dump(){
            GraphJson json = new GraphJson(hfunc);
            return json.dumpGraph_OP(opnodes, varnodes, edges_o2v, edges_v2o, edges_o2o);
        }

        // display pdg in ghidra
        private PdgDisplay displayer;

        public void display() throws Exception{
            displayer = new PdgDisplay(func, hfunc);
            displayer.display();
        }
        class PdgDisplay extends GraphDisplay {
            public PdgDisplay(Function func, HighFunction hfunc){
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
                for (PcodeOp from: edges_o2o.keySet()){
                    for(PcodeOp to: edges_o2o.get(from)){
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

    public class PDG_INST extends Graph_INST{

        CDG.CDG_INST cdg;
        DDG.DDG_INST ddg;
        HashMap<PcodeOp, HashSet<PcodeOp>> ddg_edges;
        HashMap<PcodeOp, HashSet<PcodeOp>> cdg_edges;

        public PDG_INST(HighFunction highFunction) throws CancelledException {
            super();
            func = highFunction.getFunction();
            hfunc = highFunction;
            cdg = new CDG().new CDG_INST(highFunction);
            ddg = new DDG().new DDG_INST(highFunction);
            buildGraph();
        }
        public void buildGraph(){
            nodes = ddg.getNodes();
            nodes.addAll(cdg.getNodes());
            ddg_edges = ddg.getEdges();
            cdg_edges = cdg.getEdges();
        }

        // dump into json
        public JSONObject dump(){
            GraphJson json = new GraphJson(hfunc);
            return json.dumpGraph_INST(nodes, ddg_edges, cdg_edges);
        }
//        public JSONObject dump(){
//            PDGJson pdg_json = new PDGJson(hfunc);
//            return pdg_json.dumpGraph();
//        }
//        class PDGJson extends GraphJson{
//            JSONObject ddg_js;
//            JSONObject cdg_js;
//            public PDGJson(HighFunction hfunc){
//                super(hfunc);
//                ddg_js = ddg.dump(1);
//                cdg_js = cdg.dump(2);
//            }
//            public JSONObject dumpGraph(){
//                nodes_dump = (ArrayList<Integer> )ddg_js.get("nodes");
//                nodesVerb = (HashMap<String, String>)ddg_js.get("nverbs");
//                edges_dump = (ArrayList<Integer[]>)ddg_js.get("edges");
//                ArrayList<Integer[]> edges_dump_cdg = (ArrayList<Integer[]>)cdg_js.get("edges");
//                edges_dump.addAll(edges_dump_cdg);
//                funcOut.put("nodes", nodes_dump);
//                funcOut.put("nverbs", nodesVerb);
//                funcOut.put("edges", edges_dump);
//                return funcOut;
//            }
//        }

        // display pdg in ghidra
        private PDGDisplay displayer;
        public void display() throws Exception{
            displayer = new PDGDisplay(func, hfunc);
            displayer.display();
        }
        class PDGDisplay extends GraphDisplay {
            public PDGDisplay(Function func, HighFunction hfunc){
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
                HashMap<Integer, AttributedVertex> vertices = new HashMap<>();
                for (PcodeOp op: nodes){
                    AttributedVertex o = createOpVertex((PcodeOpAST) op);
                }
                for (PcodeOp from: ddg_edges.keySet()){
                    for(PcodeOp to: ddg_edges.get(from)){
                        AttributedEdge edge = createEdge(from, to);
                    }
                }
                for (PcodeOp from: cdg_edges.keySet()){
                    for(PcodeOp to: cdg_edges.get(from)){
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
}
