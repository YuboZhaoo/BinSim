import com.bai.graph.*;
import com.bai.graph.CFG;
import com.bai.graph.CDFG;
import com.bai.graph.DDG;
import com.bai.graph.PDG;
import com.bai.graph.LDDG;
import com.bai.util.Architecture;
import com.bai.util.GlobalState;

import ghidra.framework.plugintool.PluginTool;
import ghidra.program.model.address.Address;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.FunctionIterator;
import ghidra.program.model.listing.FunctionManager;
import ghidra.program.model.listing.Program;
import ghidra.program.model.pcode.HighFunction;
import ghidra.program.model.symbol.Symbol;
import ghidra.util.Msg;
import ghidra.util.UndefinedFunction;
import org.json.JSONObject;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;


public class BuildGraph extends Analysis{
    public void setUpDecompiler(Program program) throws Exception {
        super.setUpDecompiler(program);
        GlobalState.ghidraScript = this;
        GlobalState.flatAPI = this;
        GlobalState.currentProgram = program;

//        GlobalState.arch = new Architecture(GlobalState.currentProgram);
        GlobalState.decomplib = decomplib;
        GlobalState.monitor = monitor;
        GlobalState.state = state;
    }
    @Override
    public void solver(HighFunction hfunc){}

    @Override
    public void run_ghidra() throws Exception{
        setUpDecompiler(this.currentProgram);
        PluginTool tool = state.getTool();

        Function func = this.getFunctionContaining(this.currentAddress);
        if(func == null){
            func = UndefinedFunction.findFunction(currentProgram, this.currentAddress, monitor);
            if(func == null){
                Msg.showError(this, tool.getToolFrame(), "Build Graph in Ghidra Error",
                        "No function at address: " + this.currentAddress.toString());
                return;
            }
        }
        HighFunction hfunc = decompileFunc(func);

//        JSONObject funcOut = new JSONObject();


        String graph_type = askChoice("Select Graph Type", "Select Graph Type",
                List.of("CFG_INST", "CFG_BB", "DDG_OP", "DDG_INST","LDDG_OP", "CDFG_OP", "CDFG_INST", "CDG_INST", "ProbCDG", "PDG_OP","PDG_INST", "PPDG"), "PDG");
        switch (graph_type){
            case "CFG_INST":
                CFG.CFG_INST cfg = new CFG().new CFG_INST(hfunc);
                cfg.display();
                break;
            case "CFG_BB":
                CFG.CFG_BB cfg_bb = new CFG().new CFG_BB(hfunc);
                cfg_bb.display();
                break;
            case "DDG_OP":
                DDG.DDG_OP ddg = new DDG().new DDG_OP(hfunc);
                ddg.display();
                break;
            case "DDG_INST":
                DDG.DDG_INST ddg_i = new DDG().new DDG_INST(hfunc);
                ddg_i.display();
//                JSONObject dumppedGraph = ddg_i.dump();
//                funcOut.put("func_name", func.getName());
//                funcOut.put("PDG", dumppedGraph);
//                writeJson(funcOut, "/home/yuboz/work/Research/Binsim/BinaryAnalysis/src/"+this.currentProgram.getName()+".json");
                printf("Analyze function: %s successfully\n", func.getName());
                break;
            case "LDDG_OP":
                LDDG.LDDG_OP lddg_op = new LDDG().new LDDG_OP(hfunc);
                lddg_op.display();
                break;
            case "CDG_INST":
                CDG.CDG_INST cdg = new CDG().new CDG_INST(hfunc);
                cdg.display();
                break;
            case "CDFG_INST":
                CDFG.CDFG_INST cdfg_inst = new CDFG().new CDFG_INST(hfunc);
                cdfg_inst.display();
                break;
            case "CDFG_OP":
                CDFG.CDFG_OP cdfg_op = new CDFG().new CDFG_OP(hfunc);
                cdfg_op.display();
                break;
            case "ProbCDG":
                ProbCDG.CDG_INST pcdg = new ProbCDG().new CDG_INST(hfunc);
                pcdg.display();
                break;
            case "PDG_OP":
                PDG.PDG_OP pdg = new PDG().new PDG_OP(hfunc);
                pdg.display();
                break;
            case "PDG_INST":
                PDG.PDG_INST pdg_i = new PDG().new PDG_INST(hfunc);
                pdg_i.display();
                break;
            case "PPDG":
                PPDG.PPDG_OP ppdg = new PPDG().new PPDG_OP(hfunc);
                ppdg.display();
                break;
            default:
                Msg.showError(this, tool.getToolFrame(), "Build Graph in Ghidra Error",
                        "Invalid graph type selected: " + graph_type);
                break;
        }
    }



    public static void writeJson(JSONObject obj, String savePath) {
        try (final Writer writer = new FileWriter(new File(savePath))) {
            obj.write(writer);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void write_err_log(String logPath, String content) throws IOException {
        File file = new File(logPath);
        FileWriter log = new FileWriter(file, true);
        log.write(content+"\n");
        log.close();
    }
    private String funclist_name; // arg0, such as: arm32-clang-3.5-O0_curl
    private String funclist_path; // arg1 , path to txt and txt's name
    private String logPath; // arg2, path to error log, also the output root path
    private String outputPath; // logPath + "/Output"
    private String idb_path; // arg3, idb_path for the json to index, such as "IDBs/Dataset-1/curl/arm32-clang-3.5-O0_curl.i64"
    private List<String> graph_type; // arg4, such as PDG
    private void parseArgs(){
        var args = this.getScriptArgs();
        funclist_name = args[0];         // "arm32-clang-3.5-O0_curl"
        funclist_path = args[1];         // "../DBs/Dataset-1/index/training/arm32-clang-3.5-O0_curl.txt"
        logPath = args[2];               // "../DBs/Dataset-1/PDG/training/"
        outputPath = args[2]+"output/";  // "../DBs/Dataset-1/PDG/training/output"
        idb_path = args[3];              // "IDBs/Dataset-1/curl/arm32-clang-3.5-O0_curl.i64"
        String tmp = args[4];     // "PDG"
        graph_type = Arrays.asList(tmp.split("\\+"));
    }
    private void beginInfo(){
        Date now = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String time = dateFormat.format(now);
        printf("\n\n\n--------------------------analyze begin for %s--------------------------------------------------------------\n", funclist_name);
        printf("time: %s\n",time);
        printf("name: %s\n",funclist_name);
        printf("target path: %s\n", funclist_path);
        printf("output path: %s\n", outputPath);
        printf("graph type: %s\n", graph_type);
    }
    // TODO has problem
    public void addr_trans(HashMap<Address, String> funs_addrs, String target_info_path) throws IOException {
        ArrayList<String> fun_addrs_raw = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(target_info_path))) {
            String line;
            while ((line = br.readLine()) != null) {
                fun_addrs_raw.add(line);
            }
        } catch (IOException e) {
            printf("file error: %s\n", e);
            write_err_log("./" + "error_log.txt","open file error:"+e+"\n");
            return;
        }

        Address base = currentProgram.getImageBase();
        printf("base       %s\n", base.toString());
        long base_num = Long.parseLong(base.toString(), 16);
        int flag = 0; //whether add base addr
        for (var addr: fun_addrs_raw){
            if (addr.startsWith("0x")) {
                addr = addr.substring(2);
            }
            long addr_num = Long.parseLong(addr, 16);
            if(addr_num < base_num){
                flag = 1;
                break;
            }
            Address address = getAddressFactory().getAddress(addr);
//            Function function = getFunctionContaining(getAddressFactory().getAddress(addr));
            Function function = getFunctionAt(address);
            if (function==null){
                // may be undefined function in ghidra
                UndefinedFunction ufunc = UndefinedFunction.findFunction(currentProgram, address, monitor);
                if(ufunc == null){
                    // may be decompiled failed in ghidra
                    Symbol funcname = getSymbolAt(address);
                    if(funcname == null){
                        flag = 1;
                        break;
                    }
                }
            }
        }

//        ArrayList<Address> funs_addrs = new ArrayList<>();
        if(flag==1){
            for (var addr: fun_addrs_raw){
                var addr_raw = addr;
                if (addr.startsWith("0x")) {
                    addr = addr.substring(2);
                }
                long addr_num = Long.parseLong(addr, 16);
                addr_num = addr_num + base_num;
                funs_addrs.put(getAddressFactory().getAddress(Long.toHexString(addr_num)), addr_raw);
            }
        }
        else{
            for (var addr: fun_addrs_raw){
                var addr_raw = addr;
                if (addr.startsWith("0x")) {
                    addr = addr.substring(2);
                }
                funs_addrs.put(getAddressFactory().getAddress(addr), addr_raw);
            }
        }

    }
    String mod = "dataset";
//    String mod = "file";
    @Override
    public void run_headless() throws Exception{
        setUpDecompiler(this.currentProgram);

        if(mod.equals("dataset")){
            parseArgs();
            beginInfo();

            // open the func list txt and transfer addr in it into ghidra's address
            HashMap<Address, String> funs_addrs = new HashMap<>();
            addr_trans(funs_addrs, funclist_path);

            JSONObject binOut = new JSONObject();
            for (Address addr: funs_addrs.keySet()){
                String addr_in_funlist = funs_addrs.get(addr);
                Function function = getFunctionAt(addr);
                if (function==null){
                    // may be undefined function in ghidra
                    function = UndefinedFunction.findFunction(currentProgram, addr, monitor);
                    if(function == null){
                        if(getSymbolAt(addr)!=null){
                            write_err_log(logPath + "error_log.txt", "ghidra can't recognize this function but it has a symbol: "+funclist_path+"  addr: "+addr.toString());
                            printf("canot get function %s at addr: %s", funclist_name, addr.toString());
                        }
                        else {
                            write_err_log(logPath + "error_log.txt", "ghidra can't find function: "+funclist_path+"  addr: "+addr.toString());
                            printf("canot get function %s at addr: %s", funclist_name, addr.toString());
                        }
                        continue;
                    }
                }
                try{
                    HighFunction hfunction = decompileFunc(function);
                    printf("\nFound target function %s @ 0x%x %s in %s\n", function.getName(),
                            function.getEntryPoint().getOffset(), addr, this.currentProgram.getName());
                    if (hfunction != null) {
                        JSONObject funcOut = new JSONObject();
                        funcOut.put("func_name", function.getName());
                        if (graph_type.contains("CFG_INST")) {
                            CFG.CFG_INST cfg_op = new CFG().new CFG_INST(hfunction);
                            JSONObject dumpedGraph = cfg_op.dump();
                            funcOut.put("CFG_INST", dumpedGraph);
                        }
                        if (graph_type.contains("DDG_OP")) {
                            DDG.DDG_OP ddg_op = new DDG().new DDG_OP(hfunction);
                            JSONObject dumpedGraph = ddg_op.dump();
                            funcOut.put("DDG_OP", dumpedGraph);
                        }
                        if (graph_type.contains("DDG_INST")) {
                            DDG.DDG_INST ddg_inst = new DDG().new DDG_INST(hfunction);
                            JSONObject dumpedGraph = ddg_inst.dump();
                            funcOut.put("DDG_INST", dumpedGraph);
                        }
                        if (graph_type.contains("CDG_INST")) {
                            CDG.CDG_INST cdg_inst = new CDG().new CDG_INST(hfunction);
                            JSONObject dumpedGraph = cdg_inst.dump();
                            funcOut.put("CDG_INST", dumpedGraph);
                        }
                        if (graph_type.contains("CDFG_INST")) {
                            CDFG.CDFG_INST cdfg_inst = new CDFG().new CDFG_INST(hfunction);
                            JSONObject dumpedGraph = cdfg_inst.dump();
                            funcOut.put("CDFG_INST", dumpedGraph);
                        }
                        if (graph_type.contains("CDFG_OP")) {
                            CDFG.CDFG_OP cdfg_op = new CDFG().new CDFG_OP(hfunction);
                            JSONObject dumpedGraph = cdfg_op.dump();
                            funcOut.put("CDFG_OP", dumpedGraph);
                        }
                        if (graph_type.contains("PDG_OP")) {
                            PDG.PDG_OP pdg_op = new PDG().new PDG_OP(hfunction);
                            JSONObject dumpedGraph = pdg_op.dump();
                            funcOut.put("PDG_OP", dumpedGraph);
                        }
                        if (graph_type.contains("PDG_INST")) {
                            PDG.PDG_INST pdg_inst = new PDG().new PDG_INST(hfunction);
                            JSONObject dumpedGraph = pdg_inst.dump();
                            funcOut.put("PDG_INST", dumpedGraph);
                        }
                        binOut.put(addr_in_funlist, funcOut);
                        printf("Analyze function: %s %s successfully\n", function.getName(), funclist_name);
                    }
                } catch(Exception e) {
                    write_err_log(logPath + "error_log.txt", "analyze function "+function.getName()+" failed, "
                            + funclist_path + "  addr: " + addr.toString());
                }
            }
            JSONObject binOutWrap = new JSONObject();
            binOutWrap.put(idb_path, binOut);
            writeJson(binOutWrap, outputPath+this.currentProgram.getName()+".json");
            printf("\n--------------------------analyze end for %s-------------------------------------------------------------\n\n\n", funclist_name);
        }
        else { // TODO pair binary file mode
            // TODO implement the arg parse specially for it
            parseArgs();
            beginInfo();
            JSONObject binOut = new JSONObject();

            FunctionManager fm = this.currentProgram.getFunctionManager();
            FunctionIterator funcs = fm.getFunctions(true);
            for (Function function: funcs){
                HighFunction hfunction = decompileFunc(function);
                if (hfunction != null) {
                    if(graph_type.contains("DDG_INST")){
                        DDG.DDG_INST ddg_inst = new DDG().new DDG_INST(hfunction);
                        JSONObject dumpedGraph = ddg_inst.dump();

                        JSONObject funcOut = new JSONObject();
                        funcOut.put("func_name", function.getName());
                        funcOut.put("DDG_INST", dumpedGraph);
                        binOut.put(function.getEntryPoint().toString(), funcOut);
                        printf("Analyze function: %s successfully\n", function.getName());
                    } else if(graph_type.contains("CFG_INST")){
                        CFG.CFG_INST cfg = new CFG().new CFG_INST(hfunction);
                        JSONObject dumpedGraph = cfg.dump();

                        JSONObject funcOut = new JSONObject();
                        funcOut.put("func_name", function.getName());
                        funcOut.put("CFG_INST", dumpedGraph);
                        binOut.put(function.getEntryPoint().toString(), funcOut);
                        printf("Analyze function: %s %s successfully\n", function.getName(), funclist_name);
                    } else {
                        printf("No such graph type: %s\n", graph_type);
                        break;
                    }

                }
            }
            JSONObject binOutWrap = new JSONObject();
            binOutWrap.put(idb_path, binOut);
            writeJson(binOutWrap, outputPath+this.currentProgram.getName()+".json");
        }

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
