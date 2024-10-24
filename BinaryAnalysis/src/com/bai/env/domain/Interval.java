package com.bai.env.domain;

import com.bai.env.AbsEnv;
import com.bai.util.GlobalState;

import java.math.BigInteger;

// Interval domain
public class Interval extends AbsDomain {
    private static final long NEG_INF = Long.MIN_VALUE;
    private static final long POS_INF = Long.MAX_VALUE;
    private long lower;
    private long upper;
    private BigInteger lowerBig;
    private BigInteger upperBig;


    public Interval(Interval other) {
        this.lower = other.lower;
        this.upper = other.upper;
    }
    public Interval(long lower, long upper) {
        this.lower = lower;
        this.upper = upper;
    }
    public Interval(long constant){ // for constant
        this.lower = constant;
        this.upper = constant;
    }
    public Interval() { // bottom
        this.lower = 1L;
        this.upper = 0L;
    }
    public Interval(boolean isTop){ // get top if true
        if(isTop){
            this.lower = NEG_INF;
            this.upper = POS_INF;
        }
        else {
            this.lower = 1L;
            this.upper = 0L;
        }
    }

    public static long getNegInf() { return NEG_INF; }
    public static long getPosInf() { return POS_INF; }
    public boolean isBottom(){ // [1,0]
        return this.lower == 1L && this.upper == 0L;
    }
    public boolean isTop(){
        return this.lower == NEG_INF && this.upper == POS_INF;
    }
    public boolean lowerIsTop(){ return this.lower == NEG_INF; }
    public boolean upperIsTop(){ return this.upper == POS_INF; }

    public boolean isSingle(){
        return this.lower == this.upper;
    }

    public boolean isBoolean(){
        return !isBottom() && (this.lower == 0L || this.lower == 1L) && (this.upper == 0L || this.upper == 1L);
    }

    public long getLower() {
        return lower;
    }

    public long getUpper() {
        return upper;
    }

    @Override
    public AbsDomain join(AbsDomain other) {
        if (!(other instanceof Interval)) {
            throw new IllegalArgumentException("Cannot join with non-interval state");
        }
        Interval otherInterval = (Interval) other;

        if(otherInterval.isBottom()){
            return new Interval(this);
        }
        if(this.isBottom()){
            return new Interval(otherInterval);
        }
        if(this.isTop() || otherInterval.isTop()){
            return new Interval(true);
        }

        long newLower = Math.min(this.lower, otherInterval.lower);
        long newUpper = Math.max(this.upper, otherInterval.upper);
        return new Interval(newLower, newUpper);
    }
    @Override
    public AbsDomain intersect(AbsDomain other){
        if (!(other instanceof Interval)) {
            throw new IllegalArgumentException("Cannot intersect with non-interval state");
        }
        Interval otherInterval = (Interval) other;

        if(otherInterval.isBottom()){
            return new Interval(this);
        }
        if(this.isBottom()){
            return new Interval(otherInterval);
        }
        if(this.isTop())return new Interval(otherInterval);
        if(otherInterval.isTop()) return new Interval(this);
//        if(this.isTop() || otherInterval.isTop()){
//            return new Interval(true);
//        }

        long newLower = Math.max(this.lower, otherInterval.lower);
        long newUpper = Math.min(this.upper, otherInterval.upper);

        if(newLower <= newUpper)
            return new Interval(newLower, newUpper);
        else
            return new Interval();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Interval interval = (Interval) o;
        return lower == interval.lower && upper == interval.upper;
    }

    @Override
    public String toString() {
        if(this.isBottom()) return "unknown";
        return "[" + (lower == NEG_INF ? "-inf" : lower) + ", " + (upper == POS_INF ? "inf" : upper) + "]";
    }

    public Interval add(Interval other) {
        // TODO check for overflow
        if(this.isBottom() || other.isBottom()){
            return new Interval(true);
        }
        long newLower = (this.lower == NEG_INF || other.lower == NEG_INF) ? NEG_INF : this.lower + other.lower;
        long newUpper = (this.upper == POS_INF || other.upper == POS_INF) ? POS_INF : this.upper + other.upper;
        return new Interval(newLower, newUpper);
    }

    /// [a, b] − [c, d] =
    /// [min (a − c, a − d, b − c, b − d),
    /// max (a − c, a − d, b − c, b − d)] = [a − d, b − c]
    public Interval sub(Interval other) {
        // TODO check for overflow
        if(this.isBottom() || other.isBottom()){
            return new Interval(true);
        }
        long newLower = (this.lower == NEG_INF || other.upper == POS_INF) ? NEG_INF : this.lower - other.upper;
        long newUpper = (this.upper == POS_INF || other.lower == NEG_INF) ? POS_INF : this.upper - other.lower;
        return new Interval(newLower, newUpper);
    }

    /// Add and Mul are commutatives. So, they are a little different
    /// of the other operations.
    // [a, b] * [c, d] = [Min(a*c, a*d, b*c, b*d), Max(a*c, a*d, b*c, b*d)]
    public Interval mul(Interval other) {
        // TODO check for overflow
        if(this.isBottom() || other.isBottom()){
            return new Interval(true);
        }

        // TODO deal with Top
        long newLower = (this.lower == NEG_INF || this.upper == POS_INF  || other.lower == NEG_INF || other.upper == POS_INF) ? NEG_INF :
                Math.min(Math.min(this.lower * other.lower, this.lower * other.upper),
                         Math.min(this.upper * other.lower, this.upper * other.upper));
        long newUpper = (this.lower == NEG_INF || this.upper == POS_INF  || other.lower == NEG_INF || other.upper == POS_INF) ? POS_INF :
                Math.max(Math.max(this.lower * other.lower, this.lower * other.upper),
                        Math.max(this.upper * other.lower, this.upper * other.upper));
        return new Interval(newLower, newUpper);
    }

    // [a, b] / [c, d] = ( [a, b] / ([c, d] ∩ [1, +∞]) ) ∪ ([a, b] / ([c, d] ∩ [−∞, −1]))
    //                      [Min(a/c, a/d), Max(b/c, b/d)]  if c >= 1
    //                      [Min(b/c, b/d), Max(a/c, a/d)]  if d <= -1
    public Interval div(Interval other) {
        // TODO check for overflow
        if(this.isBottom() || other.isBottom()){
            return new Interval(true);
        }
        if(this.isTop() || other.isTop()){
            return new Interval(true);
        }
        // TODO deal with div 0
        if( other.getLower()==0L || other.getUpper()==0L)
            return new Interval(true);

        long a = this.getLower(), b = this.getUpper();
        Interval tmp1 = (Interval) other.intersect(new Interval(1L, POS_INF));
        Interval tmp2 = (Interval) other.intersect(new Interval(NEG_INF, -1L));

        long c1 = tmp1.getLower() ,d1 = tmp1.getUpper(), c2 = tmp2.getLower(), d2 = tmp2.getUpper();

        Interval res1 = new Interval(Math.min(a/c1, a/d1), Math.max(b/c1, b/d1));
        Interval res2 = new Interval(Math.min(b/c2, b/d2), Math.max(a/c2, a/d2)) ;

        return (Interval) res1.join(res2);
    }

    // get the opposite number
    public Interval neg() {
        if(this.isBottom() || this.isTop()){
            return new Interval(true);
        }
        long res_l , res_u;
        if(this.lower == NEG_INF){
            res_u = POS_INF;
        } else if (this.lower == POS_INF) {
            res_u = NEG_INF;
        } else res_u = -this.lower;

        if(this.upper == POS_INF){
            res_l = NEG_INF;
        } else if (this.upper == NEG_INF) {
            res_l = POS_INF;
        } else res_l = -this.upper;

        return new Interval(res_l, res_u);
    }

    // invert by bit
    public Interval neg_bit() {
        if(this.isBottom() || this.isTop()){
            return new Interval(true);
        }
        long res_l , res_u;
        if(this.lower == NEG_INF){
            res_u = POS_INF;
        } else if (this.lower == POS_INF) {
            res_u = NEG_INF;
        } else res_u = ~this.lower;

        if(this.upper == POS_INF){
            res_l = NEG_INF;
        } else if (this.upper == NEG_INF) {
            res_l = POS_INF;
        } else res_l = ~this.upper;

        return new Interval(res_l, res_u);
    }

    // logical shift right
    public Interval lshr(Interval other) {
        if(this.isBottom() || this.isTop()){
            return new Interval(true);
        }
        if(other.getLower() < 0L || other.getUpper() == POS_INF || other.isBottom() || other.getUpper()>64L){
            return new Interval(true);
        }

        long a = this.getLower(), b = this.getUpper(),c = other.getLower(), d = other.getUpper();
        long res_l = Math.min(Math.min(a>>>c, a>>>d), Math.min(b>>>c, b>>>d));
        long res_u = Math.max(Math.max(a>>>c, a>>>d), Math.max(b>>>c, b>>>d));
        return new Interval(res_l, res_u);
    }

    // arithmetic shift right
    public Interval shr(Interval other) {
        if(this.isBottom() || this.isTop()){
            return new Interval(true);
        }
        if(other.getLower() < 0L || other.getUpper() == POS_INF || other.isBottom() || other.getUpper()>64L){
            return new Interval(true);
        }

        long a = this.getLower(), b = this.getUpper(),c = other.getLower(), d = other.getUpper();
        long res_l = Math.min(Math.min(a>>c, a>>d), Math.min(b>>c, b>>d));
        long res_u = Math.max(Math.max(a>>c, a>>d), Math.max(b>>c, b>>d));
        return new Interval(res_l, res_u);
    }

    // arithmetic shift left
    public Interval shl(Interval other) {
        if(this.isBottom() || this.isTop()){
            return new Interval(true);
        }
        if(other.getLower() < 0L || other.getUpper() == POS_INF || other.isBottom() || other.getUpper()>64L){
            return new Interval(true);
        }

        long a = this.getLower(), b = this.getUpper(),c = other.getLower(), d = other.getUpper();
        long res_l = Math.min(Math.min(a<<c, a<<d), Math.min(b<<c, b<<d));
        long res_u = Math.max(Math.max(a<<c, a<<d), Math.max(b<<c, b<<d));
        return new Interval(res_l, res_u);
    }


}