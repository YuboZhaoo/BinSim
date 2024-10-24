package com.bai.util;

import com.bai.env.ALoc;

import com.bai.env.region.Heap;
import com.bai.env.region.Local;

import ghidra.app.decompiler.DecompInterface;
import ghidra.app.script.GhidraScript;
import ghidra.app.script.GhidraState;
import ghidra.program.flatapi.FlatProgramAPI;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.Program;
import ghidra.util.task.TaskMonitor;

/**
 * Global state of current analysis.
 */
public class GlobalState {

    public static Program currentProgram;

    public static FlatProgramAPI flatAPI;

    public static GhidraScript ghidraScript;

    //TODO not support mips
    // TODO this is needed in ALoc and Interpreter
    public static Architecture arch;

    // use for display graph

    public static DecompInterface decomplib;
    public static TaskMonitor monitor;
    public static GhidraState state;



    public static boolean debug;

    /** e_entry from ELF header **/
    public static Function eEntryFunction;

    /**
     * @hidden
     */
    public static void reset() {

        ALoc.resetPool();

        Heap.resetPool();
        Local.resetPool();

        System.gc();
    }
}
