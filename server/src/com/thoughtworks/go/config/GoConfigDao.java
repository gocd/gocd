/*
 * Copyright 2016 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.config;

import com.rits.cloning.Cloner;
import com.thoughtworks.go.config.commands.CheckedUpdateCommand;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.update.AddEnvironmentCommand;
import com.thoughtworks.go.config.update.ConfigUpdateCheckFailedException;
import com.thoughtworks.go.config.validation.GoConfigValidity;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.presentation.TriStateSelection;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EnvironmentConfigService;
import com.thoughtworks.go.server.util.UserHelper;
import com.thoughtworks.go.util.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

/**
 * @understands how to modify the cruise config sources
 */
@Component
public class GoConfigDao {
    private static final Logger LOGGER = LoggerFactory.getLogger(GoConfigDao.class);
    private CachedGoConfig cachedConfigService;
    private Cloner cloner = new Cloner();

    @Autowired
    public GoConfigDao(CachedGoConfig cachedConfigService) {
        this.cachedConfigService = cachedConfigService;
    }

    public String fileLocation() {
        return cachedConfigService.getFileLocation();
    }

    public void updateMailHost(MailHost mailHost) {
        updateConfig(mailHostUpdater(mailHost));
    }

    public void addPipeline(PipelineConfig pipelineConfig, String groupName) {
        updateConfig(pipelineAdder(pipelineConfig, groupName));
    }

    public void addEnvironment(final BasicEnvironmentConfig environmentConfig) {
        updateConfig(new UpdateConfigCommand() {
            public CruiseConfig update(CruiseConfig cruiseConfig) {
                cruiseConfig.getEnvironments().add(environmentConfig);
                return cruiseConfig;
            }
        });
    }

    public CruiseConfig loadForEditing() {
        return cachedConfigService.loadForEditing();
    }

    public CruiseConfig loadMergedForEditing() {
        return cachedConfigService.loadMergedForEditing();
    }

    public CruiseConfig load() {
        return cachedConfigService.currentConfig();
    }

    public String md5OfConfigFile() {
        return cachedConfigService.currentConfig().getMd5();
    }

    public void updateConfig(EntityConfigUpdateCommand command, Username currentUser) {
        LOGGER.info("Config update for pipeline request by {} is in queue - {}", currentUser, command);
        synchronized (GoConfigWriteLock.class) {
            LOGGER.info("Config update for pipeline request by {} is being processed", currentUser);
            if (!command.canContinue(cachedConfigService.currentConfig())) {
                throw new ConfigUpdateCheckFailedException();
            }
            cachedConfigService.writeEntityWithLock(command, currentUser);
        }
    }

    public ConfigSaveState updateConfig(UpdateConfigCommand command) {
        ConfigSaveState configSaveState = null;
        LOGGER.info("Config update request by {} is in queue - {}", UserHelper.getUserName().getUsername(), command);
        synchronized (GoConfigWriteLock.class) {
            try {
                LOGGER.info("Config update request by {} is being processed", UserHelper.getUserName().getUsername());
                if (command instanceof CheckedUpdateCommand) {
                    CheckedUpdateCommand checkedCommand = (CheckedUpdateCommand) command;
                    if (!checkedCommand.canContinue(cachedConfigService.currentConfig())) {
                        throw new ConfigUpdateCheckFailedException();
                    }
                }
                configSaveState = cachedConfigService.writeWithLock(command);
            } finally {
                if (command instanceof ConfigAwareUpdate) {
                    ((ConfigAwareUpdate) command).afterUpdate(clonedConfig());
                }
                LOGGER.info("Config update request by {} is completed", UserHelper.getUserName().getUsername());
            }
        }
        return configSaveState;
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
        private List<UpdateConfigCommand> commands = new ArrayList<>();

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


    private UpdateConfigCommand pipelineAdder(final PipelineConfig pipelineConfig, final String groupName) {
        return new UpdateConfigCommand() {
            public CruiseConfig update(CruiseConfig cruiseConfig) {
                cruiseConfig.addPipeline(groupName, pipelineConfig);
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

    public GoConfigHolder loadConfigHolder() {
        return cachedConfigService.loadConfigHolder();
    }

}
