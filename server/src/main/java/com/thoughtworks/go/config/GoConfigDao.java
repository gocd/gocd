/*
 * Copyright Thoughtworks, Inc.
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

import com.thoughtworks.go.config.commands.CheckedUpdateCommand;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.update.ConfigUpdateCheckFailedException;
import com.thoughtworks.go.config.update.FullConfigUpdateCommand;
import com.thoughtworks.go.config.validation.GoConfigValidity;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import org.jetbrains.annotations.TestOnly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.function.Function;

/**
 * Understands how to modify the cruise config sources
 */
@Component
public class GoConfigDao {
    private static final Logger LOGGER = LoggerFactory.getLogger(GoConfigDao.class);
    private final CachedGoConfig cachedConfigService;

    @Autowired
    public GoConfigDao(CachedGoConfig cachedConfigService) {
        this.cachedConfigService = cachedConfigService;
    }

    public String fileLocation() {
        return cachedConfigService.getFileLocation();
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

    public void updateConfig(EntityConfigUpdateCommand<?> command, Username currentUser) {
        LOGGER.info("Config update for entity request by {} is in queue - {}", currentUser, command);
        synchronized (GoConfigWriteLock.class) {
            try {
                LOGGER.info("Config update for entity request by {} is being processed", currentUser);
                if (!command.canContinue(cachedConfigService.currentConfig())) {
                    throw new ConfigUpdateCheckFailedException();
                }
                cachedConfigService.writeEntityWithLock(command, currentUser);
            } catch (Exception e) {
                LOGGER.error("Config update for entity failed", e);
                throw e;
            } finally {
                LOGGER.info("Entity update for request by {} is completed", currentUser);
            }
        }
    }

    public ConfigSaveState updateConfig(UpdateConfigCommand command) {
        return doSaveWith(command, c -> {
            if (c instanceof CheckedUpdateCommand checkedCommand) {
                if (!checkedCommand.canContinue(cachedConfigService.currentConfig())) {
                    throw new ConfigUpdateCheckFailedException();
                }
            }
            return cachedConfigService.writeWithLock(c);
        });
    }

    public ConfigSaveState updateFullConfig(FullConfigUpdateCommand command) {
        return doSaveWith(command, cachedConfigService::writeFullConfigWithLock);
    }

    private <T extends UpdateConfigCommand> ConfigSaveState doSaveWith(T command, Function<T, ConfigSaveState> configOperation) {
        LOGGER.info("Config update request by {} is in queue - {}", SessionUtils.currentUsername().getUsername(), command);
        synchronized (GoConfigWriteLock.class) {
            try {
                LOGGER.info("Config update request {} by {} is being processed", command, SessionUtils.currentUsername().getUsername());
                return configOperation.apply(command);
            } catch (Exception e) {
                LOGGER.error("{} failed", command, e);
                throw e;
            } finally {
                LOGGER.info("Config update request by {} is completed", SessionUtils.currentUsername().getUsername());
            }
        }
    }

    public GoConfigValidity checkConfigFileValid() {
        return cachedConfigService.checkConfigFileValid();
    }

    public void registerListener(ConfigChangedListener listener) {
        cachedConfigService.registerListener(listener);
    }

    @TestOnly
    public void reloadListeners() {
        cachedConfigService.reloadListeners();
    }

    @TestOnly
    public void forceReload() {
        cachedConfigService.forceReload();
    }

    public GoConfigHolder loadConfigHolder() {
        return cachedConfigService.loadConfigHolder();
    }

}
