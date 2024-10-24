package com.bai.env.domain;

// Abstract domain class
public abstract class AbsDomain {
    public AbsDomain(){};
    public abstract boolean isTop();
    public abstract boolean isBottom();
    public abstract boolean equals(Object o);
    public abstract AbsDomain join(AbsDomain other);
    public abstract AbsDomain intersect(AbsDomain other);
}
