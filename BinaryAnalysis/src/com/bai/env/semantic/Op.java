package com.bai.env.semantic;

import com.bai.env.domain.IntervalSet;
import ghidra.program.model.pcode.HighFunction;
import ghidra.program.model.pcode.PcodeOp;

public class Op {
    public void getCFG(HighFunction hfunc){
        hfunc.getBasicBlocks();
    }
    public IntervalSet transferFunction(PcodeOp inst, IntervalSet curState){
        switch (inst.getOpcode()) {
            case PcodeOp.UNIMPLEMENTED:
                break;
            case PcodeOp.COPY:
            case PcodeOp.LOAD:
            case PcodeOp.BRANCH:
                break;
            case PcodeOp.CBRANCH:
                break;
            case PcodeOp.BRANCHIND:
                break;
            case PcodeOp.CALL:
            case PcodeOp.CALLIND:
                break;
            case PcodeOp.CALLOTHER:
                break;
            case PcodeOp.RETURN:
                break;
            case PcodeOp.BOOL_AND:
            case PcodeOp.BOOL_OR:
            case PcodeOp.FLOAT_EQUAL:
            case PcodeOp.FLOAT_NOTEQUAL:
            case PcodeOp.FLOAT_LESS:
            case PcodeOp.FLOAT_LESSEQUAL:
            case PcodeOp.INT_EQUAL:
            case PcodeOp.INT_NOTEQUAL:
            case PcodeOp.INT_SLESS:
            case PcodeOp.INT_SLESSEQUAL:
            case PcodeOp.INT_LESS:
            case PcodeOp.INT_LESSEQUAL:
            case PcodeOp.INT_ZEXT:
            case PcodeOp.INT_SEXT:
            case PcodeOp.FLOAT_ADD:
            case PcodeOp.INT_CARRY:
            case PcodeOp.INT_SCARRY:
            case PcodeOp.INT_ADD:
            case PcodeOp.FLOAT_SUB:
            case PcodeOp.INT_SBORROW:
            case PcodeOp.INT_SUB:
            case PcodeOp.SUBPIECE:
            case PcodeOp.BOOL_NEGATE:
            case PcodeOp.INT_NEGATE:
            case PcodeOp.INT_2COMP:
            case PcodeOp.FLOAT_NEG:
            case PcodeOp.INDIRECT:
            case PcodeOp.FLOAT_NAN:
            case PcodeOp.FLOAT_ABS:
            case PcodeOp.FLOAT_SQRT:
            case PcodeOp.FLOAT_INT2FLOAT:
            case PcodeOp.FLOAT_FLOAT2FLOAT:
            case PcodeOp.FLOAT_TRUNC:
            case PcodeOp.FLOAT_CEIL:
            case PcodeOp.FLOAT_FLOOR:
            case PcodeOp.FLOAT_ROUND:

            case PcodeOp.PIECE:

            case PcodeOp.BOOL_XOR:
            case PcodeOp.INT_XOR:

            case PcodeOp.INT_LEFT:

            case PcodeOp.INT_RIGHT:
            case PcodeOp.INT_SRIGHT:

            case PcodeOp.INT_AND:

            case PcodeOp.INT_OR:
            case PcodeOp.INT_MULT:
            case PcodeOp.FLOAT_MULT:
            case PcodeOp.INT_DIV:
            case PcodeOp.INT_SDIV:
            case PcodeOp.FLOAT_DIV:
            case PcodeOp.INT_REM:
            case PcodeOp.INT_SREM:
            case PcodeOp.MULTIEQUAL:
            case PcodeOp.CAST:
            case PcodeOp.PTRADD:
            case PcodeOp.PTRSUB:
            case PcodeOp.CPOOLREF:
            case PcodeOp.NEW:
            default:
                return curState;

        }
        return curState;
    }
}