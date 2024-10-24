import ghidra.app.decompiler.DecompInterface;
import ghidra.app.decompiler.DecompileException;
import ghidra.app.decompiler.DecompileOptions;
import ghidra.app.decompiler.DecompileResults;
import ghidra.app.script.GhidraScript;
import ghidra.framework.options.ToolOptions;
import ghidra.framework.plugintool.PluginTool;
import ghidra.framework.plugintool.util.OptionsService;
import ghidra.program.model.lang.Language;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.Program;
import ghidra.program.model.pcode.HighFunction;
import ghidra.program.model.pcode.PcodeBlockBasic;
import ghidra.program.model.pcode.PcodeOp;
import ghidra.util.Msg;

import java.util.Iterator;

public abstract class Analysis extends GhidraScript {
    public void solver(HighFunction hfunc) {print("jaja");print("hahah\n");}
    protected DecompInterface decomplib;
    protected Language language;
//    public DecompileResults decompileFunction(Function f) {
//        DecompileResults dRes = null;
//        try {
//            dRes = decomplib.decompileFunction(f, decomplib.getOptions().getDefaultTimeout(), getMonitor());
//        } catch (Exception exc) {
//            printf("EXCEPTION IN DECOMPILATION!\n");
//            exc.printStackTrace();
//        }
//        return dRes;
//    }
    public static PcodeOp getLastInst(PcodeBlockBasic bb){
        Iterator<PcodeOp> instIter = bb.getIterator();
        PcodeOp lastOp = null;
        while (instIter.hasNext()) {
            lastOp = instIter.next();
        }
        return lastOp;
    }

    public static boolean isConditional(PcodeBlockBasic bb){
        return getLastInst(bb).getOpcode() == PcodeOp.CBRANCH;
    }

    public void run_headless() throws Exception {

    }
//    public HighFunction decompile(Function func){
//        DecompileOptions options = new DecompileOptions();
//        DecompInterface ifc = new DecompInterface();
//        ifc.setOptions(options);
//
////        if (!ifc.openProgram(this.currentProgram)) {
////            throw new DecompileException("Decompiler",
////                    "Unable to initialize: " + ifc.getLastMessage());
////        }
//        if (!ifc.openProgram(this.currentProgram)) {
//            return null;
//        }
////        ifc.setSimplificationStyle("normalize");
//        ifc.setSimplificationStyle("decompile");
//        DecompileResults res = ifc.decompileFunction(func, 30, null);
//        HighFunction hfunc = res.getHighFunction();
//
//        return hfunc;
//    }


    public void setUpDecompiler(Program program) throws Exception {
        this.language = program.getLanguage();
        this.decomplib = new DecompInterface();
        DecompileOptions options;
        options = new DecompileOptions();
        PluginTool tool = state.getTool();
        if (tool != null) {
            OptionsService service = tool.getService(OptionsService.class);
            if (service != null) {
                ToolOptions opt = service.getOptions("Decompiler");
                options.grabFromToolAndProgram(null, opt, program);
            }
        }
        decomplib.setOptions(options);
        decomplib.toggleCCode(true);
        decomplib.toggleSyntaxTree(true);
        // must be "decompile"
        decomplib.setSimplificationStyle("decompile");
//        decomplib.setSimplificationStyle("normalize");

        if (!decomplib.openProgram(this.currentProgram)) {
            throw new DecompileException("Decompiler",
                    "Unable to initialize: " + decomplib.getLastMessage());
        }
    }
    public HighFunction decompileFunc(Function func){
        DecompileResults res = this.decomplib.decompileFunction(func, 30, null);
        HighFunction hfunc = res.getHighFunction();
//        this.language = hfunc.getLanguage();
        return hfunc;
    }

    public HighFunction run_ghidra_init() throws Exception{
        setUpDecompiler(this.currentProgram);

        Function func = this.getFunctionContaining(this.currentAddress);
        if (func == null) {
            Msg.showWarn(this, state.getTool().getToolFrame(), "GraphAST Error",
                    "No Function at current location");
            return null;
        }
        HighFunction hfunc = decompileFunc(func);
        PluginTool tool = state.getTool();
        return hfunc;
    }

    public void run_ghidra() throws Exception{
        HighFunction hfunc = run_ghidra_init();
    }
    @Override
    public void run() throws Exception {
        run_ghidra();
    }
}


// Analysis.java
//public abstract class Analysis extends GhidraScript {
//    public void solver(HighFunction hfunc) {print("hahah\n");}
//    private DecompInterface decomplib;
//    public DecompileResults decompileFunction(Function f) {
//        DecompileResults dRes = null;
//        try {
//            dRes = decomplib.decompileFunction(f, decomplib.getOptions().getDefaultTimeout(), getMonitor());
//        } catch (Exception exc) {
//            printf("EXCEPTION IN DECOMPILATION!\n");
//            exc.printStackTrace();
//        }
//        return dRes;
//    }
//    public DecompInterface setUpDecompiler(Program program) {
//        DecompInterface decompInterface = new DecompInterface();
//        DecompileOptions options;
//        options = new DecompileOptions();
//        PluginTool tool = state.getTool();
//        if (tool != null) {
//            OptionsService service = tool.getService(OptionsService.class);
//            if (service != null) {
//                ToolOptions opt = service.getOptions("Decompiler");
//                options.grabFromToolAndProgram(null, opt, program);
//            }
//        }
//        decompInterface.setOptions(options);
//        decompInterface.toggleCCode(true);
//        decompInterface.toggleSyntaxTree(true);
//        decompInterface.setSimplificationStyle("decompile");
//        return decompInterface;
//    }
//    public void run_headless() throws Exception {
//
//    }
//    public HighFunction run_ghidra_init() throws Exception{
//        Function func = this.getFunctionContaining(this.currentAddress);
//        if (func == null) {
//            Msg.showWarn(this, state.getTool().getToolFrame(), "GraphAST Error",
//                    "No Function at current location");
//            return null;
//        }
//
//        DecompileOptions options = new DecompileOptions();
//        DecompInterface ifc = new DecompInterface();
//        ifc.setOptions(options);
//
//        if (!ifc.openProgram(this.currentProgram)) {
//            throw new DecompileException("Decompiler",
//                    "Unable to initialize: " + ifc.getLastMessage());
//        }
//        ifc.setSimplificationStyle("normalize");
//        DecompileResults res = ifc.decompileFunction(func, 30, null);
//        HighFunction hfunc = res.getHighFunction();
//        PluginTool tool = state.getTool();
//        return hfunc;
//    }
//    public void run_ghidra() throws Exception{
//        HighFunction hfunc = run_ghidra_init();
//    }
//    @Override
//    public void run() throws Exception {
//        print("nima\n");
//        run_ghidra();
//    }
//}

