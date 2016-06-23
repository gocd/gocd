package com.thoughtworks.go.server.service;

import com.sun.xml.internal.ws.api.pipe.Pipe;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.domain.PipelineGroups;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.service.ConfigRepository;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.UUID;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})

public class PipelineConfigsServiceIntegrationTest {

    @Autowired
    private ConfigCache configCache;
    @Autowired
    private ConfigElementImplementationRegistry registry;
    @Autowired
    private GoConfigService goConfigService;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private DatabaseAccessHelper dbHelper;
    @Autowired
    private GoConfigDao goConfigDao;


    private GoConfigFileHelper configHelper;

    @Before
    public void setUp() throws Exception {
        configHelper = new GoConfigFileHelper();
        dbHelper.onSetUp();
        configHelper.usingCruiseConfigDao(goConfigDao).initializeConfigFile();
        configHelper.onSetUp();

    }

    @After
    public void tearDown() throws Exception {
        configHelper.onTearDown();
        dbHelper.onTearDown();

    }


    @Test
    public void shouldUpdatePipelineGroupAuthorizationWhenUserIsSuperAdmin() throws Exception {

        String pipelineName = UUID.randomUUID().toString();
        PipelineConfig pipelineConfig = GoConfigMother.createPipelineConfigWithMaterialConfig(pipelineName, new GitMaterialConfig("FOO"));
        String groupName = "groupName";
        goConfigService.addPipeline(pipelineConfig, groupName);
        setupSecurity();

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        Authorization authorization = new Authorization(new OperationConfig(new AdminUser(new CaseInsensitiveString("admin"))));
        PipelineConfigsService pipelineConfigsService = new PipelineConfigsService(configCache, registry, goConfigService, securityService);

        PipelineConfigs pipelineConfigs = goConfigService.getAllPipelinesInGroup(groupName);
        assertThat(new Authorization(), is(pipelineConfigs.getAuthorization()));

        pipelineConfigsService.updateAuthorization(authorization, groupName, result, new Username(new CaseInsensitiveString("root")));

        PipelineConfigs pipelineConfigsAfterUpdatingAuthorization = goConfigService.getAllPipelinesInGroup(groupName);
        assertThat(authorization, is(pipelineConfigsAfterUpdatingAuthorization.getAuthorization()));
        assertThat(result.toString(), result.isSuccessful(), is(true));
    }


    @Test
    public void shouldUpdatePipelineGroupAuthorizationWhenUserIsGroupAdmin() throws Exception {

        String pipelineName = UUID.randomUUID().toString();
        PipelineConfig pipelineConfig = GoConfigMother.createPipelineConfigWithMaterialConfig(pipelineName, new GitMaterialConfig("FOO"));
        String groupName = "groupName";
        goConfigService.addPipeline(pipelineConfig, groupName);
        setupSecurity();
        configHelper.setAdminPermissionForGroup("groupName", "groupAdmin");

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        PipelineConfigsService pipelineConfigsService = new PipelineConfigsService(configCache, registry, goConfigService, securityService);

        Authorization authorization = new Authorization(new OperationConfig(new AdminUser(new CaseInsensitiveString("newAdmin"))));
        pipelineConfigsService.updateAuthorization(authorization, groupName, result, new Username(new CaseInsensitiveString("groupAdmin")));

        PipelineConfigs pipelineConfigsAfterUpdatingAuthorization = goConfigService.getAllPipelinesInGroup(groupName);
        assertThat(authorization, is(pipelineConfigsAfterUpdatingAuthorization.getAuthorization()));
        assertThat(result.toString(), result.isSuccessful(), is(true));
    }


    @Test
    public void shouldNotUpdatePipelineGroupAuthorizationWhenUserIsNotGroupAdminAndNotSuperAdmin() throws Exception {
        String pipelineName = UUID.randomUUID().toString();
        PipelineConfig pipelineConfig = GoConfigMother.createPipelineConfigWithMaterialConfig(pipelineName, new GitMaterialConfig("FOO"));
        String groupName = "groupName";
        goConfigService.addPipeline(pipelineConfig, groupName);
        setupSecurity();
        configHelper.setAdminPermissionForGroup("groupName", "groupAdmin");

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        PipelineConfigsService pipelineConfigsService = new PipelineConfigsService(configCache, registry, goConfigService, securityService);

        Authorization authorization = new Authorization(new OperationConfig(new AdminUser(new CaseInsensitiveString("newAdmin"))));
        pipelineConfigsService.updateAuthorization(authorization, groupName, result, new Username(new CaseInsensitiveString("NotAnAdmin")));

        assertThat(result.toString(), result.isSuccessful(), is(false));
        assertThat(result.toString(), result.httpCode(), is(401));
        assertThat(result.toString(), result.toString().contains("UNAUTHORIZED_TO_EDIT_GROUP"), is(true));
    }

    @Test
    public void shouldReturnInvalidGroupErrorWhenGroupIsNotAvailable() throws Exception {
        String pipelineName = UUID.randomUUID().toString();
        PipelineConfig pipelineConfig = GoConfigMother.createPipelineConfigWithMaterialConfig(pipelineName, new GitMaterialConfig("FOO"));
        String groupName = "groupName";
        goConfigService.addPipeline(pipelineConfig, groupName);
        setupSecurity();

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        PipelineConfigsService pipelineConfigsService = new PipelineConfigsService(configCache, registry, goConfigService, securityService);

        Authorization authorization = new Authorization(new OperationConfig(new AdminUser(new CaseInsensitiveString("newAdmin"))));
        pipelineConfigsService.updateAuthorization(authorization, "invaldGroupName", result, new Username(new CaseInsensitiveString("groupAdmin")));

        assertThat(result.toString(), result.isSuccessful(), is(false));
        assertThat(result.toString(), result.httpCode(), is(404));
        assertThat(result.toString(), result.toString().contains("PIPELINE_GROUP_NOT_FOUND"), is(true));
    }

    private void setupSecurity() {
        SecurityConfig securityConfig = new SecurityConfig(new LdapConfig(new GoCipher()), new PasswordFileConfig("foo"), false);
        securityConfig.adminsConfig().add(new AdminUser(new CaseInsensitiveString("root")));
        configHelper.addSecurity(securityConfig);

    }

}