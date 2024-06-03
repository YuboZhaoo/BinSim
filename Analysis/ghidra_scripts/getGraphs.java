import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

import ghidra.app.decompiler.*;
import ghidra.app.decompiler.DecompInterface;
import ghidra.app.decompiler.DecompileOptions;
import ghidra.app.decompiler.DecompileResults;
import ghidra.app.plugin.core.graph.AddressBasedGraphDisplayListener;
import ghidra.app.script.GhidraScript;
import ghidra.app.script.GhidraState;
import ghidra.app.services.GraphDisplayBroker;
import ghidra.framework.options.ToolOptions;
import ghidra.framework.plugintool.Plugin;
import ghidra.framework.plugintool.PluginTool;
import ghidra.framework.plugintool.util.OptionsService;
import ghidra.graph.GDirectedGraph;
import ghidra.graph.GEdge;
import ghidra.graph.GraphAlgorithms;
import ghidra.program.model.address.*;
import ghidra.program.model.lang.Register;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.Program;
import ghidra.program.model.pcode.*;
import ghidra.service.graph.*;
import ghidra.util.Msg;

import ghidra.service.graph.AttributedEdge;
import ghidra.service.graph.AttributedVertex;
import ghidra.graph.*;
import ghidra.program.model.address.Address;
import ghidra.util.exception.CancelledException;

import ghidra.util.task.TaskMonitor;
import jnr.constants.platform.PRIO;
import org.json.JSONArray;
import org.json.JSONObject;


class Graph extends GhidraScript{
//    protected Program currentProgram;
    protected TaskMonitor monitor;
    protected DecompInterface decomplib;

    protected Function func;
    protected HighFunction hfunc;
    protected AttributedGraph graph;
    protected static final String COLOR_ATTRIBUTE = "Color";
    protected static final String ICON_ATTRIBUTE = "Icon";

    // member var must be inited before use them
    public void init(){
        graph = new AttributedGraph();
    }
    public Graph() {
        super();
    }
//    private DecompInterface setUpDecompiler(Program program) {
//        DecompInterface decompInterface = new DecompInterface();
//        DecompileOptions options = new DecompileOptions();
//        PluginTool tool = this.state.getTool();
//        if (tool != null) {
//            OptionsService service = tool.getService(OptionsService.class);
//            if (service != null) {
//                ToolOptions opt = service.getOptions("Decompiler");
//                options.grabFromToolAndProgram((Plugin) null, opt, program);
//            }
//        }
//        decompInterface.setOptions(options);
//        decompInterface.toggleCCode(true);
//        decompInterface.toggleSyntaxTree(true);
//        decompInterface.setSimplificationStyle("decompile");
//        return decompInterface;
//    }
    public Graph(DecompInterface decomplib, TaskMonitor monitor, GhidraState state, Function function, HighFunction highFunction) {

//        this.currentProgram = program;
        this.decomplib = decomplib;
        this.monitor = monitor;
        this.state = state;
        this.func = function;
        this.hfunc = highFunction;
        init();
    }
    protected Iterator<PcodeOpAST> getPcodeOpIterator() {
        Iterator<PcodeOpAST> opiter = hfunc.getPcodeOps();
        return opiter;
    }
    @Override
    public void run() throws Exception {}
    public void getGraph_ghidra() throws Exception {
        displayGraph();
    }
    public static void writeJson(JSONObject obj, String savePath) {
        try (final Writer writer = new FileWriter(new File(savePath))) {
            obj.write(writer);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
//    protected void decompileFunction() throws DecompileException {
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
//        hfunc = res.getHighFunction();
//    }
    protected JSONObject dumpGraph() throws Exception {
        JSONObject funcOut = new JSONObject();
        ArrayList<Integer> nodes = new ArrayList<Integer>();
        ArrayList<Integer[]> edges = new ArrayList<Integer[]>();
        JSONObject nodesVerb = new JSONObject();
        return funcOut;
    }
    protected AttributedEdge createEdge(AttributedVertex in, AttributedVertex out) {
        return graph.addEdge(in, out);
    }
    private String getVarnodeKey(VarnodeAST vn) {
        PcodeOp op = vn.getDef();
        String id;
        if (op != null) {
            id = op.getSeqnum().getTarget().toString(true) + " v " +
                    Integer.toString(vn.getUniqueId());
        }
        else {
            id = "i v " + Integer.toString(vn.getUniqueId());
        }
        return id;
    }
    protected AttributedVertex getVarnodeVertex(Map<Integer, AttributedVertex> vertices,
                                                VarnodeAST vn) {
        AttributedVertex res;
        res = vertices.get(vn.getUniqueId());
        if (res == null) {
            res = createVarnodeVertex(vn);
            vertices.put(vn.getUniqueId(), res);
        }
        return res;
    }
    protected AttributedVertex createVarnodeVertex(VarnodeAST vn) { // create node of varnode
//        String name = vn.getAddress().toString(true);
        String name = vn.toString();
        String id = getVarnodeKey(vn);
        String colorattrib = "Red";
        if (vn.isConstant()) {
            colorattrib = "DarkGreen";
        }
        else if (vn.isRegister()) {
            colorattrib = "Blue";
            if (vn.isInput()) {
                Register reg = func.getProgram().getRegister(vn.getAddress(), vn.getSize());
                if (reg != null) {
                    name = "arg_" + reg.getName();
                }else {
                    name =  "arg_" + name;
                }
            }
        }
        else if (vn.isUnique()) {
            colorattrib = "Black";
        }
        else if (vn.isPersistant()) {
            colorattrib = "DarkOrange";
        }
        else if (vn.isAddrTied()) {
            colorattrib = "Orange";
        }
        AttributedVertex vert = graph.addVertex(id, name);
        if (vn.isInput()) {
            vert.setAttribute(ICON_ATTRIBUTE, "TriangleDown");
        }
        else {
            vert.setAttribute(ICON_ATTRIBUTE, "Circle");
        }
        vert.setAttribute(COLOR_ATTRIBUTE, colorattrib);
        return vert;
    }
    protected String getOpKey(PcodeOpAST op) { // get the id of the pcode inst
        SequenceNumber sq = op.getSeqnum();
        String id =
                sq.getTarget().toString(true) + " o " + Integer.toString(op.getSeqnum().getTime());
        return id;
    }
    protected AttributedVertex createOpVertex(PcodeOpAST op) { // create node of pcode operation
        String name = op.getMnemonic();
        String id = getOpKey(op);
        int opcode = op.getOpcode();
        if ((opcode == PcodeOp.LOAD) || (opcode == PcodeOp.STORE)) {
            Varnode vn = op.getInput(0);
            AddressSpace addrspace =
                    func.getProgram().getAddressFactory().getAddressSpace((int) vn.getOffset());
            name += ' ' + addrspace.getName();
        }
        else if (opcode == PcodeOp.INDIRECT) {
            Varnode vn = op.getInput(1);
            if (vn != null) {
                PcodeOp indOp = hfunc.getOpRef((int) vn.getOffset());
                if (indOp != null) {
                    name += " (" + indOp.getMnemonic() + ')';
                }
            }
        }
        AttributedVertex vert = graph.addVertex(id, name);
        vert.setAttribute(ICON_ATTRIBUTE, "Square");
        return vert;
    }
    protected void displayGraph() throws Exception {
        PluginTool tool = state.getTool();
        if (tool == null) {
            println("Script is not running in GUI");
        }
        GraphDisplayBroker graphDisplayBroker = tool.getService(GraphDisplayBroker.class);
        if (graphDisplayBroker == null) {
            Msg.showError(this, tool.getToolFrame(), "GraphAST Error",
                    "No graph display providers found: Please add a graph display provider to your tool");
            return;
        }
        GraphDisplay graphDisplay =
                graphDisplayBroker.getDefaultGraphDisplay(false, monitor);
        String description = "AST Data Flow Graph For " + func.getName();

        graphDisplay.setGraph(graph, description, false, monitor);

        // Install a handler so the selection/location will map
        graphDisplay.setGraphDisplayListener(
                new ASTGraphDisplayListener(tool, graphDisplay, hfunc, func.getProgram()));
    }
    class ASTGraphDisplayListener extends AddressBasedGraphDisplayListener {

        HighFunction highfunc;

        public ASTGraphDisplayListener(PluginTool tool, GraphDisplay display, HighFunction high,
                                       Program program) {
            super(tool, program, display);
            highfunc = high;
        }
        @Override
        protected Set<AttributedVertex> getVertices(AddressSetView selection) {
            return Collections.emptySet();
        }

        @Override
        protected AddressSet getAddresses(Set<AttributedVertex> vertices) {
            AddressSet set = new AddressSet();
            for (AttributedVertex vertex : vertices) {
                Address address = getAddress(vertex);
                if (address != null) {
                    set.add(address);
                }
            }
            return set;
        }
        @Override
        protected Address getAddress(AttributedVertex vertex) {
            if (vertex == null) {
                return null;
            }
            String vertexId = vertex.getId();
            int firstcolon = vertexId.indexOf(':');
            if (firstcolon == -1) {
                return null;
            }

            int firstSpace = vertexId.indexOf(' ');
            String addrString = vertexId.substring(0, firstSpace);
            return getAddress(addrString);
        }

        @Override
        public GraphDisplayListener cloneWith(GraphDisplay graphDisplay) {
            return new ASTGraphDisplayListener(tool, graphDisplay, highfunc, currentProgram);
        }
    }
}

class DDG extends Graph{
    @Override
    public void init(){
        super.init();
    }
    public DDG() {
        super();
    }
    public DDG (DecompInterface decomplib, TaskMonitor monitor, GhidraState state, Function function, HighFunction highFunction) {
        super(decomplib, monitor, state, function, highFunction);
        init();

    }
    protected void buildDDG() {
        HashMap<Integer, AttributedVertex> vertices = new HashMap<>();
        Iterator<PcodeOpAST> opiter = getPcodeOpIterator();
        while (opiter.hasNext()) {
            PcodeOpAST op = opiter.next();
            AttributedVertex o = createOpVertex(op);
            for (int i = 0; i < op.getNumInputs(); ++i) {
                int opcode = op.getOpcode();
                if ((i == 0) && ((opcode == PcodeOp.LOAD) || (opcode == PcodeOp.STORE))) {
                    continue;
                }
                if ((i == 1) && (opcode == PcodeOp.INDIRECT)) {
                    continue;
                }
                VarnodeAST vn = (VarnodeAST) op.getInput(i);
                if (vn != null) {
                    AttributedVertex v = getVarnodeVertex(vertices, vn);
                    createEdge(v, o);
                }
            }
            VarnodeAST outvn = (VarnodeAST) op.getOutput();
            if (outvn != null) {
                AttributedVertex outv = getVarnodeVertex(vertices, outvn);
                if (outv != null) {
                    createEdge(o, outv);
                }
            }
        }
    }
    public void getGraph_ghidra() throws Exception {
        buildDDG();
        displayGraph();
    }
//    public void run_in_ghidra() throws Exception {
//        func = this.getFunctionContaining(this.currentAddress);
//        if (func == null) {
//            Msg.showWarn(this, state.getTool().getToolFrame(), "GraphAST Error",
//                    "No Function at current location");
//            return;
//        }
//        decompileFunction();
//        buildDDG();
//        displayGraph();
//    }
}

// CFG in inst level
class CFG extends Graph{
    public CFG() {
        super();
    }
    public CFG (DecompInterface decomplib, TaskMonitor monitor, GhidraState state, Function function, HighFunction highFunction) {
        super(decomplib, monitor, state, function, highFunction);
        init();
    }
    @Override
    protected AttributedVertex createOpVertex(PcodeOpAST op) { // create node of pcode operation
        String name = op.toString();
        String id = getOpKey(op);
        AttributedVertex vert = graph.addVertex(id, name);
        vert.setAttribute(ICON_ATTRIBUTE, "Square");
        return vert;
    }
    protected void buildCFG() {
//        HashMap<Integer, AttributedVertex> vertices = new HashMap<>();
        HashMap<PcodeOp, AttributedVertex> map = new HashMap<PcodeOp, AttributedVertex>();
        Iterator<PcodeOpAST> opiter = getPcodeOpIterator();
        while (opiter.hasNext()) {
            PcodeOpAST op = opiter.next();
            AttributedVertex o = createOpVertex(op);
            map.put(op, o);
        }
        opiter = getPcodeOpIterator();
        HashSet<PcodeBlockBasic> seenParents = new HashSet<PcodeBlockBasic>();
        HashMap<PcodeBlock, AttributedVertex> first = new HashMap<>();
        HashMap<PcodeBlock, AttributedVertex> last = new HashMap<>();
        while (opiter.hasNext()) {
            PcodeOpAST op = opiter.next();
            PcodeBlockBasic parent = op.getParent();
            if (seenParents.contains(parent)) {
                continue;
            }
            Iterator<PcodeOp> iterator = parent.getIterator();
            PcodeOp prev = null;
            PcodeOp next = null;
            while (iterator.hasNext()) {
                next = iterator.next();
                if (prev == null && map.containsKey(next)) {
                    first.put(parent, map.get(next));
                }
                if (prev != null && map.containsKey(prev) && map.containsKey(next)) {
                    AttributedEdge edge = createEdge(map.get(prev), map.get(next));
                    edge.setAttribute(COLOR_ATTRIBUTE, "Black");
                }
                prev = next;
            }
            if (next != null && map.containsKey(next)) {
                last.put(parent, map.get(next));
            }
            seenParents.add(parent);
        }
        Set<PcodeBlock> keySet = first.keySet();
        for (PcodeBlock block : keySet) {
            for (int i = 0; i < block.getInSize(); i++) {
                PcodeBlock in = block.getIn(i);
                if (last.containsKey(in)) {
                    AttributedEdge edge = createEdge(last.get(in), first.get(block));
                    edge.setAttribute(COLOR_ATTRIBUTE, "Red");
                }
            }
        }
    }
    public void getGraph_ghidra() throws Exception {
        buildCFG();
        displayGraph();
    }

    @Override
    public void run() throws Exception {}
}

// TODO CFG in block level, not implemented
class CFG_block extends Graph{
    @Override
    protected AttributedVertex createOpVertex(PcodeOpAST op) { // create node of pcode operation
        String name = op.toString();
        String id = getOpKey(op);
        AttributedVertex vert = graph.addVertex(id, name);
        vert.setAttribute(ICON_ATTRIBUTE, "Square");
        return vert;
    }
    protected void buildCFG() {
//        HashMap<Integer, AttributedVertex> vertices = new HashMap<>();
        HashMap<PcodeOp, AttributedVertex> map = new HashMap<PcodeOp, AttributedVertex>();
        Iterator<PcodeOpAST> opiter = getPcodeOpIterator();
        while (opiter.hasNext()) {
            PcodeOpAST op = opiter.next();
            AttributedVertex o = createOpVertex(op);
            map.put(op, o);
        }
        opiter = getPcodeOpIterator();
        HashSet<PcodeBlockBasic> seenParents = new HashSet<PcodeBlockBasic>();
        HashMap<PcodeBlock, AttributedVertex> first = new HashMap<>();
        HashMap<PcodeBlock, AttributedVertex> last = new HashMap<>();
        while (opiter.hasNext()) {
            PcodeOpAST op = opiter.next();
            PcodeBlockBasic parent = op.getParent();
            if (seenParents.contains(parent)) {
                continue;
            }
            Iterator<PcodeOp> iterator = parent.getIterator();
            PcodeOp prev = null;
            PcodeOp next = null;
            while (iterator.hasNext()) {
                next = iterator.next();
                if (prev == null && map.containsKey(next)) {
                    first.put(parent, map.get(next));
                }
                if (prev != null && map.containsKey(prev) && map.containsKey(next)) {
                    AttributedEdge edge = createEdge(map.get(prev), map.get(next));
                    edge.setAttribute(COLOR_ATTRIBUTE, "Black");
                }
                prev = next;
            }
            if (next != null && map.containsKey(next)) {
                last.put(parent, map.get(next));
            }
            seenParents.add(parent);
        }
        Set<PcodeBlock> keySet = first.keySet();
        for (PcodeBlock block : keySet) {
            for (int i = 0; i < block.getInSize(); i++) {
                PcodeBlock in = block.getIn(i);
                if (last.containsKey(in)) {
                    AttributedEdge edge = createEdge(last.get(in), first.get(block));
                    edge.setAttribute(COLOR_ATTRIBUTE, "Red");
                }
            }
        }
    }
//    @Override
//    public void run_in_ghidra() throws Exception {
//        PluginTool tool = state.getTool();
//        if (tool == null) {
//            println("Script is not running in GUI");
//        }
//        GraphDisplayBroker graphDisplayBroker = tool.getService(GraphDisplayBroker.class);
//        if (graphDisplayBroker == null) {
//            Msg.showError(this, tool.getToolFrame(), "GraphAST Error",
//                    "No graph display providers found: Please add a graph display provider to your tool");
//            return;
//        }
//
//        func = this.getFunctionContaining(this.currentAddress);
//        if (func == null) {
//            Msg.showWarn(this, state.getTool().getToolFrame(), "GraphAST Error",
//                    "No Function at current location");
//            return;
//        }
//
//        decompileFunction();
//
//        graph = new AttributedGraph();
//        buildCFG();
//
//        GraphDisplay graphDisplay =
//                graphDisplayBroker.getDefaultGraphDisplay(false, monitor);
//        String description = "AST Data Flow Graph For " + func.getName();
//
//        graphDisplay.setGraph(graph, description, false, monitor);
//
//        // Install a handler so the selection/location will map
//        graphDisplay.setGraphDisplayListener(
//                new ASTGraphDisplayListener(tool, graphDisplay, hfunc, func.getProgram()));
//    }

    @Override
    public void run() throws Exception {}
}

class CDFG extends Graph {
    public CDFG() {
        super();
    }
    public CDFG (DecompInterface decomplib, TaskMonitor monitor, GhidraState state, Function function, HighFunction highFunction) {
        super(decomplib, monitor, state, function, highFunction);
        init();
    }
    protected void buildCDFG() {

        HashMap<Integer, AttributedVertex> vertices = new HashMap<>();

        Iterator<PcodeOpAST> opiter = getPcodeOpIterator();
        HashMap<PcodeOp, AttributedVertex> map = new HashMap<PcodeOp, AttributedVertex>();
        while (opiter.hasNext()) {
            PcodeOpAST op = opiter.next();
            AttributedVertex o = createOpVertex(op);
            map.put(op, o);
            for (int i = 0; i < op.getNumInputs(); ++i) {
                if ((i == 0) &&
                        ((op.getOpcode() == PcodeOp.LOAD) || (op.getOpcode() == PcodeOp.STORE))) {
                    continue;
                }
                if ((i == 1) && (op.getOpcode() == PcodeOp.INDIRECT)) {
                    continue;
                }
                VarnodeAST vn = (VarnodeAST) op.getInput(i);
                if (vn != null) {
                    AttributedVertex v = getVarnodeVertex(vertices, vn);
                    createEdge(v, o);
                }
            }
            VarnodeAST outvn = (VarnodeAST) op.getOutput();
            if (outvn != null) {
                AttributedVertex outv = getVarnodeVertex(vertices, outvn);
                if (outv != null) {
                    createEdge(o, outv);
                }
            }
        }
        opiter = getPcodeOpIterator();
        HashSet<PcodeBlockBasic> seenParents = new HashSet<PcodeBlockBasic>();
        HashMap<PcodeBlock, AttributedVertex> first = new HashMap<>();
        HashMap<PcodeBlock, AttributedVertex> last = new HashMap<>();
        while (opiter.hasNext()) {
            PcodeOpAST op = opiter.next();
            PcodeBlockBasic parent = op.getParent();
            if (seenParents.contains(parent)) {
                continue;
            }
            Iterator<PcodeOp> iterator = parent.getIterator();
            PcodeOp prev = null;
            PcodeOp next = null;
            while (iterator.hasNext()) {
                next = iterator.next();
                if (prev == null && map.containsKey(next)) {
                    first.put(parent, map.get(next));
                }
//                if (prev != null && map.containsKey(prev) && map.containsKey(next)) {
//                    AttributedEdge edge = createEdge(map.get(prev), map.get(next));
//                    edge.setAttribute(COLOR_ATTRIBUTE, "Black");
//                }
                prev = next;
            }
            if (next != null && map.containsKey(next)) {
                last.put(parent, map.get(next));
            }
            seenParents.add(parent);
        }
        Set<PcodeBlock> keySet = first.keySet();
        for (PcodeBlock block : keySet) {
            for (int i = 0; i < block.getInSize(); i++) {
                PcodeBlock in = block.getIn(i);
                if (last.containsKey(in)) {
                    AttributedEdge edge = createEdge(last.get(in), first.get(block));
                    edge.setAttribute(COLOR_ATTRIBUTE, "Red");
                }
            }
        }
    }

    //    @Override
    public void getGraph_ghidra() throws Exception {
        buildCDFG();
        displayGraph();
    }


    @Override
    public void run() throws Exception {}

}




class PcodeBlockVtx {
    private final PcodeBlockBasic pcodeBlock;
    private final String name;
    public PcodeBlockVtx(PcodeBlockBasic codeBlock, String name) { // for normal block
        this.pcodeBlock = codeBlock;
        this.name = name;
    }
    public PcodeBlockVtx(String name) { // for entry and exit node
        this.pcodeBlock = null;
        this.name = name;
    }
    public PcodeBlockBasic getCodeBlock() {
        return pcodeBlock;
    }

    public String getName() {
        return name;
    }
}


// PDG = CDG + DDG
// add edge will create nodes if not have
class PDG extends DDG{
    // RCFG in block level, can not output in ghidra, for calculate post-domtree only
    protected GDirectedGraph<PcodeBlockVtx, GEdge<PcodeBlockVtx>> rcfg;
    protected HashMap<PcodeOp, HashSet<PcodeOp>> ctrlDep;
    //    protected HashSet<AttributedEdge> ctrlDep;
    protected GDirectedGraph<PcodeBlockVtx, GEdge<PcodeBlockVtx>> postDominanceTree;
    Map<PcodeBlockBasic, PcodeBlockVtx> instanceMap;
    HashMap<PcodeOp, AttributedVertex> instanceMap_ddg;
    public PDG() {
        super();
    }
    public PDG (DecompInterface decomplib, TaskMonitor monitor, GhidraState state, Function function, HighFunction highFunction) {
        super(decomplib, monitor, state, function, highFunction);
        init();

    }
    @Override
    public void init() {
        super.init();
        ctrlDep = new HashMap<PcodeOp, HashSet<PcodeOp>>();
        rcfg = GraphFactory.createDirectedGraph();
        instanceMap_ddg = new HashMap<PcodeOp, AttributedVertex>();
    }
    protected void buildRevCFG(HighFunction hfunc) {
        instanceMap = new HashMap<PcodeBlockBasic, PcodeBlockVtx>();

        ArrayList<PcodeBlockBasic> blocks = hfunc.getBasicBlocks();
        PcodeBlockVtx exit_node = new PcodeBlockVtx("exit");
        rcfg.addVertex(exit_node);

//        printf("build rcfg: block num: %d\n", blocks.size());

        PcodeBlockBasic block = null;
        while (!blocks.isEmpty()) {
            block = blocks.remove(0);
            PcodeBlockVtx fromVtx = instanceMap.get(block);
            if (fromVtx == null) {
                fromVtx = new PcodeBlockVtx(block, block.toString());
                instanceMap.put(block, fromVtx);
                rcfg.addVertex(fromVtx);
            }
            for(int i=0;i<block.getOutSize();i++){
                PcodeBlockBasic succ_block = (PcodeBlockBasic)block.getOut(i);
                if (succ_block!=null){
                    PcodeBlockVtx toVtx = instanceMap.get(succ_block);
                    if (toVtx == null) {
                        toVtx = new PcodeBlockVtx(succ_block, succ_block.toString());
                        instanceMap.put(succ_block, toVtx);
                        rcfg.addVertex(toVtx);
                    }
                    rcfg.addEdge(new DefaultGEdge(toVtx, fromVtx));
                }
            }
            if(block.getOutSize()==0){
                rcfg.addEdge(new DefaultGEdge(exit_node, fromVtx));
            }
        }

//        printf("build rcfg: block num: %d\n", rcfg.getVertices().size());
//        printf("build rcfg: edge num: %d\n", rcfg.getEdges().size());
        assert rcfg.getVertices().size()==blocks.size()+1;
    }
    private PcodeBlockVtx containsAny(Collection<PcodeBlockVtx> srcBB, Collection<PcodeBlockVtx> desBB) {
        for (PcodeBlockVtx des : desBB) {
            if (srcBB.contains(des)) {
                return des;
            }
        }
        return null;
    }
    public static <T> T getLastElement(Iterator<T> iterator) {
        T lastElement = null;
        while (iterator.hasNext()) {
            lastElement = iterator.next();
        }
        return lastElement;
    }
    public void addControlDepFromNodeToBB(PcodeOp srcOp, PcodeBlockBasic block) {
        Iterator<PcodeOp> ins_iter = block.getIterator();
        if (!ctrlDep.containsKey(srcOp)) {
            ctrlDep.put(srcOp, new HashSet<PcodeOp>());
        }
        HashSet<PcodeOp> control = ctrlDep.get(srcOp);
        while (ins_iter.hasNext()) {
            PcodeOp p = ins_iter.next();
//            AttributedEdge edge = createEdge(srcOp, p);
            control.add(p);
        }
    }
    public void addControlDepFromDominatedBlockToDominator(PcodeOp srcOp, PcodeBlockVtx srcBB, PcodeBlockVtx desBB,
                                                           GDirectedGraph<PcodeBlockVtx, GEdge<PcodeBlockVtx>> postDominanceTree) {
        Collection<PcodeBlockVtx> pdOfSrc = postDominanceTree.getPredecessors(srcBB);
        Collection<PcodeBlockVtx> dominatedBlock = new HashSet<PcodeBlockVtx>();
        dominatedBlock.add(desBB);
        PcodeBlockVtx nearestCommonDominator;
        // walk up along the Post Dominance Tree, start from desBB
        while (true) {
            Collection<PcodeBlockVtx> newDominatedBlock = new HashSet<PcodeBlockVtx>();
            for (PcodeBlockVtx pd : dominatedBlock) {
                newDominatedBlock.addAll(postDominanceTree.getPredecessors(pd));
                addControlDepFromNodeToBB(srcOp, pd.getCodeBlock());
            }
            nearestCommonDominator = this.containsAny(pdOfSrc, newDominatedBlock);
            if (nearestCommonDominator != null) {
                break;
            }
            dominatedBlock = newDominatedBlock;
        }
        if (nearestCommonDominator.equals(srcBB)) {
            addControlDepFromNodeToBB(srcOp, srcBB.getCodeBlock());
        }
    }
    protected void getCtrlDep()throws CancelledException{
        for (GEdge edge : rcfg.getEdges()) {
            PcodeBlockVtx desBB = (PcodeBlockVtx)edge.getStart();
            PcodeBlockVtx srcBB = (PcodeBlockVtx)edge.getEnd();
            if (!GraphAlgorithms.findDominance(rcfg, desBB, this.monitor).contains(srcBB)) {
                Iterator<PcodeOp> iter = srcBB.getCodeBlock().getIterator();
                PcodeOp srcOp =  getLastElement(iter);
                addControlDepFromDominatedBlockToDominator(srcOp, srcBB, desBB, postDominanceTree);
            }
        }
    }
    @Override
    protected void buildDDG() {
        HashMap<Integer, AttributedVertex> vertices = new HashMap<>();
        Iterator<PcodeOpAST> opiter = getPcodeOpIterator();
        try {
            while (opiter.hasNext()) {
                PcodeOpAST op = opiter.next();
                AttributedVertex o = createOpVertex(op);
                instanceMap_ddg.put(op, o);
                for (int i = 0; i < op.getNumInputs(); ++i) {
                    int opcode = op.getOpcode();
                    if ((i == 0) && ((opcode == PcodeOp.LOAD) || (opcode == PcodeOp.STORE))) {
                        continue;
                    }
                    if ((i == 1) && (opcode == PcodeOp.INDIRECT)) {
                        continue;
                    }
                    VarnodeAST vn = (VarnodeAST) op.getInput(i);
                    if (vn != null) {
                        AttributedVertex v = getVarnodeVertex(vertices, vn);
                        createEdge(v, o);
                    }
                }
                VarnodeAST outvn = (VarnodeAST) op.getOutput();
                if (outvn != null) {
                    AttributedVertex outv = getVarnodeVertex(vertices, outvn);
                    if (outv != null) {
                        createEdge(o, outv);
                    }
                }
            }
        }catch (Exception e) {
            printf("error: %s\n", e);
            e.getMessage();
        }
    }
    protected void addCtrlDepToDDG()throws CancelledException{

        for(var srcOp:ctrlDep.keySet()){
//            printf("src:  %s\n",srcOp.toString());
            for (var desOp: ctrlDep.get(srcOp)){
                var srcVtx = instanceMap_ddg.get(srcOp);
                var desVtx = instanceMap_ddg.get(desOp);
                if(desVtx!=null && srcVtx!=null){
//                    printf("        des: %s\n", desOp.toString());
                    AttributedEdge edge = createEdge(srcVtx, desVtx);
                    edge.setAttribute(COLOR_ATTRIBUTE, "Red");
                }
            }
        }
    }
    public void getGraph_ghidra() throws Exception {
        buildRevCFG(hfunc);
        postDominanceTree = GraphAlgorithms.findDominanceTree(rcfg, this.monitor);
        getCtrlDep();
        buildDDG();
        addCtrlDepToDDG();
        displayGraph();
    }
    public JSONObject getGraph_headless() throws Exception {
        buildRevCFG(hfunc);
        postDominanceTree = GraphAlgorithms.findDominanceTree(rcfg, this.monitor);
        getCtrlDep();
        buildDDG();
        addCtrlDepToDDG();
        return dumpGraph();
    }


//    public void run_in_ghidra_simple() throws Exception {
//        init();
//        func = this.getFunctionContaining(this.currentAddress);
//        if (func == null) {
//            Msg.showWarn(this, state.getTool().getToolFrame(), "GraphAST Error",
//                    "No Function at current location");
//            return;
//        }
//
//        decompileFunction();
//        buildRevCFG(hfunc);
//        postDominanceTree = GraphAlgorithms.findDominanceTree(rcfg, this.monitor);
//
//        getCtrlDep();
//        buildDDG();
//        addCtrlDepToDDG();
//
//        displayGraph();
//
//    }


//    @Override
//    public void run_in_ghidra() throws Exception {
//        init();
//        func = this.getFunctionContaining(this.currentAddress);
//        if (func == null) {
//            Msg.showWarn(this, state.getTool().getToolFrame(), "GraphAST Error",
//                    "No Function at current location");
//            return;
//        }
//
//        decompileFunction();
//
//        buildRevCFG(hfunc);
//        for(var node: rcfg.getVertices()){
//            printf(node.getName());
//            printf("\n");
//        }
//        for(var edge: rcfg.getEdges()){
//            printf(edge.toString());
//            printf("\n");
//        }
//
//        postDominanceTree = GraphAlgorithms.findDominanceTree(rcfg, this.monitor);
//        printf("\n\npost dom tree");
//
//        getCtrlDep();
//        printf(ctrlDep.toString());
//        for(var dep:ctrlDep.keySet()){
//            printf("src:  %s\n",dep);
//            printf("sink:  %s\n",ctrlDep.get(dep).toString());
//        }
//
//        buildDDG();
//        addCtrlDepToDDG();
//
//        displayGraph();
//
//    }

    protected JSONObject dumpGraph() throws Exception {
        JSONObject funcOut = new JSONObject();
        HashMap<String, Integer> id_map = new HashMap<>();
        ArrayList<Integer> nodes = new ArrayList<Integer>();
        ArrayList<Integer[]> ddgedges = new ArrayList<Integer[]>();
        ArrayList<Integer[]> cdgedges = new ArrayList<Integer[]>();
        JSONObject nodesVerb = new JSONObject();
        int i = 0;
        for (var node: graph.vertexSet()){
//            printf("\nnode id %s\n",node.getId());
//            printf("node name %s\n",node.getName());
            id_map.put(node.getId(), i);
            nodes.add(i);
            nodesVerb.put(String.format("%d", i), node.getName());
            i++;
        }
        for (AttributedEdge edge: graph.edgeSet()){
//            printf("\nedge src %s\n",graph.getEdgeSource(edge));
//            printf("edge target %s\n",graph.getEdgeTarget(edge));
            var attr = edge.getAttribute(COLOR_ATTRIBUTE);
            if(attr == null){
//                printf("null %d %d 0\n",id_map.get(graph.getEdgeSource(edge).getId()), id_map.get(graph.getEdgeTarget(edge).getId()));
                ddgedges.add(new Integer[] { id_map.get(graph.getEdgeSource(edge).getId()), id_map.get(graph.getEdgeTarget(edge).getId()), 1}); // 0 in past
            }else if (attr.equals("Red")){
//                printf("red %d %d 1\n",id_map.get(graph.getEdgeSource(edge).getId()), id_map.get(graph.getEdgeTarget(edge).getId()));
                cdgedges.add(new Integer[] { id_map.get(graph.getEdgeSource(edge).getId()), id_map.get(graph.getEdgeTarget(edge).getId()), 2});
            }
        }
//        funcOut.put("fname", func.getName());
//        funcOut.put("faddr_ghidra", func.getEntryPoint());
        funcOut.put("nodes", nodes);
        funcOut.put("nverbs", nodesVerb);
        funcOut.put("ddgedges", ddgedges);
        funcOut.put("cdgedges", cdgedges);

//        printf(funcOut.toString());
        return funcOut;
        // idb_path, addr in txt, fun name

    }
}


public class getGraphs extends GhidraScript{
    private DecompInterface decomplib;
    private String logPath;
    private String idb_path;
    public void init() throws Exception {
    }
    public static void writeJson(JSONObject obj, String savePath) {
        try (final Writer writer = new FileWriter(new File(savePath))) {
            obj.write(writer);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    public DecompileResults decompileFunction(Function f) {
        DecompileResults dRes = null;

        try {
            dRes = decomplib.decompileFunction(f, decomplib.getOptions().getDefaultTimeout(), getMonitor());
//			DecompilerSwitchAnalysisCmd cmd = new DecompilerSwitchAnalysisCmd(dRes);
//			cmd.applyTo(currentProgram);
        } catch (Exception exc) {
            printf("EXCEPTION IN DECOMPILATION!\n");
            exc.printStackTrace();
        }
        return dRes;
    }
    private DecompInterface setUpDecompiler(Program program) {
        DecompInterface decompInterface = new DecompInterface();
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
        decompInterface.setOptions(options);
        decompInterface.toggleCCode(true);
        decompInterface.toggleSyntaxTree(true);
        decompInterface.setSimplificationStyle("decompile");
        return decompInterface;
    }
    public void addr_trans(HashMap<Address, String> funs_addrs, String target_info_path) throws IOException {
        ArrayList<String> fun_addrs_raw = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(target_info_path))) {
            String line;
            while ((line = br.readLine()) != null) {
                fun_addrs_raw.add(line);
            }
        } catch (IOException e) {
            printf("file error: %s\n", e);
            write_err_log(this.logPath + "error_log.txt","open file error:"+e+"\n");
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
//            Function function = getFunctionContaining(getAddressFactory().getAddress(addr));
            Function function = getFunctionAt(getAddressFactory().getAddress(addr));
            if (function==null){
                flag = 1;
                break;
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
    public void write_err_log(String logPath, String content) throws IOException {
        File file = new File(logPath);
        FileWriter log = new FileWriter(file, true);
        log.write(content+"\n");
        log.close();
    }

    public void run_headless() throws Exception {
        decomplib = setUpDecompiler(currentProgram);
        if (!decomplib.openProgram(currentProgram)) {
            printf("Decompiler error: %s\n", decomplib.getLastMessage());
            return;
        }
        var args = this.getScriptArgs();

        String target_info_name = args[0]; // arm32-clang-3.5-O0_curl
        String target_info_path = args[1];
        this.logPath = args[2];
        String outputPath = args[2]+"output/";
        this.idb_path = args[3];
        Date now = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String time = dateFormat.format(now);
        printf("\n\n\n--------------------------analyze begin for %s--------------------------------------------------------------\n", target_info_name);
        printf("time: %s\n",time);
        printf("name: %s\n",target_info_name);
        printf("target path: %s\n", target_info_path);
        printf("output path: %s\n", outputPath);

        HashMap<Address, String> funs_addrs = new HashMap<>();
        // transfer addr into ghidra address
        addr_trans(funs_addrs, target_info_path);

        // initialize all the global var
        init();


        JSONObject binOut = new JSONObject();
        for (Address addr: funs_addrs.keySet()){
            String addr_in_funlist = funs_addrs.get(addr);

                Function function = getFunctionAt(addr);
                if (function==null){
                    write_err_log(logPath + "error_log.txt", "ghidra can't find function: "+target_info_path+"  addr: "+addr.toString());
                    printf("canot get function %s at addr: %s", target_info_name, addr.toString());
                    continue;
                }
                try {
                    DecompileResults dRes = decompileFunction(function);
                    HighFunction hfunction = dRes.getHighFunction();

                    printf("\nFound target function %s @ 0x%x %s in %s\n", function.getName(),
                            function.getEntryPoint().getOffset(), addr, this.currentProgram.getName());
//                    printf("addr in dataset: %s, proj name: %s\n", addr, target_info_name);
                    if (hfunction != null) {
//                    PDG pdg = new PDG(decomplib, monitor, state, function);
                        PDG pdg = new PDG(decomplib, monitor, state, function, hfunction);
                        JSONObject dumppedGraph = pdg.getGraph_headless();

                        JSONObject funcOut = new JSONObject();
                        funcOut.put("func_name", function.getName());
                        funcOut.put("PDG", dumppedGraph);
                        binOut.put(addr_in_funlist, funcOut);
                        printf("Analyze function: %s %s successfully\n", function.getName(), target_info_name);
                    }
                }catch(Exception e) {
                    write_err_log(logPath + "error_log.txt", "analyze function "+function.getName()+" failed, "
                            +target_info_path+"  addr: "+addr.toString());
                }
        }
        JSONObject binOutWrap = new JSONObject();
        binOutWrap.put(idb_path, binOut);
        writeJson(binOutWrap, outputPath+this.currentProgram.getName()+".json");
        printf("\n--------------------------analyze end for %s-------------------------------------------------------------\n\n\n", target_info_name);
    }
    public void run_ghidra() throws Exception{
        Function func = this.getFunctionContaining(this.currentAddress);
        if (func == null) {
            Msg.showWarn(this, state.getTool().getToolFrame(), "GraphAST Error",
                    "No Function at current location");
            return;
        }

        DecompileOptions options = new DecompileOptions();
        DecompInterface ifc = new DecompInterface();
        ifc.setOptions(options);

        if (!ifc.openProgram(this.currentProgram)) {
            throw new DecompileException("Decompiler",
                    "Unable to initialize: " + ifc.getLastMessage());
        }
        ifc.setSimplificationStyle("normalize");
        DecompileResults res = ifc.decompileFunction(func, 30, null);
        HighFunction hfunc = res.getHighFunction();
        PluginTool tool = state.getTool();
        // TODO 下拉栏选择图种类
        DDG ddg = new DDG(decomplib, monitor, state, func, hfunc);
        ddg.getGraph_ghidra();
//        CFG cfg = new CFG(decomplib, monitor, state, func, hfunc);
//        cfg.getGraph_ghidra();
//        CDFG cddg = new CDFG(decomplib, monitor, state, func, hfunc);
//        cddg.getGraph_ghidra();
//        PDG pdg = new PDG(decomplib, monitor, state, func, hfunc);
//        pdg.getGraph_ghidra();
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


