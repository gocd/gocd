package com.thoughtworks.go.plugin.access.configrepo.contract;

public class CRTimer {
    private final String timerSpec;
    private final boolean onlyOnChanges;

    public CRTimer(String timerSpec, boolean onlyOnChanges) {
        this.timerSpec = timerSpec;
        this.onlyOnChanges = onlyOnChanges;
    }

    public String getTimerSpec() {
        return timerSpec;
    }

    public boolean isOnlyOnChanges() {
        return onlyOnChanges;
    }
}
