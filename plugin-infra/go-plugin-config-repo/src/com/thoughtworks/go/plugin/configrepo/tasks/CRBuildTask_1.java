package com.thoughtworks.go.plugin.configrepo.tasks;

import com.thoughtworks.go.plugin.configrepo.ErrorCollection;
import org.apache.commons.lang.StringUtils;

public class CRBuildTask_1 extends CRTask_1 {
    public static final String RAKE_TYPE_NAME = "rake";
    public static final String ANT_TYPE_NAME = "ant";
    public static final String NANT_TYPE_NAME = "nant";

    public static CRBuildTask_1 rake()
    {
        return new CRBuildTask_1(RAKE_TYPE_NAME);
    }
    public static CRBuildTask_1 rake(String rakeFile)
    {
        return new CRBuildTask_1(RAKE_TYPE_NAME,rakeFile,null,null);
    }
    public static CRBuildTask_1 rake(String rakeFile,String target)
    {
        return new CRBuildTask_1(RAKE_TYPE_NAME,rakeFile,target,null);
    }
    public static CRBuildTask_1 rake(String rakeFile,String target,String workingDirectory)
    {
        return new CRBuildTask_1(RAKE_TYPE_NAME,rakeFile,target,workingDirectory);
    }

    public static CRBuildTask_1 ant()
    {
        return new CRBuildTask_1(ANT_TYPE_NAME,null,null,null);
    }
    public static CRBuildTask_1 ant(String antFile)
    {
        return new CRBuildTask_1(ANT_TYPE_NAME,antFile,null,null);
    }
    public static CRBuildTask_1 ant(String antFile,String target)
    {
        return new CRBuildTask_1(ANT_TYPE_NAME,antFile,target,null);
    }
    public static CRBuildTask_1 ant(String antFile,String target,String workingDirectory)
    {
        return new CRBuildTask_1(ANT_TYPE_NAME,antFile,target,workingDirectory);
    }

    public static CRNantTask_1 nant()
    {
        return new CRNantTask_1(NANT_TYPE_NAME,null,null,null,null);
    }
    public static CRNantTask_1 nant(String nantFile)
    {
        return new CRNantTask_1(NANT_TYPE_NAME,nantFile,null,null,null);
    }
    public static CRNantTask_1 nant(String nantFile,String target)
    {
        return new CRNantTask_1(NANT_TYPE_NAME,nantFile,target,null,null);
    }
    public static CRNantTask_1 nant(String nantFile,String target,String workingDirectory)
    {
        return new CRNantTask_1(NANT_TYPE_NAME,nantFile,target,workingDirectory,null);
    }
    public static CRNantTask_1 nant(String nantFile,String target,String workingDirectory,String nantPath)
    {
        return new CRNantTask_1(NANT_TYPE_NAME,nantFile,target,workingDirectory,nantPath);
    }

    private String buildFile;
    private String target;
    private String workingDirectory;

    public CRBuildTask_1(String type,String buildFile,String target,String workingDirectory){
        super(type);
        this.buildFile = buildFile;
        this.target = target;
        this.workingDirectory = workingDirectory;
    }
    public CRBuildTask_1(String type){
        super(type);
    }


    public String getBuildFile() {
        return buildFile;
    }

    public void setBuildFile(String buildFile) {
        this.buildFile = buildFile;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        CRBuildTask_1 buildTask = (CRBuildTask_1)o;
        if(buildTask == null)
            return  false;

        if(!super.equals(buildTask))
            return false;

        if (buildFile != null ? !buildFile.equals(buildTask.buildFile) : buildTask.buildFile != null) {
            return false;
        }
        if (target != null ? !target.equals(buildTask.target) : buildTask.target != null) {
            return false;
        }
        if (workingDirectory != null ? !workingDirectory.equals(buildTask.workingDirectory) : buildTask.workingDirectory != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (buildFile != null ? buildFile.hashCode() : 0);
        result = 31 * result + (target != null ? target.hashCode() : 0);
        result = 31 * result + (workingDirectory != null ? workingDirectory.hashCode() : 0);
        return result;
    }

    @Override
    public void getErrors(ErrorCollection errors) {
        validateType(errors);
        validateOnCancel(errors);
    }
}
