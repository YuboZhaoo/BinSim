package com.bai.analysis;


import com.bai.env.domain.Interval;
import com.bai.env.domain.IntervalSet;
import ghidra.program.model.pcode.*;

import java.util.*;



public class IntervalAnalysis extends Analysis {
    private Queue<PcodeBlockBasic> worklist;
    private HashMap<PcodeBlockBasic, IntervalSet> states;
    public IntervalAnalysis() {
        this.states = new HashMap<>();
        this.worklist = new LinkedList<>();
    }
    public void initState(HighFunction hfunc){
        for (int i = 0; i < hfunc.getFunctionPrototype().getNumParams(); ++i) {
            if (hfunc.getFunctionPrototype().getParam(i).getHighVariable() == null)
                continue;
            Varnode key = hfunc.getFunctionPrototype().getParam(i).getHighVariable().getRepresentative();
//            entryDDEdgesLine.put(key, new HashSet<PcodeOp>());
        }
    }
    public Interval[] binaryOP(PcodeOp inst, IntervalSet curState){
        VarnodeAST input1 = (VarnodeAST) inst.getInput(0);
        VarnodeAST input2 = (VarnodeAST) inst.getInput(1);
        Interval interval1 = curState.getInterval(input1);
        Interval interval2 = curState.getInterval(input2);
        return new Interval[]{interval1, interval2};
    }
    public void store_abs(PcodeOp inst, IntervalSet curState){
        Varnode addressSpaceId = inst.getInput(0);
        assert addressSpaceId.isConstant();

        VarnodeAST dst_ptr = (VarnodeAST) inst.getInput(1);
        VarnodeAST src = (VarnodeAST) inst.getInput(2);

        Interval interval_dst_addr = curState.getInterval(dst_ptr);
        Interval interval_src = curState.getInterval(src);

        if(!interval_dst_addr.isSingle()){
            return;
        }

    }
    public IntervalSet transferFunction(PcodeOp inst, IntervalSet curState){
        VarnodeAST output = (VarnodeAST) inst.getOutput();
        VarnodeAST input1, input2;
        Interval interval1, interval2;
        switch (inst.getOpcode()) {
            case PcodeOp.INT_ADD:
                input1 = (VarnodeAST) inst.getInput(0);
                interval1 = curState.getInterval(input1);
                input2 = (VarnodeAST) inst.getInput(1);
                interval2 = curState.getInterval(input2);
                curState.setInterval(output, interval1.add(interval2));
                break;
            case PcodeOp.INT_SUB:
                input1 = (VarnodeAST) inst.getInput(0);
                interval1 = curState.getInterval(input1);
                input2 = (VarnodeAST) inst.getInput(1);
                interval2 = curState.getInterval(input2);
                curState.setInterval(output, interval1.sub(interval2));
                break;
            case PcodeOp.INT_ZEXT:
            case PcodeOp.INT_SEXT:
                input1 = (VarnodeAST) inst.getInput(0);
                interval1 = curState.getInterval(input1);
                curState.setInterval(output, interval1);
            case PcodeOp.COPY:
                input1 = (VarnodeAST) inst.getInput(0);
                interval1 = curState.getInterval(input1);
                curState.setInterval(output, interval1);
                break;
            case PcodeOp.LOAD:
                break;
            case PcodeOp.STORE:
                break;


            default:
                return curState;
        }
        return curState;
    }
    @Override
    public void solver(HighFunction hfunc) {
        ArrayList<PcodeBlockBasic> bbs = hfunc.getBasicBlocks();
        for (int i = 0; i < bbs.size(); i++) {
            states.put(bbs.get(i), new IntervalSet());
        }

        PcodeBlockBasic entry = bbs.get(0);
        worklist.add(entry);

        // worklist-based fixed point algorithm
        while (!worklist.isEmpty()) {
            PcodeBlockBasic curBlock = worklist.poll();
            IntervalSet inState = new IntervalSet();
            for (int i = 0; i < curBlock.getInSize(); i++) {
                IntervalSet in_i = states.get((PcodeBlockBasic)curBlock.getIn(i));
                inState.join(in_i);
            }

            IntervalSet curState = inState;
            Iterator<PcodeOp> instIter = curBlock.getIterator();
            while (instIter.hasNext()) {
                PcodeOp inst = instIter.next();
                curState = transferFunction(inst, curState);
            }
            if(!curState.equals(states.get(curBlock))){//TODO if curState after computing is NOT equal to states.get(curBlock)
                states.put(curBlock, curState);
                for (int i = 0; i < curBlock.getOutSize(); i++) {
                    worklist.add((PcodeBlockBasic)curBlock.getOut(i));
                }
            }
        }
    }

    @Override
    public void run_ghidra() throws Exception{
        HighFunction hfunc = run_ghidra_init();
        solver(hfunc);
    }

    @Override
    public void run() throws Exception {
        if(this.getScriptArgs().length!=0) {
            run_headless();
        }else{
            run_ghidra();
        }
    }
}




