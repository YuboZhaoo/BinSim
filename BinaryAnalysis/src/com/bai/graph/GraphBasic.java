package com.bai.graph;

import ghidra.program.model.listing.Function;
import ghidra.program.model.pcode.*;

import java.util.HashMap;
import java.util.HashSet;

public class GraphBasic {
    Function func;
    HighFunction hfunc;

    public class Graph_OP{
        HashSet<VarnodeAST> varnodes;
        HashSet<PcodeOp> opnodes;
        HashMap<PcodeOp, HashSet<VarnodeAST>> edges_o2v;
        HashMap<VarnodeAST, HashSet<PcodeOp>> edges_v2o;
        HashMap<PcodeOp, HashSet<PcodeOp>> edges_o2o;

        public HashSet<VarnodeAST> getVarnodes() {return varnodes;}
        public HashSet<PcodeOp> getOpnodes() {return opnodes;}
        public HashMap<VarnodeAST, HashSet<PcodeOp>> getEdges_v2o() {return edges_v2o;}
        public HashMap<PcodeOp, HashSet<VarnodeAST>> getEdges_o2v() {return edges_o2v;}
        public HashMap<PcodeOp, HashSet<PcodeOp>> getEdges_o2o() {return edges_o2o;}

        public Graph_OP(){
            varnodes = new HashSet<>();
            opnodes = new HashSet<>();
            edges_o2v = new HashMap<>();
            edges_v2o = new HashMap<>();
            edges_o2o = new HashMap<>();
        }
        public void addEdge_o2v(PcodeOp from, VarnodeAST to){
            HashSet<VarnodeAST> target = edges_o2v.getOrDefault(from, new HashSet<>());
            target.add(to);
            edges_o2v.put(from, target);
        }
        public void addEdge_v2o(VarnodeAST from, PcodeOp to){
            HashSet<PcodeOp> target = edges_v2o.getOrDefault(from, new HashSet<>());
            target.add(to);
            edges_v2o.put(from, target);
        }

        // not used in bipartite graph such as ddg_op
        public void addEdge_o2o(PcodeOp from, PcodeOp to){
            HashSet<PcodeOp> target = edges_o2o.getOrDefault(from, new HashSet<>());
            target.add(to);
            edges_o2o.put(from, target);
        }

    }

    public class Graph_INST{
        HashSet<PcodeOp> nodes;
        HashMap<PcodeOp, HashSet<PcodeOp>> edges;
        public Graph_INST(){
            nodes = new HashSet<>();
            edges = new HashMap<>();
        }
        public void addEdge(PcodeOp from, PcodeOp to){
            HashSet<PcodeOp> target = edges.getOrDefault(from, new HashSet<>());
            target.add(to);
            edges.put(from, target);
        }
        public HashSet<PcodeOp> getNodes() { return nodes; }
        public HashMap<PcodeOp, HashSet<PcodeOp>> getEdges() { return edges; }
    }

    public class Graph_BB{
        HashSet<PcodeBlockBasic> nodes;
        HashMap<PcodeBlockBasic, HashSet<PcodeBlockBasic>> edges;
        public Graph_BB(){
            nodes = new HashSet<>();
            edges = new HashMap<>();
        }
        public void addEdge(PcodeBlockBasic from, PcodeBlockBasic to){
            HashSet<PcodeBlockBasic> target = edges.getOrDefault(from, new HashSet<>());
            target.add(to);
            edges.put(from, target);
        }
    }

}
