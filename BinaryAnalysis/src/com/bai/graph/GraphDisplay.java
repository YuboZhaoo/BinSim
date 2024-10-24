package com.bai.graph;

import com.bai.util.GlobalState;
import ghidra.app.decompiler.DecompInterface;
import ghidra.app.plugin.core.graph.AddressBasedGraphDisplayListener;
import ghidra.app.script.GhidraState;
import ghidra.app.services.GraphDisplayBroker;
import ghidra.framework.plugintool.PluginTool;
import ghidra.program.model.address.Address;
import ghidra.program.model.address.AddressSet;
import ghidra.program.model.address.AddressSetView;
import ghidra.program.model.address.AddressSpace;
import ghidra.program.model.lang.Register;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.Program;
import ghidra.program.model.pcode.*;
import ghidra.service.graph.AttributedEdge;
import ghidra.service.graph.AttributedGraph;
import ghidra.service.graph.AttributedVertex;
import ghidra.service.graph.GraphDisplayListener;
import ghidra.util.Msg;
import ghidra.util.task.TaskMonitor;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

public class GraphDisplay {
    protected TaskMonitor monitor;
    protected DecompInterface decomplib;

    protected Function func;
    protected HighFunction hfunc;
    protected GhidraState state;
    protected AttributedGraph graph;
    protected static final String COLOR_ATTRIBUTE = "Color";
    protected static final String ICON_ATTRIBUTE = "Icon";



    // member var must be inited before use them
    public GraphDisplay(Function func, HighFunction hfunc) {
        this.decomplib = GlobalState.decomplib;
        this.monitor = GlobalState.monitor;
        this.state = GlobalState.state;
        this.func = func;
        this.hfunc = hfunc;
        this.graph = new AttributedGraph();

    }

    protected AttributedEdge createEdge(AttributedVertex in, AttributedVertex out) {
        return graph.addEdge(in, out);
    }
    // for bb level graph, such as cfg_bb
    protected AttributedEdge createEdge(PcodeBlockBasic in, PcodeBlockBasic out) {
        AttributedVertex from = graph.getVertex(getBBKey(in));
        AttributedVertex to = graph.getVertex(getBBKey(out));
        return graph.addEdge(from, to);
    }
    // for inst level graph, such as cfg_inst
    protected AttributedEdge createEdge(PcodeOp in, PcodeOp out) {
        AttributedVertex from = graph.getVertex(getOpKey( (PcodeOpAST) in ));
        AttributedVertex to = graph.getVertex(getOpKey( (PcodeOpAST) out ));
        return graph.addEdge(from, to);
    }
    // for op level graph, such as ddg_op
    protected AttributedEdge createEdge(PcodeOp in, VarnodeAST out,  HashMap<Integer, AttributedVertex> vertices) {
        AttributedVertex from = graph.getVertex(getOpKey( (PcodeOpAST) in ));
        AttributedVertex to = getVarnodeVertex(vertices, out);

        return graph.addEdge(from, to);
    }
    protected AttributedEdge createEdge(VarnodeAST in, PcodeOp out,  HashMap<Integer, AttributedVertex> vertices) {
        AttributedVertex from = getVarnodeVertex(vertices, in);
        AttributedVertex to = graph.getVertex(getOpKey( (PcodeOpAST) out ));
        return graph.addEdge(from, to);
    }
    protected String getBBKey(PcodeBlockBasic bb) {
        String id =
                bb.getIndex() + " o " + bb.toString();
        return id;
    }
    protected String getOpKey(PcodeOpAST op) { // get the id of the pcode inst
        SequenceNumber sq = op.getSeqnum();
        String id =
                sq.getTarget().toString(true) + " o " + Integer.toString(op.getSeqnum().getTime());
        return id;
    }
    protected String getVarnodeKey(VarnodeAST vn) {
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
        PluginTool tool = GlobalState.state.getTool();
        if (tool == null) {
            GlobalState.ghidraScript.println("Script is not running in GUI");
        }
        GraphDisplayBroker graphDisplayBroker = tool.getService(GraphDisplayBroker.class);
        if (graphDisplayBroker == null) {
            Msg.showError(this, tool.getToolFrame(), "GraphAST Error",
                    "No graph display providers found: Please add a graph display provider to your tool");
            return;
        }
        ghidra.service.graph.GraphDisplay graphDisplay =
                graphDisplayBroker.getDefaultGraphDisplay(false, monitor);
        String description = "AST Data Flow Graph For " + func.getName();

        graphDisplay.setGraph(graph, description, false, monitor);

        // Install a handler so the selection/location will map
        graphDisplay.setGraphDisplayListener(
                new ASTGraphDisplayListener(tool, graphDisplay, hfunc, func.getProgram()));
    }

    class ASTGraphDisplayListener extends AddressBasedGraphDisplayListener {

        HighFunction highfunc;

        public ASTGraphDisplayListener(PluginTool tool, ghidra.service.graph.GraphDisplay display, HighFunction high,
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
        public GraphDisplayListener cloneWith(ghidra.service.graph.GraphDisplay graphDisplay) {
            return new ASTGraphDisplayListener(tool, graphDisplay, highfunc, GlobalState.currentProgram);
        }
    }

}
