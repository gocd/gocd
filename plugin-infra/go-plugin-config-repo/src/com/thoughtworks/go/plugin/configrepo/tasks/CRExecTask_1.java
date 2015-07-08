package com.thoughtworks.go.plugin.configrepo.tasks;

import com.thoughtworks.go.plugin.configrepo.ErrorCollection;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CRExecTask_1 extends CRTask_1 {
    public static final String TYPE_NAME = "exec";

    private String command ;
    private String workingDirectory;
    private Long timeout ;
    private List<String> args = new ArrayList<>();

    public CRExecTask_1() {
        super(TYPE_NAME);
    }
    public CRExecTask_1(String command) {
        super(TYPE_NAME);
        this.command = command;
    }
    public CRExecTask_1(String command,String workingDirectory, Long timeout,CRRunIf_1 runIf,CRTask_1 onCancel,String... args) {
        super(TYPE_NAME,runIf,onCancel);
        this.command = command;
        this.workingDirectory = workingDirectory;
        this.timeout = timeout;
        this.args = Arrays.asList(args);
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public Long getTimeout() {
        return timeout;
    }

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

    public List<String> getArgs() {
        return args;
    }

    public void setArgs(List<String> args) {
        this.args = args;
    }

    @Override
    public void getErrors(ErrorCollection errors) {
        validateCommand(errors);
        validateOnCancel(errors);
    }

    private void validateCommand(ErrorCollection errors) {
        if (StringUtils.isBlank(command)) {
            errors.add(this, "Exec task has no command specified");
        }
    }

    public void addArgument(String arg) {
        this.args.add(arg);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        CRExecTask_1 that = (CRExecTask_1)o;
        if(that == null)
            return  false;

        if(!super.equals(that))
            return false;

        if (command != null ? !command.equals(that.command) : that.command != null) {
            return false;
        }
        if (workingDirectory != null ? !workingDirectory.equals(that.workingDirectory) : that.workingDirectory != null) {
            return false;
        }
        if (timeout != null ? !timeout.equals(that.timeout) : that.timeout != null) {
            return false;
        }
        if (args != null ? !CollectionUtils.isEqualCollection(this.args, that.args) : that.args != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (command != null ? command.hashCode() : 0);
        result = 31 * result + (workingDirectory != null ? workingDirectory.hashCode() : 0);
        result = 31 * result + (timeout != null ? timeout.hashCode() : 0);
        return result;
    }
}