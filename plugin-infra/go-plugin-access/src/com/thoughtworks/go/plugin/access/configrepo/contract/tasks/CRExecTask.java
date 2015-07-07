package com.thoughtworks.go.plugin.access.configrepo.contract.tasks;

import java.util.List;

public class CRExecTask extends CRTask  {
    private final String command ;
    private final String workingDirectory;
    private final Long timeout ;
    private final List<String> args;

    public CRExecTask(CRRunIf runIf, CRTask onCancel,
                      String command,String workingDirectory,Long timeout,List<String> args) {
        super(runIf, onCancel);
        this.command = command;
        this.workingDirectory = workingDirectory;
        this.timeout = timeout;
        this.args = args;
    }

    public String getCommand() {
        return command;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public Long getTimeout() {
        return timeout;
    }

    public List<String> getArgs() {
        return args;
    }
}