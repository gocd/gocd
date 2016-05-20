package com.thoughtworks.go.buildsession;

import com.thoughtworks.go.domain.BuildCommand;

public class OrCommandExecutor implements BuildCommandExecutor {
    @Override
    public boolean execute(BuildCommand command, BuildSession buildSession) {
        for (BuildCommand subCommand : command.getSubCommands()) {
            if(buildSession.newTestingSession().build(subCommand).isPassed()) {
                return true;
            }
        }
        return false;
    }
}
