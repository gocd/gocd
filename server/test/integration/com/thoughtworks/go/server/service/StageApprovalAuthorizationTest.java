/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.AdminRole;
import com.thoughtworks.go.config.AdminUser;
import com.thoughtworks.go.config.Approval;
import com.thoughtworks.go.config.AuthConfig;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.Role;
import com.thoughtworks.go.config.RoleUser;
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.helper.StageConfigMother;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static com.thoughtworks.go.config.PipelineConfigs.DEFAULT_GROUP;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class StageApprovalAuthorizationTest {
    private static final GoConfigFileHelper CONFIG_HELPER = new GoConfigFileHelper();
    private static final String PIPELINE_NAME = "cruise";

    private AuthConfig authConfigWithUserJez = new AuthConfig(new AdminUser(new CaseInsensitiveString("jez")));
    private AuthConfig authConfigWithAdminRole = new AuthConfig(new AdminRole(new CaseInsensitiveString("adminRole")));

    @Autowired private GoConfigDao goConfigDao;
    @Autowired private SecurityService securityService;
    private static final String STAGE_NAME = "dev";

    @Before
    public void setUp() throws Exception {
        CONFIG_HELPER.usingCruiseConfigDao(goConfigDao);
        CONFIG_HELPER.onSetUp();
        CONFIG_HELPER.addPipeline(PIPELINE_NAME, STAGE_NAME);
    }

    @After
    public void tearDown() throws Exception {
        CONFIG_HELPER.onTearDown();
    }

    @Test
    public void shouldAuthorizeIfUserIsInApprovalList() throws Exception {
        CONFIG_HELPER.addSecurityWithAdminConfig();
        StageConfig stage = StageConfigMother.custom("ft", authConfigWithUserJez);
        PipelineConfig pipeline = CONFIG_HELPER.addStageToPipeline(PIPELINE_NAME, stage);

        assertThat("User jez should have permission on ft stage",
                securityService.hasOperatePermissionForStage(CaseInsensitiveString.str(pipeline.name()), CaseInsensitiveString.str(stage.name()), "jez"), is(true));
    }

    @Test
    public void shouldAuthorizeIfRoleIsInApprovalList() throws Exception {
        CONFIG_HELPER.addSecurityWithAdminConfig();
        CONFIG_HELPER.addRole(new Role(new CaseInsensitiveString("adminRole"), new RoleUser(new CaseInsensitiveString("tester"))));

        StageConfig stage = StageConfigMother.custom("test", authConfigWithAdminRole);
        PipelineConfig pipeline = CONFIG_HELPER.addStageToPipeline(PIPELINE_NAME, stage);

        assertThat("User tester should have permission on test stage",
                securityService.hasOperatePermissionForStage(CaseInsensitiveString.str(pipeline.name()), CaseInsensitiveString.str(stage.name()), "tester"), is(true));

    }

    @Test
    public void shouldUsePipelineGroupAuthorizationIfNoStageAuthorizationDefined() throws Exception {
        CONFIG_HELPER.addSecurityWithAdminConfig();
        CONFIG_HELPER.setOperatePermissionForGroup(DEFAULT_GROUP, "user1");

        assertThat(securityService.hasOperatePermissionForStage(PIPELINE_NAME, STAGE_NAME, "user1"), is(true));
        assertThat(securityService.hasOperatePermissionForStage(PIPELINE_NAME, STAGE_NAME, "anyone"), is(false));
    }

    @Test
    public void stageAuthorizationShouldOverrideGroupAuthorization() throws Exception {
        CONFIG_HELPER.addSecurityWithAdminConfig();
        CONFIG_HELPER.setOperatePermissionForGroup(DEFAULT_GROUP, "user1", "jez");
        CONFIG_HELPER.setOperatePermissionForStage(PIPELINE_NAME, STAGE_NAME, "jez");

        assertThat(securityService.hasOperatePermissionForStage(PIPELINE_NAME, STAGE_NAME, "jez"), is(true));
        assertThat(securityService.hasOperatePermissionForStage(PIPELINE_NAME, STAGE_NAME, "user1"), is(false));
    }

    @Test
    public void shouldUseStageAuthorizationForFirstStage() throws Exception {
        CONFIG_HELPER.addSecurityWithAdminConfig();
        CONFIG_HELPER.setOperatePermissionForStage(PIPELINE_NAME, STAGE_NAME, "jez");

        assertThat(securityService.hasOperatePermissionForFirstStage(PIPELINE_NAME, "jez"), is(true));
        assertThat(securityService.hasOperatePermissionForFirstStage(PIPELINE_NAME, "anyone"), is(false));
    }

    @Test
    public void shouldNotAuthorizeIfUserIsNotDefinedInApprovalList() throws Exception {
        CONFIG_HELPER.addSecurityWithAdminConfig();
        StageConfig stage = StageConfigMother.custom("ft", authConfigWithUserJez);
        PipelineConfig pipeline = CONFIG_HELPER.addStageToPipeline(PIPELINE_NAME, stage);

        assertThat("User hacker should not have permission on ft stage",
                securityService.hasOperatePermissionForStage(CaseInsensitiveString.str(pipeline.name()), CaseInsensitiveString.str(stage.name()), "hacker"), is(false));
    }

    @Test
    public void shouldAuthorizeIfSecurityIsTurnedOff() throws Exception {
        StageConfig stage = StageConfigMother.custom("ft", authConfigWithUserJez);
        PipelineConfig pipeline = CONFIG_HELPER.addStageToPipeline(PIPELINE_NAME, stage);

        assertThat("User hacker should have permission on ft stage since security is turned off",
                securityService.hasOperatePermissionForStage(CaseInsensitiveString.str(pipeline.name()), CaseInsensitiveString.str(stage.name()), "hacker"), is(true));
    }

    @Test
    public void shouldAuthorizeAnyUserIfNoAuthorizationDefinedForAutoApproval() throws Exception {
        CONFIG_HELPER.addSecurityWithAdminConfig();
        StageConfig stage = StageConfigMother.custom("ft", Approval.automaticApproval());
        PipelineConfig pipeline = CONFIG_HELPER.addStageToPipeline(PIPELINE_NAME, stage);

        assertThat("Cruise should not have permission on ft stage unless configured with permission",
                securityService.hasOperatePermissionForStage(CaseInsensitiveString.str(pipeline.name()), CaseInsensitiveString.str(stage.name()), "cruise"), is(true));
    }

    @Test
    public void shouldAuthorizeUserCruiseIfUserIsAuthorisedToOperateAutoStage() throws Exception {
        CONFIG_HELPER.addSecurityWithAdminConfig();
        CONFIG_HELPER.setOperatePermissionForStage(PIPELINE_NAME, STAGE_NAME, "cruise");
        StageConfig stage = StageConfigMother.custom("ft", new Approval(new AuthConfig(new AdminUser(new CaseInsensitiveString("cruise")))));
        PipelineConfig pipeline = CONFIG_HELPER.addStageToPipeline(PIPELINE_NAME, stage);
        assertThat(securityService.hasOperatePermissionForStage(CaseInsensitiveString.str(pipeline.name()), CaseInsensitiveString.str(stage.name()), "cruise"), is(true));
        assertThat(securityService.hasOperatePermissionForStage(PIPELINE_NAME, STAGE_NAME, "anyone"), is(false));        
    }

}
