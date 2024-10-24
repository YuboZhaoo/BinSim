package com.bai.env.semantic;

import com.bai.env.ALoc;
import com.bai.env.AbsEnv;
import com.bai.env.domain.AbsDomain;
import com.bai.env.domain.AbsDomainFactory;
import com.bai.env.domain.Interval;

import com.bai.env.region.Global;
import com.bai.util.GlobalState;
import generic.stl.Pair;
import ghidra.feature.vt.gui.actions.ApplyAndReplaceMarkupItemAction;
import ghidra.program.model.address.Address;
import ghidra.program.model.data.Pointer;
import ghidra.program.model.pcode.*;
import jnr.ffi.mapper.AbstractFromNativeType;

import java.util.List;

public class IntervalInterpreter extends Interpreter{

//    private String domainType;
//
//    public void setDomainType(String domainType){ this.domainType = domainType;}
    public IntervalInterpreter(){
        domainType = "interval";
    }
//    protected String domainType;

    public AbsEnv initParas(HighFunction hfunc){
        AbsEnv outState = new AbsEnv(domainType);
        for (int i = 0; i < hfunc.getFunctionPrototype().getNumParams(); ++i) {

            GlobalState.ghidraScript.print(hfunc.getFunctionPrototype().getParam(i).getName()+"          ");

            HighVariable highVariable = hfunc.getFunctionPrototype().getParam(i).getHighVariable();
            if(highVariable == null){
                GlobalState.ghidraScript.print("          (not used param)\n");
                continue;
            }
            Varnode param = hfunc.getFunctionPrototype().getParam(i).getHighVariable().getRepresentative();
            if(highVariable.getDataType() instanceof Pointer){
                setAbsValue(param, AbsDomainFactory.createAbsDomain(domainType, (i+1)*1000000), outState, true);
                ALoc aloc = ALoc.getALoc(Global.getInstance(), (i+1)*1000000, 8);
                outState.set(aloc, AbsDomainFactory.createAbsDomain(domainType, true), true);
            } else{
                setAbsValue(param, AbsDomainFactory.createAbsDomain(domainType, true), outState, true);
            }
            GlobalState.ghidraScript.print("          "+param.toString()+"    init value"+getAbsValue(param, outState, null).toString()+"\n");

        }
        return outState;
    }

    public AbsEnv transferFunction(PcodeOp inst, AbsEnv curState){
        AbsEnv outState;
        switch (inst.getOpcode()) {
            // math
            case PcodeOp.INT_ADD:
                outState = interpret_INT_ADD(inst, curState);
                break;
            case PcodeOp.INT_SUB:
                outState = interpret_INT_SUB(inst, curState);
                break;
            case PcodeOp.INT_MULT:
                outState = interpret_INT_MULT(inst, curState);
                break;
            case PcodeOp.INT_DIV:
                outState = interpret_INT_DIV(inst, curState);
                break;
            // memory op
            case PcodeOp.COPY:
                outState = interpret_COPY(inst, curState);
                break;
            case PcodeOp.LOAD:
                outState = interpret_LOAD(inst, curState);
                break;
            case PcodeOp.STORE:
                outState = interpret_STORE(inst, curState);
                break;
            // branch op
            case PcodeOp.BRANCH:
                // TODO
                outState = new AbsEnv(curState);
                break;
//            case PcodeOp.CBRANCH:
//                // TODO CBRANCH in unified method
//                outState = new AbsEnv(curState);
//                break;
            case PcodeOp.BRANCHIND:
                // TODO
                outState = new AbsEnv(curState);
                break;

            // logical op
            case PcodeOp.INT_2COMP:
                outState = interpret_INT_2COMP(inst, curState);
                break;
            case PcodeOp.INT_NEGATE:
                outState = interpret_INT_NEGATE(inst, curState);
                break;
            case PcodeOp.INT_LEFT:
                outState = interpret_INT_LEFT(inst, curState);
                break;
            case PcodeOp.INT_RIGHT:
                outState = interpret_INT_RIGHT(inst, curState);
                break;
            case PcodeOp.INT_SRIGHT:
                outState = interpret_INT_SRIGHT(inst, curState);
                break;
            case PcodeOp.INT_XOR:
                // TODO xor can not be calculated
                outState = new AbsEnv(curState);
                break;
            case PcodeOp.INT_AND:
                outState = interpret_INT_AND(inst, curState);
                break;
            case PcodeOp.INT_OR:
                outState = interpret_INT_OR(inst, curState);
                break;

            // TODO bool
            case PcodeOp.BOOL_NEGATE:
                outState = interpret_BOOL_NEGATE(inst, curState);
                break;
            case PcodeOp.BOOL_XOR:
                outState = interpret_BOOL_XOR(inst, curState);
                break;
            case PcodeOp.BOOL_AND:
                outState = interpret_BOOL_AND(inst, curState);
                break;
            case PcodeOp.BOOL_OR:
                outState = interpret_BOOL_OR(inst, curState);
                break;

            // TODO float
//            case PcodeOp.FLOAT_LESS:
//                outState = interpret_FLOAT_LESS(inst, curState);
//                break;
//            case PcodeOp.FLOAT_SUB:
//                outState = interpret_FLOAT_SUB(inst, curState);
//                break;

            // TODO zext and sext
            case PcodeOp.INT_ZEXT:
            case PcodeOp.INT_SEXT:
                GlobalState.ghidraScript.print("                                    unsupported now\n");
                outState = new AbsEnv(curState);
                break;

            //  compare inst's operand will be calculated in cbranch
            //  now we just set the res as [0,1]
            case PcodeOp.INT_EQUAL:
            case PcodeOp.INT_NOTEQUAL:
            case PcodeOp.INT_LESS:
            case PcodeOp.INT_SLESS:
            case PcodeOp.INT_LESSEQUAL:
            case PcodeOp.INT_SLESSEQUAL:
                GlobalState.ghidraScript.print("                                    compare op\n");
                outState = interpret_int_compare(inst, curState);
                break;

            default:
                GlobalState.ghidraScript.print("                                    unsupported now\n");
                outState = new AbsEnv(curState);
        }
        return outState;
    }


    // set the result op as [0,1]
    private AbsEnv interpret_bool_compare(PcodeOp inst, AbsEnv curState){
        AbsEnv outState = new AbsEnv(curState);
        Varnode dst = inst.getOutput();
        setAbsValue(dst, AbsDomainFactory.createAbsDomain(this.domainType, 0,1), outState, true);
        return outState;
    }

    // set the result op as [0,1]
    private AbsEnv interpret_int_compare(PcodeOp inst, AbsEnv curState){
        AbsEnv outState = new AbsEnv(curState);
        Varnode dst = inst.getOutput();
        setAbsValue(dst, AbsDomainFactory.createAbsDomain(this.domainType, 0,1), outState, true);
        return outState;
    }

    private long minOR(long a, long b, long c, long d) {
        long m, temp;
        // TODO
//        m = 0x80000000 >> __builtin_clz(a ^ c);
        m = 0x80000000L;
        while (m != 0) {
            if ((~a & c & m) != 0) {
                temp = (a | m) & -m;
                if (temp <= b) {
                    a = temp;
                    break;
                }
            } else if ((a & ~c & m) != 0) {
                temp = (c | m) & -m;
                if (temp <= d) {
                    c = temp;
                    break;
                }
            }
            m = m >> 1;
        }
        return a | c;
    }
    private long maxOR(long a, long b, long c, long d) {
        long m, temp;
        // TODO
//        m = 0x80000000 >> __builtin_clz(b & d);
        m = 0x80000000L;
        while (m != 0) {
            if ((b & d & m) != 0) {
                temp = (b - m) | (m - 1);
                if (temp >= a) {
                    b = temp;
                    break;
                }
                temp = (d - m) | (m - 1);
                if (temp >= c) {
                    d = temp;
                    break;
                }
            }
            m = m >> 1;
        }
        return b | d;
    }

    public Interval int_or(long a, long b, long c, long d){

        int flaga = 0, flagb = 0, flagc = 0, flagd = 0;
        if(a>=0) flaga = 1; else flaga = -1;
        if(b>=0) flagb = 1; else flagb = -1;
        if(c>=0) flagc = 1; else flagc = -1;
        if(d>=0) flagd = 1; else flagd = -1;

        if(flaga==-1 && flagb==-1 && flagc==-1 && flagd==-1){ // - - - -
            return (Interval) AbsDomainFactory.createAbsDomain(this.domainType, minOR(a,b,c,d),maxOR(a,b,c,d));
        } else if (flaga==-1 && flagb==-1 && flagc==-1 && flagd==1) { // - - - +
            return (Interval) AbsDomainFactory.createAbsDomain(this.domainType, a,-1L);
        } else if (flaga==-1 && flagb==-1 && flagc==1 && flagd==1) { // - - + +
            return (Interval) AbsDomainFactory.createAbsDomain(this.domainType, minOR(a,b,c,d),maxOR(a,b,c,d));
        } else if (flaga==-1 && flagb==1 && flagc==-1 && flagd==-1) { // - + - -
            return (Interval) AbsDomainFactory.createAbsDomain(this.domainType, c,-1L);
        } else if (flaga==-1 && flagb==1 && flagc==-1 && flagd==1) { // - + - +
            return (Interval) AbsDomainFactory.createAbsDomain(this.domainType, Math.min(a,c),maxOR(0,b,0,d));
        } else if (flaga==-1 && flagb==1 && flagc==1 && flagd==1) { // - + + +
            return (Interval) AbsDomainFactory.createAbsDomain(this.domainType, minOR(a,Interval.getPosInf(),c,d),maxOR(0,b,c,d));
        } else if (flaga==1 && flagb==1 && flagc==-1 && flagd==-1) { // + + - -
            return (Interval) AbsDomainFactory.createAbsDomain(this.domainType, minOR(a,b,c,d),maxOR(a,b,c,d));
        } else if (flaga==1 && flagb==1 && flagc==-1 && flagd==1) { // + + - +
            return (Interval) AbsDomainFactory.createAbsDomain(this.domainType, minOR(a,b,c,Interval.getPosInf()),maxOR(a,b,0,d));
        } else if (flaga==1 && flagb==1 && flagc==1 && flagd==1) { // + + + +
            return (Interval) AbsDomainFactory.createAbsDomain(this.domainType, minOR(a,b,c,d),maxOR(a,b,c,d));
        } else{
            return (Interval) AbsDomainFactory.createAbsDomain(this.domainType, true);
        }
    }

    public AbsEnv interpret_INT_OR(PcodeOp inst, AbsEnv curState){
        AbsEnv outState = new AbsEnv(curState);

        Varnode op1 = inst.getInput(0);
        Varnode op2 = inst.getInput(1);
        Varnode dst = inst.getOutput();

        Interval op1Itvl = (Interval) getAbsValue(op1, curState, inst);
        Interval op2Itvl = (Interval) getAbsValue(op2, curState, inst);
        if(op1Itvl.lowerIsTop() || op1Itvl.upperIsTop() || op2Itvl.lowerIsTop() || op2Itvl.upperIsTop()){
            setAbsValue(dst, AbsDomainFactory.createAbsDomain(this.domainType, true), outState, true);
            return outState;
        }
        if(op1Itvl.isBottom() && !op2Itvl.isBottom()){
            setAbsValue(dst, op2Itvl, outState, true);
            return outState;
        } else if (!op1Itvl.isBottom() && op2Itvl.isBottom()) {
            setAbsValue(dst, op1Itvl, outState, true);
            return outState;
        } else if (op1Itvl.isBottom() && op2Itvl.isBottom()) {
            setAbsValue(dst, AbsDomainFactory.createAbsDomain(this.domainType, true), outState, true);
            return outState;
        }

        long a = op1Itvl.getLower(), b = op1Itvl.getUpper(),c = op2Itvl.getLower(), d = op2Itvl.getUpper();
        setAbsValue(dst, int_or(a,b,c,d), outState, true);


//        int flaga = 0, flagb = 0, flagc = 0, flagd = 0;
//        if(op1Itvl.getLower()>=0) flaga = 1; else flaga = -1;
//        if(op1Itvl.getUpper()>=0) flagb = 1; else flagb = -1;
//        if(op2Itvl.getLower()>=0) flagc = 1; else flagc = -1;
//        if(op2Itvl.getUpper()>=0) flagd = 1; else flagd = -1;
//        if(flaga==-1 && flagb==-1 && flagc==-1 && flagd==-1){ // - - - -
//            setAbsValue(dst, AbsDomainFactory.createAbsDomain(this.domainType, minOR(a,b,c,d),maxOR(a,b,c,d)), outState, true);
//        } else if (flaga==-1 && flagb==-1 && flagc==-1 && flagd==1) { // - - - +
//            setAbsValue(dst, AbsDomainFactory.createAbsDomain(this.domainType, a,-1L), outState, true);
//        } else if (flaga==-1 && flagb==-1 && flagc==1 && flagd==1) { // - - + +
//            setAbsValue(dst, AbsDomainFactory.createAbsDomain(this.domainType, minOR(a,b,c,d),maxOR(a,b,c,d)), outState, true);
//        } else if (flaga==-1 && flagb==1 && flagc==-1 && flagd==-1) { // - + - -
//            setAbsValue(dst, AbsDomainFactory.createAbsDomain(this.domainType, c,-1L), outState, true);
//        } else if (flaga==-1 && flagb==1 && flagc==-1 && flagd==1) { // - + - +
//            setAbsValue(dst, AbsDomainFactory.createAbsDomain(this.domainType, Math.min(a,c),maxOR(0,b,0,d)), outState, true);
//        } else if (flaga==-1 && flagb==1 && flagc==1 && flagd==1) { // - + + +
//            setAbsValue(dst, AbsDomainFactory.createAbsDomain(this.domainType, minOR(a,Interval.getPosInf(),c,d),maxOR(0,b,c,d)), outState, true);
//        } else if (flaga==1 && flagb==1 && flagc==-1 && flagd==-1) { // + + - -
//            setAbsValue(dst, AbsDomainFactory.createAbsDomain(this.domainType, minOR(a,b,c,d),maxOR(a,b,c,d)), outState, true);
//        } else if (flaga==1 && flagb==1 && flagc==-1 && flagd==1) { // + + - +
//            setAbsValue(dst, AbsDomainFactory.createAbsDomain(this.domainType, minOR(a,b,c,Interval.getPosInf()),maxOR(a,b,0,d)), outState, true);
//        } else if (flaga==1 && flagb==1 && flagc==1 && flagd==1) { // + + + +
//            setAbsValue(dst, AbsDomainFactory.createAbsDomain(this.domainType, minOR(a,b,c,d),maxOR(a,b,c,d)), outState, true);
//        } else{
//            setAbsValue(dst, AbsDomainFactory.createAbsDomain(this.domainType, true), outState, true);
//        }

//        GlobalState.ghidraScript.print(inst+"\n");
        GlobalState.ghidraScript.print("                                    INT_OR\n");
        GlobalState.ghidraScript.print("                                    input "+op1Itvl.toString()+"    "+op2Itvl.toString()+"   ");
        GlobalState.ghidraScript.print("                                    output "+(Interval) getAbsValue(dst, outState, inst)+"\n");

        return outState;
    }

    public AbsEnv interpret_INT_AND(PcodeOp inst, AbsEnv curState){
        AbsEnv outState = new AbsEnv(curState);

        Varnode op1 = inst.getInput(0);
        Varnode op2 = inst.getInput(1);
        Varnode dst = inst.getOutput();

        Interval op1Itvl = (Interval) getAbsValue(op1, curState, inst);
        Interval op2Itvl = (Interval) getAbsValue(op2, curState, inst);

        if(op1Itvl.lowerIsTop() || op1Itvl.upperIsTop() || op2Itvl.lowerIsTop() || op2Itvl.upperIsTop()){
            setAbsValue(dst, AbsDomainFactory.createAbsDomain(this.domainType, true), outState, true);
            return outState;
        }
        if(op1Itvl.isBottom() && !op2Itvl.isBottom()){
            setAbsValue(dst, op2Itvl, outState, true);
            return outState;
        } else if (!op1Itvl.isBottom() && op2Itvl.isBottom()) {
            setAbsValue(dst, op1Itvl, outState, true);
            return outState;
        } else if (op1Itvl.isBottom() && op2Itvl.isBottom()) {
            setAbsValue(dst, AbsDomainFactory.createAbsDomain(this.domainType, true), outState, true);
            return outState;
        }

        long a = ~op1Itvl.getLower(), b = ~op1Itvl.getUpper(), c = ~op2Itvl.getLower(), d = ~op2Itvl.getUpper();
        Interval tmp = int_or(a,b,c,d), res = null;
        if(tmp.isTop()){
            res = (Interval) AbsDomainFactory.createAbsDomain(domainType, true);
        }else if(~tmp.getLower()<=~tmp.getUpper()){
            res = (Interval) AbsDomainFactory.createAbsDomain(domainType, ~tmp.getLower(), ~tmp.getUpper());
        } else {
            GlobalState.ghidraScript.print("ERROR in INT AND!!!!\n");
            res = (Interval) AbsDomainFactory.createAbsDomain(domainType, true);
        }
        setAbsValue(dst, res, outState, true);

        GlobalState.ghidraScript.print("                                    INT_AND\n");
        GlobalState.ghidraScript.print("                                    input "+op1Itvl.toString()+"    "+op2Itvl.toString()+"   ");
        GlobalState.ghidraScript.print("                                    output "+(Interval) getAbsValue(dst, outState, inst)+"\n");
        return outState;
    }

//    public Pair<AbsEnv, AbsEnv> transferFunction(PcodeOp inst, AbsEnv curState, boolean isConditional){
//        return null;
//    }

    public Pair<AbsEnv, AbsEnv>  interpret_CBRANCH(PcodeOp inst, AbsEnv curState){
        Pair<AbsEnv, AbsEnv> outState;

        Varnode condVar = inst.getInput(1);
        PcodeOp condInst = condVar.getDef();

        switch (condInst.getOpcode()){
            // compare op
            case PcodeOp.INT_EQUAL:
                // TODO NOT completed with false
                outState = interpret_INT_EQUAL(condInst, curState);
                break;
            case PcodeOp.INT_NOTEQUAL:
                // TODO NOT completed with true
                outState = interpret_INT_NOTEQUAL(condInst, curState);
                break;
            case PcodeOp.INT_LESS:
                outState = interpret_INT_LESS(condInst, curState);
                break;
            case PcodeOp.INT_SLESS:
                outState = interpret_INT_SLESS(condInst, curState);
                break;
            case PcodeOp.INT_LESSEQUAL:
                outState = interpret_INT_LESSEQUAL(condInst, curState);
                break;
            case PcodeOp.INT_SLESSEQUAL:
                outState = interpret_INT_SLESSEQUAL(condInst, curState);
                break;

            default:
                GlobalState.ghidraScript.print("                                    cmp inst "+ condInst.getMnemonic() +" is unsupported now\n");
                outState = new Pair<>(new AbsEnv(curState), new AbsEnv(curState));
        }
        return outState;
    }

    public void outputCondRes(String opName, Varnode op1Tr, Varnode op2Tr, Interval resTr, Varnode op1Fls, Varnode op2Fls, Interval resFls){
        GlobalState.ghidraScript.print("                                    "+ opName +"\n");
        GlobalState.ghidraScript.print("                                    True: input "+op1Tr.toString()+"    "+op2Tr.toString()+"   ");
        GlobalState.ghidraScript.print(" output "+resTr+"\n");
        GlobalState.ghidraScript.print("                                    False: input "+op1Fls.toString()+"    "+op2Fls.toString()+"   ");
        GlobalState.ghidraScript.print(" output "+resFls+"\n");
        GlobalState.ghidraScript.print("\n");
    }

    public Pair<AbsEnv, AbsEnv> interpret_INT_EQUAL(PcodeOp inst, AbsEnv curState){
        AbsEnv outStateTrue = new AbsEnv(curState);
        AbsEnv outStateFalse = new AbsEnv(curState);
        Varnode dst = inst.getOutput();
        setAbsValue(dst, AbsDomainFactory.createAbsDomain(this.domainType, 1), outStateTrue, true);
        setAbsValue(dst, AbsDomainFactory.createAbsDomain(this.domainType, 0), outStateFalse, true);

        Varnode op1 = inst.getInput(0);
        Varnode op2 = inst.getInput(1);
        if(op1.isConstant()){
            // True [2,2]  [1,3]
            Interval op1Itvl = (Interval) AbsDomainFactory.createAbsDomain(this.domainType, getTrueValue(op1.getOffset(), op1.getSize()));
            Interval op2Itvl = (Interval) getAbsValue(op2, outStateTrue, inst);
            Interval  resInterval =  (Interval) op1Itvl.intersect(op2Itvl);
            setAbsValue(op2, resInterval, outStateTrue, true);
            // TODO false

            GlobalState.ghidraScript.print("                                    INT_EQUAL\n");
            GlobalState.ghidraScript.print("                                    input "+op1Itvl.toString()+"    "+op2Itvl.toString()+"\n");
            GlobalState.ghidraScript.print("                                    output "+resInterval+"\n");
            GlobalState.ghidraScript.print("\n");
            printCompareOpTr(inst, op1Itvl, op2Itvl, resInterval);

            return  new Pair<>(outStateFalse, outStateTrue);

        }else if(op2.isConstant()){
            Interval op1Itvl = (Interval) getAbsValue(op1, outStateTrue, inst);
            Interval op2Itvl = (Interval) AbsDomainFactory.createAbsDomain(this.domainType, getTrueValue(op2.getOffset(), op2.getSize()));
            Interval  resInterval =  (Interval) op1Itvl.intersect(op2Itvl);
            setAbsValue(op1, resInterval, outStateTrue, true);

            // TODO false
            GlobalState.ghidraScript.print("                                    INT_EQUAL\n");
            GlobalState.ghidraScript.print("                                    input "+op1Itvl.toString()+"    "+op2Itvl.toString()+"\n");
            GlobalState.ghidraScript.print("                                    output "+resInterval+"\n");
            GlobalState.ghidraScript.print("\n");
            printCompareOpTr(inst, op1Itvl, op2Itvl, resInterval);

            return  new Pair<>(outStateFalse, outStateTrue);
        }
        // TODO var and var
        return new Pair<>(outStateFalse, outStateTrue);
    }

    public Pair<AbsEnv, AbsEnv> interpret_INT_NOTEQUAL(PcodeOp inst, AbsEnv curState){
        AbsEnv outStateTrue = new AbsEnv(curState);
        AbsEnv outStateFalse = new AbsEnv(curState);
        Varnode dst = inst.getOutput();
        setAbsValue(dst, AbsDomainFactory.createAbsDomain(this.domainType, 1), outStateTrue, true);
        setAbsValue(dst, AbsDomainFactory.createAbsDomain(this.domainType, 0), outStateFalse, true);

        Varnode op1 = inst.getInput(0);
        Varnode op2 = inst.getInput(1);
        if(op1.isConstant()){
            // False [2,2]  [1,3]
            Interval op1Fls = (Interval) AbsDomainFactory.createAbsDomain(this.domainType, getTrueValue(op1.getOffset(), op1.getSize()));
            Interval op2Fls = (Interval) getAbsValue(op2, outStateTrue, inst);
            Interval resFls =  (Interval) op1Fls.intersect(op2Fls);
            setAbsValue(op2, resFls, outStateTrue, true);
            // TODO True

            GlobalState.ghidraScript.print("                                    INT_NOTEQUAL\n");
            GlobalState.ghidraScript.print("                                    False: input "+op1Fls.toString()+"    "+op2Fls.toString()+"   ");
            GlobalState.ghidraScript.print(" output "+(Interval) getAbsValue(op2, outStateFalse, inst)+"\n");
            GlobalState.ghidraScript.print("\n");
            printCompareOpFls(inst, op1Fls, op2Fls, (Interval) getAbsValue(op2, outStateFalse, inst));

            return  new Pair<>(outStateFalse, outStateTrue);

        }else if(op2.isConstant()){
            Interval op1Fls = (Interval) getAbsValue(op1, outStateTrue, inst);
            Interval op2Fls = (Interval) AbsDomainFactory.createAbsDomain(this.domainType, getTrueValue(op2.getOffset(), op2.getSize()));
            Interval resFls =  (Interval) op1Fls.intersect(op2Fls);
            setAbsValue(op1, resFls, outStateTrue, true);

            // TODO false
            GlobalState.ghidraScript.print("                                    INT_NOTEQUAL\n");
            GlobalState.ghidraScript.print("                                    False: input "+op1Fls.toString()+"    "+op2Fls.toString()+"   ");
            GlobalState.ghidraScript.print(" output "+(Interval) getAbsValue(op2, outStateFalse, inst)+"\n");
            GlobalState.ghidraScript.print("\n");
            printCompareOpFls(inst, op1Fls, op2Fls, (Interval) getAbsValue(op2, outStateFalse, inst));

            return  new Pair<>(outStateFalse, outStateTrue);
        }

        return new Pair<>(outStateFalse, outStateTrue);
    }

    public Pair<AbsEnv, AbsEnv> interpret_INT_LESS(PcodeOp inst, AbsEnv curState){
        // TODO Sign int  (const, 0xffffffff, 4) (finished)
        AbsEnv outStateTrue = new AbsEnv(curState);
        AbsEnv outStateFalse = new AbsEnv(curState);
        Varnode dst = inst.getOutput();
        setAbsValue(dst, AbsDomainFactory.createAbsDomain(this.domainType, 1), outStateTrue, true);
        setAbsValue(dst, AbsDomainFactory.createAbsDomain(this.domainType, 0), outStateFalse, true);

        Varnode op1 = inst.getInput(0);
        Varnode op2 = inst.getInput(1);
        if(op1.isConstant() && !op2.isConstant()){
            // True: [2,2] < x, x = [1,4], -> [2+1, inf] ^ [1,4] = [3,4]
            Interval op1Tr = (Interval) AbsDomainFactory.createAbsDomain(this.domainType,
                    op1.getOffset()+1, Interval.getPosInf());
            Interval op2Tr = (Interval) getAbsValue(op2, outStateTrue, inst);
            Interval  resTr =  (Interval) op1Tr.intersect(op2Tr);
            setAbsValue(op2, resTr, outStateTrue, true);

            // TODO false [2,2] >= x, x = [1,4], -> [0, 2] ^ [1,4] = [1,2]
            Interval op1Fls = (Interval) AbsDomainFactory.createAbsDomain(this.domainType,
                    0, op1.getOffset());
            Interval op2Fls = (Interval) getAbsValue(op2, outStateFalse, inst);
            Interval  resFls =  (Interval) op1Fls.intersect(op2Fls);
            setAbsValue(op2, resFls, outStateFalse, true);

            GlobalState.ghidraScript.print("                                    INT_LESS\n");
            GlobalState.ghidraScript.print("                                    True: input "+op1Tr.toString()+"    "+op2Tr.toString()+"   ");
            GlobalState.ghidraScript.print(" output "+(Interval) getAbsValue(op2, outStateTrue, inst)+"\n");
            GlobalState.ghidraScript.print("                                    False: input "+op1Fls.toString()+"    "+op2Fls.toString()+"   ");
            GlobalState.ghidraScript.print(" output "+(Interval) getAbsValue(op2, outStateFalse, inst)+"\n");
            GlobalState.ghidraScript.print("\n");
            printCompareOpTr(inst, op1Tr, op2Tr, (Interval) getAbsValue(op2, outStateTrue, inst));
            printCompareOpFls(inst, op1Fls, op2Fls, (Interval) getAbsValue(op2, outStateFalse, inst));

            return  new Pair<>(outStateFalse, outStateTrue);

        }else if(!op1.isConstant() && op2.isConstant()){
            // True: x < [2,2], x = [1,4], -> [0, 2-1] ^ [1,4] = [1,1]
            Interval op1Tr = (Interval) getAbsValue(op1, outStateTrue, inst);
            Interval op2Tr = (Interval) AbsDomainFactory.createAbsDomain(this.domainType,
                    0, op2.getOffset()-1);
            Interval  resTr =  (Interval) op1Tr.intersect(op2Tr);
            setAbsValue(op1, resTr, outStateTrue, true);

            // TODO false x >= [2,2], x = [1,4], -> [2, inf] ^ [1,4] = [1,1]
            Interval op1Fls = (Interval) getAbsValue(op1, outStateFalse, inst);
            Interval op2Fls = (Interval) AbsDomainFactory.createAbsDomain(this.domainType,
                    op2.getOffset(), Interval.getPosInf());
            Interval  resFls =  (Interval) op1Fls.intersect(op2Fls);
            setAbsValue(op1, resFls, outStateFalse, true);

            GlobalState.ghidraScript.print("                                    INT_LESS\n");
            GlobalState.ghidraScript.print("                                    True: input "+op1Tr.toString()+"    "+op2Tr.toString()+"   ");
            GlobalState.ghidraScript.print(" output "+(Interval) getAbsValue(op1, outStateTrue, inst)+"\n");
            GlobalState.ghidraScript.print("                                    False: input "+op1Fls.toString()+"    "+op2Fls.toString()+"   ");
            GlobalState.ghidraScript.print(" output "+(Interval) getAbsValue(op1, outStateFalse, inst)+"\n");
            GlobalState.ghidraScript.print("\n");
            printCompareOpTr(inst, op1Tr, op2Tr, (Interval) getAbsValue(op2, outStateTrue, inst));
            printCompareOpFls(inst, op1Fls, op2Fls, (Interval) getAbsValue(op2, outStateFalse, inst));

            return  new Pair<>(outStateFalse, outStateTrue);
        }
        return new Pair<>(outStateFalse, outStateTrue);
    }

    public Pair<AbsEnv, AbsEnv> interpret_INT_SLESS(PcodeOp inst, AbsEnv curState){
        // TODO Sign int  (const, 0xffffffff, 4) (finished)
        AbsEnv outStateTrue = new AbsEnv(curState);
        AbsEnv outStateFalse = new AbsEnv(curState);
        Varnode dst = inst.getOutput();
        setAbsValue(dst, AbsDomainFactory.createAbsDomain(this.domainType, 1), outStateTrue, true);
        setAbsValue(dst, AbsDomainFactory.createAbsDomain(this.domainType, 0), outStateFalse, true);

        Varnode op1 = inst.getInput(0);
        Varnode op2 = inst.getInput(1);
        if(op1.isConstant() && !op2.isConstant()){
            // True: [2,2] < x, x = [1,4], -> [2+1, inf] ^ [1,4] = [3,4]
            Interval op1Tr = (Interval) AbsDomainFactory.createAbsDomain(this.domainType,
                    getTrueValue(op1.getOffset(), op1.getSize())+1, Interval.getPosInf());
            Interval op2Tr = (Interval) getAbsValue(op2, outStateTrue, inst);
            Interval  resTr =  (Interval) op1Tr.intersect(op2Tr);
            setAbsValue(op2, resTr, outStateTrue, true);

            // TODO false [2,2] >= x, x = [1,4], -> [-inf, 2] ^ [1,4] = [1,2]
            Interval op1Fls = (Interval) AbsDomainFactory.createAbsDomain(this.domainType,
                    Interval.getNegInf(), getTrueValue(op1.getOffset(), op1.getSize()));
            Interval op2Fls = (Interval) getAbsValue(op2, outStateFalse, inst);
            Interval  resFls =  (Interval) op1Fls.intersect(op2Fls);
            setAbsValue(op2, resFls, outStateFalse, true);

            GlobalState.ghidraScript.print("                                    INT_SLESS\n");
            GlobalState.ghidraScript.print("                                    True: input "+op1Tr.toString()+"    "+op2Tr.toString()+"   ");
            GlobalState.ghidraScript.print(" output "+(Interval) getAbsValue(op2, outStateTrue, inst)+"\n");
            GlobalState.ghidraScript.print("                                    False: input "+op1Fls.toString()+"    "+op2Fls.toString()+"   ");
            GlobalState.ghidraScript.print(" output "+(Interval) getAbsValue(op2, outStateFalse, inst)+"\n");
            GlobalState.ghidraScript.print("\n");
            printCompareOpTr(inst, op1Tr, op2Tr, (Interval) getAbsValue(op2, outStateTrue, inst));
            printCompareOpFls(inst, op1Fls, op2Fls, (Interval) getAbsValue(op2, outStateFalse, inst));

            return  new Pair<>(outStateFalse, outStateTrue);

        }else if(!op1.isConstant() && op2.isConstant()){
            // True: x < [2,2], x = [1,4], -> [-inf,2-1] ^ [1,4] = [1,1]
            Interval op1Tr = (Interval) getAbsValue(op1, outStateTrue, inst);
            Interval op2Tr = (Interval) AbsDomainFactory.createAbsDomain(this.domainType,
                              Interval.getNegInf(), getTrueValue(op2.getOffset()-1, op2.getSize()));
            Interval  resTr =  (Interval) op1Tr.intersect(op2Tr);
            setAbsValue(op1, resTr, outStateTrue, true);

            // TODO false x >= [2,2]
            Interval op1Fls = (Interval) getAbsValue(op1, outStateFalse, inst);
            Interval op2Fls = (Interval) AbsDomainFactory.createAbsDomain(this.domainType,
                     getTrueValue(op2.getOffset(), op2.getSize()), Interval.getPosInf());
            Interval  resFls =  (Interval) op1Fls.intersect(op2Fls);
            setAbsValue(op1, resFls, outStateFalse, true);

            GlobalState.ghidraScript.print("                                    INT_SLESS\n");
            GlobalState.ghidraScript.print("                                    True: input "+op1Tr.toString()+"    "+op2Tr.toString()+"   ");
            GlobalState.ghidraScript.print(" output "+(Interval) getAbsValue(op1, outStateTrue, inst)+"\n");
            GlobalState.ghidraScript.print("                                    False: input "+op1Fls.toString()+"    "+op2Fls.toString()+"   ");
            GlobalState.ghidraScript.print(" output "+(Interval) getAbsValue(op1, outStateFalse, inst)+"\n");
            GlobalState.ghidraScript.print("\n");
            printCompareOpTr(inst, op1Tr, op2Tr, (Interval) getAbsValue(op2, outStateTrue, inst));
            printCompareOpFls(inst, op1Fls, op2Fls, (Interval) getAbsValue(op2, outStateFalse, inst));

            return  new Pair<>(outStateFalse, outStateTrue);
        }
        return new Pair<>(outStateFalse, outStateTrue);
    }

    // <= for unsigned int
    public Pair<AbsEnv, AbsEnv> interpret_INT_LESSEQUAL(PcodeOp inst, AbsEnv curState){
        // TODO Sign int  (const, 0xffffffff, 4) (finished)
        AbsEnv outStateTrue = new AbsEnv(curState);
        AbsEnv outStateFalse = new AbsEnv(curState);
        Varnode dst = inst.getOutput();
        setAbsValue(dst, AbsDomainFactory.createAbsDomain(this.domainType, 1), outStateTrue, true);
        setAbsValue(dst, AbsDomainFactory.createAbsDomain(this.domainType, 0), outStateFalse, true);

        Varnode op1 = inst.getInput(0);
        Varnode op2 = inst.getInput(1);
        if(op1.isConstant() && !op2.isConstant()){
            // True: [2,2] <= x, x = [1,4], -> [2, inf] ^ [1,4] = [3,4]
            Interval op1Tr = (Interval) AbsDomainFactory.createAbsDomain(this.domainType,
                                op1.getOffset(), Interval.getPosInf());
            Interval op2Tr = (Interval) getAbsValue(op2, outStateTrue, inst);
            Interval  resTr =  (Interval) op1Tr.intersect(op2Tr);
            setAbsValue(op2, resTr, outStateTrue, true);

            // TODO false [2,2] > x, x = [1,4], -> [0, 2-1] ^ [1,4] = [1,2]
            Interval op1Fls = (Interval) AbsDomainFactory.createAbsDomain(this.domainType,
                    0, op1.getOffset()-1);
            Interval op2Fls = (Interval) getAbsValue(op2, outStateFalse, inst);
            Interval resFls =  (Interval) op1Fls.intersect(op2Fls);
            setAbsValue(op2, resFls, outStateFalse, true);

            GlobalState.ghidraScript.print("                                    INT_LESSEQUAL\n");
            GlobalState.ghidraScript.print("                                    True: input "+op1Tr.toString()+"    "+op2Tr.toString()+"   ");
            GlobalState.ghidraScript.print(" output "+(Interval) getAbsValue(op2, outStateTrue, inst)+"\n");
            GlobalState.ghidraScript.print("                                    False: input "+op1Fls.toString()+"    "+op2Fls.toString()+"   ");
            GlobalState.ghidraScript.print(" output "+(Interval) getAbsValue(op2, outStateFalse, inst)+"\n");
            GlobalState.ghidraScript.print("\n");
            printCompareOpTr(inst, op1Tr, op2Tr, (Interval) getAbsValue(op2, outStateTrue, inst));
            printCompareOpFls(inst, op1Fls, op2Fls, (Interval) getAbsValue(op2, outStateFalse, inst));

            return  new Pair<>(outStateFalse, outStateTrue);

        }else if(!op1.isConstant() && op2.isConstant()){
            // True: x <= [2,2], x = [1,4], -> [-inf,2] ^ [1,4] = [1,1]
            Interval op1Tr = (Interval) getAbsValue(op1, outStateTrue, inst);
            Interval op2Tr = (Interval) AbsDomainFactory.createAbsDomain(this.domainType,
                    0, op2.getOffset());
            Interval  resTr =  (Interval) op1Tr.intersect(op2Tr);
            setAbsValue(op1, resTr, outStateTrue, true);

            // TODO false x > [2,2], x = [1,4], -> [2+1, inf] ^ [1,4] = [1,1]
            Interval op1Fls = (Interval) getAbsValue(op1, outStateFalse, inst);
            Interval op2Fls = (Interval) AbsDomainFactory.createAbsDomain(this.domainType,
                    op2.getOffset()+1, Interval.getPosInf());
            Interval  resFls =  (Interval) op1Fls.intersect(op2Fls);
            setAbsValue(op1, resFls, outStateFalse, true);

            GlobalState.ghidraScript.print("                                    INT_LESSEQUAL\n");
            GlobalState.ghidraScript.print("                                    True: input "+op1Tr.toString()+"    "+op2Tr.toString()+"   ");
            GlobalState.ghidraScript.print(" output "+(Interval) getAbsValue(op1, outStateTrue, inst)+"\n");
            GlobalState.ghidraScript.print("                                    False: input "+op1Fls.toString()+"    "+op2Fls.toString()+"   ");
            GlobalState.ghidraScript.print(" output "+(Interval) getAbsValue(op1, outStateFalse, inst)+"\n");
            GlobalState.ghidraScript.print("\n");
            printCompareOpTr(inst, op1Tr, op2Tr, (Interval) getAbsValue(op2, outStateTrue, inst));
            printCompareOpFls(inst, op1Fls, op2Fls, (Interval) getAbsValue(op2, outStateFalse, inst));

            return  new Pair<>(outStateFalse, outStateTrue);
        }
        return new Pair<>(outStateFalse, outStateTrue);
    }

    public Pair<AbsEnv, AbsEnv> interpret_INT_SLESSEQUAL(PcodeOp inst, AbsEnv curState){
        // TODO Sign int  (const, 0xffffffff, 4) (finished)
        AbsEnv outStateTrue = new AbsEnv(curState);
        AbsEnv outStateFalse = new AbsEnv(curState);
        Varnode dst = inst.getOutput();
        setAbsValue(dst, AbsDomainFactory.createAbsDomain(this.domainType, 1), outStateTrue, true);
        setAbsValue(dst, AbsDomainFactory.createAbsDomain(this.domainType, 0), outStateFalse, true);

        Varnode op1 = inst.getInput(0);
        Varnode op2 = inst.getInput(1);
        if(op1.isConstant() && !op2.isConstant()){
            // True: [2,2] <= x, x = [1,4], -> [2, inf] ^ [1,4] = [3,4]
            Interval op1Tr = (Interval) AbsDomainFactory.createAbsDomain(this.domainType,
                    getTrueValue(op1.getOffset(), op1.getSize()), Interval.getPosInf());
            Interval op2Tr = (Interval) getAbsValue(op2, outStateTrue, inst);
            Interval  resTr =  (Interval) op1Tr.intersect(op2Tr);
            setAbsValue(op2, resTr, outStateTrue, true);

            // TODO false [2,2] > x, x = [1,4], -> [-inf, 2-1] ^ [1,4] = [1,2]
            Interval op1Fls = (Interval) AbsDomainFactory.createAbsDomain(this.domainType,
                    Interval.getNegInf(), getTrueValue(op1.getOffset()-1, op1.getSize()));
            Interval op2Fls = (Interval) getAbsValue(op2, outStateFalse, inst);
            Interval  resFls =  (Interval) op1Fls.intersect(op2Fls);
            setAbsValue(op2, resFls, outStateFalse, true);

            GlobalState.ghidraScript.print("                                    INT_SLESSEQUAL\n");
            GlobalState.ghidraScript.print("                                    True: input "+op1Tr.toString()+"    "+op2Tr.toString()+"   ");
            GlobalState.ghidraScript.print(" output "+(Interval) getAbsValue(op2, outStateTrue, inst)+"\n");
            GlobalState.ghidraScript.print("                                    False: input "+op1Fls.toString()+"    "+op2Fls.toString()+"   ");
            GlobalState.ghidraScript.print(" output "+(Interval) getAbsValue(op2, outStateFalse, inst)+"\n");
            GlobalState.ghidraScript.print("\n");
            printCompareOpTr(inst, op1Tr, op2Tr, (Interval) getAbsValue(op2, outStateTrue, inst));
            printCompareOpFls(inst, op1Fls, op2Fls, (Interval) getAbsValue(op2, outStateFalse, inst));

            return  new Pair<>(outStateFalse, outStateTrue);

        }else if(!op1.isConstant() && op2.isConstant()){
            // True: x <= [2,2], x = [1,4], -> [-inf,2] ^ [1,4] = [1,1]
            Interval op1Tr = (Interval) getAbsValue(op1, outStateTrue, inst);
            Interval op2Tr = (Interval) AbsDomainFactory.createAbsDomain(this.domainType,
                    Interval.getNegInf(), getTrueValue(op2.getOffset(), op2.getSize()));
            Interval  resTr =  (Interval) op1Tr.intersect(op2Tr);
            setAbsValue(op1, resTr, outStateTrue, true);

            // TODO false x > [2,2], x = [1,4], -> [2+1, inf] ^ [1,4] = [1,1]
            Interval op1Fls = (Interval) getAbsValue(op1, outStateFalse, inst);
            Interval op2Fls = (Interval) AbsDomainFactory.createAbsDomain(this.domainType,
                    getTrueValue(op2.getOffset()+1, op2.getSize()), Interval.getPosInf());
            Interval  resFls =  (Interval) op1Fls.intersect(op2Fls);
            setAbsValue(op1, resFls, outStateFalse, true);

            GlobalState.ghidraScript.print("                                    INT_SLESSEQUAL\n");
            GlobalState.ghidraScript.print("                                    True: input "+op1Tr.toString()+"    "+op2Tr.toString()+"   ");
            GlobalState.ghidraScript.print(" output "+(Interval) getAbsValue(op1, outStateTrue, inst)+"\n");
            GlobalState.ghidraScript.print("                                    False: input "+op1Fls.toString()+"    "+op2Fls.toString()+"   ");
            GlobalState.ghidraScript.print(" output "+(Interval) getAbsValue(op1, outStateFalse, inst)+"\n");
            GlobalState.ghidraScript.print("\n");
            printCompareOpTr(inst, op1Tr, op2Tr, (Interval) getAbsValue(op2, outStateTrue, inst));
            printCompareOpFls(inst, op1Fls, op2Fls, (Interval) getAbsValue(op2, outStateFalse, inst));

            return  new Pair<>(outStateFalse, outStateTrue);
        }
        return new Pair<>(outStateFalse, outStateTrue);
    }


    // arithmetic negation operation [-5, 3] -> [-3, 5]
    public AbsEnv interpret_INT_2COMP(PcodeOp inst, AbsEnv curState){
        AbsEnv outState = new AbsEnv(curState);

        Varnode op1 = inst.getInput(0);
        Varnode dst = inst.getOutput();

        Interval op1Interval = (Interval) getAbsValue(op1, curState, inst);
        Interval resInterval = op1Interval.neg();
        setAbsValue(dst, resInterval, outState, true);

        printUnaryOp(inst, op1Interval, resInterval);
        return outState;
    }

    // negation operation [-1, 2] -> [-3, 0]
    //                    111111, 000010  -> 1111101, 000000
    public AbsEnv interpret_INT_NEGATE(PcodeOp inst, AbsEnv curState){
        AbsEnv outState = new AbsEnv(curState);

        Varnode op1 = inst.getInput(0);
        Varnode dst = inst.getOutput();

        Interval op1Interval = (Interval) getAbsValue(op1, curState, inst);
        Interval resInterval = op1Interval.neg_bit();

        setAbsValue(dst, resInterval, outState, true);

        printUnaryOp(inst, op1Interval, resInterval);
        return outState;
    }

    public AbsEnv interpret_INT_LEFT(PcodeOp inst, AbsEnv curState){
        AbsEnv outState = new AbsEnv(curState);

        Varnode op1 = inst.getInput(0);
        Varnode op2 = inst.getInput(1);
        Varnode dst = inst.getOutput();

        Interval op1Interval = (Interval) getAbsValue(op1, curState, inst);
        Interval op2Interval = (Interval) getAbsValue(op2, curState, inst);
        Interval resInterval = op1Interval.shl(op2Interval);
        setAbsValue(dst, resInterval, outState, true);

        printBinOp(inst, op1Interval, op2Interval, resInterval);
        return outState;
    }

    public AbsEnv interpret_INT_RIGHT(PcodeOp inst, AbsEnv curState){
        AbsEnv outState = new AbsEnv(curState);

        Varnode op1 = inst.getInput(0);
        Varnode op2 = inst.getInput(1);
        Varnode dst = inst.getOutput();

        Interval op1Interval = (Interval) getAbsValue(op1, curState, inst);
        Interval op2Interval = (Interval) getAbsValue(op2, curState, inst);
        Interval resInterval = op1Interval.lshr(op2Interval);
        setAbsValue(dst, resInterval, outState, true);

        printBinOp(inst, op1Interval, op2Interval, resInterval);
        return outState;
    }

    public AbsEnv interpret_INT_SRIGHT(PcodeOp inst, AbsEnv curState){
        AbsEnv outState = new AbsEnv(curState);

        Varnode op1 = inst.getInput(0);
        Varnode op2 = inst.getInput(1);
        Varnode dst = inst.getOutput();

        Interval op1Interval = (Interval) getAbsValue(op1, curState, inst);
        Interval op2Interval = (Interval) getAbsValue(op2, curState, inst);
        Interval resInterval = op1Interval.shr(op2Interval);
        setAbsValue(dst, resInterval, outState, true);

        printBinOp(inst, op1Interval, op2Interval, resInterval);
        return outState;
    }

    public AbsEnv interpret_BRANCH(PcodeOp inst, AbsEnv curState){
        return null;
    }

    public AbsEnv interpret_IBRANCH(PcodeOp inst, AbsEnv curState){
        return null;
    }

    public AbsEnv interpret_LOAD(PcodeOp inst, AbsEnv curState) {
        AbsEnv outState = new AbsEnv(curState);

        Varnode addressSpaceId = inst.getInput(0);
        assert addressSpaceId.isConstant();

        Varnode src = inst.getInput(1);
        Varnode dst = inst.getOutput();

        Interval srcPtrItvl = (Interval) getAbsValue(src, outState, inst);
        Interval newSrcItvl = (Interval) AbsDomainFactory.createAbsDomain(domainType);


        if (srcPtrItvl.lowerIsTop() || srcPtrItvl.upperIsTop()) {
            setAbsValue(dst, AbsDomainFactory.createAbsDomain(this.domainType, true), outState, true);
            return outState;
        }

        if(srcPtrItvl.isSingle()){
            ALoc aloc = ALoc.getALoc(Global.getInstance(), srcPtrItvl.getLower(), 8);
            Interval srcItvl = (Interval) outState.get(aloc);
            setAbsValue(dst, srcItvl, outState, true);

            if(GlobalState.debug){
                GlobalState.ghidraScript.print("                                    LOAD\n");
                GlobalState.ghidraScript.print("                                    inputput addr:"+getAbsValue(src,outState,inst).toString()+
                        "     value:"+outState.get(ALoc.getALoc(Global.getInstance(), srcPtrItvl.getLower(), 8)).toString()+"\n");
                GlobalState.ghidraScript.print("                                    output "+getAbsValue(dst, outState, inst)+"\n");
                GlobalState.ghidraScript.print("\n");
            }
        }
        else{
            long lowerPtr = srcPtrItvl.getLower();
            long upperPtr = srcPtrItvl.getUpper();
            setAbsValue(dst, AbsDomainFactory.createAbsDomain(this.domainType, true), outState, true);
        }

        return outState;
    }

    public AbsEnv interpret_STORE(PcodeOp inst, AbsEnv curState) {
        AbsEnv outState = new AbsEnv(curState);

        Varnode addressSpaceId = inst.getInput(0);
//        assert addressSpaceId.isConstant();

        Varnode dst = inst.getInput(1);
        Varnode src = inst.getInput(2);

        Interval srcItvl = (Interval) getAbsValue(src, outState, inst);
        Interval dstPtrItvl = (Interval) getAbsValue(dst, outState, inst);

//        GlobalState.ghidraScript.print(inst+"\n");

        if (dstPtrItvl.isTop()) {
            return outState;
        }

        if(dstPtrItvl.isSingle()){
            ALoc aloc = ALoc.getALoc(Global.getInstance(), dstPtrItvl.getLower(), 8);
            outState.set(aloc, srcItvl, true);

            if(GlobalState.debug){
                GlobalState.ghidraScript.print("                                    STORE\n");
                GlobalState.ghidraScript.print("                                    input "+getAbsValue(src, outState, inst)+"\n");
                GlobalState.ghidraScript.print("                                    output addr:"+getAbsValue(dst,outState,inst).toString()+
                        "     value:"+outState.get(ALoc.getALoc(Global.getInstance(), dstPtrItvl.getLower(), 8)).toString()+"\n");
                GlobalState.ghidraScript.print("\n");
            }
        }
        else{
            long lowerPtr = dstPtrItvl.getLower();
            long upperPtr = dstPtrItvl.getUpper();
            setAbsValue(dst, AbsDomainFactory.createAbsDomain(this.domainType, true), outState, true);
        }

        return outState;
    }

    //TODO access uninitialized mem
    public AbsEnv interpret_COPY(PcodeOp inst, AbsEnv curState){
        AbsEnv outState = new AbsEnv(curState);

        Varnode src = inst.getInput(0);
        Varnode dst = inst.getOutput();

        Interval srcInterval = (Interval) getAbsValue(src, curState, inst);
        setAbsValue(dst, srcInterval, outState, true);

        printUnaryOp(inst, srcInterval, getAbsValue(dst,outState,inst));

        return outState;
    }

    public AbsEnv interpret_INT_ADD(PcodeOp inst, AbsEnv curState){
        AbsEnv outState = new AbsEnv(curState);

        Varnode op1 = inst.getInput(0);
        Varnode op2 = inst.getInput(1);
        Varnode dst = inst.getOutput();

        Interval op1Interval = (Interval) getAbsValue(op1, curState, inst);
        Interval op2Interval = (Interval) getAbsValue(op2, curState, inst);
        Interval resInterval = op1Interval.add(op2Interval);
        setAbsValue(dst, resInterval, outState, true);

        printBinOp(inst, op1Interval, op2Interval, resInterval);
        return outState;
    }

    public AbsEnv interpret_INT_SUB(PcodeOp inst, AbsEnv curState){
        AbsEnv outState = new AbsEnv(curState);

        Varnode op1 = inst.getInput(0);
        Varnode op2 = inst.getInput(1);
        Varnode dst = inst.getOutput();

        Interval op1Interval = (Interval) getAbsValue(op1, curState, inst);
        Interval op2Interval = (Interval) getAbsValue(op2, curState, inst);
        Interval resInterval = op1Interval.sub(op2Interval);
        setAbsValue(dst, resInterval, outState, true);

        printBinOp(inst, op1Interval, op2Interval, resInterval);
        return outState;
    }

    public AbsEnv interpret_INT_MULT(PcodeOp inst, AbsEnv curState){
        AbsEnv outState = new AbsEnv(curState);

        Varnode op1 = inst.getInput(0);
        Varnode op2 = inst.getInput(1);
        Varnode dst = inst.getOutput();

        Interval op1Interval = (Interval) getAbsValue(op1, curState, inst);
        Interval op2Interval = (Interval) getAbsValue(op2, curState, inst);
        Interval resInterval = op1Interval.mul(op2Interval);
        setAbsValue(dst, resInterval, outState, true);

//        GlobalState.ghidraScript.print(inst+"\n");
        printBinOp(inst, op1Interval, op2Interval, resInterval);
        return outState;
    }

    public AbsEnv interpret_INT_DIV(PcodeOp inst, AbsEnv curState){
        AbsEnv outState = new AbsEnv(curState);

        Varnode op1 = inst.getInput(0);
        Varnode op2 = inst.getInput(1);
        Varnode dst = inst.getOutput();

        Interval op1Interval = (Interval) getAbsValue(op1, curState, inst);
        Interval op2Interval = (Interval) getAbsValue(op2, curState, inst);
        Interval resInterval = op1Interval.div(op2Interval);
        setAbsValue(dst, resInterval, outState, true);

        printBinOp(inst, op1Interval, op2Interval, resInterval);
        return outState;
    }

    public AbsEnv interpret_FLOAT_LESS(PcodeOp inst, AbsEnv curState){
        AbsEnv outState = new AbsEnv(curState);

        Varnode op1 = inst.getInput(0);
        Varnode op2 = inst.getInput(1);
        Varnode dst = inst.getOutput();

        if(op2.isConstant()){
            GlobalState.ghidraScript.print(" float       "+op1.getHigh().getDataType()+"\n");
            GlobalState.ghidraScript.print(" float1       "+op2.getHigh().getDataType()+"\n");

            float floatValue = Float.intBitsToFloat((int) op2.getOffset());
            GlobalState.ghidraScript.print(" float2       "+floatValue+"\n");

        }


        return outState;
    }
    public AbsEnv interpret_FLOAT_SUB(PcodeOp inst, AbsEnv curState){
        AbsEnv outState = new AbsEnv(curState);

        Varnode op1 = inst.getInput(0);
        Varnode op2 = inst.getInput(1);
        Varnode dst = inst.getOutput();

        if(op2.isConstant()){
            GlobalState.ghidraScript.print(" float       "+op1.getHigh().getDataType()+"\n");
            GlobalState.ghidraScript.print(" float       "+op2.getHigh().getDataType()+"\n");
        }

        return outState;
    }

    public AbsEnv interpret_BOOL_NEGATE(PcodeOp inst, AbsEnv curState){
        AbsEnv outState = new AbsEnv(curState);

        Varnode op1 = inst.getInput(0);
        Varnode dst = inst.getOutput();

        Interval op1Interval = (Interval) getAbsValue(op1, curState, inst);
        Interval resInterval;

        if(op1Interval.isBoolean() && op1Interval.isSingle()){
            resInterval = (Interval) AbsDomainFactory.createAbsDomain(this.domainType, (~op1Interval.getLower()) & 1);
        } else {
            resInterval = (Interval) AbsDomainFactory.createAbsDomain(this.domainType, 0,1);
        }

//        if(op1Interval.getLower()==1L && op1Interval.getUpper()==1L){
//            resInterval = (Interval) AbsDomainFactory.createAbsDomain(this.domainType, 0);
//        } else if (op1Interval.getLower()==0L && op1Interval.getUpper()==0L) {
//            resInterval = (Interval) AbsDomainFactory.createAbsDomain(this.domainType, 1);
//        } else {
//            resInterval = (Interval) AbsDomainFactory.createAbsDomain(this.domainType, 0,1);
//        }

        setAbsValue(dst, resInterval, outState, true);
        printUnaryOp(inst, op1Interval, resInterval);

        return outState;
    }

    public AbsEnv interpret_BOOL_XOR(PcodeOp inst, AbsEnv curState){
        AbsEnv outState = new AbsEnv(curState);

        Varnode op1 = inst.getInput(0);
        Varnode op2 = inst.getInput(1);
        Varnode dst = inst.getOutput();

        Interval op1Interval = (Interval) getAbsValue(op1, curState, inst);
        Interval op2Interval = (Interval) getAbsValue(op2, curState, inst);
        Interval resInterval;

        if(op1Interval.isBoolean() && op1Interval.isSingle() && op1Interval.isBoolean() && op1Interval.isSingle()){
            resInterval = (Interval) AbsDomainFactory.createAbsDomain(this.domainType,
                    (op1Interval.getLower() & 1) ^ (op2Interval.getLower() & 1) );
        } else {
            resInterval = (Interval) AbsDomainFactory.createAbsDomain(this.domainType, 0,1);
        }

        setAbsValue(dst, resInterval, outState, true);
        printBinOp(inst, op1Interval, op2Interval, resInterval);
        return outState;
    }

    public AbsEnv interpret_BOOL_AND(PcodeOp inst, AbsEnv curState){
        AbsEnv outState = new AbsEnv(curState);

        Varnode op1 = inst.getInput(0);
        Varnode op2 = inst.getInput(1);
        Varnode dst = inst.getOutput();

        Interval op1Interval = (Interval) getAbsValue(op1, curState, inst);
        Interval op2Interval = (Interval) getAbsValue(op2, curState, inst);
        Interval resInterval;

        if(op1Interval.isBoolean() && op1Interval.isSingle() && op1Interval.isBoolean() && op1Interval.isSingle()){
            resInterval = (Interval) AbsDomainFactory.createAbsDomain(this.domainType,
                    (op1Interval.getLower() & 1) & (op2Interval.getLower() & 1) );
        } else {
            resInterval = (Interval) AbsDomainFactory.createAbsDomain(this.domainType, 0,1);
        }

        setAbsValue(dst, resInterval, outState, true);
        printBinOp(inst, op1Interval, op2Interval, resInterval);
        return outState;
    }

    public AbsEnv interpret_BOOL_OR(PcodeOp inst, AbsEnv curState){
        AbsEnv outState = new AbsEnv(curState);

        Varnode op1 = inst.getInput(0);
        Varnode op2 = inst.getInput(1);
        Varnode dst = inst.getOutput();

        Interval op1Interval = (Interval) getAbsValue(op1, curState, inst);
        Interval op2Interval = (Interval) getAbsValue(op2, curState, inst);
        Interval resInterval;

        if(op1Interval.isBoolean() && op1Interval.isSingle() && op1Interval.isBoolean() && op1Interval.isSingle()){
            resInterval = (Interval) AbsDomainFactory.createAbsDomain(this.domainType,
                    (op1Interval.getLower() & 1) | (op2Interval.getLower() & 1) );
        } else {
            resInterval = (Interval) AbsDomainFactory.createAbsDomain(this.domainType, 0,1);
        }

        setAbsValue(dst, resInterval, outState, true);
        printBinOp(inst, op1Interval, op2Interval, resInterval);
        return outState;
    }


    //    public Interval[] binaryOP(PcodeOp inst, AbsEnv curState){
//        VarnodeAST input1 = (VarnodeAST) inst.getInput(0);
//        VarnodeAST input2 = (VarnodeAST) inst.getInput(1);
//        Interval interval1 = curState.getInterval(input1);
//        Interval interval2 = curState.getInterval(input2);
//        return new Interval[]{interval1, interval2};
//    }
//    public void store_abs(PcodeOp inst, AbsEnv curState){
//        Varnode addressSpaceId = inst.getInput(0);
//        assert addressSpaceId.isConstant();
//
//        VarnodeAST dst_ptr = (VarnodeAST) inst.getInput(1);
//        VarnodeAST src = (VarnodeAST) inst.getInput(2);
//
//        Interval interval_dst_addr = curState.getInterval(dst_ptr);
//        Interval interval_src = curState.getInterval(src);
//
//        if(!interval_dst_addr.isSingle()){
//            return;
//        }
//
//    }


}
