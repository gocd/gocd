package com.thoughtworks.go.plugin.access.configrepo.contract.tasks;

public class CRBuildTask extends CRTask {

    public static CRBuildTask rake(CRRunIf runIf, CRTask onCancel,
                                   String buildFile,String target,String workingDirectory)
    {
        return  new CRBuildTask(runIf,onCancel,buildFile,target,workingDirectory,CRBuildFramework.rake);
    }
    public static CRBuildTask ant(CRRunIf runIf, CRTask onCancel,
                                   String buildFile,String target,String workingDirectory)
    {
        return  new CRBuildTask(runIf,onCancel,buildFile,target,workingDirectory,CRBuildFramework.ant);
    }

    private final String buildFile;
    private final String target;
    private final String workingDirectory;
    private final CRBuildFramework type;

    public CRBuildTask(CRRunIf runIf, CRTask onCancel,
                       String buildFile,String target,String workingDirectory,CRBuildFramework type) {
        super(runIf, onCancel);
        this.buildFile = buildFile;
        this.target= target;
        this.workingDirectory = workingDirectory;
        this.type = type;
    }

    public String getBuildFile() {
        return buildFile;
    }

    public String getTarget() {
        return target;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public CRBuildFramework getType() {
        return type;
    }
}
