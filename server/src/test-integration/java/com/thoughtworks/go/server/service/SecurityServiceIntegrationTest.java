/*
 * Copyright 2024 Thoughtworks, Inc.
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
import com.thoughtworks.go.domain.SecureSiteUrl;
import com.thoughtworks.go.domain.SiteUrl;
import com.thoughtworks.go.helper.ConfigFileFixture;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.function.Consumer;

import static com.thoughtworks.go.util.SystemEnvironment.ALLOW_EVERYONE_TO_VIEW_OPERATE_GROUPS_WITH_NO_GROUP_AUTHORIZATION_SETUP;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
    "classpath:/applicationContext-global.xml",
    "classpath:/applicationContext-dataLocalAccess.xml",
    "classpath:/testPropertyConfigurer.xml",
    "classpath:/spring-all-servlet.xml",
})
public class SecurityServiceIntegrationTest {
    private static final String GROUP_NAME = "group";
    private static final String PIPELINE_NAME = "cruise";
    private static final String STAGE_NAME = "dev";
    private static final String JOB_NAME = "job";
    private static final String ADMIN = "admin1";
    private static final String PIPELINE_ADMIN = "pipelineAdmin";
    private static final String VIEWER = "viewer";
    private static final String OPERATOR = "operator";
    private static final String HACKER = "hacker";

    @Autowired private GoConfigDao goConfigDao;
    @Autowired private SecurityService securityService;
    @Autowired private SystemEnvironment systemEnvironment;
    @Autowired private DatabaseAccessHelper dbHelper;
    private GoConfigFileHelper configHelper;

    @BeforeEach
    public void setUp() throws Exception {
        configHelper = new GoConfigFileHelper();
        configHelper.usingCruiseConfigDao(goConfigDao);
        configHelper.onSetUp();
        configHelper.addPipelineWithGroup(GROUP_NAME, PIPELINE_NAME, STAGE_NAME, JOB_NAME);
        configHelper.addSecurityWithAdminConfig();
        dbHelper.onSetUp();
    }

    @AfterEach
    public void tearDown() throws Exception {
        configHelper.onTearDown();
        dbHelper.onTearDown();
    }

    @Test
    public void shouldReturnTrueIfUserHasViewPermission() {
        configHelper.setViewPermissionForGroup(GROUP_NAME, VIEWER);
        assertThat(securityService.hasViewPermissionForGroup(VIEWER, GROUP_NAME)).isTrue();
    }

    @Test
    public void userShouldNotHaveViewPermissionToGroupWithNoAuth_WhenDefaultPermissionIsToDeny() {
        withDefaultGroupPermission(false, (o) -> assertThat(securityService.hasViewPermissionForGroup(VIEWER, GROUP_NAME)).isEqualTo(false));
    }

    @Test
    public void userShouldHaveViewPermissionToGroupWithNoAuth_WhenDefaultPermissionIsToAllow() {
        withDefaultGroupPermission(true, (o) -> assertThat(securityService.hasViewPermissionForGroup(VIEWER, GROUP_NAME)).isEqualTo(true));
    }

    @Test
    public void userShouldHavePermissionIfAGroupAdmin() {
        configHelper.setAdminPermissionForGroup(GROUP_NAME, PIPELINE_ADMIN);
        assertThat(securityService.hasViewPermissionForGroup(PIPELINE_ADMIN, GROUP_NAME)).isTrue();
        assertThat(securityService.hasOperatePermissionForGroup(new CaseInsensitiveString(PIPELINE_ADMIN), GROUP_NAME)).isTrue();
        assertThat(securityService.hasOperatePermissionForPipeline(new CaseInsensitiveString(PIPELINE_ADMIN), PIPELINE_NAME)).isTrue();
        assertThat(securityService.hasViewPermissionForPipeline(Username.valueOf(PIPELINE_ADMIN), PIPELINE_NAME)).isTrue();
        assertThat(securityService.hasViewPermissionForPipeline(new Username(new CaseInsensitiveString(PIPELINE_ADMIN)), PIPELINE_NAME)).isTrue();
        assertThat(securityService.hasOperatePermissionForStage(PIPELINE_NAME, STAGE_NAME, PIPELINE_ADMIN)).isTrue();
    }

    @Test
    public void shouldNotGiveOperatePermissionsToAViewOnlyUser() {
        String viewOnly = "view_only";
        configHelper.setViewPermissionForGroup(GROUP_NAME, viewOnly);

        assertThat(securityService.hasViewPermissionForGroup(viewOnly, GROUP_NAME)).isTrue();
        assertThat(securityService.hasOperatePermissionForGroup(new CaseInsensitiveString(viewOnly), GROUP_NAME)).isFalse();
        assertThat(securityService.hasOperatePermissionForPipeline(new CaseInsensitiveString(viewOnly), PIPELINE_NAME)).isFalse();
        assertThat(securityService.hasViewPermissionForPipeline(Username.valueOf(viewOnly), PIPELINE_NAME)).isTrue();
        assertThat(securityService.hasOperatePermissionForStage(PIPELINE_NAME, STAGE_NAME, viewOnly)).isFalse();

        assertThat(securityService.hasViewPermissionForGroup(viewOnly, GROUP_NAME)).isTrue();
        assertThat(securityService.hasOperatePermissionForGroup(new CaseInsensitiveString(viewOnly), GROUP_NAME)).isFalse();
        assertThat(securityService.hasOperatePermissionForPipeline(new CaseInsensitiveString(viewOnly), PIPELINE_NAME)).isFalse();
        assertThat(securityService.hasViewPermissionForPipeline(Username.valueOf(viewOnly), PIPELINE_NAME)).isTrue();
        assertThat(securityService.hasOperatePermissionForStage(PIPELINE_NAME, STAGE_NAME, viewOnly)).isFalse();
    }

    @Test
    public void userShouldHavePermissionToOperateOnAStageIfAGroupAdmin() {
        configHelper.setAdminPermissionForGroup(GROUP_NAME, PIPELINE_ADMIN);
        configHelper.setOperatePermissionForGroup(GROUP_NAME, OPERATOR);
        configHelper.setOperatePermissionForStage(PIPELINE_NAME, STAGE_NAME, OPERATOR);
        assertThat(securityService.hasOperatePermissionForStage(PIPELINE_NAME, STAGE_NAME, PIPELINE_ADMIN)).isTrue();
    }

    @Test
    public void userShouldNotHaveOperatePermissionToGroupWithNoAuth_WhenDefaultPermissionIsToDeny() {
        withDefaultGroupPermission(false, (o) -> assertThat(securityService.hasOperatePermissionForGroup(new CaseInsensitiveString(OPERATOR), GROUP_NAME)).isEqualTo(false));
    }

    @Test
    public void userShouldHaveOperatePermissionToGroupWithNoAuth_WhenDefaultPermissionIsToAllow() {
        withDefaultGroupPermission(true, (o) -> assertThat(securityService.hasOperatePermissionForGroup(new CaseInsensitiveString(OPERATOR), GROUP_NAME)).isEqualTo(true));
    }

    @Test
    public void userShouldNotHaveOperatePermissionToStageInGroupWithNoAuth_WhenDefaultPermissionIsToDeny_WhenStageOperatePermissionsAreNotDefined() {
        withDefaultGroupPermission(false, (o) -> {
            assertThat(securityService.hasOperatePermissionForStage(PIPELINE_NAME, STAGE_NAME, VIEWER)).isFalse();
            assertThat(securityService.hasOperatePermissionForStage(PIPELINE_NAME, STAGE_NAME, OPERATOR)).isFalse();
        });
    }

    @Test
    public void userShouldHaveOperatePermissionToStageInGroupWithNoAuth_WhenDefaultPermissionIsToAllow_WhenStageOperatePermissionsAreNotDefined() {
        withDefaultGroupPermission(true, (o) -> {
            assertThat(securityService.hasOperatePermissionForStage(PIPELINE_NAME, STAGE_NAME, VIEWER)).isTrue();
            assertThat(securityService.hasOperatePermissionForStage(PIPELINE_NAME, STAGE_NAME, OPERATOR)).isTrue();
        });
    }

    @Test
    public void operatePermissionOfStageIsNotInfluencedByDefaultPermissions_ForGroupsWithNoAuthDefined_ButWithStageOperatePermissionsDefined() {
        withDefaultGroupPermission(true, (o) -> {
            configHelper.setOperatePermissionForStage(PIPELINE_NAME, STAGE_NAME, OPERATOR);

            assertThat(securityService.hasOperatePermissionForStage(PIPELINE_NAME, STAGE_NAME, VIEWER)).isFalse();
            assertThat(securityService.hasOperatePermissionForStage(PIPELINE_NAME, STAGE_NAME, OPERATOR)).isTrue();
        });

        withDefaultGroupPermission(false, (o) -> {
            configHelper.setOperatePermissionForStage(PIPELINE_NAME, STAGE_NAME, OPERATOR);

            assertThat(securityService.hasOperatePermissionForStage(PIPELINE_NAME, STAGE_NAME, VIEWER)).isFalse();
            assertThat(securityService.hasOperatePermissionForStage(PIPELINE_NAME, STAGE_NAME, OPERATOR)).isTrue();
        });
    }

    @Test
    public void shouldGiveTheGroupsModifiableByAdmin() {
        configHelper.addAdmins("admin");
        configHelper.addPipelineWithGroup("newGroup", "newPipeline", "newStage", "newJob");
        List<String> groups = securityService.modifiableGroupsForUser(new Username(new CaseInsensitiveString("admin")));
        assertThat(groups).contains(GROUP_NAME, "newGroup");
    }

    @Test
    public void shouldGiveTheGroupsModifiableByAGroupAdmin() {
        configHelper.addPipelineWithGroup("newGroup", "newPipeline", "newStage", "newJob");
        configHelper.setAdminPermissionForGroup("newGroup", "groupAdmin");
        List<String> groups = securityService.modifiableGroupsForUser(new Username(new CaseInsensitiveString("groupAdmin")));
        assertThat(groups).contains("newGroup");
    }

    @Test
    public void shouldGiveAllTheGroupsForAUserWhoIsAnAdminAndAGroupAdmin() {
        configHelper.addAdmins("admin");
        configHelper.addPipelineWithGroup("newGroup", "newPipeline", "newStage", "newJob");
        configHelper.setAdminPermissionForGroup("newGroup", "admin");
        List<String> groups = securityService.modifiableGroupsForUser(new Username(new CaseInsensitiveString("admin")));
        assertThat(groups).contains(GROUP_NAME, "newGroup");
    }

    @Test
    public void shouldNotGiveAnyGroupsForAUserWhoIsNotAnAdminOrAGroupAdmin() {
        configHelper.addAdmins("admin");
        configHelper.addPipelineWithGroup("newGroup", "newPipeline", "newStage", "newJob");
        configHelper.setAdminPermissionForGroup("newGroup", "groupAdmin");
        List<String> groups = securityService.modifiableGroupsForUser(new Username(new CaseInsensitiveString("loser")));
        assertThat(groups.isEmpty()).isTrue();
    }

    @Test
    public void shouldReturnTrueIfUserHasViewOrOperatePermissionForPipeline() {
        configHelper.setViewPermissionForGroup(GROUP_NAME, VIEWER);
        assertThat(securityService.hasViewOrOperatePermissionForPipeline(new Username(new CaseInsensitiveString(VIEWER)), PIPELINE_NAME)).isTrue();

        configHelper.setOperatePermissionForGroup(GROUP_NAME, OPERATOR);
        assertThat(securityService.hasViewOrOperatePermissionForPipeline(new Username(new CaseInsensitiveString(OPERATOR)), PIPELINE_NAME)).isTrue();
    }

    @Test
    public void shouldReturnFalseIfUserDoesNotHaveViewPermission() {
        configHelper.setViewPermissionForGroup(GROUP_NAME, VIEWER);
        assertThat(securityService.hasViewPermissionForGroup(HACKER, GROUP_NAME)).isFalse();
    }

    @Test
    public void shouldNotCheckViewPermissionIfPipelineDoesNotExist() {
        assertThat(securityService.hasViewPermissionForPipeline(Username.valueOf(VIEWER), "noSuchAPipeline")).isTrue();
    }

    @Test
    public void shouldNotCheckOperatePermissionIfPipelineDoesNotExist() {
        assertThat(securityService.hasOperatePermissionForPipeline(new CaseInsensitiveString(OPERATOR), "noSuchAPipeline")).isTrue();
    }

    @Test
    public void shouldNotCheckAdminPermissionsIfPipelineDoesNotExist() {
        assertThat(securityService.hasAdminPermissionsForPipeline(new Username(ADMIN), new CaseInsensitiveString("non-existent-pipeline"))).isTrue();
    }

    @Test
    public void shouldReturnTrueForSuperAdminIfPipelineExists() {
        assertThat(securityService.hasAdminPermissionsForPipeline(new Username(ADMIN), new CaseInsensitiveString(PIPELINE_NAME))).isTrue();
    }

    @Test
    public void shouldReturnTrueForGroupAdminForPipeline() {
        configHelper.setAdminPermissionForGroup(GROUP_NAME, PIPELINE_ADMIN);
        assertThat(securityService.hasAdminPermissionsForPipeline(new Username(PIPELINE_ADMIN), new CaseInsensitiveString(PIPELINE_NAME))).isTrue();
    }

    @Test
    public void shouldReturnFalseForNonAdminUserForPipeline() {
        assertThat(securityService.hasAdminPermissionsForPipeline(new Username(VIEWER), new CaseInsensitiveString(PIPELINE_NAME))).isFalse();
    }

    @Test
    public void shouldNotCheckViewPermissionIfSecurityIsTurnedOff() {
        configHelper.turnOffSecurity();
        configHelper.setViewPermissionForGroup(GROUP_NAME, VIEWER);
        assertThat(securityService.hasViewPermissionForGroup(HACKER, GROUP_NAME)).isTrue();
    }

    @Test
    public void shouldNotCheckOperationPermissionIfSecurityIsTurnedOff() {
        configHelper.turnOffSecurity();
        configHelper.setOperatePermissionForGroup(GROUP_NAME, OPERATOR);
        assertThat(securityService.hasOperatePermissionForGroup(new CaseInsensitiveString(HACKER), GROUP_NAME)).isTrue();
    }

    @Test
    public void shouldReturnTrueIfUserHasOperatePermission() {
        configHelper.setOperatePermissionForGroup(GROUP_NAME, OPERATOR);
        assertThat(securityService.hasOperatePermissionForGroup(new CaseInsensitiveString(OPERATOR), GROUP_NAME)).isTrue();
    }

    @Test
    public void shouldGrantAdminViewPermissionForAllGroups() {
        configHelper.setViewPermissionForGroup(GROUP_NAME, VIEWER);
        assertThat(securityService.hasViewPermissionForGroup(ADMIN, GROUP_NAME)).isTrue();
    }

    @Test
    public void shouldGrantAdminOperatePermissionForAllGroups() {
        configHelper.setOperatePermissionForGroup(GROUP_NAME, OPERATOR);
        configHelper.addAuthorizedUserForStage(PIPELINE_NAME, STAGE_NAME, OPERATOR);
        assertThat(securityService.hasOperatePermissionForGroup(new CaseInsensitiveString(ADMIN), GROUP_NAME)).isTrue();
    }

    @Test
    public void isUserAdminOfGroup_shouldBeTrueForASuperAdmin() {
        configHelper.addAdmins("root");
        assertThat(securityService.isUserAdminOfGroup(new CaseInsensitiveString("root"), GROUP_NAME)).isTrue();
    }

    @Test
    public void isUserAdminOfGroup_shouldBeTrueForAGroupAdmin() {
        configHelper.setAdminPermissionForGroup(GROUP_NAME, OPERATOR);
        assertThat(securityService.isUserAdminOfGroup(new CaseInsensitiveString(OPERATOR), GROUP_NAME)).isTrue();
    }

    @Test
    public void isUserAdminOfGroup_shouldBeFalseForANonGroupAdmin() {
        assertThat(securityService.isUserAdminOfGroup(new CaseInsensitiveString("some-unauthorized-user"), GROUP_NAME)).isFalse();
    }

    @Test
    public void shouldGrantAdminOperatePermissionForAllStages() {
        configHelper.setOperatePermissionForGroup(GROUP_NAME, OPERATOR);
        configHelper.addAuthorizedUserForStage(PIPELINE_NAME, STAGE_NAME, OPERATOR);
        assertThat(securityService.hasOperatePermissionForStage(PIPELINE_NAME, STAGE_NAME, ADMIN)).isTrue();
    }

    @Test
    public void shouldReturnAllPipelinesThatUserHasViewPermissionsFor() throws Exception {
        configHelper.onTearDown();
        configHelper.saveFullConfig(CONFIG_WITH_2_GROUPS, true);
        assertThat(securityService.viewablePipelinesFor(new Username(new CaseInsensitiveString("blah"))).size()).isEqualTo(0);
        assertThat(securityService.viewablePipelinesFor(new Username(new CaseInsensitiveString("admin")))).isEqualTo(List.of(new CaseInsensitiveString("pipeline1"), new CaseInsensitiveString("pipeline2")));
        assertThat(securityService.viewablePipelinesFor(new Username(new CaseInsensitiveString("pavan")))).isEqualTo(List.of(new CaseInsensitiveString("pipeline3")));
    }

    @Test
    public void shouldReturnAllPipelinesWithNoSecurity() throws Exception {
        configHelper.onTearDown();
        configHelper.saveFullConfig(ConfigFileFixture.multipleMaterial("<hg url='http://localhost'/>"), true);
        assertThat(securityService.viewablePipelinesFor(new Username(new CaseInsensitiveString("admin")))).isEqualTo(List.of(new CaseInsensitiveString("ecl"), new CaseInsensitiveString("ec2"), new CaseInsensitiveString("framework")));
        assertThat(securityService.viewablePipelinesFor(Username.ANONYMOUS)).isEqualTo(List.of(new CaseInsensitiveString("ecl"), new CaseInsensitiveString("ec2"), new CaseInsensitiveString("framework")));
    }

    @Test
    public void shouldNotHaveOperatePermissionsOnStagesThatDoNotExist() {
        assertThat(securityService.hasOperatePermissionForStage(PIPELINE_NAME, "doesnt-exist", "raghu")).isFalse();
    }

    @Test
    public void shouldNotHaveOperatePermissionsOnPipelinesThatDoNotExist() {
        assertThat(securityService.hasOperatePermissionForStage("doesnt-exists", "doesnt-exist", "raghu")).isFalse();
    }

    @Test
    public void shouldReturnSiteUrlAsCasServiceBaseUrlIfOnlySiteUrlIsDefined() {
        configHelper.setBaseUrls(new SiteUrl("http://example.com"), new SecureSiteUrl());
        assertThat(securityService.casServiceBaseUrl()).isEqualTo("http://example.com");
    }

    @Test
    public void shouldReturnSecureSiteUrlAsCasServiceBaseUrlIfBothSiteUrlAndSecureSiteUrlAreDefined() {
        configHelper.setBaseUrls(new SiteUrl("http://example.com"), new SecureSiteUrl("https://example.com"));
        assertThat(securityService.casServiceBaseUrl()).isEqualTo("https://example.com");
    }

    @Test
    public void shouldReturnTrueIfUserIsAuthorizedToViewAnyOfTheTemplatesListed() {
        String templateAdmin = "template-admin-1";
        Authorization authorization = new Authorization(new AdminsConfig(new AdminUser(new CaseInsensitiveString(templateAdmin))));
        configHelper.addTemplate("pipeline-name", authorization, "stage-name");

        boolean isAuthorized = securityService.isAuthorizedToViewAndEditTemplates(new Username(new CaseInsensitiveString(templateAdmin)));

        assertThat(isAuthorized).isTrue();
    }

    @Test
    public void shouldReturnTrueIfUserIsTemplateAdminAndCanEditTemplate() {
        String templateAdmin = "template-admin-1";
        Authorization authorization = new Authorization(new AdminsConfig(new AdminUser(new CaseInsensitiveString(templateAdmin))));
        CaseInsensitiveString templateName = new CaseInsensitiveString("pipeline-name");
        configHelper.addTemplate("pipeline-name", authorization, "stage-name");

        boolean isAuthorized = securityService.isAuthorizedToEditTemplate(templateName, new Username(new CaseInsensitiveString(templateAdmin)));

        assertThat(isAuthorized).isTrue();
    }

    @Test
    public void shouldReturnFalseIfUserIsTemplateAdminAndCannotEditTemplate() {
        String templateAdmin = "template-admin-1";
        String templateAdminNotForThisTemplate = "template-admin-2";
        Authorization authorization = new Authorization(new AdminsConfig(new AdminUser(new CaseInsensitiveString(templateAdmin))));
        CaseInsensitiveString templateName = new CaseInsensitiveString("pipeline-name");
        configHelper.addTemplate("pipeline-name", authorization, "stage-name");

        boolean isAuthorized = securityService.isAuthorizedToEditTemplate(templateName, new Username(new CaseInsensitiveString(templateAdminNotForThisTemplate)));

        assertThat(isAuthorized).isFalse();
    }

    private void withDefaultGroupPermission(boolean defaultAllow, Consumer<Object> body) {
        boolean previousValue = systemEnvironment.get(ALLOW_EVERYONE_TO_VIEW_OPERATE_GROUPS_WITH_NO_GROUP_AUTHORIZATION_SETUP);
        try {
            systemEnvironment.set(ALLOW_EVERYONE_TO_VIEW_OPERATE_GROUPS_WITH_NO_GROUP_AUTHORIZATION_SETUP, defaultAllow);
            body.accept(null);
        } finally {
            systemEnvironment.set(ALLOW_EVERYONE_TO_VIEW_OPERATE_GROUPS_WITH_NO_GROUP_AUTHORIZATION_SETUP, previousValue);
        }
    }

    private static final String CONFIG_WITH_2_GROUPS = """
            <cruise schemaVersion='16'>
            <server artifactsdir='artifacts' ><license user="Cruise team internal">E+2WI6OuZ6hQ9wnNZGaiIQzGaLerbJR73qC+4OXlTDhC3Vafq8phXVPjFzUWXzpeBjcyytmQetqKG0TCKSoOhlDKdVrO982jHv7Gal6fz1kD0KbKoNnWo9vwqTEXndOfr+qoVr9KydLtyC3WdpDyjw7fPTBmB/eZmaTHKvZvJHHeYbKsvX8kZPYwhQT6oxbzwylqOPhJAiq6EKxS2S0jk1h0Uy5c07IiE4+y8PmwoElnfl3kpAARHMv40vfxamttp6IljBCuJ2fXQ0rXuukA/jIkv1i78A6dqL0Ii3RAIjRvglVHeI9HT9a0SyOR0eUMorFJJPDoqUnb1TVu/Ij3EQ==</license>
                <security>
             <passwordFile path="/home/cruise/admins.properties"/>
                  <roles>
                    <role name="simple" >
                       <user>admin</user>
                       <user>pavan</user>
                    </role>
                  </roles>
                  <admins>
                       <user>raghu</user>
                       <user>jumble</user>
                  </admins>
                </security>
            </server>
            <pipelines group="first">
             <authorization>
                    <view>
                       <user>admin</user>
                    </view>
                </authorization><pipeline name='pipeline1'>
                <materials>
                  <svn url ="svnurl"/>
                </materials>
              <stage name='mingle'>
                <jobs>
                  <job name='cardlist'><tasks><exec command='echo'><runif status='passed' /></exec></tasks></job>
                </jobs>
              </stage>
            </pipeline>
            <pipeline name='pipeline2'>
                <materials>
                  <svn url ="svnurl"/>
                </materials>
              <stage name='mingle'>
                <jobs>
                  <job name='cardlist'><tasks><exec command='echo'><runif status='passed' /></exec></tasks></job>
                </jobs>
              </stage>
            </pipeline>
            </pipelines>
            <pipelines group='second'>
             <authorization>
                    <view>
                       <user>pavan</user>
                    </view>
                </authorization><pipeline name='pipeline3'>
                <materials>
                  <svn url ="svnurl"/>
                </materials>
              <stage name='mingle'>
                <jobs>
                  <job name='cardlist'><tasks><exec command='echo'><runif status='passed' /></exec></tasks></job>
                </jobs>
              </stage>
            </pipeline>
            </pipelines>
            </cruise>""";
}
