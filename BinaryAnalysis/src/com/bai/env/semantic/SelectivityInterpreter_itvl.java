package com.bai.env.semantic;

import com.bai.env.AbsEnv;
import com.bai.env.domain.AbsDomainFactory;
import com.bai.env.domain.Interval;
import com.bai.util.GlobalState;
import generic.stl.Pair;
// ghidra
import ghidra.program.model.pcode.PcodeOp;
import ghidra.program.model.pcode.Varnode;
// abc sovler
import vlab.cs.ucsb.edu.DriverProxy;
import vlab.cs.ucsb.edu.DriverProxy.Option;
import vlab.cs.ucsb.edu.ModelCounter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class SelectivityInterpreter_itvl extends IntervalInterpreter{
    public SelectivityInterpreter_itvl(){
        domainType = "interval";
    }
    public double getProb_CBRANCH(PcodeOp inst, AbsEnv curState){
        double trProbability = -1;

        Varnode condVar = inst.getInput(1);
        PcodeOp condInst = condVar.getDef();
        String opcode = getMnemonicStr(condInst);
        if(!opcode.equals("")){
            Varnode op1 = condInst.getInput(0);
            Varnode op2 = condInst.getInput(1);
            Interval op1Itvl = null, op2Itvl = null;
            if(op1.isConstant()){
                op1Itvl = (Interval) AbsDomainFactory.createAbsDomain(this.domainType, getTrueValue(op1.getOffset(), op1.getSize()));
            }else {
                op1Itvl = (Interval) getAbsValue(op1, curState, condInst);
            }
            if(op2.isConstant()){
                op2Itvl = (Interval) AbsDomainFactory.createAbsDomain(this.domainType, getTrueValue(op2.getOffset(), op2.getSize()));
            }else {
                op2Itvl = (Interval) getAbsValue(op2, curState, condInst);
            }

            List<Pair<String,String>> model_counting_vars = new ArrayList<Pair<String,String>>();
            model_counting_vars.add(new Pair<>("var1","int"));
            model_counting_vars.add(new Pair<>("var2","int"));
            Pair<String,String> cons = getConstraint(opcode, op1Itvl, op2Itvl);
            GlobalState.ghidraScript.print(cons.first+"\n");
            GlobalState.ghidraScript.print(cons.second+"\n");

            trProbability = getModelCount(cons, model_counting_vars);

            GlobalState.ghidraScript.print("True probability " + String.format("%.2f",trProbability) + ",  False probability "+String.format("%.2f", (1-trProbability)) +"\n");

        }
        return trProbability;
    }
    public double getModelCount(Pair<String,String> cons, List<Pair<String,String>> model_counting_vars) {
        float probability = -1;
        String domainConstraint = cons.first, constraint = cons.second;
        ModelCounter modelCounter = new ModelCounter(31, "abc.linear_integer_arithmetic");

        BigDecimal cond_count = modelCounter.getModelCount(constraint, model_counting_vars, false);
        BigDecimal dom_count = modelCounter.getModelCount(domainConstraint, model_counting_vars, false);
//    System.out.println("Branch domain count:" + dom_count);
        return cond_count.doubleValue() / dom_count.doubleValue();
    }

    public String getMnemonicStr(PcodeOp condInst){
        String op = "";
        switch (condInst.getOpcode()){
            // compare op
            case PcodeOp.INT_EQUAL:
                op =  "=";
                break;
            case PcodeOp.INT_NOTEQUAL:
                op =  "!=";
                break;
            case PcodeOp.INT_LESS:
                op = "<";
                break;
            case PcodeOp.INT_SLESS:
                op = "<";
                break;
            case PcodeOp.INT_LESSEQUAL:
                op = "<=";
                break;
            case PcodeOp.INT_SLESSEQUAL:
                op = "<=";
                break;
            default:
                GlobalState.ghidraScript.print("                                    cmp inst "
                        + condInst.getMnemonic() +" is unsupported for constraint sovler now\n");
                op = "";
        }
        return op;
    }
    public String getConstarintOfInterval(Interval var, String name){
        String constraint = "";
        if(!var.isTop() && !var.isBottom()){
            long lower = var.getLower();
            long upper = var.getUpper();
            if(lower != Interval.getNegInf()){
                if(lower<0){
                    constraint += String.format("(assert (>= %s (- %s) ) )\n", name, Long.toString(-lower));
//                    constraint += "(assert (>= " + name + "(- " + Long.toString(lower) +")))\n";
                } else {
                    constraint += String.format("(assert (>= %s %s ) )\n", name, Long.toString(lower));
//                    constraint += "(assert (>= " + name + Long.toString(lower) +"))\n";
                }
            }
            if(upper != Interval.getPosInf()){
                if(upper<0){
                    constraint += String.format("(assert (<= %s (- %s) ) )\n", name, Long.toString(-upper));
//                    constraint += "(assert (<= " + name + "(- " + Long.toString(upper) +")))\n";
                } else {
                    constraint += String.format("(assert (<= %s %s ) )\n", name, Long.toString(upper));
//                    constraint += "(assert (<= " + name + Long.toString(upper) +"))\n";
                }
            }
        }
        return constraint;
    }
    public Pair<String,String> getConstraint(String op, Interval var1, Interval var2){
        String domainConstraint = "(declare-fun var1 () Int)\n" + "(declare-fun var2 () Int)\n";
        domainConstraint += getConstarintOfInterval(var1, "var1");
        domainConstraint += getConstarintOfInterval(var2, "var2");
        String constraint = domainConstraint + "(assert (" + op + " var1 var2 ))\n";
        return new Pair<>(domainConstraint, constraint);
    }

    //    public String getConstraint_v0(String op, Interval var1, Interval var2){
//        String constraint = "(declare-fun var1 () Int)\n" + "(declare-fun var2 () Int)\n";
//        if(!var1.isTop()&&var1.isBottom()){
//            long lower = var1.getLower();
//            long upper = var1.getUpper();
//            if(lower != Interval.getNegInf()){
//               if(lower<0){
//                   constraint += "(assert (>= var1 (- " + Long.toString(lower) +")))\n";
//               } else {
//                   constraint += "(assert (>= var1 " + Long.toString(lower) +"))\n";
//               }
//            }
//            if(upper != Interval.getPosInf()){
//                if(upper<0){
//                    constraint += "(assert (<= var1 (- " + Long.toString(upper) +")))\n";
//                } else {
//                    constraint += "(assert (<= var1 " + Long.toString(upper) +"))\n";
//                }
//            }
//        }
//        if(!var2.isTop()&&var2.isBottom()){
//            long lower = var2.getLower();
//            long upper = var2.getUpper();
//            if(lower != Interval.getNegInf()){
//                if(lower<0){
//                    constraint += "(assert (>= var1 (- " + Long.toString(lower) +")))\n";
//                } else {
//                    constraint += "(assert (>= var1 " + Long.toString(lower) +"))\n";
//                }
//            }
//            if(upper != Interval.getPosInf()){
//                if(upper<0){
//                    constraint += "(assert (<= var1 (- " + Long.toString(upper) +")))\n";
//                } else {
//                    constraint += "(assert (<= var1 " + Long.toString(upper) +"))\n";
//                }
//            }
//        }
//        return null;
//    }
}
