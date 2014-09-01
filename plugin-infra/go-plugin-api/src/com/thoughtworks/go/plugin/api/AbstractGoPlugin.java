package com.thoughtworks.go.plugin.api;

public abstract class AbstractGoPlugin implements GoPlugin {

    protected GoApplicationAccessor goApplicationAccessor;

    @Override
    public void initializeGoApplicationAccessor(GoApplicationAccessor goApplicationAccessor) {
        this.goApplicationAccessor = goApplicationAccessor;
    }
}
