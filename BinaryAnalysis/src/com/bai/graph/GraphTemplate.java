package com.bai.graph;

import ghidra.program.model.listing.Function;
import ghidra.program.model.pcode.*;
import ghidra.service.graph.AttributedVertex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class GraphTemplate extends GraphBasic{
    public class Temp_OP extends Graph_OP{
        private TempDisplay displayer;

        public Temp_OP(HighFunction highFunction){
            super();
            func = highFunction.getFunction();
            hfunc = highFunction;
            displayer = new TempDisplay(func, hfunc);
            buildGraph(hfunc);
        }

        protected void buildGraph(HighFunction hfunc) {
            HashMap<Integer, AttributedVertex> vertices = new HashMap<>();
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
                    varnodes.add(vn);
                    if (vn != null) {
                        addEdge_v2o(vn, op);
                    }
                }
                VarnodeAST outvn = (VarnodeAST) op.getOutput();
                varnodes.add(outvn);
                if (outvn != null) {
                    addEdge_o2v(op, outvn);
                }
            }
        }
        public void dump(){

        }
        class TempDisplay extends GraphDisplay{
            public TempDisplay(Function func, HighFunction hfunc){
                super(func, hfunc);
            }
        }
    }


    public class Temp_INST extends Graph_INST{
        private TempDisplay displayer;

        public Temp_INST(HighFunction highFunction){
            super();
            func = highFunction.getFunction();
            hfunc = highFunction;
            displayer = new TempDisplay(func, hfunc);
            buildGraph(hfunc);
        }

        public void buildGraph(HighFunction hfunc){
            ArrayList<PcodeBlockBasic> bbs = hfunc.getBasicBlocks();
            for (PcodeBlockBasic bb: bbs){
                Iterator<PcodeOp> instIter = bb.getIterator();
                PcodeOp prev = null;
                while (instIter.hasNext()) {
                    PcodeOp inst = instIter.next();
                    nodes.add(inst);
                    if(prev != null){
                        edges.getOrDefault(prev, new HashSet<>()).add(inst);
                        HashSet<PcodeOp> target = edges.getOrDefault(prev, new HashSet<>());
                        target.add(inst);
                        edges.put(prev, target);
                    }
                    prev = inst;
                }
                for (int i = 0; i < bb.getOutSize(); i++) {
                    PcodeBlockBasic bb_succ =  (PcodeBlockBasic) bb.getOut(i);
                    Iterator<PcodeOp> iter_succ = bb_succ.getIterator();
                    if(iter_succ.hasNext()){
                        HashSet<PcodeOp> target = edges.getOrDefault(prev, new HashSet<>());
                        target.add(iter_succ.next());
                        edges.put(prev, target);
                    }
                }
            }
        }
        public void dump(){

        }
        class TempDisplay extends GraphDisplay{
            public TempDisplay(Function func, HighFunction hfunc){
                super(func, hfunc);
            }
        }
    }
}
