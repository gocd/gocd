package com.thoughtworks.go.buildsession;

import com.thoughtworks.go.domain.BuildCommand;

public class AndCommandExecutor implements BuildCommandExecutor {
    @Override
    public boolean execute(BuildCommand command, BuildSession buildSession) {
        for (BuildCommand subCommand: command.getSubCommands()) {
            if(buildSession.newTestingSession().build(subCommand).isFailed()) {
                return false;
            }
        }
        return true;
    }
}
