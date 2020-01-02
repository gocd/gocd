/*
 * Copyright 2020 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.TaskProperty;
import com.thoughtworks.go.domain.config.Arguments;
import com.thoughtworks.go.util.FilenameUtil;
import com.thoughtworks.go.utils.CommandUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This was copied from the ExecBuilder class in ccmain. Look for references there.
 */
@ConfigTag("exec")
public class ExecTask extends AbstractTask implements CommandTask {

    public static final String TYPE = "exec";
    @ConfigAttribute("command")
    private String command = "";
    @ConfigAttribute(value = "args", allowNull = true)
    private String args = "";
    @ConfigAttribute(value = "workingdir", allowNull = true)
    private String workingDirectory;
    @ConfigAttribute("timeout")
    private Long timeout = NO_TIMEOUT_FOR_COMMANDLINE;
    @ConfigSubtag(label = "arguments")
    private Arguments argList = new Arguments();
    public static final String EXEC_CONFIG_ERROR = "Can not use both 'args' attribute and 'arg' sub element in 'exec' element!";

    private static final long NO_TIMEOUT_FOR_COMMANDLINE = -1;
    public static final String COMMAND = "command";
    public static final String ARGS = "args";
    public static final String ARG_LIST_STRING = "argListString";
    public static final String WORKING_DIR = "workingDirectory";
    public static final String TIMEOUT = "timeout";
    private final String CUSTOM_COMMAND = "Custom Command";

    public ExecTask() {
    }

    public ExecTask(String command, String args, String workingDir) {
        this(command, workingDir);
        this.args = args;
    }

    //used for test
    public ExecTask(String args, Arguments argList) {
        this("echo", args, argList);//TODO: delete me, there is a validation that enforces not both attributes are populated - shilpa / jj
    }

    @Override
    public String describe() {
        if (null != argList && !argList.isEmpty()) {
            return CommandUtils.shellJoin((String[]) ArrayUtils.add(argList.toStringArray(), 0, command));
        }

        if (null != args && !"".equals(args)) {
            return command + " " + args;
        }

        return command;
    }

    public ExecTask(String command, String args, Arguments argList) {
        this.command = command;
        this.args = args;
        this.argList = argList;
    }

    public ExecTask(String command, Arguments argList, String workingDir) {
        this(command, workingDir);
        this.argList = argList;
    }

    private ExecTask(String command, String workingDir) {
        this.command = command;
        this.workingDirectory = workingDir;
    }

    @Override
    protected void setTaskConfigAttributes(Map attributeMap) {
        if (attributeMap.containsKey(COMMAND)) {
            command = (String) attributeMap.get(COMMAND);
        }
        if (attributeMap.containsKey(ARG_LIST_STRING)) {
            clearCurrentArgsAndArgList();
            String value = (String) attributeMap.get(ARG_LIST_STRING);
            if (!StringUtils.isBlank(value)) {
                String[] arguments = value.split("\n");
                for (String arg : arguments) {
                    argList.add(new Argument(arg));
                }
            }
        }
        if (attributeMap.containsKey(ARGS)) {
            String val = (String) attributeMap.get(ARGS);
            setArgs(val);
        }
        if (attributeMap.containsKey(WORKING_DIR)) {
            final String newWorkingDir = (String) attributeMap.get(WORKING_DIR);
            setWorkingDirectory(newWorkingDir);
        }
    }

    public void setArgsList(String[] arguments) {
        clearCurrentArgsAndArgList();
        for (String arg : arguments) {
            argList.add(new Argument(arg));
        }
    }

    public void setArgs(String val) {
        clearCurrentArgsAndArgList();
        if (!StringUtils.isBlank(val)) {
            this.args = val;
        }
    }

    public void setWorkingDirectory(String newWorkingDir) {
        workingDirectory = StringUtils.isBlank(newWorkingDir) ? null : newWorkingDir;
    }

    private void clearCurrentArgsAndArgList() {
        args = "";
        argList.clear();
    }

    @Override
    public void validateTask(ValidationContext ctx) {
        validateCommand();
        if (!usingBothArgsAndArgList()) {
            errors.add(ARGS, EXEC_CONFIG_ERROR);
            errors.add(ARG_LIST_STRING, EXEC_CONFIG_ERROR);
        }
        if (workingDirectory != null && !FilenameUtil.isNormalizedPathOutsideWorkingDir(workingDirectory)) {
            if (ctx.isWithinPipelines()) {
                errors.add(WORKING_DIR,
                        String.format("The path of the working directory for the custom command in job '%s' in stage '%s' of pipeline '%s' is outside the agent sandbox.", ctx.getJob().name(),
                                ctx.getStage().name(), ctx.getPipeline().name()));
            } else {
                errors.add(WORKING_DIR,
                        String.format("The path of the working directory for the custom command in job '%s' in stage '%s' of template '%s' is outside the agent sandbox.", ctx.getJob().name(),
                                ctx.getStage().name(), ctx.getTemplate().name()));
            }
        }

        for (Argument argument : getArgList()) {
            if (!argument.errors().isEmpty()) {
                errors.add(ARG_LIST_STRING, argument.errors().asString());
            }
        }
    }

    private void validateCommand() {
        if (StringUtils.isBlank(command)) {
            errors.add(COMMAND, "Command cannot be empty");
        }
    }

    @Override
    public String getTaskType() {
        return TYPE;
    }

    @Override
    public String getTypeForDisplay() {
        return CUSTOM_COMMAND;
    }

    @Override
    public List<TaskProperty> getPropertiesForDisplay() {
        ArrayList<TaskProperty> taskProperties = new ArrayList<>();
        taskProperties.add(new TaskProperty("Command", command));
        String arguments = arguments();
        if (!arguments.isEmpty()) {
            taskProperties.add(new TaskProperty("Arguments", arguments));
        }
        if (workingDirectory != null) {
            taskProperties.add(new TaskProperty("Working Directory", workingDirectory));
        }
        if (!(timeout == null || timeout.equals(NO_TIMEOUT_FOR_COMMANDLINE))) {
            taskProperties.add(new TaskProperty("Timeout", timeout.toString()));
        }
        return taskProperties;
    }

    private String argsAsString(final String delimiter) {
        StringBuilder args = new StringBuilder();
        for (int i = 0; i < argList.size(); ) {
            args.append(argList.get(i).getValue());
            if (++i < argList.size()) {
                args.append(delimiter);
            }
        }
        return args.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ExecTask execTask = (ExecTask) o;

        if (timeout != execTask.timeout) {
            return false;
        }
        if (args != null ? !args.equals(execTask.args) : execTask.args != null) {
            return false;
        }
        if (argList != null ? !argList.equals(execTask.argList) : execTask.argList != null) {
            return false;
        }
        if (command != null ? !command.equals(execTask.command) : execTask.command != null) {
            return false;
        }
        if (workingDirectory != null ? !workingDirectory.equals(execTask.workingDirectory) : execTask.workingDirectory != null) {
            return false;
        }
        return super.equals(execTask);
    }

    @Override
    public int hashCode() {
        int result;
        result = command.hashCode();
        result = 31 * result + (args != null ? args.hashCode() : 0);
        result = 31 * result + (workingDirectory != null ? workingDirectory.hashCode() : 0);
        result = 31 * result + (int) (timeout ^ (timeout >>> 32));
        return result;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public long getTimeout() {
        return timeout;
    }

    public Arguments getArgList() {
        return argList;
    }

    public String getArgListString() {
        return argsAsString("\n");
    }

    private boolean usingBothArgsAndArgList() {
        return args.isEmpty() || argList.isEmpty();
    }

    @Override
    public String command() {
        return command;
    }

    public String getCommand() {
        return command();
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getArgs() {
        return args;
    }

    @Override
    public String workingDirectory() {
        return workingDirectory;
    }

    @Override
    public String toString() {
        return "ExecTask{" +
                "command='" + command + '\'' +
                ", args='" + args + '\'' +
                ", workingDir='" + workingDirectory + '\'' +
                ", timeout=" + timeout +
                ", argList=" + argList +
                '}';
    }

    @Override
    public String arguments() {
        if (!args.isEmpty()) {
            return args;
        }
        return argsAsString(" ");
    }
}
