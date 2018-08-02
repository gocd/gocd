package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.AdminsConfig;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.update.AdminsConfigUpdateCommand;
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
                result.unprocessableEntity("Validation failed while updating System Admins, check errors.");
            } else {
                if (!result.hasMessage()) {
                    LOGGER.error(e.getMessage(), e);
                    result.internalServerError(saveFailedWithReason("An error occurred while updating the System Admins. Please check the logs for more information."));
                }
            }
        }
    }
}


