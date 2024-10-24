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

import java.util.HashMap;
import java.util.HashSet;

public class CDFG extends GraphBasic{
    public class CDFG_OP extends Graph_OP {
        CFG.CFG_INST cfg;
        DDG.DDG_OP ddg;
        public CDFG_OP(HighFunction highFunction) throws CancelledException {
            super();
            func = highFunction.getFunction();
            hfunc = highFunction;
            cfg = new CFG().new CFG_INST(highFunction);
            ddg = new DDG().new DDG_OP(highFunction);
            buildGraph();
        }
        public void buildGraph(){
//            opnodes = cdg.getNodes();
//            opnodes.addAll(ddg.getOpnodes());
            opnodes = ddg.getOpnodes();
            varnodes = ddg.getVarnodes();

            edges_o2o = cfg.getEdges();
            edges_o2v = ddg.getEdges_o2v();
            edges_v2o = ddg.getEdges_v2o();
        }

        // dump into json
        public JSONObject dump(){
            GraphJson json = new GraphJson(hfunc);
            return json.dumpGraph_OP(opnodes, varnodes, edges_o2v, edges_v2o, edges_o2o);
        }

        // display pdg in ghidra
        private CFGDisplay displayer;

        public void display() throws Exception{
            displayer = new CFGDisplay(func, hfunc);
            displayer.display();
        }
        class CFGDisplay extends GraphDisplay {
            public CFGDisplay(Function func, HighFunction hfunc){
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
                displayGraph();
            }
        }
    }
    public class CDFG_INST extends Graph_INST {
        CFG.CFG_INST cfg;
        DDG.DDG_INST ddg;
        HashMap<PcodeOp, HashSet<PcodeOp>> ddg_edges;
        HashMap<PcodeOp, HashSet<PcodeOp>> cfg_edges;

        public CDFG_INST(HighFunction highFunction) throws CancelledException {
            super();
            func = highFunction.getFunction();
            hfunc = highFunction;
            cfg = new CFG().new CFG_INST(highFunction);
            ddg = new DDG().new DDG_INST(highFunction);
            buildGraph();
        }
        public void buildGraph(){
            nodes = ddg.getNodes();
            nodes.addAll(cfg.getNodes());
            ddg_edges = ddg.getEdges();
            cfg_edges = cfg.getEdges();
        }

        // dump into json
        public JSONObject dump(){
            GraphJson json = new GraphJson(hfunc);
            return json.dumpGraph_INST(nodes, ddg_edges, cfg_edges);
        }

        // display cdfg in ghidra
        private CFGDisplay displayer;
        public void display() throws Exception{
            displayer = new CFGDisplay(func, hfunc);
            displayer.display();
        }
        class CFGDisplay extends GraphDisplay {
            public CFGDisplay(Function func, HighFunction hfunc){
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
                for (PcodeOp from: cfg_edges.keySet()){
                    for(PcodeOp to: cfg_edges.get(from)){
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
                displayGraph();
            }
        }
    }
}
