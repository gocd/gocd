/*
 * Copyright 2016 ThoughtWorks, Inc.
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
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.config.validation.GoConfigValidity;
import com.thoughtworks.go.i18n.Localizable;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.responses.GoConfigOperationalResponse;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PipelineConfigsService {

    private GoConfigService goConfigService;
    private final ConfigCache configCache;
    private final ConfigElementImplementationRegistry registry;
    private final SecurityService securityService;
    private MagicalGoConfigXmlLoader magicalGoConfigXmlLoader;

    @Autowired
    public PipelineConfigsService(ConfigCache configCache, ConfigElementImplementationRegistry registry, GoConfigService goConfigService, SecurityService securityService) {
        this.goConfigService = goConfigService;
        this.configCache = configCache;
        this.registry = registry;
        this.securityService = securityService;
        this.magicalGoConfigXmlLoader = new MagicalGoConfigXmlLoader(configCache, registry);
    }

    public GoConfigOperationalResponse<PipelineConfigs> updateXml(String groupName, String xmlPartial, final String md5, Username username, HttpLocalizedOperationResult result) throws Exception {
        if (!userHasPermissions(username, groupName, result)) {
            return new GoConfigOperationalResponse<>(GoConfigValidity.valid(), null);
        }
        GoConfigValidity goConfigValidity = goConfigService.groupSaver(groupName).saveXml(xmlPartial, md5);
        if (!goConfigValidity.isValid()) {
            handleError(groupName, goConfigValidity, result);
            return new GoConfigOperationalResponse<>(goConfigValidity, null);
        }
        Localizable savedSuccessMessage = LocalizedMessage.string("SAVED_CONFIGURATION_SUCCESSFULLY");
        Localizable localizableMessage = goConfigValidity.wasMerged() ? LocalizedMessage.composite(savedSuccessMessage, LocalizedMessage.string("CONFIG_MERGED")) : savedSuccessMessage;
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

    private void handleError(String groupName, GoConfigValidity goConfigValidity, HttpLocalizedOperationResult result) {
        if (goConfigValidity.isMergeConflict() || goConfigValidity.isPostValidationError()) {
            result.badRequest(LocalizedMessage.string("FLASH_MESSAGE_ON_CONFLICT"));
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

    private boolean userHasPermissions(Username username, String groupName, HttpLocalizedOperationResult result) {
        try {
            if (!securityService.isUserAdminOfGroup(username.getUsername(), groupName)) {
                result.unauthorized(LocalizedMessage.string("UNAUTHORIZED_TO_EDIT_GROUP", groupName), HealthStateType.unauthorisedForGroup(groupName));
                return false;
            }
        } catch (Exception e) {
            result.notFound(LocalizedMessage.string("PIPELINE_GROUP_NOT_FOUND", groupName), HealthStateType.general(HealthStateScope.forGroup(groupName)));
            return false;
        }
        return true;
    }

    private Localizable.CurryableLocalizable updateFailedMessage(String groupName, String message) {
        return LocalizedMessage.string("UPDATE_GROUP_XML_FAILED", groupName, message);
    }

}
