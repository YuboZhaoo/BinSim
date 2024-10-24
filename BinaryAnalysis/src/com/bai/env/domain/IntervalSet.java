package com.bai.env.domain;

import ghidra.program.model.pcode.VarnodeAST;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

// TODO not implemented
public class IntervalSet{
    public HashMap<VarnodeAST,Interval> state;
    public IntervalSet(){
        this.state = new HashMap<>();
    }
    public void join(IntervalSet other){
        for (Map.Entry<VarnodeAST, Interval> entry : other.state.entrySet()) {
            VarnodeAST var = entry.getKey();
            Interval otherInterval = entry.getValue();
            if (this.state.containsKey(var)) {
                Interval currentInterval = this.state.get(var);
                this.state.put(var, (Interval) currentInterval.join(otherInterval));
            } else {
                this.state.put(var, otherInterval);
            }
        }
    }
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IntervalSet that = (IntervalSet) o;
        return Objects.equals(state, that.state);
    }
    public Interval getInterval(VarnodeAST var) {
        return state.getOrDefault(var, new Interval());
    }
    public void setInterval(VarnodeAST var, Interval interval) {
        state.put(var, interval);
    }

}

