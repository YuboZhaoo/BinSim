import com.bai.env.AbsEnv;
import com.bai.env.semantic.IntervalInterpreter;
import com.bai.util.Architecture;
import com.bai.util.GlobalState;
import generic.stl.Pair;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.Program;
import ghidra.program.model.pcode.HighFunction;
import ghidra.program.model.pcode.PcodeBlockBasic;
import ghidra.program.model.pcode.PcodeOp;

import java.util.*;


public class IntervalAnalysis extends Analysis {
    protected Queue<PcodeBlockBasic> worklist;
    protected HashMap<PcodeBlockBasic, AbsEnv> states; // normal out state & false out state
    protected HashMap<PcodeBlockBasic, AbsEnv> condStates; // TRUE Branch Out state
    protected Set<PcodeBlockBasic> condBBs;

    private IntervalInterpreter interpreter;
    private static String domainType = "interval";

    public IntervalAnalysis() {
        this.states = new HashMap<>();
        this.condStates = new HashMap<>();
        this.worklist = new LinkedList<>();
        this.interpreter = new IntervalInterpreter();
    }

    public void setUpDecompiler(Program program) throws Exception {
        super.setUpDecompiler(program);
        GlobalState.ghidraScript = this;
        GlobalState.flatAPI = this;
        GlobalState.currentProgram = program;
        GlobalState.arch = new Architecture(GlobalState.currentProgram);
    }

    public void setUpDecompiler_wrap(Program program) throws Exception {
        super.setUpDecompiler(program);
    }

    @Override
    public void solver(HighFunction hfunc) {
        ArrayList<PcodeBlockBasic> bbs = hfunc.getBasicBlocks();
//        PcodeBlockBasic entry = bbs.get(0);

        print("Interval Analysis Starts..... BB nums: "+bbs.size()+"  Para nums: "+hfunc.getFunctionPrototype().getNumParams()+"\n");

        PcodeBlockBasic entry = bbs.get(0);
        AbsEnv initEnv = interpreter.initParas(hfunc);

        // init states
        for (int i = 0; i < bbs.size(); i++) {
            PcodeBlockBasic bb = bbs.get(i);
            states.put(bb, new AbsEnv(domainType));
            if(Analysis.isConditional(bb)){
                condStates.put(bb, new AbsEnv(domainType));
            }
            worklist.add(bbs.get(i));
        }
        condBBs = condStates.keySet();

        // worklist-based fixed point algorithm
        while (!worklist.isEmpty()) {
            PcodeBlockBasic curBlock = worklist.poll();
            AbsEnv inState = new AbsEnv(domainType);
            for (int i = 0; i < curBlock.getInSize(); i++) {
                PcodeBlockBasic precBlock = (PcodeBlockBasic)curBlock.getIn(i);
                AbsEnv tmpState = null;

                if(condBBs.contains(precBlock) && precBlock.getTrueOut() == curBlock){ // True Branch
                    tmpState = inState.join(condStates.get((PcodeBlockBasic)curBlock.getIn(i)));
                }else{ // normal state and false branch
                    tmpState = inState.join(states.get((PcodeBlockBasic)curBlock.getIn(i)));
                }

                if(tmpState!=null) inState = tmpState;
            }
            if(curBlock.equals(entry)){
                AbsEnv tmpState = inState.join(initEnv);
                if(tmpState!=null) inState = tmpState;
            }

            AbsEnv curState = inState;
            AbsEnv trState = null;
            Iterator<PcodeOp> instIter = curBlock.getIterator();
//            print("\n\n BB:  "+curBlock.getStart().toString()+"\n");
            print("\n\n BB:  "+curBlock.toString()+"\n");
            while (instIter.hasNext()) {
                PcodeOp inst = instIter.next();
                print(inst+"\n");
                if(inst.getOpcode() == PcodeOp.CBRANCH){
                    Pair<AbsEnv, AbsEnv> pair = interpreter.interpret_CBRANCH(inst, curState);
                    curState = pair.first;
                    trState = pair.second;
                }
//                if(!instIter.hasNext() && condBBs.contains(curBlock)){ // cbranch inst
//
//                }
                else curState = interpreter.transferFunction(inst, curState);
            }

            // cond bb
            if(condBBs.contains(curBlock)){ // TODO if curState after computing is NOT equal to states.get(curBlock)
                if(!curState.equals(states.get(curBlock))){
                    states.put(curBlock, curState);
                    worklist.add((PcodeBlockBasic)curBlock.getFalseOut());
                }
                if(!trState.equals(condStates.get(curBlock))){
                    condStates.put(curBlock, trState);
                    worklist.add((PcodeBlockBasic)curBlock.getTrueOut());
                }
            }
            // normal bb
            else if(!curState.equals(states.get(curBlock))){ // TODO if curState after computing is NOT equal to states.get(curBlock)
                states.put(curBlock, curState);
                for (int i = 0; i < curBlock.getOutSize(); i++) {
                    worklist.add((PcodeBlockBasic)curBlock.getOut(i));
                }
            }
        }
    }

    // an interface for other class
    public void intervalAnalysis(HighFunction hfunc) {
        ArrayList<PcodeBlockBasic> bbs = hfunc.getBasicBlocks();
//        PcodeBlockBasic entry = bbs.get(0);

        print("Interval Analysis Starts..... BB nums: "+bbs.size()+"  Para nums: "+hfunc.getFunctionPrototype().getNumParams()+"\n");

        PcodeBlockBasic entry = bbs.get(0);
        AbsEnv initEnv = interpreter.initParas(hfunc);

        // init states
        for (int i = 0; i < bbs.size(); i++) {
            PcodeBlockBasic bb = bbs.get(i);
            states.put(bb, new AbsEnv(domainType));
            if(Analysis.isConditional(bb)){
                condStates.put(bb, new AbsEnv(domainType));
            }
            worklist.add(bbs.get(i));
        }
        condBBs = condStates.keySet();

        // worklist-based fixed point algorithm
        while (!worklist.isEmpty()) {
            PcodeBlockBasic curBlock = worklist.poll();
            AbsEnv inState = new AbsEnv(domainType);
            for (int i = 0; i < curBlock.getInSize(); i++) {
                PcodeBlockBasic precBlock = (PcodeBlockBasic)curBlock.getIn(i);
                AbsEnv tmpState = null;

                if(condBBs.contains(precBlock) && precBlock.getTrueOut() == curBlock){ // True Branch
                    tmpState = inState.join(condStates.get((PcodeBlockBasic)curBlock.getIn(i)));
                }else{ // normal state and false branch
                    tmpState = inState.join(states.get((PcodeBlockBasic)curBlock.getIn(i)));
                }

                if(tmpState!=null) inState = tmpState;
            }
            if(curBlock.equals(entry)){
                AbsEnv tmpState = inState.join(initEnv);
                if(tmpState!=null) inState = tmpState;
            }

            AbsEnv curState = inState;
            AbsEnv trState = null;
            Iterator<PcodeOp> instIter = curBlock.getIterator();
//            print("\n\n BB:  "+curBlock.getStart().toString()+"\n");
            print("\n\n BB:  "+curBlock.toString()+"\n");
            while (instIter.hasNext()) {
                PcodeOp inst = instIter.next();
                print(inst+"\n");
                if(inst.getOpcode() == PcodeOp.CBRANCH){
                    Pair<AbsEnv, AbsEnv> pair = interpreter.interpret_CBRANCH(inst, curState);
                    curState = pair.first;
                    trState = pair.second;
                }
//                if(!instIter.hasNext() && condBBs.contains(curBlock)){ // cbranch inst
//
//                }
                else curState = interpreter.transferFunction(inst, curState);
            }

            // cond bb
            if(condBBs.contains(curBlock)){ // TODO if curState after computing is NOT equal to states.get(curBlock)
                if(!curState.equals(states.get(curBlock))){
                    states.put(curBlock, curState);
                    worklist.add((PcodeBlockBasic)curBlock.getFalseOut());
                }
                if(!trState.equals(condStates.get(curBlock))){
                    condStates.put(curBlock, trState);
                    worklist.add((PcodeBlockBasic)curBlock.getTrueOut());
                }
            }
            // normal bb
            else if(!curState.equals(states.get(curBlock))){ // TODO if curState after computing is NOT equal to states.get(curBlock)
                states.put(curBlock, curState);
                for (int i = 0; i < curBlock.getOutSize(); i++) {
                    worklist.add((PcodeBlockBasic)curBlock.getOut(i));
                }
            }
        }
    }

    @Override
    public void run_ghidra() throws Exception{
        setUpDecompiler(this.currentProgram);
        Function func = this.getFunctionContaining(this.currentAddress);
        HighFunction hfunc = decompileFunc(func);
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

class IntervalAnalysis_v0 extends Analysis {
    private Queue<PcodeBlockBasic> worklist;
    private HashMap<PcodeBlockBasic, AbsEnv> states;
    private IntervalInterpreter interpreter;
    private static String domainType = "interval";
    public IntervalAnalysis_v0() {
        this.states = new HashMap<>();
        this.worklist = new LinkedList<>();
        this.interpreter = new IntervalInterpreter();
    }
//    public void initState(HighFunction hfunc){
//        for (int i = 0; i < hfunc.getFunctionPrototype().getNumParams(); ++i) {
//            if (hfunc.getFunctionPrototype().getParam(i).getHighVariable() == null)
//                continue;
//            Varnode key = hfunc.getFunctionPrototype().getParam(i).getHighVariable().getRepresentative();
////            entryDDEdgesLine.put(key, new HashSet<PcodeOp>());
//        }
//    }

    public void setUpDecompiler(Program program) throws Exception {
        super.setUpDecompiler(program);
        GlobalState.ghidraScript = this;
        GlobalState.flatAPI = this;
        GlobalState.currentProgram = program;
        GlobalState.arch = new Architecture(GlobalState.currentProgram);
    }

    @Override
    public void solver(HighFunction hfunc) {
        ArrayList<PcodeBlockBasic> bbs = hfunc.getBasicBlocks();
//        PcodeBlockBasic entry = bbs.get(0);

        print("Interval Analysis Starts..... BB nums: "+bbs.size()+"  Para nums: "+hfunc.getFunctionPrototype().getNumParams()+"\n");

        PcodeBlockBasic entry = bbs.get(0);
        AbsEnv initEnv = interpreter.initParas(hfunc);


        for (int i = 0; i < bbs.size(); i++) {
            states.put(bbs.get(i), new AbsEnv(domainType));
            worklist.add(bbs.get(i));
        }

        // worklist-based fixed point algorithm
        while (!worklist.isEmpty()) {
            PcodeBlockBasic curBlock = worklist.poll();
            AbsEnv inState = new AbsEnv(domainType);
            for (int i = 0; i < curBlock.getInSize(); i++) {
                AbsEnv in_i = states.get((PcodeBlockBasic)curBlock.getIn(i));
                curBlock.getTrueOut();
//                inState = inState.join(in_i);
                AbsEnv tmpState = inState.join(in_i);
                if(tmpState!=null) inState = tmpState;
            }
            if(curBlock.equals(entry)){
                AbsEnv tmpState = inState.join(initEnv);
                if(tmpState!=null) inState = tmpState;
//                inState = initEnv;
            }

            AbsEnv curState = inState;
            Iterator<PcodeOp> instIter = curBlock.getIterator();
            print("\n\n BB:  "+curBlock.getStart().toString()+"\n");
            while (instIter.hasNext()) {
                PcodeOp inst = instIter.next();
                print(inst+"\n");
                curState = interpreter.transferFunction(inst, curState);
//                curState = new AbsEnv(interpreter.transferFunction(inst, curState));
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
        setUpDecompiler(this.currentProgram);
        Function func = this.getFunctionContaining(this.currentAddress);
        HighFunction hfunc = decompileFunc(func);
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




