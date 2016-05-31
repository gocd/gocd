package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.presentation.TriStateSelection;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static com.thoughtworks.go.util.ExceptionUtils.bombIfNull;

@Deprecated //needed for old pages. Delete once API and SPA is ready
public class ModifyEnvironmentCommand implements UpdateConfigCommand {
    private final String uuid;
    private final String environmentName;
    private final TriStateSelection.Action action;

    public ModifyEnvironmentCommand(String uuid, String environmentName, TriStateSelection.Action action) {
        this.uuid = uuid;
        this.environmentName = environmentName;
        this.action = action;
    }

    public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
        AgentConfig agentConfig = cruiseConfig.agents().getAgentByUuid(uuid);
        bombIfNull(agentConfig, "Unable to set agent resources; Agent [" + uuid + "] not found.");
        EnvironmentConfig environmentConfig = cruiseConfig.getEnvironments().find(new CaseInsensitiveString(environmentName));
        if (environmentConfig == null) {
            agentConfig.addError("environments", "Environment [" + environmentName + "] does not exist.");
        } else {
            if (action.equals(TriStateSelection.Action.add)) {
                environmentConfig.addAgentIfNew(uuid);
            } else if (action.equals(TriStateSelection.Action.remove)) {
                environmentConfig.removeAgent(uuid);
            } else if (action.equals(TriStateSelection.Action.nochange)) {
                //do nothing
            } else {
                bomb(String.format("unsupported action '%s'", action));
            }
        }
        return cruiseConfig;
    }
}