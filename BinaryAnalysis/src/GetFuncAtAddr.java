import com.bai.util.GlobalState;
import ghidra.program.model.address.Address;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.FunctionIterator;
import ghidra.program.model.listing.FunctionManager;
import ghidra.program.model.listing.Program;
import ghidra.program.model.pcode.HighFunction;
import ghidra.program.model.pcode.PcodeBlockBasic;
import ghidra.program.model.pcode.PcodeOp;
import ghidra.program.model.pcode.PcodeOpAST;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class GetFuncAtAddr extends Analysis{
    private HashMap<PcodeBlockBasic, Integer> testMap;
    public GetFuncAtAddr() {
        this.testMap = new HashMap<>();
    }
    public void setUpDecompiler(Program program) throws Exception {
        super.setUpDecompiler(program);
        GlobalState.ghidraScript = this;
        GlobalState.flatAPI = this;
        GlobalState.currentProgram = program;
//        GlobalState.arch = new Architecture(GlobalState.currentProgram);
    }
    @Override
    public void run_ghidra() throws Exception{
        setUpDecompiler(this.currentProgram);
        Address addr = getAddressFactory().getAddress("0011b338");

        print(addr.toString()+"\n");
        Function func = this.getFunctionAt(addr);
        if(func == null){
            print("hahaha\n");
        }
//        print(func.getName());
//        HighFunction hfunc = decompileFunc(func);
    }
    // no need to copy
    @Override
    public void run() throws Exception {
        if(this.getScriptArgs().length!=0) {
            run_headless();
        }else{
            run_ghidra();
        }
    }
}
