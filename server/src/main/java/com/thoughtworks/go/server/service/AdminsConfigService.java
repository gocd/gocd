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

import com.google.common.collect.Sets;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.update.AdminsConfigUpdateCommand;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.config.Admin;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.BulkUpdateAdminsResult;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.thoughtworks.go.i18n.LocalizedMessage.saveFailedWithReason;

@Component
public class AdminsConfigService {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AdminsConfigService.class);

    private final GoConfigService goConfigService;
    private EntityHashingService entityHashingService;

    @Autowired
    public AdminsConfigService(GoConfigService goConfigService, EntityHashingService entityHashingService) {
        this.goConfigService = goConfigService;
        this.entityHashingService = entityHashingService;
    }

    public AdminsConfig systemAdmins() {
        return goConfigService.serverConfig().security().adminsConfig();
    }

    public void update(Username currentUser, AdminsConfig config, String md5, LocalizedOperationResult result) {
        updateConfig(currentUser, result, new AdminsConfigUpdateCommand(goConfigService, config, currentUser, result, entityHashingService, md5));
    }

    private void updateConfig(Username currentUser, LocalizedOperationResult result, EntityConfigUpdateCommand<AdminsConfig> command) {
        try {
            goConfigService.updateConfig(command, currentUser);
        } catch (Exception e) {
            if (e instanceof GoConfigInvalidException) {
                String adminsTag = AdminsConfig.class.getAnnotation(ConfigTag.class).value();
                String errors = deDuplicatedErrors(((GoConfigInvalidException) e).getCruiseConfig().getAllErrors());
                result.unprocessableEntity(LocalizedMessage.entityConfigValidationFailed(adminsTag, errors));
            } else {
                if (!result.hasMessage()) {
                    LOGGER.error(e.getMessage(), e);
                    result.internalServerError(saveFailedWithReason("An error occurred while updating the System Admins. Please check the logs for more information."));
                }
            }
        }
    }

    //Hack to remove duplicate errors. See `AdminRole.addError`
    private String deDuplicatedErrors(List<ConfigErrors> allErrors) {
        Set<String> errors = allErrors.stream().map(ConfigErrors::firstError).collect(Collectors.toSet());
        return StringUtils.join(errors, ",");
    }

    public BulkUpdateAdminsResult bulkUpdate(Username currentUser,
                                             List<String> usersToAdd,
                                             List<String> usersToRemove,
                                             List<String> rolesToAdd,
                                             List<String> rolesToRemove,
                                             String md5) {
        Set<Admin> existingAdmins = new HashSet<>(systemAdmins());
        BulkUpdateAdminsResult result = validateUsersAndRolesForBulkUpdate(usersToRemove, rolesToRemove, existingAdmins);
        if (!result.isSuccessful()) {
            return result;
        }

        usersToAdd.forEach(user -> existingAdmins.add(new AdminUser(user)));
        rolesToAdd.forEach(role -> existingAdmins.add(new AdminRole(role)));
        usersToRemove.forEach(user -> existingAdmins.remove(new AdminUser(new CaseInsensitiveString(user))));
        rolesToRemove.forEach(role -> existingAdmins.remove(new AdminRole(new CaseInsensitiveString(role))));
        AdminsConfigUpdateCommand command = new AdminsConfigUpdateCommand(goConfigService, new AdminsConfig(existingAdmins),
                currentUser, result, entityHashingService, md5);
        updateConfig(currentUser, result, command);
        result.setAdminsConfig(command.getEntity());
        return result;
    }

    private BulkUpdateAdminsResult validateUsersAndRolesForBulkUpdate(List<String> usersToRemove, List<String> rolesToRemove,
                                                                      Set<Admin> existingAdmins) {
        Set<CaseInsensitiveString> existingAdminNames = existingAdmins.stream().map(Admin::getName).collect(Collectors.toSet());
        Sets.SetView<CaseInsensitiveString> invalidUsersToRemove = Sets.difference(caseInsensitive(usersToRemove), existingAdminNames);
        Sets.SetView<CaseInsensitiveString> invalidRolesToRemove = Sets.difference(caseInsensitive(rolesToRemove), existingAdminNames);
        BulkUpdateAdminsResult result = new BulkUpdateAdminsResult();
        if (invalidUsersToRemove.size() > 0) {
            result.setNonExistentUsers(invalidUsersToRemove);
            result.unprocessableEntity("Update failed because some users or roles do not exist under super admins.");
        }
        if (invalidRolesToRemove.size() > 0) {
            result.setNonExistentRoles(invalidRolesToRemove);
            result.unprocessableEntity("Update failed because some users or roles do not exist under super admins.");
        }
        return result;
    }

    private Set<CaseInsensitiveString> caseInsensitive(List<String> list) {
        return list.stream().map(CaseInsensitiveString::new).collect(Collectors.toSet());
    }
}
