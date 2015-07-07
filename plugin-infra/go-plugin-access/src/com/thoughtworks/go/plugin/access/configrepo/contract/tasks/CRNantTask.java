package com.thoughtworks.go.plugin.access.configrepo.contract.tasks;

public class CRNantTask extends CRBuildTask {
    private final String nantPath;

    public CRNantTask(CRRunIf runIf, CRTask onCancel, String buildFile, String target, String workingDirectory,String nantPath) {
        super(runIf, onCancel, buildFile, target, workingDirectory, CRBuildFramework.nant);
        this.nantPath = nantPath;
    }

    public String getNantPath() {
        return nantPath;
    }
}
