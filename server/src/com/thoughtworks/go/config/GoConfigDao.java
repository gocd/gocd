/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.config;

import com.rits.cloning.Cloner;
import com.thoughtworks.go.config.update.ConfigUpdateCheckFailedException;
import com.thoughtworks.go.config.validation.GoConfigValidity;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.metrics.domain.context.Context;
import com.thoughtworks.go.metrics.domain.probes.ProbeType;
import com.thoughtworks.go.metrics.service.MetricsProbeService;
import com.thoughtworks.go.presentation.TriStateSelection;
import com.thoughtworks.go.util.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.thoughtworks.go.util.ExceptionUtils.*;

/**
 * @understands how to modify the cruise config sources
 */
@Component
public class GoConfigDao {
    private CachedGoConfig cachedConfigService;
    private MetricsProbeService metricsProbeService;
    private final Object writeLock;
    private Cloner cloner = new Cloner();

    //used in tests
    public GoConfigDao(CachedGoConfig cachedConfigService, MetricsProbeService metricsProbeService) {
        this.cachedConfigService = cachedConfigService;
        this.metricsProbeService = metricsProbeService;
        writeLock = new Object();
    }

    @Autowired
    public GoConfigDao(MergedGoConfig cachedConfigService, MetricsProbeService metricsProbeService) {
        this.cachedConfigService = cachedConfigService;
        this.metricsProbeService = metricsProbeService;
        writeLock = new Object();
    }

    public String fileLocation() {
        return cachedConfigService.getFileLocation();
    }

    public void addAgent(final AgentConfig agentConfig) {
        updateConfig(createAddAgentCommand(agentConfig));
    }

    public void updateMailHost(MailHost mailHost) {
        updateConfig(mailHostUpdater(mailHost));
    }

    public void addPipeline(PipelineConfig pipelineConfig, String groupName) {
        updateConfig(pipelineAdder(pipelineConfig, groupName));
    }

    public void addEnvironment(BasicEnvironmentConfig environmentConfig) {
        updateConfig(environmentAdder(environmentConfig));
    }

    public void updateAgentResources(final String uuid, final Resources resources) {
        updateConfig(new UpdateResourcesCommand(uuid, resources));
    }

    public void updateAgentApprovalStatus(final String uuid, final Boolean isDenied) {
        updateConfig(updateApprovalStatus(uuid, isDenied));
    }

    public static UpdateConfigCommand updateApprovalStatus(final String uuid, final Boolean isDenied) {
        return new UpdateAgentApprovalStatus(uuid, isDenied);
    }

    public static DeleteAgent deleteAgentCommand(String uuid) {
        return new DeleteAgent(uuid);
    }

    public void deleteAgents(AgentInstance... agentInstances) {
        updateConfig(commandForDeletingAgents(agentInstances));
    }

    CompositeConfigCommand commandForDeletingAgents(AgentInstance... agentInstances) {
        GoConfigDao.CompositeConfigCommand command = new GoConfigDao.CompositeConfigCommand();
        for (AgentInstance agentInstance : agentInstances) {
            command.addCommand(deleteAgentCommand(agentInstance.getUuid()));
        }
        return command;
    }

    private static class UpdateAgentIp implements UpdateConfigCommand, UserAware {
        private final String uuid;
        private final String ipAddress;
        private final String userName;

        private UpdateAgentIp(String uuid, String ipAddress, String userName) {
            this.uuid = uuid;
            this.ipAddress = ipAddress;
            this.userName = userName;
        }

        public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
            AgentConfig agentConfig = cruiseConfig.agents().getAgentByUuid(uuid);
            bombIfNull(agentConfig, "Unable to set agent ipAddress; Agent [" + uuid + "] not found.");
            agentConfig.setIpAddress(ipAddress);
            return cruiseConfig;
        }

        public ConfigModifyingUser user() {
            return new ConfigModifyingUser(userName);
        }
    }

    public static class UpdateAgentHostname implements UpdateConfigCommand, UserAware {
        private final String uuid;
        private final String hostname;
        private final String userName;

        public UpdateAgentHostname(String uuid, String hostname, String userName) {
            this.uuid = uuid;
            this.hostname = hostname;
            this.userName = userName;
        }

        public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
            AgentConfig agentConfig = cruiseConfig.agents().getAgentByUuid(uuid);
            bombIfNull(agentConfig, "Unable to set agent hostname; Agent [" + uuid + "] not found.");
            agentConfig.setHostName(hostname);
            return cruiseConfig;
        }

        public ConfigModifyingUser user() {
            return new ConfigModifyingUser(userName);
        }
    }

    public void updateAgentIp(final String uuid, final String ipAddress, String userName) {
        updateConfig(new UpdateAgentIp(uuid, ipAddress, userName));
    }

    public CruiseConfig loadForEditing() {
        return cachedConfigService.loadForEditing();
    }

    public CruiseConfig load() {
        return cachedConfigService.currentConfig();
    }

    public String md5OfConfigFile() {
        return cachedConfigService.currentConfig().getMd5();
    }

    public ConfigSaveState updateConfig(UpdateConfigCommand command) {
        Context context = metricsProbeService.begin(ProbeType.UPDATE_CONFIG);
        try {
            synchronized (writeLock) {
                try {
                    if (command instanceof CheckedUpdateCommand) {
                        CheckedUpdateCommand checkedCommand = (CheckedUpdateCommand) command;
                        if (!checkedCommand.canContinue(cachedConfigService.currentConfig())) {
                            throw new ConfigUpdateCheckFailedException();
                        }
                    }

                    return cachedConfigService.writeWithLock(command);
                } finally {
                    if (command instanceof ConfigAwareUpdate) {
                        ((ConfigAwareUpdate) command).afterUpdate(clonedConfig());
                    }
                }
            }
        } finally {
            metricsProbeService.end(ProbeType.UPDATE_CONFIG, context);
        }
    }

    private CruiseConfig clonedConfig() {
        return cloner.deepClone(cachedConfigService.currentConfig());
    }

    public GoConfigValidity checkConfigFileValid() {
        return cachedConfigService.checkConfigFileValid();
    }

    public void registerListener(ConfigChangedListener listener) {
        cachedConfigService.registerListener(listener);
    }

    /**
     * @deprecated Used only in tests
     */
    public void reloadListeners() {
        cachedConfigService.reloadListeners();
    }

    /**
     * @deprecated Used only in tests
     */
    public void forceReload() {
        cachedConfigService.forceReload();
    }

    public static class CompositeConfigCommand implements UpdateConfigCommand {
        private List<UpdateConfigCommand> commands = new ArrayList<UpdateConfigCommand>();

        public CompositeConfigCommand(UpdateConfigCommand... commands) {
            this.commands.addAll(Arrays.asList(commands));
        }

        public void addCommand(UpdateConfigCommand command) {
            commands.add(command);
        }

        public List<UpdateConfigCommand> getCommands() {
            return commands;
        }

        public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
            for (UpdateConfigCommand command : commands) {
                cruiseConfig = command.update(cruiseConfig);
            }
            return cruiseConfig;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            CompositeConfigCommand command = (CompositeConfigCommand) o;

            if (commands != null ? !commands.equals(command.commands) : command.commands != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return commands != null ? commands.hashCode() : 0;
        }

        @Override
        public String toString() {
            return "CompositeConfigCommand{" +
                    "commands=" + commands +
                    '}';
        }
    }

    public static class NoOverwriteCompositeConfigCommand extends CompositeConfigCommand implements NoOverwriteUpdateConfigCommand {
        private final String md5;

        public NoOverwriteCompositeConfigCommand(String md5, UpdateConfigCommand... commands) {
            super(commands);
            this.md5 = md5;
        }

        public String unmodifiedMd5() {
            return md5;
        }
    }


    public static UpdateConfigCommand createAddAgentCommand(final AgentConfig agentConfig) {
        return new AddAgentCommand(agentConfig);
    }

    private UpdateConfigCommand pipelineAdder(final PipelineConfig pipelineConfig, final String groupName) {
        return new UpdateConfigCommand() {
            public CruiseConfig update(CruiseConfig cruiseConfig) {
                cruiseConfig.addPipeline(groupName, pipelineConfig);
                return cruiseConfig;
            }
        };
    }

    private UpdateConfigCommand environmentAdder(final BasicEnvironmentConfig environmentConfig) {
        return new UpdateConfigCommand() {
            public CruiseConfig update(CruiseConfig cruiseConfig) {
                cruiseConfig.addEnvironment(environmentConfig);
                return cruiseConfig;
            }
        };
    }


    public UpdateConfigCommand mailHostUpdater(final MailHost mailHost) {
        return new UpdateConfigCommand() {
            public CruiseConfig update(CruiseConfig cruiseConfig) {
                cruiseConfig.server().updateMailHost(mailHost);
                return cruiseConfig;
            }
        };
    }

    /**
     * @understands how to add an agent to the config file
     */
    private static class AddAgentCommand implements UpdateConfigCommand {
        private final AgentConfig agentConfig;

        public AddAgentCommand(AgentConfig agentConfig) {
            this.agentConfig = agentConfig;
        }

        public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
            cruiseConfig.agents().add(agentConfig);
            return cruiseConfig;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            AddAgentCommand that = (AddAgentCommand) o;

            if (agentConfig != null ? !agentConfig.equals(that.agentConfig) : that.agentConfig != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return agentConfig != null ? agentConfig.hashCode() : 0;
        }

        @Override
        public String toString() {
            return "AddAgentcommand{" +
                    "agentConfig=" + agentConfig +
                    '}';
        }

    }

    /**
     * @understands how to update the agent approval status
     */
    private static class UpdateAgentApprovalStatus implements UpdateConfigCommand {
        private final String uuid;
        private final Boolean denied;

        public UpdateAgentApprovalStatus(String uuid, Boolean denied) {
            this.uuid = uuid;
            this.denied = denied;
        }

        public CruiseConfig update(CruiseConfig cruiseConfig) {
            AgentConfig agentConfig = cruiseConfig.agents().getAgentByUuid(uuid);
            bombIfNull(agentConfig, "Unable to set agent approval status; Agent [" + uuid + "] not found.");
            agentConfig.setDisabled(denied);
            return cruiseConfig;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            UpdateAgentApprovalStatus that = (UpdateAgentApprovalStatus) o;

            if (denied != null ? !denied.equals(that.denied) : that.denied != null) {
                return false;
            }
            if (uuid != null ? !uuid.equals(that.uuid) : that.uuid != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = uuid != null ? uuid.hashCode() : 0;
            result = 31 * result + (denied != null ? denied.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "UpdateAgentApprovalStatus{" +
                    "uuid='" + uuid + '\'' +
                    ", denied=" + denied +
                    '}';
        }
    }

    /**
     * @understands how to delete agent
     */
    private static class DeleteAgent implements UpdateConfigCommand {
        private final String uuid;

        public DeleteAgent(String uuid) {
            this.uuid = uuid;
        }

        public CruiseConfig update(CruiseConfig cruiseConfig) {
            AgentConfig agentConfig = cruiseConfig.agents().getAgentByUuid(uuid);
            if (agentConfig.isNull()) {
                bomb("Unable to delete agent; Agent [" + uuid + "] not found.");
            }
            cruiseConfig.getEnvironments().removeAgentFromAllEnvironments(uuid);
            cruiseConfig.agents().remove(agentConfig);
            return cruiseConfig;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            DeleteAgent that = (DeleteAgent) o;

            if (uuid != null ? !uuid.equals(that.uuid) : that.uuid != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return uuid != null ? uuid.hashCode() : 0;
        }

        @Override
        public String toString() {
            return "DeleteAgent{" +
                    "uuid='" + uuid + '\'' +
                    '}';
        }
    }

    public static class UpdateResourcesCommand implements UpdateConfigCommand {
        private final String uuid;
        private final Resources resources;

        public UpdateResourcesCommand(String uuid, Resources resources) {
            this.uuid = uuid;
            this.resources = resources;
        }

        public CruiseConfig update(CruiseConfig cruiseConfig) {
            AgentConfig agentConfig = cruiseConfig.agents().getAgentByUuid(uuid);
            bombIfNull(agentConfig, "Unable to set agent resources; Agent [" + uuid + "] not found.");
            agentConfig.setResources(resources);
            return cruiseConfig;
        }
    }

    public static class ModifyResourcesCommand implements UpdateConfigCommand {
        private final String uuid;
        private final Resource resource;
        private final TriStateSelection.Action action;

        public ModifyResourcesCommand(String uuid, Resource resource, TriStateSelection.Action action) {
            this.uuid = uuid;
            this.resource = resource;
            this.action = action;
        }

        public CruiseConfig update(CruiseConfig cruiseConfig) {
            AgentConfig agentConfig = cruiseConfig.agents().getAgentByUuid(uuid);
            bombIfNull(agentConfig, "Unable to set agent resources; Agent [" + uuid + "] not found.");
            if (action.equals(TriStateSelection.Action.add)) {
                agentConfig.addResource(resource);
            } else if (action.equals(TriStateSelection.Action.remove)) {
                agentConfig.removeResource(resource);
            } else if (action.equals(TriStateSelection.Action.nochange)) {
                //do nothing
            } else {
                bomb(String.format("unsupported action '%s'", action));
            }
            return cruiseConfig;
        }
    }

    public static class ModifyRoleCommand implements UpdateConfigCommand {
        private String user;
        private TriStateSelection roleSelection;

        public ModifyRoleCommand(String user, TriStateSelection roleSelection) {
            this.user = user;
            this.roleSelection = roleSelection;
        }

        public CruiseConfig update(CruiseConfig cruiseConfig) {
            RolesConfig rolesConfig = cruiseConfig.server().security().getRoles();
            String roleName = roleSelection.getValue();
            Role role = rolesConfig.findByName(new CaseInsensitiveString(roleName));
            switch (roleSelection.getAction()) {
                case add:
                    if (role == null) {
                        role = new Role(new CaseInsensitiveString(roleName), new Users());
                        rolesConfig.add(role);
                    }
                    if (!role.hasMember(new CaseInsensitiveString(user))) {
                        role.addUser(new RoleUser(new CaseInsensitiveString(user)));
                    }
                    break;
                case remove:
                    if (role != null) {
                        role.removeUser(new RoleUser(new CaseInsensitiveString(user)));
                    }
                    break;
                case nochange:
                    break;
                default:
                    throw ExceptionUtils.bomb("unrecognized Action: " + roleSelection.getAction());
            }
            return cruiseConfig;
        }
    }

    public static class ModifyAdminPrivilegeCommand implements UpdateConfigCommand {
        private String user;
        private TriStateSelection adminPrivilegeSelection;

        public static final UserRoleMatcher ALWAYS_FALSE_MATCHER = new UserRoleMatcher() {
            public boolean match(CaseInsensitiveString userName, CaseInsensitiveString roleName) {
                return false;
            }
        };

        public ModifyAdminPrivilegeCommand(String user, TriStateSelection adminPrivilege) {
            this.user = user;
            this.adminPrivilegeSelection = adminPrivilege;
        }

        public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
            final AdminsConfig adminsConfig = cruiseConfig.server().security().adminsConfig();
            switch (adminPrivilegeSelection.getAction()) {
                case add:
                    if (!adminsConfig.hasUser(new CaseInsensitiveString(user), ALWAYS_FALSE_MATCHER)) {
                        adminsConfig.add(new AdminUser(new CaseInsensitiveString(user)));
                    }
                    break;
                case remove:
                    adminsConfig.remove(new AdminUser(new CaseInsensitiveString(user)));
                    break;
            }
            return cruiseConfig;
        }
    }

    public static class ModifyEnvironmentCommand implements UpdateConfigCommand {
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
            EnvironmentConfig environmentConfig = cruiseConfig.getEnvironments().named(new CaseInsensitiveString(environmentName));
            if (action.equals(TriStateSelection.Action.add)) {
                environmentConfig.addAgentIfNew(uuid);
            } else if (action.equals(TriStateSelection.Action.remove)) {
                environmentConfig.removeAgent(uuid);
            } else if (action.equals(TriStateSelection.Action.nochange)) {
                //do nothing
            } else {
                bomb(String.format("unsupported action '%s'", action));
            }
            return cruiseConfig;
        }
    }

    public GoConfigHolder loadConfigHolder() {
        return cachedConfigService.loadConfigHolder();
    }
}
