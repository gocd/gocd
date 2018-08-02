package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.AdminsConfig;
import com.thoughtworks.go.config.Role;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.update.AdminConfigReplaceCommand;
import com.thoughtworks.go.domain.config.Admin;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.thoughtworks.go.i18n.LocalizedMessage.saveFailedWithReason;

@Component
public class AdminsConfigService {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AdminsConfigService.class);

    private final GoConfigService goConfigService;

    @Autowired
    public AdminsConfigService(GoConfigService goConfigService) {
        this.goConfigService = goConfigService;
    }

    public AdminsConfig findAdmins() {
        return goConfigService.serverConfig().security().adminsConfig();
    }

    public void replace(Username currentUser, AdminsConfig config, LocalizedOperationResult result) {
        update(currentUser, config, result, new AdminConfigReplaceCommand(goConfigService, config, currentUser, result));
    }

    protected void update(Username currentUser, AdminsConfig adminsConfig, LocalizedOperationResult result, EntityConfigUpdateCommand<AdminsConfig> command) {
        try {
            goConfigService.updateConfig(command, currentUser);
        } catch (Exception e) {
            if (!result.hasMessage()) {
                    LOGGER.error(e.getMessage(), e);
                    result.internalServerError(saveFailedWithReason("An error occurred while saving the admin config. Please check the logs for more information."));
                }
            }
        }
    }


