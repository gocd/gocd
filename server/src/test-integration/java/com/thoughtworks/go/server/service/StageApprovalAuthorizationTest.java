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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.helper.StageConfigMother;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static com.thoughtworks.go.config.PipelineConfigs.DEFAULT_GROUP;
import static org.assertj.core.api.Assertions.assertThat;
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class StageApprovalAuthorizationTest {
    private static final GoConfigFileHelper CONFIG_HELPER = new GoConfigFileHelper();
    private static final String PIPELINE_NAME = "cruise";

    private AuthConfig authConfigWithUserJez = new AuthConfig(new AdminUser(new CaseInsensitiveString("jez")));
    private AuthConfig authConfigWithAdminRole = new AuthConfig(new AdminRole(new CaseInsensitiveString("adminRole")));

    @Autowired private GoConfigDao goConfigDao;
    @Autowired private SecurityService securityService;
    private static final String STAGE_NAME = "dev";

    @BeforeEach
    public void setUp() throws Exception {
        CONFIG_HELPER.usingCruiseConfigDao(goConfigDao);
        CONFIG_HELPER.onSetUp();
        CONFIG_HELPER.addPipeline(PIPELINE_NAME, STAGE_NAME);
    }

    @AfterEach
    public void tearDown() {
        CONFIG_HELPER.onTearDown();
    }

    @Test
    public void shouldAuthorizeIfUserIsInApprovalList() {
        CONFIG_HELPER.addSecurityWithAdminConfig();
        StageConfig stage = StageConfigMother.custom("ft", authConfigWithUserJez);
        PipelineConfig pipeline = CONFIG_HELPER.addStageToPipeline(PIPELINE_NAME, stage);

        assertThat(securityService.hasOperatePermissionForStage(CaseInsensitiveString.str(pipeline.name()), CaseInsensitiveString.str(stage.name()), "jez")).isTrue();
    }

    @Test
    public void shouldAuthorizeIfRoleIsInApprovalList() {
        CONFIG_HELPER.addSecurityWithAdminConfig();
        CONFIG_HELPER.addRole(new RoleConfig(new CaseInsensitiveString("adminRole"), new RoleUser(new CaseInsensitiveString("tester"))));

        StageConfig stage = StageConfigMother.custom("test", authConfigWithAdminRole);
        PipelineConfig pipeline = CONFIG_HELPER.addStageToPipeline(PIPELINE_NAME, stage);

        assertThat(securityService.hasOperatePermissionForStage(CaseInsensitiveString.str(pipeline.name()), CaseInsensitiveString.str(stage.name()), "tester")).isTrue();

    }

    @Test
    public void shouldUsePipelineGroupAuthorizationIfNoStageAuthorizationDefined() {
        CONFIG_HELPER.addSecurityWithAdminConfig();
        CONFIG_HELPER.setOperatePermissionForGroup(DEFAULT_GROUP, "user1");

        assertThat(securityService.hasOperatePermissionForStage(PIPELINE_NAME, STAGE_NAME, "user1")).isTrue();
        assertThat(securityService.hasOperatePermissionForStage(PIPELINE_NAME, STAGE_NAME, "anyone")).isFalse();
    }

    @Test
    public void stageAuthorizationShouldOverrideGroupAuthorization() {
        CONFIG_HELPER.addSecurityWithAdminConfig();
        CONFIG_HELPER.setOperatePermissionForGroup(DEFAULT_GROUP, "user1", "jez");
        CONFIG_HELPER.setOperatePermissionForStage(PIPELINE_NAME, STAGE_NAME, "jez");

        assertThat(securityService.hasOperatePermissionForStage(PIPELINE_NAME, STAGE_NAME, "jez")).isTrue();
        assertThat(securityService.hasOperatePermissionForStage(PIPELINE_NAME, STAGE_NAME, "user1")).isFalse();
    }

    @Test
    public void shouldUseStageAuthorizationForFirstStage() {
        CONFIG_HELPER.addSecurityWithAdminConfig();
        CONFIG_HELPER.setOperatePermissionForStage(PIPELINE_NAME, STAGE_NAME, "jez");

        assertThat(securityService.hasOperatePermissionForFirstStage(PIPELINE_NAME, "jez")).isTrue();
        assertThat(securityService.hasOperatePermissionForFirstStage(PIPELINE_NAME, "anyone")).isFalse();
    }

    @Test
    public void shouldNotAuthorizeIfUserIsNotDefinedInApprovalList() {
        CONFIG_HELPER.addSecurityWithAdminConfig();
        StageConfig stage = StageConfigMother.custom("ft", authConfigWithUserJez);
        PipelineConfig pipeline = CONFIG_HELPER.addStageToPipeline(PIPELINE_NAME, stage);

        assertThat(securityService.hasOperatePermissionForStage(CaseInsensitiveString.str(pipeline.name()), CaseInsensitiveString.str(stage.name()), "hacker")).isFalse();
    }

    @Test
    public void shouldAuthorizeIfSecurityIsTurnedOff() {
        StageConfig stage = StageConfigMother.custom("ft", authConfigWithUserJez);
        PipelineConfig pipeline = CONFIG_HELPER.addStageToPipeline(PIPELINE_NAME, stage);

        assertThat(securityService.hasOperatePermissionForStage(CaseInsensitiveString.str(pipeline.name()), CaseInsensitiveString.str(stage.name()), "hacker")).isTrue();
    }

    @Test
    public void shouldAuthorizeUserCruiseIfUserIsAuthorisedToOperateAutoStage() {
        CONFIG_HELPER.addSecurityWithAdminConfig();
        CONFIG_HELPER.setOperatePermissionForStage(PIPELINE_NAME, STAGE_NAME, "cruise");
        StageConfig stage = StageConfigMother.custom("ft", new Approval(new AuthConfig(new AdminUser(new CaseInsensitiveString("cruise")))));
        PipelineConfig pipeline = CONFIG_HELPER.addStageToPipeline(PIPELINE_NAME, stage);
        assertThat(securityService.hasOperatePermissionForStage(CaseInsensitiveString.str(pipeline.name()), CaseInsensitiveString.str(stage.name()), "cruise")).isTrue();
        assertThat(securityService.hasOperatePermissionForStage(PIPELINE_NAME, STAGE_NAME, "anyone")).isFalse();
    }
}
