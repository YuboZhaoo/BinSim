package com.bai.graph;

import com.bai.util.GlobalState;
import ghidra.program.model.lang.Language;
import ghidra.program.model.listing.Function;
import ghidra.program.model.pcode.*;
import ghidra.service.graph.AttributedEdge;
import ghidra.service.graph.AttributedVertex;
import org.json.JSONObject;

import java.util.*;

// FIXME
public class LDDG extends GraphBasic{
    class LongDepAnalysis{
        private Language language;
        private Set<PcodeOp> sources;
        private Set<PcodeOp> sinks;
        private HashMap<PcodeOp, HashSet<PcodeOp>> longRangeDependencies;
        private HighFunction hfunc;
        private GraphForAnalysis ddg_anls;
        class GraphForAnalysis {
            private HashMap<PcodeOp, Set<PcodeOp>> adjacencyList;
            private HashMap<PcodeOp, Integer> inDegreeMap;

            public GraphForAnalysis() {
                this.adjacencyList = new HashMap<>();
                this.inDegreeMap = new HashMap<>();
            }

            public void addNode(PcodeOp node) {
                adjacencyList.putIfAbsent(node, new HashSet<>());
                inDegreeMap.putIfAbsent(node, 0);
            }

            public void addEdge(PcodeOp from, PcodeOp to) {
                adjacencyList.putIfAbsent(from, new HashSet<>());
                adjacencyList.putIfAbsent(to, new HashSet<>());
                adjacencyList.get(from).add(to);
                inDegreeMap.put(to, inDegreeMap.getOrDefault(to, 0) + 1);
            }

            public Set<PcodeOp> getSuccessors(PcodeOp node) {
                return adjacencyList.getOrDefault(node, Collections.emptySet());
            }

            public HashSet<PcodeOp> getEntryNodes() {
                HashSet<PcodeOp> entryNodes = new HashSet<>();
                for (HashMap.Entry<PcodeOp, Integer> entry : inDegreeMap.entrySet()) {
                    if (entry.getValue() == 0) {
                        entryNodes.add(entry.getKey());
                    }
                }
                return entryNodes;
            }

            public Set<PcodeOp> getAllNodes() {
                return adjacencyList.keySet();
            }

            public int getEdgeNum() {
                int edgeCount = 0;
                for (Set<PcodeOp> edges : adjacencyList.values()) {
                    edgeCount += edges.size();
                }
                return edgeCount;
            }
        }
        protected void buildDDG_analysis() {
            // Populate the analysis graph from the DDG
            Iterator<PcodeOpAST> opIter = hfunc.getPcodeOps();
            while (opIter.hasNext()) {
                PcodeOpAST op = opIter.next();
                ddg_anls.addNode(op);
                for (int i = 0; i < op.getNumInputs(); ++i) {
                    Varnode vnode = op.getInput(i);
                    PcodeOpAST op_def = (PcodeOpAST)vnode.getDef();
                    if(op_def!=null){
                        ddg_anls.addNode(op_def);
                        ddg_anls.addEdge(op_def, op);
                    }
                }
            }
        }
        public HashMap<PcodeOp, HashSet<PcodeOp>> getLongDep() {
            return longRangeDependencies;
        }
        public LongDepAnalysis(HighFunction highFunction){
            hfunc = highFunction;
            sources = new HashSet<>();
            sinks = new HashSet<>();
            longRangeDependencies = new HashMap<>();
            language = highFunction.getLanguage();
            ddg_anls = new GraphForAnalysis();
            solver();
        }
        private void solver(){
            buildDDG_analysis();
            identifySourcesAndSinks();
            performTaintAnalysis();
        }
        public String touchMemAddr(Varnode vaddress){
            if (vaddress.toString(language).equals("RSI")) {
                return "ARG2";
            } else if (vaddress.toString(language).equals("RDX")) {
                return "ARG3";
            } else if (vaddress.toString(language).equals("RCX")) {
                return "ARG4";
            } else if (vaddress.toString(language).equals("R8")) {
                return "ARG5";
            } else if (vaddress.toString(language).equals("R9")) {
                return "ARG6";
            }
            return null;
        }
        // Identify sources and sinks
        protected void identifySourcesAndSinks() {
            Iterator<PcodeOpAST> opIter = hfunc.getPcodeOps();
            while (opIter.hasNext()) {
                PcodeOp op = opIter.next();
                if (isSource(op)) {
                    sources.add(op);
                }
                if (isSink(op)) {
                    sinks.add(op);
                }
            }
        }
        protected boolean isSource(PcodeOp op) {
            // Implement logic to identify sources (function parameters, input, reads from global/heap memory)
            switch (op.getOpcode()){
                case PcodeOp.LOAD:
                    String mem = touchMemAddr(op.getInput(1));
                    if(mem!=null){
                        return true;
                    }
                    if(op.getInput(1).getAddress().isMemoryAddress()||op.getInput(1).isUnique()){
                        return true;
                    }
                    break;
                case PcodeOp.COPY:
                    String mem2 = touchMemAddr(op.getInput(0));
                    if(mem2!=null){
                        return true;
                    }
                    if(op.getInput(0).isAddress()){
                        return true;
                    }
                    break;
                case PcodeOp.INDIRECT:
                case PcodeOp.CALL:
                case PcodeOp.CALLIND:
                case PcodeOp.CALLOTHER:
                    var output = op.getOutput();
                    if(output != null){
                        return true;
                    }
                    break;
                default:
                    return false;
            }
            return false;
        }
        protected boolean isSink(PcodeOp op) {
            // Implement logic to identify sinks (return values, output, writes to global/heap memory)
            switch ( op.getOpcode()){
                case PcodeOp.STORE:
                    String mem_s = touchMemAddr(op.getInput(1));
                    if(mem_s!=null){
                        return true;
                    }
                    if(op.getInput(1).getAddress().isMemoryAddress()||op.getInput(1).isUnique()){
                        return true;
                    }
                    break;

                case PcodeOp.COPY:
                    mem_s = touchMemAddr(op.getOutput());
                    if(mem_s!=null){
                        return true;
                    }
                    if(op.getOutput().isAddress()){
                        return true;
                    }
                    break;

                case PcodeOp.CALL:
                case PcodeOp.CALLIND:
                case PcodeOp.CALLOTHER:
                case PcodeOp.INDIRECT:
                    int input_num = op.getNumInputs();
                    if(input_num>0){
                        return true;
                    }
                    break;
                case PcodeOp.RETURN:
                    return true;
            }
            return false;
        }
        protected class PcodeOp_Srcs {
            PcodeOp op;
            HashSet<PcodeOp> sources;
            PcodeOp_Srcs(PcodeOp op, HashSet<PcodeOp> sources) {
                this.op = op;
                this.sources = sources;
            }
        }
        protected void performTaintAnalysis() {
            Queue<PcodeOp_Srcs> worklist = new LinkedList<>();
            HashMap<PcodeOp, HashSet<PcodeOp>> absStates = new HashMap<>();
            for (PcodeOp entryNode : ddg_anls.getEntryNodes()) {
                worklist.add(new PcodeOp_Srcs(entryNode, new HashSet<>()));
                GlobalState.ghidraScript.print(entryNode+"\n");
//            absStates.put(entryNode, new HashSet<>());
            }
//        printf("\n\nperformTaintAnalysis\n");
//        Map<PcodeOpAST, Set<PcodeOpAST>> taintFlow = new HashMap<>();

            while (!worklist.isEmpty()) {
                PcodeOp_Srcs current = worklist.poll();
                PcodeOp currentOp = current.op;
                HashSet<PcodeOp> currentSources = current.sources;

                // Update state with incoming sources
                boolean stateChanged = false;
                HashSet<PcodeOp> state = absStates.getOrDefault(currentOp, null);

                if (state == null) {
                    state = new HashSet<>();
                    stateChanged = true;
                }


                stateChanged = state.addAll(currentSources) || stateChanged;

                // If current operation is a source, add it to the state
                if (sources.contains(currentOp)) {
                    stateChanged = state.add(currentOp) || stateChanged;
                }

                // If the state changed, propagate it to successors
                if (stateChanged) {
                    absStates.put(currentOp, state);
                    for (PcodeOp succOp : ddg_anls.getSuccessors(currentOp)) {
                        worklist.add(new PcodeOp_Srcs(succOp, new HashSet<>(state)));
                    }
                }
                // If the current operation is a sink, record the long-range dependency
                if (sinks.contains(currentOp)) {
                    longRangeDependencies.putIfAbsent(currentOp, new HashSet<>());
                    longRangeDependencies.get(currentOp).addAll(state);
//                printf("sink: %s\n",currentOp.toString());
//                for(var s:state){
//                    printf("        %s\n",s.toString());
//                }
                }
            }
        }
    }
    public class LDDG_OP extends Graph_OP{
        private LongDepAnalysis longDepAnal;
        private HashMap<PcodeOp, HashSet<PcodeOp>> longRangeDependencies;
        public LDDG_OP(HighFunction highFunction){
            super();
            func = highFunction.getFunction();
            hfunc = highFunction;
            longDepAnal = new LongDepAnalysis(hfunc);
            longRangeDependencies = longDepAnal.getLongDep();
//            edges_o2o = longRangeDependencies;
            buildGraph();
        }

        protected void buildGraph() {
            Iterator<PcodeOpAST> opiter = hfunc.getPcodeOps();
            while (opiter.hasNext()) {
                PcodeOpAST op = opiter.next();
                opnodes.add(op);

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
                        varnodes.add(vn);
                        addEdge_v2o(vn, op);
                    }
                }
                VarnodeAST outvn = (VarnodeAST) op.getOutput();
                if (outvn != null) {
                    varnodes.add(outvn);
                    addEdge_o2v(op, outvn);
                }
            }
            for(PcodeOp src: longRangeDependencies.keySet()){
                for(PcodeOp sink: longRangeDependencies.get(src)){
                    if(!src.equals(sink)){
                        addEdge_o2o(src,sink);
                        GlobalState.ghidraScript.print("jisdjfisjidjfsf\n");
                    }
                }
            }
        }

        // dump to json
        public JSONObject dump(){
//            DDGJson ddg_json = new DDGJson(hfunc);
//            return ddg_json.dumpGraph();

            GraphJson json = new GraphJson(hfunc);
            return json.dumpGraph_OP(opnodes, varnodes, edges_o2v, edges_v2o, edges_o2o);
        }

        // display in ghidra
        private DDGDisplay displayer;
        public void display() throws Exception{
            displayer = new DDGDisplay(func, hfunc);
            displayer.display();
        }
        class DDGDisplay extends GraphDisplay{
            public DDGDisplay(Function func, HighFunction hfunc){
                super(func, hfunc);
            }
            public void buildAttrGraph(){
                HashMap<Integer, AttributedVertex> vertices = new HashMap<>();
                for (PcodeOp op: opnodes){
                    AttributedVertex o = createOpVertex((PcodeOpAST) op);
                }
                for (VarnodeAST vn: varnodes){
                    AttributedVertex o = getVarnodeVertex(vertices, vn);
                }
                for (PcodeOp from: edges_o2v.keySet()){
                    for(VarnodeAST to: edges_o2v.get(from)){
                        createEdge(from, to, vertices);
                    }
                }
                for (VarnodeAST from: edges_v2o.keySet()){
                    for(PcodeOp to: edges_v2o.get(from)){
                        createEdge(from, to, vertices);
                    }
                }
                for (PcodeOp from: edges_o2o.keySet()){
                    for(PcodeOp to: edges_o2o.get(from)){
                        AttributedEdge edge = createEdge(from, to);
                        edge.setAttribute(COLOR_ATTRIBUTE, "Purple");
                    }
                }
            }
            public void display() throws Exception {
                buildAttrGraph();
                displayGraph();
            }
        }
    }

    public class LDDG_INST extends Graph_INST{
        public LDDG_INST(HighFunction highFunction){
            super();
            func = highFunction.getFunction();
            hfunc = highFunction;
            buildGraph(hfunc);
        }
        public void buildGraph(HighFunction hfunc){
            Iterator<PcodeOpAST> opiter = hfunc.getPcodeOps();
            while (opiter.hasNext()) {
                PcodeOpAST op = opiter.next();
                nodes.add(op);
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
                        PcodeOp src = vn.getDef();
                        if(src!=null){
                            nodes.add(src);
                            addEdge(src, op);
                        }
                    }
                }
            }
        }

        // dump to json
        public JSONObject dump(){
            GraphJson json = new GraphJson(hfunc);
            return json.dumpGraph_INST(nodes, edges);
        }

        // display ddg in ghidra
        private DDGDisplay displayer;
        public void display() throws Exception{
            displayer = new DDGDisplay(func, hfunc);
            displayer.display();
        }
        class DDGDisplay extends GraphDisplay {
            public DDGDisplay(Function func, HighFunction hfunc){
                super(func, hfunc);
            }
            @Override
            protected AttributedVertex createOpVertex(PcodeOpAST op) { // create node of pcode operation
                String name = op.toString();
                String id = getOpKey(op);
                AttributedVertex vert = graph.addVertex(id, name);
                vert.setAttribute(ICON_ATTRIBUTE, "Square");
                return vert;
            }
            public void buildAttrGraph(){
                for (PcodeOp op: nodes){
                    AttributedVertex o = displayer.createOpVertex((PcodeOpAST) op);
                }
                for (PcodeOp from: edges.keySet()){
                    for(PcodeOp to: edges.get(from)){
                        createEdge(from, to);
                    }
                }
            }
            public void display() throws Exception {
                buildAttrGraph();
                displayGraph();
//                GlobalState.ghidraScript.printf("attr graph node num %d\n", graph.vertexSet().size());
//                for(PcodeOp node:nodes){
//                    GlobalState.ghidraScript.printf("%s\n", node.toString());
//                }
            }
        }

    }
}
