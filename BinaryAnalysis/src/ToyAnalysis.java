
//import com.bai.analysis.Analysis;

import ghidra.program.model.address.Address;
import ghidra.program.model.data.Pointer;
import ghidra.program.model.listing.Data;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.FunctionIterator;
import ghidra.program.model.pcode.*;

import java.util.ArrayList;
import java.util.Iterator;

public class ToyAnalysis extends Analysis{
    //    private DecompInterface decomplib;
//    private Language language;
    public boolean isString(Varnode op){
//        assert op.isConstant();
//        Data data = getDataContaining(op.getAddress());
//        if(data!=null && data.hasStringValue()){
//            printf("                ------------------------------------------------------------%s\n", data.getValue().toString());
//            return true;
//        }
//        return false;
        Address addr_const = op.getAddress();
        String addressString = addr_const.toString(false);
        Address addr_ram = currentProgram.getAddressFactory().getAddress(addressString);
//        printf("                address %s", op.getAddress());
        Data data = getDataContaining(addr_ram);
        if(data!=null && data.hasStringValue()){
            printf("                --------string-------%s\n", data.getValue().toString());
            return true;
        } else if (data!=null && data.isConstant()) {
            printf("                --------constant-------%s\n", data.getValue().toString());
            return true;
        } else if (data!=null && data.isPointer()) {
            printf("                --------pointer-------%s\n", data.getValue().toString());
            return true;
        } else if (data!=null && data.isArray()) {
            printf("                --------Array-------%s\n", data.getPathName());
            return true;
        } else if (data!=null && data.isDefined()) {
            printf("                --------Defined-------%s\n", data.getValue().toString());
            return true;
        } else if (data!=null && data.isDynamic()) {
            printf("                --------Dynamic-------%s\n", data.getValue().toString());
            return true;
        }
        return false;
    }
    public void getParams(HighFunction hfunc){
        for (int i = 0; i < hfunc.getFunctionPrototype().getNumParams(); ++i) {
            HighSymbol paramSym = hfunc.getFunctionPrototype().getParam(i);
            String paramName = paramSym.getName();
            print(paramName+"\n");
            HighVariable highVariable = paramSym.getHighVariable();

            if(highVariable != null){
                if(highVariable.getDataType() instanceof Pointer){
                    print("pointer\n");
                }
                Varnode param = paramSym.getHighVariable().getRepresentative();
                print(param.toString()+"   "+param.toString(language)+"\n");
            }else print("(not used param)\n");
        }
    }

    public FunctionIterator getFuncs(){
        FunctionIterator functionManager = this.currentProgram.getFunctionManager().getFunctions(true);
        return functionManager;
    }
    public void iterFuncs(FunctionIterator functionManager){
        for (Function func : functionManager) {
            HighFunction hfunc = decompileFunc(func);
            if (hfunc!=null && hfunc.getFunctionPrototype()!=null){
                int paraNum = hfunc.getFunctionPrototype().getNumParams();
                if(paraNum>7)print(func.getName()+"\n");
            }
        }
    }
    public void outputFuncUsingParaInStack(){
        FunctionIterator iter = getFuncs();
        iterFuncs(iter);
    }

    // is parament
    public boolean isInput(Varnode varnode){
        return varnode.isInput();
    }

    public static long convertToTrueValue(long value, int sizeInBytes) {
        int sizeInBits = sizeInBytes * 8; // 转换为位数
        long mask = (1L << sizeInBits) - 1; // 根据位数生成掩码
        long signedValue = value & mask; // 应用掩码来获取补码表示的值

        // 如果补码表示的值为负数（最高位为1），则需要将其转换为真值
        if ((signedValue & (1L << (sizeInBits - 1))) != 0) {
            signedValue = signedValue - (1L << sizeInBits);
        }

        return signedValue;
    }


    @Override
    public void solver(HighFunction hfunc) {
        ArrayList<PcodeBlockBasic> bbs = hfunc.getBasicBlocks();
        for (PcodeBlockBasic bb: bbs){
            print("bb  "+bb.toString()+"\n");
            for (int i = 0; i < bb.getInSize(); i++) {
                if( Analysis.isConditional((PcodeBlockBasic) bb.getIn(i)) && ((PcodeBlockBasic) bb.getIn(i)).getTrueOut() == bb){
                    print("haha " +bb.getIn(i).toString() +"   "+bb.toString()+"\n");
                }
            }

//            print(bb.getFalseOut().toString()+"\n");
//            print(Analysis.getLastInst(bb).getMnemonic()+"\n");
            String last = Analysis.getLastInst(bb).getMnemonic();
            if(last.equals("CBRANCH")){
                print(last+"\n");
                print("true "+bb.getTrueOut().toString()+"   ");
                print("false "+bb.getFalseOut().toString()+"\n");
            }
            Iterator<PcodeOp> instIter = bb.getIterator();
            while (instIter.hasNext()) {
                PcodeOp inst = instIter.next();
                print(inst.toString()+"\n");
//                print(inst.toString()+"\ninput\n");
                int innum = inst.getNumInputs();
                for (int i = 0; i < innum; i++) {


                    if(inst.getInput(i).isConstant()) {
                        print("     constant");
                        print(inst.getInput(i).toString()+"\n");
                        print(inst.getInput(i).getSize()+"\n");
                        print(convertToTrueValue(inst.getInput(i).getOffset(), inst.getInput(i).getSize())+"\n");

//                        print("     "+inst.getInput(i).getAddress().toString()+"\n");
//                        isString(inst.getInput(i));
                    }
//                    } else if (inst.getInput(i).isAddress()) {
//                        print("     address\n");
//                    } else if (inst.getInput(i).isUnique()) {
//                        print("     Unique\n");
//                    } else if (inst.getInput(i).isRegister()) {
//                        print("     Register\n");
//                    } else if(inst.getInput(i).getAddress().isStackAddress()){
//                        print(inst.getInput(i).toString()+"\n");
////                        print("                                     --------Stack\n");
//                    }
                    if(inst.getInput(i).isInput()){
                        print("                                              --------isInput    ");

                        if(this.language==null)print("gggggggggggggggggggggg\n");
                        if(inst.getInput(i).toString(this.language)!=null){
                            print(inst.getInput(i).toString(this.language));
                        }
                        print("\n");
                    }
                }
//                print("\n");
                Varnode out = inst.getOutput();
//                if(out != null){
//                    print("output\n");
//                    print(out.toString());
//                    if(out.isConstant()){
//                        print("     constant\n");
//                    } else if (out.isAddress()) {
//                        print("     address\n");
//                    } else if (out.isUnique()) {
//                        print("     Unique\n");
//                    } else if (out.isRegister()) {
//                        print("     Register\n");
//                    } else if(out.getAddress().isStackAddress()){
//                        print("     Stack\n");
//                    }
//                }
                print("\n");
            }
        }
    }

    @Override
    public void run_ghidra() throws Exception{
        setUpDecompiler(this.currentProgram);
        Function func = this.getFunctionContaining(this.currentAddress);
        HighFunction hfunc = decompileFunc(func);
        getParams(hfunc);
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
