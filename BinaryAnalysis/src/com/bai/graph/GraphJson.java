package com.bai.graph;


import com.bai.util.GlobalState;
import ghidra.program.model.listing.Function;
import ghidra.program.model.pcode.*;
import ghidra.service.graph.AttributedGraph;
import ghidra.service.graph.AttributedVertex;
import org.json.JSONObject;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class GraphJson {

//    protected Function func;
    protected HighFunction hfunc;

    protected boolean ifOrdered; // some model such as RNN needs the sequence of tokens be ordered
    JSONObject funcOut;
    protected ArrayList<Integer> nodes_dump; // nodes

//    protected JSONObject nodesVerb;
    // TODO different to hermes, in hermes it is nodesVerb
    protected HashMap<String, String> nodesVerb; // nverbs
    ArrayList<Integer[]> edges_dump; // edges
    HashMap<PcodeOp, Integer> index;
    int edge_type;

    public GraphJson(HighFunction hfunc){
        this.ifOrdered = true;
        this.hfunc = hfunc;
        this.funcOut = new JSONObject();
        this.nodes_dump = new ArrayList<Integer>();
//        this.nodes_seq = new ArrayList<Integer>(); // only for op_level graph
//        this.nodesVerb = new JSONObject();
        this.nodesVerb = new HashMap<>();
        this.edges_dump = new ArrayList<Integer[]>();
        this.index = new HashMap<>();
        this.edge_type = 1;
    }
//    public GraphJson(HighFunction hfunc, int edge_type){
//        this.ifOrdered = true;
//        this.hfunc = hfunc;
//        this.funcOut = new JSONObject();
//        this.nodes_dump = new ArrayList<Integer>();
////        this.nodes_seq = new ArrayList<Integer>(); // only for op_level graph
////        this.nodesVerb = new JSONObject();
//        this.nodesVerb = new HashMap<>();
//        this.edges_dump = new ArrayList<Integer[]>();
//        this.index = new HashMap<>();
//        this.edge_type = edge_type;
//    }

    public ArrayList<Integer> getNodesDump() {
        return nodes_dump;
    }

    public ArrayList<Integer[]> getEdgesDump() {
        return edges_dump;
    }

    public HashMap<String, String> getNodesVerb() {
        return nodesVerb;
    }

//    protected JSONObject dumpGraph_OP(HashSet<PcodeOp> opnodes, HashSet<VarnodeAST> varnodes,
//                                      HashMap<PcodeOp, HashSet<VarnodeAST>> edges_o2v,
//                                      HashMap<VarnodeAST, HashSet<PcodeOp>> edges_v2o){
//        // original order in ddg graph, is out-of-order because of two node type and hashset
//        HashMap<VarnodeAST, Integer> index_v = new HashMap<>();;
//        int i = 0;
//        for (PcodeOp op: opnodes){
//            index.put(op, i);
//            nodes_dump.add(i);
//            nodesVerb.put(String.format("%d", i), op.toString());
//            i++;
//        }
//        for (VarnodeAST vn: varnodes){
//            index_v.put(vn, i);
//            nodes_dump.add(i);
//            nodesVerb.put(String.format("%d", i), vn.toString());
//            i++;
//        }
//        for (PcodeOp from: edges_o2v.keySet()){
//            for(VarnodeAST to: edges_o2v.get(from)){
//                edges_dump.add(new Integer[] { index.get(from), index_v.get(to), edge_type});
//            }
//        }
//        for (VarnodeAST from: edges_v2o.keySet()){
//            for(PcodeOp to: edges_v2o.get(from)){
//                edges_dump.add(new Integer[] { index_v.get(from), index.get(to), edge_type});
//            }
//        }
//        // output
//        funcOut.put("nodes", nodes_dump);
//        funcOut.put("nverbs", nodesVerb);
//        funcOut.put("edges", edges_dump);
//        return funcOut;
//    }

    // for ddg_op
    public JSONObject dumpGraph_OP(HashSet<PcodeOp> opnodes, HashSet<VarnodeAST> varnodes,
                                      HashMap<PcodeOp, HashSet<VarnodeAST>> edges_o2v,
                                      HashMap<VarnodeAST, HashSet<PcodeOp>> edges_v2o){
        HashMap<VarnodeAST, Integer> index_v = new HashMap<>();
        if(ifOrdered){
            ArrayList<Integer> nodes_seq = new ArrayList<Integer>(); // only for op_level graph, there will be duplicated nodes, such as 0,1,0
            int id = 0;
            ArrayList<PcodeBlockBasic> bbs = hfunc.getBasicBlocks();
            for (PcodeBlockBasic bb: bbs){
                Iterator<PcodeOp> instIter = bb.getIterator();
                while (instIter.hasNext()) {
                    PcodeOp op = instIter.next();
                    // output varnode
                    VarnodeAST outvn = (VarnodeAST) op.getOutput();
                    if (outvn != null) {
                        if(!index_v.containsKey(outvn)){
                            index_v.put(outvn, id);
                            nodes_seq.add(id);
                            nodesVerb.put(String.format("%d", id), outvn.toString());
                            id++;
                        } else {
                            int id_old = index_v.get(outvn);
                            nodes_seq.add(id_old);
                        }
                    }
                    // opcode
                    index.put(op, id);
                    nodes_seq.add(id);
                    nodesVerb.put(String.format("%d", id),op.getMnemonic());
                    id++;
                    // input varnode
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
                            if(!index_v.containsKey(vn)){
                                index_v.put(vn, id);
                                nodes_seq.add(id);
                                nodesVerb.put(String.format("%d", id), vn.toString());
                                id++;
                            } else {
                                int id_old = index_v.get(vn);
                                nodes_seq.add(id_old);
                            }
                        }
                    }
                }
            }
            for (int i = 0; i < id; i++) {
                nodes_dump.add(i);
            }

            funcOut.put("nodes_seq", nodes_seq);
        }
        else{ // original order in ddg graph, is out-of-order because of two node type and hashset
            int i = 0;
            for (PcodeOp op: opnodes){
                index.put(op, i);
                nodes_dump.add(i);
                nodesVerb.put(String.format("%d", i), op.toString());
                i++;
            }
            for (VarnodeAST vn: varnodes){
                index_v.put(vn, i);
                nodes_dump.add(i);
                nodesVerb.put(String.format("%d", i), vn.toString());
                i++;
            }
        }

        for (PcodeOp from: edges_o2v.keySet()){
            for(VarnodeAST to: edges_o2v.get(from)){
                edges_dump.add(new Integer[] { index.get(from), index_v.get(to), 1});
            }
        }
        for (VarnodeAST from: edges_v2o.keySet()){
            for(PcodeOp to: edges_v2o.get(from)){
                edges_dump.add(new Integer[] { index_v.get(from), index.get(to), 1});
            }
        }
        // output
        funcOut.put("nodes", nodes_dump);
        funcOut.put("nverbs", nodesVerb);
        funcOut.put("edges", edges_dump);
        return funcOut;
    }

    // for pdg_op or lddg_op
    public JSONObject dumpGraph_OP(HashSet<PcodeOp> opnodes, HashSet<VarnodeAST> varnodes,
                                      HashMap<PcodeOp, HashSet<VarnodeAST>> edges_o2v,
                                      HashMap<VarnodeAST, HashSet<PcodeOp>> edges_v2o,
                                      HashMap<PcodeOp, HashSet<PcodeOp>> edges_o2o){

        HashMap<VarnodeAST, Integer> index_v = new HashMap<>();;
        if(ifOrdered){
            ArrayList<Integer> nodes_seq = new ArrayList<Integer>(); // only for op_level graph, there will be duplicated nodes, such as 0,1,0
            int id = 0;
            ArrayList<PcodeBlockBasic> bbs = hfunc.getBasicBlocks();
            for (PcodeBlockBasic bb: bbs){
                Iterator<PcodeOp> instIter = bb.getIterator();
                while (instIter.hasNext()) {
                    PcodeOp op = instIter.next();
                    // output varnode
                    VarnodeAST outvn = (VarnodeAST) op.getOutput();
                    if (outvn != null) {
                        if(!index_v.containsKey(outvn)){
                            index_v.put(outvn, id);
                            nodes_seq.add(id);
                            nodesVerb.put(String.format("%d", id), outvn.toString());
                            id++;
                        } else {
                            int id_old = index_v.get(outvn);
                            nodes_seq.add(id_old);
                        }
                    }
                    // opcode
                    index.put(op, id);
                    nodes_seq.add(id);
                    nodesVerb.put(String.format("%d", id),op.getMnemonic());
                    id++;
                    // input varnode
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
                            if(!index_v.containsKey(vn)){
                                index_v.put(vn, id);
                                nodes_seq.add(id);
                                nodesVerb.put(String.format("%d", id), vn.toString());
                                id++;
                            } else {
                                int id_old = index_v.get(vn);
                                nodes_seq.add(id_old);
                            }
                        }
                    }
                }
            }
            for (int i = 0; i < id; i++) {
                nodes_dump.add(i);
            }
            funcOut.put("nodes_seq", nodes_seq);
        } else{ // original order in ddg graph, is out-of-order because of two node type and hashset
            int i = 0;
            for (PcodeOp op: opnodes){
                index.put(op, i);
                nodes_dump.add(i);
                nodesVerb.put(String.format("%d", i), op.toString());
                i++;
            }
            for (VarnodeAST vn: varnodes){
                index_v.put(vn, i);
                nodes_dump.add(i);
                nodesVerb.put(String.format("%d", i), vn.toString());
                i++;
            }
        }

        for (PcodeOp from: edges_o2v.keySet()){
            for(VarnodeAST to: edges_o2v.get(from)){
                edges_dump.add(new Integer[] { index.get(from), index_v.get(to), 1});
            }
        }
        for (VarnodeAST from: edges_v2o.keySet()){
            for(PcodeOp to: edges_v2o.get(from)){
                edges_dump.add(new Integer[] { index_v.get(from), index.get(to), 1});
            }
        }
        for (PcodeOp from: edges_o2o.keySet()){
            for(PcodeOp to: edges_o2o.get(from)){
                edges_dump.add(new Integer[] { index.get(from), index.get(to), 2});
            }
        }

        // output
        funcOut.put("nodes", nodes_dump);
        funcOut.put("nverbs", nodesVerb);
        funcOut.put("edges", edges_dump);
        return funcOut;
    }



    // only for inst graph with one edge type, such as ddg and cfg(one edge type)
    public JSONObject dumpGraph_INST(HashSet<PcodeOp>nodes, HashMap<PcodeOp, HashSet<PcodeOp>> edges){
        if(ifOrdered){
            int i = 0;
            ArrayList<PcodeBlockBasic> bbs = hfunc.getBasicBlocks();
            for (PcodeBlockBasic bb: bbs){
                Iterator<PcodeOp> instIter = bb.getIterator();
                while (instIter.hasNext()) {
                    PcodeOp inst = instIter.next();
                    index.put(inst, i);
                    nodes_dump.add(i);
                    nodesVerb.put(String.format("%d", i), inst.toString());
                    i++;
                }
            }

            // this will cause out of order
//            Iterator<PcodeOpAST> opiter = hfunc.getPcodeOps();
//            while (opiter.hasNext()) { // ordered in inst sequence, suitable for RNN models
//                PcodeOpAST op = opiter.next();
//                index.put(op, i);
//                nodes_dump.add(i);
//                nodesVerb.put(String.format("%d", i), op.toString());
//                i++;
//            }

            for(PcodeOp from: edges.keySet()){
                for(PcodeOp to: edges.get(from)){
                    edges_dump.add(new Integer[] { index.get(from), index.get(to), 1});
                }
            }
        }
        else{ // original order in ddg graph, may be out-of-order because of hashset
            int i = 0;
            for (PcodeOp op: nodes){
                index.put(op, i);
                nodes_dump.add(i);
                nodesVerb.put(String.format("%d", i), op.toString());
                i++;
            }
            for(PcodeOp from: edges.keySet()){
                for(PcodeOp to: edges.get(from)){
                    edges_dump.add(new Integer[] { index.get(from), index.get(to), 1});
                }
            }
        }
        funcOut.put("nodes", nodes_dump);
        funcOut.put("nverbs", nodesVerb);
        funcOut.put("edges", edges_dump);
//        for (int i = 0; i < nodesVerb.size(); i++) {
//            GlobalState.ghidraScript.printf("%s\n",nodesVerb.get(String.format("%d", i)));
//        }
        return funcOut;
    }

    // for pdg_inst
    public JSONObject dumpGraph_INST(HashSet<PcodeOp>nodes, HashMap<PcodeOp, HashSet<PcodeOp>> ddg_edges,
                                     HashMap<PcodeOp, HashSet<PcodeOp>> cdg_edges){
        // node
        if(ifOrdered){
            int i = 0;
            ArrayList<PcodeBlockBasic> bbs = hfunc.getBasicBlocks();
            for (PcodeBlockBasic bb: bbs){
                Iterator<PcodeOp> instIter = bb.getIterator();
                while (instIter.hasNext()) {
                    PcodeOp inst = instIter.next();
                    index.put(inst, i);
                    nodes_dump.add(i);
                    nodesVerb.put(String.format("%d", i), inst.toString());
                    i++;
                }
            }
        } else{ // original order in ddg graph, may be out-of-order because of hashset
            int i = 0;
            for (PcodeOp op: nodes){
                index.put(op, i);
                nodes_dump.add(i);
                nodesVerb.put(String.format("%d", i), op.toString());
                i++;
            }
        }
        // edge
        for(PcodeOp from: ddg_edges.keySet()){
            for(PcodeOp to: ddg_edges.get(from)){
                edges_dump.add(new Integer[] { index.get(from), index.get(to), 1});
            }
        }
        for(PcodeOp from: cdg_edges.keySet()){
            for(PcodeOp to: cdg_edges.get(from)){
                edges_dump.add(new Integer[] { index.get(from), index.get(to), 2});
            }
        }
        funcOut.put("nodes", nodes_dump);
        funcOut.put("nverbs", nodesVerb);
        funcOut.put("edges", edges_dump);
        return funcOut;
    }

    public JSONObject dumpGraph(){
        return null;
    }

}
/*
edge attr must be 1,2,3...
{ "IDBs/Dataset-1/curl/arm32-clang-3.5-O0_curl.i64":
            {"0x73dd4":
                    {"PDG":
                            {
                                "nodes": [0,1,2,3,4],
                                "nverbs": {"0":"(register, 0x65, 1)"},
                                "ddg_edges": [[0,1,1]],
                                "cdg_edges": [[0,1,2]]
                             }



{"IDBs/Dataset-1/curl/arm32-clang-3.5-O0_curl.i64":{
"0x73dd4":{
"LDDG":{
"nodes":[0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70],
"nverbs":{"44":"(register, 0x65, 1)","45":"CBRANCH","46":"(ram, 0x73e5c, 1)","47":"PTRADD","48":"(const, 0x1, 4)","49":"(const, 0x1, 4)","50":"PTRADD","51":"(const, 0x1, 4)","52":"(const, 0x1, 4)","53":"BRANCH","10":"(register, 0x20, 4)","54":"(ram, 0x73de8, 1)","11":"(stack, 0xfffffffffffffff4, 4)","55":"LOAD ram","12":"LOAD ram","56":"(unique, 0x78f0, 1)","13":"(unique, 0x78f0, 1)","57":"CALL","14":"INT_NOTEQUAL","58":"(ram, 0x73d44, 8)","15":"(const, 0x0, 1)","59":"(register, 0x20, 1)","16":"(register, 0x65, 1)","17":"COPY","18":"(const, 0x0, 1)","19":"(unique, 0x10000051, 1)","0":"COPY","1":"arg_r1","2":"(unique, 0x10000052, 4)","3":"COPY","4":"arg_r0","5":"(unique, 0x10000056, 4)","6":"MULTIEQUAL","7":"(register, 0x20, 4)","8":"(stack, 0xfffffffffffffff0, 4)","9":"MULTIEQUAL","60":"LOAD ram","61":"(unique, 0x78f0, 1)","62":"CALL","63":"(ram, 0x73d44, 8)","20":"CBRANCH","64":"(register, 0x20, 1)","21":"(ram, 0x73e18, 1)","65":"INT_EQUAL","22":"LOAD ram","66":"(register, 0x65, 1)","23":"(unique, 0x78f0, 1)","67":"COPY","24":"INT_NOTEQUAL","68":"(register, 0x20, 1)","25":"(const, 0x0, 1)","69":"RETURN","26":"(register, 0x65, 1)","27":"MULTIEQUAL","28":"(unique, 0x1000004e, 1)","29":"BOOL_NEGATE","70":"(const, 0x0, 4)","30":"(unique, 0x1000004f, 1)","31":"CBRANCH","32":"(ram, 0x73e78, 1)","33":"LOAD ram","34":"(unique, 0x78f0, 1)","35":"CALL","36":"(ram, 0x73d44, 8)","37":"(register, 0x20, 1)","38":"LOAD ram","39":"(unique, 0x78f0, 1)","40":"CALL","41":"(ram, 0x73d44, 8)","42":"(register, 0x20, 1)","43":"INT_NOTEQUAL"},"ddgedges":[[1,0,1],[0,2,1],[4,3,1],[3,5,1],[2,6,1],[7,6,1],[6,8,1],[5,9,1],[10,9,1],[9,11,1],[11,12,1],[12,13,1],[13,14,1],[15,14,1],[14,16,1],[18,17,1],[17,19,1],[21,20,1],[16,20,1],[8,22,1],[22,23,1],[23,24,1],[25,24,1],[24,26,1],[19,27,1],[26,27,1],[27,28,1],[28,29,1],[29,30,1],[32,31,1],[30,31,1],[11,33,1],[33,34,1],[36,35,1],[34,35,1],[35,37,1],[8,38,1],[38,39,1],[41,40,1],[39,40,1],[40,42,1],[37,43,1],[42,43,1],[43,44,1],[46,45,1],[44,45,1],[11,47,1],[48,47,1],[49,47,1],[47,10,1],[8,50,1],[51,50,1],[52,50,1],[50,7,1],[54,53,1],[11,55,1],[55,56,1],[58,57,1],[56,57,1],[57,59,1],[8,60,1],[60,61,1],[63,62,1],[61,62,1],[62,64,1],[59,65,1],[64,65,1],[65,66,1],[66,67,1],[67,68,1],[70,69,1],[68,69,1]],"lddgedges":[[62,69,2],[57,69,2]]},"func_name":"Curl_strcasecompare"},

 */