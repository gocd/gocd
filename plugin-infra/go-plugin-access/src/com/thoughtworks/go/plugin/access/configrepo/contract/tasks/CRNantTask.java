package com.thoughtworks.go.plugin.access.configrepo.contract.tasks;

public class CRNantTask extends CRBuildTask {
    private String nant_path;

    public CRNantTask(String type, String buildFile, String target, String workingDirectory, String nantPath) {
        super(type, buildFile, target, workingDirectory);
        this.nant_path = nantPath;
    }

    public CRNantTask(CRRunIf runIf, CRTask onCancel, String buildFile, String target, String workingDirectory,String nantPath) {
        super(runIf, onCancel, buildFile, target, workingDirectory, CRBuildFramework.nant);
        this.nant_path = nantPath;
    }

    public String getNantPath() {
        return nant_path;
    }

    public void setNantPath(String nantPath) {
        this.nant_path = nantPath;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        CRNantTask buildTask = (CRNantTask)o;
        if(buildTask == null)
            return  false;

        if(!super.equals(buildTask))
            return false;

        if (nant_path != null ? !nant_path.equals(buildTask.nant_path) : buildTask.nant_path != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (nant_path != null ? nant_path.hashCode() : 0);
        return result;
    }
}
