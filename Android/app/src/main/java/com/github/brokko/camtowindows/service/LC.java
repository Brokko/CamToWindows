package com.github.brokko.camtowindows.service;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

public class LC extends Lifecycle implements LifecycleOwner {

    public LC() {

    }

    @Override
    public void addObserver(@NonNull LifecycleObserver observer) {

    }

    @Override
    public void removeObserver(@NonNull LifecycleObserver observer) {

    }

    @NonNull
    @Override
    public State getCurrentState() {
        return State.RESUMED;
    }

    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return this;
    }
}
