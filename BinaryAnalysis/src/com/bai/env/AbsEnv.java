package com.bai.env;

import com.bai.env.domain.AbsDomain;

import com.bai.env.domain.AbsDomainFactory;
import ghidra.program.model.address.Address;
import ghidra.program.model.address.AddressOutOfBoundsException;
import ghidra.program.model.lang.Register;
import ghidra.program.model.mem.MemoryAccessException;
import com.bai.env.region.Global;
import com.bai.env.region.Reg;
import com.bai.env.region.RegionBase;
import com.bai.util.GlobalState;
import org.javimmutable.collections.Holder;
import org.javimmutable.collections.JImmutableMap.Entry;
import org.javimmutable.collections.tree.JImmutableTreeMap;

/**
 * Abstract Environment
 */
public class AbsEnv {

    private JImmutableTreeMap<ALoc, AbsDomain> envMap;

    private String domainType;

    public void setDomainType(String domainType){ this.domainType = domainType;}

    public String getDomainType(){ return this.domainType;}

    /**
     * Constructor for an empty abstract environment
     */
//    public AbsEnv() {
//        envMap = JImmutableTreeMap.of();
//    }
    public AbsEnv(String domainType) {
        envMap = JImmutableTreeMap.of();
        this.domainType = domainType;
    }

    public boolean equals(AbsEnv other){
        return this.envMap.equals(other.envMap);
    }



    /**
     * Shallow copy constructor from others
     */
    public AbsEnv(AbsEnv other) {
        envMap = other.envMap;
        this.domainType = other.getDomainType();
    }


    /**
     * Constructor with an inner map
     */
    public AbsEnv(JImmutableTreeMap<ALoc, AbsDomain> envMap) {
        this.envMap = envMap;
    }

    /**
     * Getter for the inner map
     */
    public JImmutableTreeMap<ALoc, AbsDomain> getEnvMap() {
        return envMap;
    }

    /**
     * Join operation for this AbsEnv and the other one
     * @param other The other AbsEnv to be joined into this one
     * @return null if the joined result is the same as the old this one, create a new AbsEnv otherwise.
     */
    public AbsEnv join(AbsEnv other) {
        AbsEnv res = new AbsEnv(this);
        for (Entry<ALoc, AbsDomain> entry : other.envMap) {
            res.set(entry.getKey(), entry.getValue(), false);
        }
        if (!res.envMap.equals(this.envMap)) {
            return res;
        }
        // unchanged
        return null;
    }

    private void setEmptyALoc(ALoc aLoc, AbsDomain oldKSet, AbsDomain newKSet, boolean isStrongUpdate) {
        assert envMap.findEntry(aLoc).isEmpty();
        if (isStrongUpdate) {
            if (!newKSet.isBottom()) {
                envMap = envMap.assign(aLoc, newKSet);
            }
        } else {
            assert (!oldKSet.isBottom());
            AbsDomain tmp = oldKSet.join(newKSet);
            if (tmp != null) {
                assert (!tmp.isBottom());
                envMap = envMap.assign(aLoc, tmp);
            } else {
                envMap = envMap.assign(aLoc, oldKSet);
            }
        }
    }

    private void setFilledALoc(ALoc aLoc, AbsDomain oldKSet, AbsDomain newKSet, boolean isStrongUpdate) {
        assert envMap.findEntry(aLoc).isFilled();
        if (isStrongUpdate) {
            if (newKSet.isBottom()) {
                envMap = envMap.delete(aLoc);
            } else if (!newKSet.equals(oldKSet)) {
                envMap = envMap.assign(aLoc, newKSet);
            }
        } else {
            assert (!oldKSet.isBottom());
            AbsDomain tmp = oldKSet.join(newKSet);
            if (tmp != null) {
                assert (!tmp.isBottom());
                envMap = envMap.assign(aLoc, tmp);
            }
        }
    }

    private AbsDomain loadFromProgram(ALoc aLoc) {
        assert aLoc.region.isGlobal();
        AbsDomain res;
        try {
            Address address = GlobalState.flatAPI.toAddr(aLoc.begin);
            byte[] buf = GlobalState.flatAPI.getBytes(address, aLoc.len);
            res = AbsDomainFactory.createAbsDomain(this.domainType, buf);
        } catch (MemoryAccessException | AddressOutOfBoundsException e) {
            res = AbsDomainFactory.createAbsDomain(this.domainType);
        }
        return res;
    }

    /**
     * Update a record with a pair of ALoc and KSet inside this AbsEnv
     * @param newALoc ALoc as the key for this record
     * @param newKSet KSet as the value for this record
     * @param isStrongUpdate Flag to indicate strong or weak update
     */
    public void set(ALoc newALoc, AbsDomain newKSet, boolean isStrongUpdate) {
//        if (!newKSet.isTop()) {
//            assert newALoc.len * 8 == newKSet.getBits();
//        }

        Holder<Entry<ALoc, AbsDomain>> holder = envMap.findEntry(newALoc);
        if (holder.isEmpty()) {
            // none overlap
            if (!newKSet.isBottom()) {
                envMap = envMap.assign(newALoc, newKSet);
            }
        } else {
            Entry<ALoc, AbsDomain> oldEntry = holder.getValue();
            ALoc oldALoc = oldEntry.getKey();
            AbsDomain oldKSet = oldEntry.getValue();

            long newBegin = newALoc.begin;
            long newEnd = newALoc.begin + newALoc.len;
            long oldBegin = oldALoc.begin;
            long oldEnd = oldALoc.begin + oldALoc.len;

            assert oldALoc.region.equals(newALoc.region);
            RegionBase region = newALoc.region;

            if (newALoc.isExactly(oldALoc)) {
                // AAAAAAAA
                // BBBBBBBB
                setFilledALoc(newALoc, oldKSet, newKSet, isStrongUpdate);
            }
        }
    }

    /**
     * Get KSet for a given ALoc
     * @param aLoc A given ALoc to be queried on
     * @return KSet as the result, may be Bottom, Noraml or Top KSet
     */
    public AbsDomain get(ALoc aLoc) {
        Holder<Entry<ALoc, AbsDomain>> holder = envMap.findEntry(aLoc);
        if (holder.isEmpty()) {
            if (aLoc.region.isGlobal()) {
                return loadFromProgram(aLoc);
            }
//            return new KSet(aLoc.len * 8);
            return AbsDomainFactory.createAbsDomain(this.domainType);
        }

        Entry<ALoc, AbsDomain> oldEntry = holder.getValue();
        ALoc oldALoc = oldEntry.getKey();
        AbsDomain oldKSet = oldEntry.getValue();

        long newBegin = aLoc.begin;
        long newEnd = aLoc.begin + aLoc.len;
        long oldBegin = oldALoc.begin;
        long oldEnd = oldALoc.begin + oldALoc.len;

        RegionBase region = aLoc.region;
        assert aLoc.region.equals(oldALoc.region);

        assert !oldKSet.isBottom();

        if (aLoc.isExactly(oldALoc)) {
            // AAAAAAAA
            // BBBBBBBB
            return oldKSet;
        }else {
            //TODO fix this 08.20
            GlobalState.ghidraScript.print("??????????\n");
            return oldKSet;
        }
//        assert false : "Not Reachable";
//        return null;
    }

    /**
     * Return the entry inside the inner map which intersects with a given ALoc,
     * return null if no intersection exists in envMap.
     * This is useful for tainting, avoid cutting up ALoc frequently.
     * @param aLoc ALoc to be queried on
     * @return the entry which intersects with the given ALoc, null otherwise.
     */
    public Entry<ALoc, AbsDomain> getOverlapEntry(ALoc aLoc) {
        Holder<Entry<ALoc, AbsDomain>> holder = envMap.findEntry(aLoc);
        return holder.getValueOrNull();
    }

    private void writeEntry(StringBuilder sb, ALoc aLoc, AbsDomain kSet) {
        sb.append(aLoc).append(" -> ").append(kSet).append("\n");
    }

//    private void writeRegEntry(StringBuilder sb, ALoc aLoc, AbsDomain kSet) {
//        Register register = GlobalState.currentProgram.getLanguage()
//                .getRegister(GlobalState.flatAPI.getAddressFactory().getRegisterSpace(), aLoc.getBegin(),
//                        aLoc.getLen());
//        if (register == null) {
//            return;
//        }
//        if (!register.isBaseRegister()) {
//            Register parentReg = register.getParentRegister();
//            ALoc parentALoc = ALoc.getALoc(Reg.getInstance(), parentReg.getOffset(), parentReg.getNumBytes());
//            AbsDomain parentKSet = this.get(parentALoc);
//            if (parentKSet.isBottom()) {
//                writeEntry(sb, aLoc, kSet);
//            } else {
//                sb.append(parentReg).append(" -> ").append(parentKSet).append("\n");
//            }
//        } else {
//            writeEntry(sb, aLoc, kSet);
//        }
//    }
//
//    @Override
//    public String toString() {
//        StringBuilder stringBuilder = new StringBuilder();
//        for (Entry<ALoc, AbsDomain> entry : envMap) {
//            ALoc aLoc = entry.getKey();
//            AbsDomain kSet = entry.getValue();
//            if (aLoc.getRegion().isReg()) {
//                writeRegEntry(stringBuilder, aLoc, kSet);
//            } else {
//                writeEntry(stringBuilder, aLoc, kSet);
//            }
//
//        }
//        return stringBuilder.toString();
//    }

}
