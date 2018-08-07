package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.AdminsConfig;
import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.update.AdminConfigReplaceCommand;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class AdminsConfigServiceTest {

    private GoConfigService configService;

    private BasicCruiseConfig cruiseConfig;

    private AdminsConfigService adminsConfigService;

    @Before
    public void setUp() throws Exception {
        configService = mock(GoConfigService.class);
        cruiseConfig = GoConfigMother.defaultCruiseConfig();

        when(configService.cruiseConfig()).thenReturn(cruiseConfig);
        adminsConfigService = new AdminsConfigService(configService);
    }


    @Test
    public void create_shouldAddAdminsToConfig() throws Exception {
        AdminsConfig config = new AdminsConfig();
        Username admin = new Username("admin");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        adminsConfigService.replace(admin,config,result);

        verify(configService).updateConfig(any(AdminConfigReplaceCommand.class), eq(admin));
    }
}
