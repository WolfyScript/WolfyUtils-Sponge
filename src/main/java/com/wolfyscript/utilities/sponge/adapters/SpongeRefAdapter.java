package com.wolfyscript.utilities.sponge.adapters;

public abstract class SpongeRefAdapter<T> {

    private final T spongeRef;

    protected SpongeRefAdapter(T referenced) {
        this.spongeRef = referenced;
    }

    public T spongeRef() {
        return spongeRef;
    }
}
