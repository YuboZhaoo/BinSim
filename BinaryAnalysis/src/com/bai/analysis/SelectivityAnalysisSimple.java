package com.bai.analysis;

import com.bai.util.Architecture;
import com.bai.util.GlobalState;
import generic.stl.Pair;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.FunctionIterator;
import ghidra.program.model.listing.FunctionManager;
import ghidra.program.model.listing.Program;
import ghidra.program.model.pcode.HighFunction;
import ghidra.program.model.pcode.PcodeBlockBasic;
import ghidra.program.model.pcode.PcodeOp;
import ghidra.program.model.pcode.Varnode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class SelectivityAnalysisSimple extends Analysis {
    private HashMap<PcodeOp, Pair<Double,Double>> selectivity;

    public HashMap<PcodeOp, Pair<Double, Double>> getSelectivity() {
        return selectivity;
    }

    public SelectivityAnalysisSimple(){
        selectivity = new HashMap<>(); // first true, second false
    }

    public SelectivityAnalysisSimple(Program program){ // for other analysis to invoke
        selectivity = new HashMap<>(); // first true, second false
    }

    public HashMap<PcodeOp, Pair<Double, Double>> solver_external(HighFunction hfunc){ // for other analysis to invoke
        selectivity = new HashMap<>();
        if(hfunc == null){ // TODO why it is null
            GlobalState.ghidraScript.printf("                                    ERROR No High Func\n");
            return null;
        }
        ArrayList<PcodeBlockBasic> bbs = hfunc.getBasicBlocks();
        for (PcodeBlockBasic bb: bbs){
            Iterator<PcodeOp> instIter = bb.getIterator();
            while (instIter.hasNext()) {
                PcodeOp inst = instIter.next();
                if(inst.getOpcode() == PcodeOp.CBRANCH)
                    interpret_CBRANCH(inst);
            }
        }
        return selectivity;
    }

    public void setUpDecompiler(Program program) throws Exception {
        super.setUpDecompiler(program);
        GlobalState.ghidraScript = this;
        GlobalState.flatAPI = this;
        GlobalState.currentProgram = program;
        GlobalState.arch = new Architecture(GlobalState.currentProgram);
    }

    public void interpret_CBRANCH(PcodeOp inst){
        Pair<Double,Double> probability = null;

        Varnode condVar = inst.getInput(1);
        PcodeOp condInst = condVar.getDef();

        switch (condInst.getOpcode()){
            // compare op
            case PcodeOp.INT_EQUAL:
                probability = new Pair<>(0.01d, 0.99d);
                break;
            case PcodeOp.INT_NOTEQUAL:
                probability = new Pair<>(0.99d, 0.01d);
                break;
            case PcodeOp.INT_LESS:
                probability = new Pair<>(0.5d, 0.5d);
                break;
            case PcodeOp.INT_SLESS:
                probability = new Pair<>(0.5d, 0.5d);
                break;
            case PcodeOp.INT_LESSEQUAL:
                probability = new Pair<>(0.5d, 0.5d);
                break;
            case PcodeOp.INT_SLESSEQUAL:
                probability = new Pair<>(0.5d, 0.5d);
                break;
            // FLOAT
            case PcodeOp.FLOAT_EQUAL:
                probability = new Pair<>(0.01d, 0.99d);
                break;
            case PcodeOp.FLOAT_NOTEQUAL:
                probability = new Pair<>(0.99d, 0.01d);
                break;
            case PcodeOp.FLOAT_LESS:
                probability = new Pair<>(0.5d, 0.5d);
                break;
            case PcodeOp.FLOAT_LESSEQUAL:
                probability = new Pair<>(0.5d, 0.5d);
                break;
            // BOOL
            case PcodeOp.BOOL_AND:
                probability = new Pair<>(0.25d, 0.75d);
                break;
            case PcodeOp.BOOL_OR:
                probability = new Pair<>(0.75d, 0.25d);
                break;
            case PcodeOp.BOOL_XOR:
                probability = new Pair<>(0.5d, 0.5d);
                break;
            case PcodeOp.BOOL_NEGATE:
                probability = new Pair<>(0.5d, 0.5d);
                break;
            // TODO special compare op
            case PcodeOp.MULTIEQUAL:
                probability = new Pair<>(0.5d, 0.5d);
                break;
            case PcodeOp.CAST:
                probability = new Pair<>(0.5d, 0.5d);
                break;
            default:
                GlobalState.ghidraScript.print("                                    cmp inst "+ condInst.getMnemonic() +" is unsupported now\n");
                probability = new Pair<>(-1d, -1d);

        }
        selectivity.put(inst, probability);
    }
    @Override
    public void solver(HighFunction hfunc){
        selectivity = new HashMap<>();
        if(hfunc == null){ // TODO why it is null
            GlobalState.ghidraScript.printf("                                    ERROR No High Func\n");
            return;
        }
        ArrayList<PcodeBlockBasic> bbs = hfunc.getBasicBlocks();
        for (PcodeBlockBasic bb: bbs){
            GlobalState.ghidraScript.print(bb.toString()+"\n");
            Iterator<PcodeOp> instIter = bb.getIterator();
            while (instIter.hasNext()) {
                PcodeOp inst = instIter.next();
                print(inst.toString() + "\n");
                if(inst.getOpcode() == PcodeOp.CBRANCH)
                    interpret_CBRANCH(inst);
            }
        }
        for (PcodeOp condInst: selectivity.keySet()){
            if(selectivity.get(condInst).first == -1d){
                GlobalState.ghidraScript.printf("                                    ERROR unknown cond inst: %s\n", condInst.getParent().getStart());
            }
//            GlobalState.ghidraScript.printf("inst:  %s\n  cond inst %s  ", condInst, condInst.getInput(1).getDef());
//            GlobalState.ghidraScript.printf("    true %f,   false  %f\n", selectivity.get(condInst).first, selectivity.get(condInst).second);
        }
    }

    public void solver(HighFunction hfunc, String funcname){
        selectivity = new HashMap<>();
        if(hfunc == null){ // TODO why it is null
            GlobalState.ghidraScript.printf("                                    ERROR No High Func\n");
            return;
        }
        ArrayList<PcodeBlockBasic> bbs = hfunc.getBasicBlocks();
        for (PcodeBlockBasic bb: bbs){
//            GlobalState.ghidraScript.print(bb.toString()+"\n");
            Iterator<PcodeOp> instIter = bb.getIterator();
            while (instIter.hasNext()) {
                PcodeOp inst = instIter.next();
//                print(inst.toString() + "\n");
                if(inst.getOpcode() == PcodeOp.CBRANCH)
                    interpret_CBRANCH(inst);
            }
        }
        for (PcodeOp condInst: selectivity.keySet()){
            if(selectivity.get(condInst).first == -1d){
//                GlobalState.ghidraScript.printf("ERROR %s, unknown cond inst: %s\n", funcname, condInst.getMnemonic());
                GlobalState.ghidraScript.printf("ERROR %s\n", funcname);
                break;
            }
//            GlobalState.ghidraScript.printf("inst:  %s\n  cond inst %s  ", condInst, condInst.getInput(1).getDef());
//            GlobalState.ghidraScript.printf("    true %f,   false  %f\n", selectivity.get(condInst).first, selectivity.get(condInst).second);
        }
    }
//    @Override
//    public void run_ghidra() throws Exception{
//        setUpDecompiler(this.currentProgram);
//        Function func = this.getFunctionContaining(this.currentAddress);
//        HighFunction hfunc = decompileFunc(func);
//        solver(hfunc);
//    }
    public void run_ghidra_all() throws Exception{
        setUpDecompiler(this.currentProgram);

        FunctionManager fm = this.currentProgram.getFunctionManager();
        FunctionIterator funcs = fm.getFunctions(true);
        for (Function func: funcs){
            if(func.isThunk()){
                continue;
            }
            GlobalState.ghidraScript.printf("func %s \n", func.getName());
            HighFunction hfunc = decompileFunc(func);
            solver(hfunc, func.getName());
        }

    }
    @Override
    public void run_ghidra() throws Exception{
        setUpDecompiler(this.currentProgram);
        Function func = this.getFunctionContaining(this.currentAddress);

        HighFunction hfunc = decompileFunc(func);
        GlobalState.ghidraScript.printf("func %s \n", func.getName());

        solver(hfunc);
    }
//    @Override
//    public void run() throws Exception {
//        if(this.getScriptArgs().length!=0) {
//            run_headless();
//        }else{
//            run_ghidra();
//        }
//    }
// func buffer_ctrl  BN_mod_exp_mont_word
    //INFO  func bn_mul_mont  (GhidraScript)
    //WARN  Decompiling 001e7280, pcode error at 001e7def: Unable to resolve constructor at 001e7def (DecompileCallback)
//INFO  func uname  (GhidraScript)
//WARN  Decompiling 004470a0, pcode error at 004470a0: Unable to disassemble EXTERNAL block location: 004470a0 (DecompileCallback)

//INFO  func buffer_ctrl  (GhidraScript)
//INFO                                      ERROR No High Func (GhidraScript)
//INFO  func buffer_callback_ctrl  (GhidraScript)
//INFO  func buffer_gets  (GhidraScript)
//INFO                                      ERROR No High Func (GhidraScript)

    //INFO  func mulx4x_internal  (GhidraScript)
    //WARN  Decompiling 001e9f00, pcode error at 001ea28f: Unable to resolve constructor at 001ea28f (DecompileCallback)
    //INFO  func bn_powerx5  (GhidraScript)
    //INFO  func bn_sqrx8x_internal  (GhidraScript)
    //WARN  Decompiling 001ea840, pcode error at 001eac4d: Unable to resolve constructor at 001eac4d (DecompileCallback)
    //WARN  Decompiling 001ea840, pcode error at 001eaf94: Unable to resolve constructor at 001eaf94 (DecompileCallback)
}
