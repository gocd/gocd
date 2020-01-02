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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.config.update.ConfigUpdateCheckFailedException;
import com.thoughtworks.go.config.update.CreatePipelineConfigsCommand;
import com.thoughtworks.go.config.update.DeletePipelineConfigsCommand;
import com.thoughtworks.go.config.update.UpdatePipelineConfigsCommand;
import com.thoughtworks.go.config.validation.GoConfigValidity;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.responses.GoConfigOperationalResponse;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.thoughtworks.go.i18n.LocalizedMessage.entityConfigValidationFailed;
import static com.thoughtworks.go.i18n.LocalizedMessage.saveFailedWithReason;
import static com.thoughtworks.go.serverhealth.HealthStateType.forbiddenForGroup;

@Service
public class PipelineConfigsService {

    private GoConfigService goConfigService;
    private final ConfigCache configCache;
    private final ConfigElementImplementationRegistry registry;
    private final SecurityService securityService;
    private EntityHashingService entityHashingService;
    private MagicalGoConfigXmlLoader magicalGoConfigXmlLoader;
    private static final Logger LOGGER = LoggerFactory.getLogger(PipelineConfigsService.class);

    @Autowired
    public PipelineConfigsService(ConfigCache configCache, ConfigElementImplementationRegistry registry, GoConfigService goConfigService, SecurityService securityService, EntityHashingService entityHashingService) {
        this.goConfigService = goConfigService;
        this.configCache = configCache;
        this.registry = registry;
        this.securityService = securityService;
        this.entityHashingService = entityHashingService;
        this.magicalGoConfigXmlLoader = new MagicalGoConfigXmlLoader(configCache, registry);
    }

    public GoConfigOperationalResponse<PipelineConfigs> updateXml(String groupName, String xmlPartial, final String md5, Username username, HttpLocalizedOperationResult result) throws Exception {
        if (!userHasPermissions(username, groupName, result)) {
            return new GoConfigOperationalResponse<>(GoConfigValidity.valid(), null);
        }
        GoConfigValidity goConfigValidity = goConfigService.groupSaver(groupName).saveXml(xmlPartial, md5);
        if (!goConfigValidity.isValid()) {
            handleError(groupName, (GoConfigValidity.InvalidGoConfig) goConfigValidity, result);
            return new GoConfigOperationalResponse<>(goConfigValidity, null);
        }
        String savedSuccessMessage = "Saved configuration successfully.";
        String localizableMessage = ((GoConfigValidity.ValidGoConfig) goConfigValidity).wasMerged() ? LocalizedMessage.composite(savedSuccessMessage, "The configuration was modified by someone else, but your changes were merged successfully.") : savedSuccessMessage;
        result.setMessage(localizableMessage);
        PipelineConfigs pipelineConfigs = magicalGoConfigXmlLoader.fromXmlPartial(xmlPartial, BasicPipelineConfigs.class);
        return new GoConfigOperationalResponse<>(goConfigValidity, pipelineConfigs);
    }

    public String getXml(String groupName, Username username, HttpLocalizedOperationResult result) {
        if (!userHasPermissions(username, groupName, result)) {
            return null;
        }
        return new MagicalGoConfigXmlWriter(configCache, registry).toXmlPartial(getConfig(groupName));
    }

    private void handleError(String groupName, GoConfigValidity.InvalidGoConfig goConfigValidity, HttpLocalizedOperationResult result) {
        if (goConfigValidity.isMergeConflict() || goConfigValidity.isPostValidationError()) {
            result.badRequest("Someone has modified the configuration and your changes are in conflict. Please review, amend and retry.");
        } else {
            result.internalServerError(updateFailedMessage(groupName, goConfigValidity.errorMessage()));
        }
    }

    private PipelineConfigs getConfig(String groupName) {
        CruiseConfig cruiseConfig = goConfigService.getConfigForEditing();
        return cruiseConfig.getGroups().findGroup(groupName);
    }

    public List<PipelineConfigs> getGroupsForUser(String userName) {
        List<PipelineConfigs> pipelineGroups = new ArrayList<>();
        for (PipelineConfigs pipelineGroup : goConfigService.groups()) {
            if (securityService.hasViewPermissionForGroup(userName, pipelineGroup.getGroup())) {
                pipelineGroups.add(pipelineGroup);
            }
        }
        return pipelineGroups;
    }

    private void update(Username currentUser, PipelineConfigs pipelineConfigs, LocalizedOperationResult result, EntityConfigUpdateCommand command) {
        try {
            goConfigService.updateConfig(command, currentUser);
        } catch (Exception e) {
            if (e instanceof GoConfigInvalidException) {
                if (!result.hasMessage()) {
                    result.unprocessableEntity(entityConfigValidationFailed(pipelineConfigs.getClass().getAnnotation(ConfigTag.class).value(), pipelineConfigs.getGroup(), e.getMessage()));
                }
            } else if (!(e instanceof ConfigUpdateCheckFailedException)) {
                LOGGER.error(e.getMessage(), e);
                result.internalServerError(saveFailedWithReason(e.getMessage()));
            }
        }
    }

    public PipelineConfigs updateGroup(Username currentUsername, PipelineConfigs pipelineConfigsFromReq, PipelineConfigs pipelineConfigsFromServer, HttpLocalizedOperationResult result) {
        UpdatePipelineConfigsCommand updatePipelineConfigsCommand = new UpdatePipelineConfigsCommand(pipelineConfigsFromServer, pipelineConfigsFromReq, result, currentUsername, entityHashingService.md5ForEntity(pipelineConfigsFromServer), entityHashingService, securityService);
        update(currentUsername, pipelineConfigsFromReq, result, updatePipelineConfigsCommand);
        return updatePipelineConfigsCommand.getPreprocessedEntityConfig();
    }

    public void deleteGroup(Username currentUser, PipelineConfigs pipelineConfigs, HttpLocalizedOperationResult result) {
        DeletePipelineConfigsCommand deletePipelineConfigsCommand = new DeletePipelineConfigsCommand(pipelineConfigs, result, currentUser, securityService);
        update(currentUser, pipelineConfigs, result, deletePipelineConfigsCommand);
        if (result.isSuccessful()) {
            result.setMessage(EntityType.PipelineGroup.deleteSuccessful(pipelineConfigs.getGroup()));
        }
    }

    public void createGroup(Username currentUser, PipelineConfigs pipelineConfigs, HttpLocalizedOperationResult result) {
        CreatePipelineConfigsCommand createPipelineConfigsCommand = new CreatePipelineConfigsCommand(pipelineConfigs, currentUser, result, securityService);
        update(currentUser, pipelineConfigs, result, createPipelineConfigsCommand);
    }

    private boolean userHasPermissions(Username username, String groupName, HttpLocalizedOperationResult result) {
        try {
            if (!securityService.isUserAdminOfGroup(username.getUsername(), groupName)) {
                result.forbidden(EntityType.PipelineGroup.forbiddenToEdit(groupName, username.getUsername()), forbiddenForGroup(groupName));
                return false;
            }
        } catch (Exception e) {
            result.notFound(EntityType.PipelineGroup.notFoundMessage(groupName), HealthStateType.general(HealthStateScope.forGroup(groupName)));
            return false;
        }
        return true;
    }

    private String updateFailedMessage(String groupName, String message) {
        return "Failed to update group '" + groupName + "'. " + message;
    }
}
