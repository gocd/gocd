package com.thoughtworks.go.plugin.configrepo.tasks;

public class CRNantTask_1 extends CRBuildTask_1 {
    private String nantPath;

    public CRNantTask_1(String type, String buildFile, String target, String workingDirectory, String nantPath) {
        super(type, buildFile, target, workingDirectory);
        this.nantPath = nantPath;
    }

    public String getNantPath() {
        return nantPath;
    }

    public void setNantPath(String nantPath) {
        this.nantPath = nantPath;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        CRNantTask_1 buildTask = (CRNantTask_1)o;
        if(buildTask == null)
            return  false;

        if(!super.equals(buildTask))
            return false;

        if (nantPath != null ? !nantPath.equals(buildTask.nantPath) : buildTask.nantPath != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (nantPath != null ? nantPath.hashCode() : 0);
        return result;
    }

}
