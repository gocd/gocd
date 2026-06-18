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

import static com.thoughtworks.go.config.CaseInsensitiveString.cis;
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
    public void userShouldNotHaveViewPermissionToGroupWithNoAuth() {
        assertThat(securityService.hasViewPermissionForGroup(VIEWER, GROUP_NAME)).isEqualTo(false);
    }

    @Test
    public void userShouldHavePermissionIfAGroupAdmin() {
        configHelper.setAdminPermissionForGroup(GROUP_NAME, PIPELINE_ADMIN);
        assertThat(securityService.hasViewPermissionForGroup(PIPELINE_ADMIN, GROUP_NAME)).isTrue();
        assertThat(securityService.hasOperatePermissionForGroup(cis(PIPELINE_ADMIN), GROUP_NAME)).isTrue();
        assertThat(securityService.hasOperatePermissionForPipeline(cis(PIPELINE_ADMIN), PIPELINE_NAME)).isTrue();
        assertThat(securityService.hasViewPermissionForPipeline(Username.valueOf(PIPELINE_ADMIN), PIPELINE_NAME)).isTrue();
        assertThat(securityService.hasViewPermissionForPipeline(new Username(cis(PIPELINE_ADMIN)), PIPELINE_NAME)).isTrue();
        assertThat(securityService.hasOperatePermissionForStage(PIPELINE_NAME, STAGE_NAME, PIPELINE_ADMIN)).isTrue();
    }

    @Test
    public void shouldNotGiveOperatePermissionsToAViewOnlyUser() {
        String viewOnly = "view_only";
        configHelper.setViewPermissionForGroup(GROUP_NAME, viewOnly);

        assertThat(securityService.hasViewPermissionForGroup(viewOnly, GROUP_NAME)).isTrue();
        assertThat(securityService.hasOperatePermissionForGroup(cis(viewOnly), GROUP_NAME)).isFalse();
        assertThat(securityService.hasOperatePermissionForPipeline(cis(viewOnly), PIPELINE_NAME)).isFalse();
        assertThat(securityService.hasViewPermissionForPipeline(Username.valueOf(viewOnly), PIPELINE_NAME)).isTrue();
        assertThat(securityService.hasOperatePermissionForStage(PIPELINE_NAME, STAGE_NAME, viewOnly)).isFalse();

        assertThat(securityService.hasViewPermissionForGroup(viewOnly, GROUP_NAME)).isTrue();
        assertThat(securityService.hasOperatePermissionForGroup(cis(viewOnly), GROUP_NAME)).isFalse();
        assertThat(securityService.hasOperatePermissionForPipeline(cis(viewOnly), PIPELINE_NAME)).isFalse();
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
    public void userShouldNotHaveOperatePermissionToGroupWithNoAuth() {
        assertThat(securityService.hasOperatePermissionForGroup(cis(OPERATOR), GROUP_NAME)).isEqualTo(false);
    }

    @Test
    public void userShouldNotHaveOperatePermissionToStageInGroupWithNoAuth_WhenStageOperatePermissionsAreNotDefined() {
        assertThat(securityService.hasOperatePermissionForStage(PIPELINE_NAME, STAGE_NAME, VIEWER)).isFalse();
        assertThat(securityService.hasOperatePermissionForStage(PIPELINE_NAME, STAGE_NAME, OPERATOR)).isFalse();
    }

    @Test
    public void operatePermissionOfStageIsNotInfluencedByDefaultPermissions_ForGroupsWithNoAuthDefined_ButWithStageOperatePermissionsDefined() {
        configHelper.setOperatePermissionForStage(PIPELINE_NAME, STAGE_NAME, OPERATOR);

        assertThat(securityService.hasOperatePermissionForStage(PIPELINE_NAME, STAGE_NAME, VIEWER)).isFalse();
        assertThat(securityService.hasOperatePermissionForStage(PIPELINE_NAME, STAGE_NAME, OPERATOR)).isTrue();
    }

    @Test
    public void shouldGiveTheGroupsModifiableByAdmin() {
        configHelper.addAdmins("admin");
        configHelper.addPipelineWithGroup("newGroup", "newPipeline", "newStage", "newJob");
        List<String> groups = securityService.modifiableGroupsForUser(new Username(cis("admin")));
        assertThat(groups).contains(GROUP_NAME, "newGroup");
    }

    @Test
    public void shouldGiveTheGroupsModifiableByAGroupAdmin() {
        configHelper.addPipelineWithGroup("newGroup", "newPipeline", "newStage", "newJob");
        configHelper.setAdminPermissionForGroup("newGroup", "groupAdmin");
        List<String> groups = securityService.modifiableGroupsForUser(new Username(cis("groupAdmin")));
        assertThat(groups).contains("newGroup");
    }

    @Test
    public void shouldGiveAllTheGroupsForAUserWhoIsAnAdminAndAGroupAdmin() {
        configHelper.addAdmins("admin");
        configHelper.addPipelineWithGroup("newGroup", "newPipeline", "newStage", "newJob");
        configHelper.setAdminPermissionForGroup("newGroup", "admin");
        List<String> groups = securityService.modifiableGroupsForUser(new Username(cis("admin")));
        assertThat(groups).contains(GROUP_NAME, "newGroup");
    }

    @Test
    public void shouldNotGiveAnyGroupsForAUserWhoIsNotAnAdminOrAGroupAdmin() {
        configHelper.addAdmins("admin");
        configHelper.addPipelineWithGroup("newGroup", "newPipeline", "newStage", "newJob");
        configHelper.setAdminPermissionForGroup("newGroup", "groupAdmin");
        List<String> groups = securityService.modifiableGroupsForUser(new Username(cis("loser")));
        assertThat(groups.isEmpty()).isTrue();
    }

    @Test
    public void shouldReturnTrueIfUserHasViewOrOperatePermissionForPipeline() {
        configHelper.setViewPermissionForGroup(GROUP_NAME, VIEWER);
        assertThat(securityService.hasViewOrOperatePermissionForPipeline(new Username(cis(VIEWER)), PIPELINE_NAME)).isTrue();

        configHelper.setOperatePermissionForGroup(GROUP_NAME, OPERATOR);
        assertThat(securityService.hasViewOrOperatePermissionForPipeline(new Username(cis(OPERATOR)), PIPELINE_NAME)).isTrue();
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
        assertThat(securityService.hasOperatePermissionForPipeline(cis(OPERATOR), "noSuchAPipeline")).isTrue();
    }

    @Test
    public void shouldNotCheckAdminPermissionsIfPipelineDoesNotExist() {
        assertThat(securityService.hasAdminPermissionsForPipeline(new Username(ADMIN), cis("non-existent-pipeline"))).isTrue();
    }

    @Test
    public void shouldReturnTrueForSuperAdminIfPipelineExists() {
        assertThat(securityService.hasAdminPermissionsForPipeline(new Username(ADMIN), cis(PIPELINE_NAME))).isTrue();
    }

    @Test
    public void shouldReturnTrueForGroupAdminForPipeline() {
        configHelper.setAdminPermissionForGroup(GROUP_NAME, PIPELINE_ADMIN);
        assertThat(securityService.hasAdminPermissionsForPipeline(new Username(PIPELINE_ADMIN), cis(PIPELINE_NAME))).isTrue();
    }

    @Test
    public void shouldReturnFalseForNonAdminUserForPipeline() {
        assertThat(securityService.hasAdminPermissionsForPipeline(new Username(VIEWER), cis(PIPELINE_NAME))).isFalse();
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
        assertThat(securityService.hasOperatePermissionForGroup(cis(HACKER), GROUP_NAME)).isTrue();
    }

    @Test
    public void shouldReturnTrueIfUserHasOperatePermission() {
        configHelper.setOperatePermissionForGroup(GROUP_NAME, OPERATOR);
        assertThat(securityService.hasOperatePermissionForGroup(cis(OPERATOR), GROUP_NAME)).isTrue();
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
        assertThat(securityService.hasOperatePermissionForGroup(cis(ADMIN), GROUP_NAME)).isTrue();
    }

    @Test
    public void isUserAdminOfGroup_shouldBeTrueForASuperAdmin() {
        configHelper.addAdmins("root");
        assertThat(securityService.isUserAdminOfGroup(cis("root"), GROUP_NAME)).isTrue();
    }

    @Test
    public void isUserAdminOfGroup_shouldBeTrueForAGroupAdmin() {
        configHelper.setAdminPermissionForGroup(GROUP_NAME, OPERATOR);
        assertThat(securityService.isUserAdminOfGroup(cis(OPERATOR), GROUP_NAME)).isTrue();
    }

    @Test
    public void isUserAdminOfGroup_shouldBeFalseForANonGroupAdmin() {
        assertThat(securityService.isUserAdminOfGroup(cis("some-unauthorized-user"), GROUP_NAME)).isFalse();
    }

    @Test
    public void shouldGrantAdminOperatePermissionForAllStages() {
        configHelper.setOperatePermissionForGroup(GROUP_NAME, OPERATOR);
        configHelper.addAuthorizedUserForStage(PIPELINE_NAME, STAGE_NAME, OPERATOR);
        assertThat(securityService.hasOperatePermissionForStage(PIPELINE_NAME, STAGE_NAME, ADMIN)).isTrue();
    }

    @Test
    public void shouldReturnAllPipelinesThatUserHasViewPermissionsFor() throws Exception {
        configHelper.saveFullConfig(CONFIG_WITH_2_GROUPS, true);
        assertThat(securityService.viewablePipelinesFor(new Username(cis("blah"))).size()).isEqualTo(0);
        assertThat(securityService.viewablePipelinesFor(new Username(cis("admin")))).isEqualTo(List.of(cis("pipeline1"), cis("pipeline2")));
        assertThat(securityService.viewablePipelinesFor(new Username(cis("pavan")))).isEqualTo(List.of(cis("pipeline3")));
    }

    @Test
    public void shouldReturnAllPipelinesWithNoSecurity() throws Exception {
        configHelper.saveFullConfig(ConfigFileFixture.multipleMaterial("<hg url='http://localhost'/>"), true);
        assertThat(securityService.viewablePipelinesFor(new Username(cis("admin")))).isEqualTo(List.of(cis("ecl"), cis("ec2"), cis("framework")));
        assertThat(securityService.viewablePipelinesFor(Username.ANONYMOUS)).isEqualTo(List.of(cis("ecl"), cis("ec2"), cis("framework")));
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
    public void shouldReturnTrueIfUserIsAuthorizedToViewAnyOfTheTemplatesListed() {
        String templateAdmin = "template-admin-1";
        Authorization authorization = new Authorization(new AdminsConfig(new AdminUser(cis(templateAdmin))));
        configHelper.addTemplate("pipeline-name", authorization, "stage-name");

        boolean isAuthorized = securityService.isAuthorizedToEditTemplates(new Username(cis(templateAdmin)));

        assertThat(isAuthorized).isTrue();
    }

    @Test
    public void shouldReturnTrueIfUserIsTemplateAdminAndCanEditTemplate() {
        String templateAdmin = "template-admin-1";
        Authorization authorization = new Authorization(new AdminsConfig(new AdminUser(cis(templateAdmin))));
        CaseInsensitiveString templateName = cis("pipeline-name");
        configHelper.addTemplate("pipeline-name", authorization, "stage-name");

        boolean isAuthorized = securityService.isAuthorizedToEditTemplate(templateName, new Username(cis(templateAdmin)));

        assertThat(isAuthorized).isTrue();
    }

    @Test
    public void shouldReturnFalseIfUserIsTemplateAdminAndCannotEditTemplate() {
        String templateAdmin = "template-admin-1";
        String templateAdminNotForThisTemplate = "template-admin-2";
        Authorization authorization = new Authorization(new AdminsConfig(new AdminUser(cis(templateAdmin))));
        CaseInsensitiveString templateName = cis("pipeline-name");
        configHelper.addTemplate("pipeline-name", authorization, "stage-name");

        boolean isAuthorized = securityService.isAuthorizedToEditTemplate(templateName, new Username(cis(templateAdminNotForThisTemplate)));

        assertThat(isAuthorized).isFalse();
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
            </cruise>
            """;
}
