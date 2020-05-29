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
package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.AdminsConfig;
import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.ConfigSaveValidationContext;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;

import static com.thoughtworks.go.i18n.LocalizedMessage.forbiddenToEdit;
import static com.thoughtworks.go.i18n.LocalizedMessage.staleResourceConfig;
import static com.thoughtworks.go.serverhealth.HealthStateType.forbidden;

public class AdminsConfigUpdateCommand implements EntityConfigUpdateCommand<AdminsConfig> {

    private final EntityHashingService hashingService;
    protected AdminsConfig preprocessedAdmin;
    protected final GoConfigService goConfigService;
    protected final AdminsConfig admin;
    protected final Username currentUser;
    protected final LocalizedOperationResult result;
    private final String digest;

    public AdminsConfigUpdateCommand(GoConfigService goConfigService, AdminsConfig adminsConfig, Username currentUser,
                                     LocalizedOperationResult result, EntityHashingService hashingService, String digest) {
        this.hashingService = hashingService;
        this.digest = digest;
        this.goConfigService = goConfigService;
        this.admin = adminsConfig;
        this.currentUser = currentUser;
        this.result = result;
        this.preprocessedAdmin = adminsConfig;
    }

    @Override
    public void update(CruiseConfig preprocessedConfig) {
        preprocessedConfig.server().security().setAdminsConfig(admin);
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        preprocessedAdmin = preprocessedConfig.server().security().adminsConfig();

        if (!preprocessedAdmin.validateTree(ConfigSaveValidationContext.forChain(preprocessedConfig))) {
            BasicCruiseConfig.copyErrors(preprocessedAdmin, admin);
            return false;
        }

        return true;
    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        return isAuthorized() && isRequestFresh(cruiseConfig);
    }
    @Override
    public void clearErrors() {
        BasicCruiseConfig.clearErrors(admin);
    }

    @Override
    public AdminsConfig getPreprocessedEntityConfig() {
        return preprocessedAdmin;
    }

    public AdminsConfig getEntity() {
        return admin;
    }

    final AdminsConfig findExistingAdmin(CruiseConfig cruiseConfig) {
        return cruiseConfig.server().security().adminsConfig();
    }


    private boolean isRequestFresh(CruiseConfig cruiseConfig) {
        AdminsConfig existingAdminsConfig = findExistingAdmin(cruiseConfig);

        boolean freshRequest = hashingService.hashForEntity(existingAdminsConfig).equals(digest);

        if (!freshRequest) {
            result.stale(staleResourceConfig("System admins", existingAdminsConfig.getClass().getName()));
        }
        return freshRequest;
    }

    private final boolean isAuthorized() {
        if (goConfigService.isUserAdmin(currentUser)) {
            return true;
        }

        result.forbidden(forbiddenToEdit(), forbidden());
        return false;
    }
}
