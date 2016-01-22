package com.thoughtworks.go.plugin.access.configrepo.contract.tasks;

import com.thoughtworks.go.plugin.access.configrepo.ErrorCollection;

public class CRBuildTask extends CRTask {
    public static final String RAKE_TYPE_NAME = "rake";
    public static final String ANT_TYPE_NAME = "ant";
    public static final String NANT_TYPE_NAME = "nant";

    public static CRBuildTask rake()
    {
        return new CRBuildTask(RAKE_TYPE_NAME);
    }
    public static CRBuildTask rake(String rakeFile)
    {
        return new CRBuildTask(RAKE_TYPE_NAME,rakeFile,null,null);
    }
    public static CRBuildTask rake(String rakeFile,String target)
    {
        return new CRBuildTask(RAKE_TYPE_NAME,rakeFile,target,null);
    }
    public static CRBuildTask rake(String rakeFile,String target,String workingDirectory)
    {
        return new CRBuildTask(RAKE_TYPE_NAME,rakeFile,target,workingDirectory);
    }

    public static CRBuildTask ant()
    {
        return new CRBuildTask(ANT_TYPE_NAME,null,null,null);
    }
    public static CRBuildTask ant(String antFile)
    {
        return new CRBuildTask(ANT_TYPE_NAME,antFile,null,null);
    }
    public static CRBuildTask ant(String antFile,String target)
    {
        return new CRBuildTask(ANT_TYPE_NAME,antFile,target,null);
    }
    public static CRBuildTask ant(String antFile,String target,String workingDirectory)
    {
        return new CRBuildTask(ANT_TYPE_NAME,antFile,target,workingDirectory);
    }

    public static CRNantTask nant()
    {
        return new CRNantTask(NANT_TYPE_NAME,null,null,null,null);
    }
    public static CRNantTask nant(String nantFile)
    {
        return new CRNantTask(NANT_TYPE_NAME,nantFile,null,null,null);
    }
    public static CRNantTask nant(String nantFile,String target)
    {
        return new CRNantTask(NANT_TYPE_NAME,nantFile,target,null,null);
    }
    public static CRNantTask nant(String nantFile,String target,String workingDirectory)
    {
        return new CRNantTask(NANT_TYPE_NAME,nantFile,target,workingDirectory,null);
    }
    public static CRNantTask nant(String nantFile,String target,String workingDirectory,String nantPath)
    {
        return new CRNantTask(NANT_TYPE_NAME,nantFile,target,workingDirectory,nantPath);
    }

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

    private String build_file;
    private String target;
    private String working_directory;

    public CRBuildTask(String type,String buildFile,String target,String workingDirectory){
        super(type);
        this.build_file = buildFile;
        this.target = target;
        this.working_directory = workingDirectory;
    }
    public CRBuildTask(String type){
        super(type);
    }

    public CRBuildTask(CRRunIf runIf, CRTask onCancel,
                       String buildFile,String target,String workingDirectory,CRBuildFramework type) {
        super(runIf, onCancel);
        this.build_file = buildFile;
        this.target= target;
        this.working_directory = workingDirectory;
        super.type = type.toString();
    }

    public String getBuildFile() {
        return build_file;
    }

    public String getTarget() {
        return target;
    }

    public String getWorkingDirectory() {
        return working_directory;
    }

    public CRBuildFramework getType() {
        if(type == null)
            return null;
        return CRBuildFramework.valueOf(type);
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        String location = getLocation(parentLocation);
        errors.checkMissing(location,"type",type);
    }

    @Override
    public String getLocation(String parent) {
        String myLocation = getLocation() == null ? parent : getLocation();
        String type = this.getType() == null ? "unknown" : this.getType().toString();
        return String.format("%s; %s build task",myLocation,type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        CRBuildTask buildTask = (CRBuildTask)o;
        if(buildTask == null)
            return  false;

        if(!super.equals(buildTask))
            return false;

        if (build_file != null ? !build_file.equals(buildTask.build_file) : buildTask.build_file != null) {
            return false;
        }
        if (target != null ? !target.equals(buildTask.target) : buildTask.target != null) {
            return false;
        }
        if (working_directory != null ? !working_directory.equals(buildTask.working_directory) : buildTask.working_directory != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (build_file != null ? build_file.hashCode() : 0);
        result = 31 * result + (target != null ? target.hashCode() : 0);
        result = 31 * result + (working_directory != null ? working_directory.hashCode() : 0);
        return result;
    }
}
