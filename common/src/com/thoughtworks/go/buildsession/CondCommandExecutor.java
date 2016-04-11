package com.thoughtworks.go.buildsession;

import com.thoughtworks.go.domain.BuildCommand;

import java.util.List;

public class CondCommandExecutor implements BuildCommandExecutor {
    @Override
    public boolean execute(BuildCommand command, BuildSession buildSession) {
        List<BuildCommand> subCommands = command.getSubCommands();
        for (int i = 0; i < subCommands.size(); i += 2) {
            if (i == subCommands.size() - 1) {
                // else branch
                return buildSession.processCommand(subCommands.get(i));
            }

            BuildCommand test = subCommands.get(i);
            BuildCommand action = subCommands.get(i + 1);
            if (buildSession.newTestingSession().build(test).isPassed()) {
                return buildSession.processCommand(action);
            }
        }
        return true;
    }
}
