package com.bai.env.semantic;

import com.bai.env.ALoc;
import com.bai.env.AbsEnv;
import com.bai.env.domain.AbsDomain;
import com.bai.env.domain.AbsDomainFactory;
import com.bai.env.domain.Interval;
import com.bai.env.region.Global;
import com.bai.env.region.Reg;
import com.bai.util.GlobalState;
import ghidra.program.model.address.Address;
import ghidra.program.model.lang.Register;
import ghidra.program.model.pcode.HighFunction;
import ghidra.program.model.pcode.PcodeOp;
import ghidra.program.model.pcode.Varnode;
import ghidra.program.model.pcode.VarnodeAST;

public abstract class Interpreter {
    protected String domainType;
    public abstract AbsEnv transferFunction(PcodeOp inst, AbsEnv curState);
    public abstract AbsEnv initParas(HighFunction hfunc);


//    private AbsDomain loadPtr(AbsVal ptr, AbsDomain inKSet, AbsEnv absEnv, Varnode dst) {
//        ALoc aLoc = ALoc.getALoc(ptr.getRegion(), ptr.getValue(), dst.getSize());
//        AbsDomain srcKSet = absEnv.get(aLoc);
//        AbsDomain outKSet = inKSet.join(srcKSet);
//        return (outKSet == null) ? inKSet : outKSet;
//    }
//
//
//    private void storePtr(AbsVal ptr, AbsDomain srcKSet, AbsEnv inOutEnv, AbsEnv tmpEnv, Varnode src) {
//        ALoc aLoc;
//        if (srcKSet.isTop()) {
//            aLoc = ALoc.getALoc(ptr.getRegion(), ptr.getValue(), src.getSize());
//        } else {
//            // NOTE : set the size as srcKSet does not follow pcode document,
//            //  but can avoid leading '00' and improve precision.
//            aLoc = ALoc.getALoc(ptr.getRegion(), ptr.getValue(), srcKSet.getBits() / 8);
//        }
//        AbsEnv env;
//        if (ptr.getRegion().isUnique() || aLoc.isPC()) {
//            env = tmpEnv;
//        } else {
//            env = inOutEnv;
//        }
//        env.set(aLoc, srcKSet, true);
//    }

    /**
     * Get the KSet of pc register.
     * @param currentAddress address of current instruction.
     * @return the KSet.
     */
    public AbsDomain getPcKSet(Address currentAddress) {
        long pcValue;
        AbsDomain pcKSet = AbsDomainFactory.createAbsDomain(domainType);
        switch (GlobalState.arch.getProcessor()) {
            case "ARM":
                Register tMode = GlobalState.currentProgram.getProgramContext().getRegister("TMode");
                boolean isThumb = GlobalState.currentProgram.getProgramContext().getRegisterValue(tMode, currentAddress)
                        .getUnsignedValue().testBit(0);
                if (isThumb) {
                    pcValue = currentAddress.getOffset() & 0xFFFFFFFCL + 4;
                } else {
                    pcValue = currentAddress.getOffset() + 8;
                }
                break;
            case "AARCH64":
                pcValue = currentAddress.getOffset();
                break;
            case "x86":
                pcValue = GlobalState.flatAPI.getInstructionAfter(currentAddress).getAddress().getOffset();
                break;
            default:
                GlobalState.ghidraScript.print("getPCKSet(): unsupported architecture");
                return pcKSet;
        }
        pcKSet = pcKSet.join(AbsDomainFactory.createAbsDomain(domainType, pcValue));
        return pcKSet;
    }

    protected AbsDomain getPCAbsValue(PcodeOp pcode, AbsEnv tmpEnv) {
        ALoc pcALoc = ALoc.getALoc(
                Reg.getInstance(), GlobalState.arch.getPcIndex(), GlobalState.arch.getDefaultPointerSize());
        AbsDomain pcKSet = tmpEnv.get(pcALoc);
        if (pcKSet.isBottom()) {
            Address currentAddress = pcode.getSeqnum().getTarget();
            return getPcKSet(currentAddress);
        }
        return pcKSet;
    }
    public static long getTrueValue(long value, int sizeInBytes) {
        int sizeInBits = sizeInBytes * 8; // 转换为位数
        long mask = (1L << sizeInBits) - 1; // 根据位数生成掩码
        long signedValue = value & mask; // 应用掩码来获取补码表示的值

        // 如果补码表示的值为负数（最高位为1），则需要将其转换为真值
        if ((signedValue & (1L << (sizeInBits - 1))) != 0) {
            signedValue = signedValue - (1L << sizeInBits);
        }

        return signedValue;
    }
    protected AbsDomain getAbsValue(Varnode src, AbsEnv inOutEnv, PcodeOp pcode) {
        if (src.isConstant()) {
            return AbsDomainFactory.createAbsDomain(this.domainType, getTrueValue(src.getOffset(),src.getSize()));
            //TODO unsigned int
        }
//        GlobalState.ghidraScript.printf("%d hahah\n",GlobalState.arch.getPcIndex());
        // TODO fix this
//        if (src.isRegister() && src.getOffset() == GlobalState.arch.getPcIndex()) {
//            return getPCAbsValue(pcode, inOutEnv);
//        }
        ALoc srcALoc = ALoc.getALoc(src);
        if (src.isUnique()) {
            return inOutEnv.get(srcALoc);
        }
//        if(srcALoc ==null){
//            GlobalState.ghidraScript.print("srcALoc null error!!!!\n");
//        }
//        if (inOutEnv.get(srcALoc)==null){
//            GlobalState.ghidraScript.print("null error!!!!\n");
//        }
        return inOutEnv.get(srcALoc);
    }
    protected AbsDomain addr2AbsValue(String addr, AbsEnv inOutEnv){
        Address addr_ram = GlobalState.currentProgram.getAddressFactory().getAddress(addr);
        return null;
    }
    protected void setAbsValue(Varnode dst, AbsDomain srcKSet, AbsEnv inOutEnv, boolean isStrongUpdate) {
        ALoc dstALoc = ALoc.getALoc(dst);
        // dst can not be constant
        if (dst.isUnique() || dstALoc.isPC()) {
            inOutEnv.set(dstALoc, srcKSet, isStrongUpdate);
        } else {
            inOutEnv.set(dstALoc, srcKSet, isStrongUpdate);
        }
    }

    protected void printUnaryOp(PcodeOp inst, AbsDomain op1, AbsDomain res){
        if(GlobalState.debug){
            GlobalState.ghidraScript.print("                                                    "+inst.getMnemonic()+"   ");
            GlobalState.ghidraScript.print("             input: "+op1);
//            GlobalState.ghidraScript.print("                                    output: "+res.toString()+"\n");
            GlobalState.ghidraScript.print("             output: "+res+"\n");
        }

    }

    // printBinOp(inst, op1Interval, op2Interval, resInterval);
    protected void printBinOp(PcodeOp inst, AbsDomain op1, AbsDomain op2, AbsDomain res){
        if(GlobalState.debug){
            GlobalState.ghidraScript.print("                                                    "+inst.getMnemonic()+"   ");
//            GlobalState.ghidraScript.print("                                    input: "+op1.toString()+"    "+op2.toString());
//            GlobalState.ghidraScript.print("                                    output: "+res+"\n");
            GlobalState.ghidraScript.print("             input: "+op1+"    "+op2);
            GlobalState.ghidraScript.print("             output: "+res+"\n");
        }
    }

    protected void printCompareOpTr(PcodeOp inst, AbsDomain op1Tr, AbsDomain op2Tr, AbsDomain res){
        if(GlobalState.debug){
            GlobalState.ghidraScript.print("                                                    "+inst.getMnemonic()+"   ");
            GlobalState.ghidraScript.print("            True: "+op1Tr.toString()+"    "+op2Tr.toString()+"   ");
            GlobalState.ghidraScript.print("            output "+res+"\n");
        }
    }
    protected void printCompareOpFls(PcodeOp inst, AbsDomain op1, AbsDomain op2, AbsDomain res){
        if(GlobalState.debug){
            GlobalState.ghidraScript.print("                                                    "+inst.getMnemonic()+"   ");
            GlobalState.ghidraScript.print("            False: "+op1.toString()+"  ^  "+op2.toString()+"   ");
            GlobalState.ghidraScript.print("            output "+res+"\n");
        }
    }

//    protected void printMemAccess(PcodeOp inst, AbsDomain inputaddr, AbsDomain op2, AbsDomain res){
//        if(GlobalState.debug){
//            GlobalState.ghidraScript.print("                                    "+inst.getMnemonic()+"\n");
//            GlobalState.ghidraScript.print("                                    input: "+op1.toString()+"    "+op2.toString());
//            GlobalState.ghidraScript.print("                                    output: "+res+"\n");
//            GlobalState.ghidraScript.print("                                    inputput addr:"+getAbsValue(src,outState,inst).toString()+
//                    "     value:"+outState.get(ALoc.getALoc(Global.getInstance(), srcPtrItvl.getLower(), 8)).toString()+"\n");
//            GlobalState.ghidraScript.print("                                    output "+getAbsValue(dst, outState, inst)+"\n");
//            GlobalState.ghidraScript.print("\n");
//        }
//    }


//    private AbsDomain getAbsValue(VarnodeAST src, AbsEnv inOutEnv, AbsEnv tmpEnv, PcodeOp pcode) {
//        if (src.isConstant()) {
//            return AbsDomainFactory.createAbsDomain(this.domainType, src.getOffset());
//        }
//        if (src.isRegister() && src.getOffset() == GlobalState.arch.getPcIndex()) {
//            return getPCAbsValue(pcode, tmpEnv);
//        }
//        ALoc srcALoc = ALoc.getALoc(src);
//        if (src.isUnique()) {
//            return tmpEnv.get(srcALoc);
//        }
//        return inOutEnv.get(srcALoc);
//    }
//    private void setAbsValue(Varnode dst, AbsDomain srcKSet, AbsEnv inOutEnv, AbsEnv tmpEnv, boolean isStrongUpdate) {
//        ALoc dstALoc = ALoc.getALoc(dst);
//        if (dst.isUnique() || dstALoc.isPC()) {
//            tmpEnv.set(dstALoc, srcKSet, isStrongUpdate);
//        } else {
//            inOutEnv.set(dstALoc, srcKSet, isStrongUpdate);
//        }
//    }
}
