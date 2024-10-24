package com.bai.env.domain;

import com.bai.util.GlobalState;

public class AbsDomainFactory {
    public static AbsDomain createAbsDomain(String domainType){
        if(domainType.equals("interval")){
            return new Interval();
        }
        GlobalState.ghidraScript.print("no such abs domain:"+domainType+"\n");
        return null;
    }

    public static long bytesTolong(byte[] bytes) {
        assert bytes.length <= 8;
        int len = bytes.length;

        long res = 0;
        if (GlobalState.arch.isLittleEndian()) {
            for (int i = 0; i < len; i++) {
                res |= (bytes[i] & 0xFFL) << (i * 8);
            }
        } else {
            for (int i = len - 1; i >= 0; i--) {
                res |= (bytes[i] & 0xFFL) << ((len - i - 1) * 8);
            }
        }
        return res;
    }

    public static AbsDomain createAbsDomain(String domainType, byte[] buf){
        if(domainType.equals("interval")){
            return new Interval(bytesTolong(buf));
        }
        GlobalState.ghidraScript.print("no such abs domain:"+domainType+"\n");
        return null;
    }

    public static AbsDomain createAbsDomain(String domainType, long constant){
        if(domainType.equals("interval")){
            return new Interval(constant);
        }
        GlobalState.ghidraScript.print("no such abs domain:"+domainType+"\n");
        return null;
    }
    public static AbsDomain createAbsDomain(String domainType, long constant1, long constant2){
        if(domainType.equals("interval")){
            return new Interval(constant1, constant2);
        }
        GlobalState.ghidraScript.print("no such abs domain:"+domainType+"\n");
        return null;
    }
    public static AbsDomain createAbsDomain(String domainType, boolean isTop){
        if(domainType.equals("interval")){
            return new Interval(isTop);
        }
        GlobalState.ghidraScript.print("no such abs domain:"+domainType+"\n");
        return null;
    }
}
