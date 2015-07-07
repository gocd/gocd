package com.thoughtworks.go.plugin.access.configrepo.contract.tasks;

public abstract class CRTask {
    private final CRRunIf runIf;
    private final CRTask onCancel;

    public CRTask(CRRunIf runIf,CRTask onCancel)
    {
        this.runIf = runIf;
        this.onCancel = onCancel;
    }

    public CRRunIf getRunIf() {
        return runIf;
    }

    public CRTask getOnCancel() {
        return onCancel;
    }
}
