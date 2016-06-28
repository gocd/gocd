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

import java.util.Arrays;
import java.util.List;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.CachedGoConfig;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.domain.ServerSiteUrlConfig;
import com.thoughtworks.go.helper.ConfigFileFixture;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
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
    @Autowired private CachedGoConfig cachedGoConfig;
    @Autowired private SecurityService securityService;
    @Autowired private GoConfigService configService;
    @Autowired private DatabaseAccessHelper dbHelper;
    private GoConfigFileHelper configHelper;

    @Before
    public void setUp() throws Exception {
        configHelper = new GoConfigFileHelper();
        configHelper.usingCruiseConfigDao(goConfigDao);
        configHelper.onSetUp();
        configHelper.addPipelineWithGroup(GROUP_NAME, PIPELINE_NAME, STAGE_NAME, JOB_NAME);
        configHelper.addSecurityWithAdminConfig();
        dbHelper.onSetUp();
    }

    @After
    public void tearDown() throws Exception {
        configHelper.onTearDown();
        dbHelper.onTearDown();
    }

    @Test
    public void shouldReturnTrueIfUserHasViewPermission() {
        configHelper.setViewPermissionForGroup(GROUP_NAME, VIEWER);
        assertThat(securityService.hasViewPermissionForGroup(VIEWER, GROUP_NAME), is(true));
    }

    @Test
    public void userShouldHavePermissionIfAGroupAdmin() {
        configHelper.setAdminPermissionForGroup(GROUP_NAME, PIPELINE_ADMIN);
        assertThat(securityService.hasViewPermissionForGroup(PIPELINE_ADMIN, GROUP_NAME), is(true));
        assertThat(securityService.hasOperatePermissionForGroup(new CaseInsensitiveString(PIPELINE_ADMIN), GROUP_NAME), is(true));
        assertThat(securityService.hasOperatePermissionForPipeline(new CaseInsensitiveString(PIPELINE_ADMIN), PIPELINE_NAME), is(true));
        assertThat(securityService.hasViewPermissionForPipeline(Username.valueOf(PIPELINE_ADMIN), PIPELINE_NAME), is(true));
        assertThat(securityService.hasViewPermissionForPipeline(new Username(new CaseInsensitiveString(PIPELINE_ADMIN)), PIPELINE_NAME), is(true));
        assertThat(securityService.hasOperatePermissionForStage(PIPELINE_NAME, STAGE_NAME, PIPELINE_ADMIN), is(true));
    }

    @Test
    public void shouldNotGiveOperatePermissionsToAViewOnlyUser() {
        String viewOnly = "view_only";
        configHelper.setViewPermissionForGroup(GROUP_NAME, viewOnly);

        assertThat(securityService.hasViewPermissionForGroup(viewOnly, GROUP_NAME), is(true));
        assertThat(securityService.hasOperatePermissionForGroup(new CaseInsensitiveString(viewOnly), GROUP_NAME), is(false));
        assertThat(securityService.hasOperatePermissionForPipeline(new CaseInsensitiveString(viewOnly), PIPELINE_NAME), is(false));
        assertThat(securityService.hasViewPermissionForPipeline(Username.valueOf(viewOnly), PIPELINE_NAME), is(true));
        assertThat(securityService.hasOperatePermissionForStage(PIPELINE_NAME, STAGE_NAME, viewOnly), is(false));

        assertThat(securityService.hasViewPermissionForGroup(viewOnly, GROUP_NAME), is(true));
        assertThat(securityService.hasOperatePermissionForGroup(new CaseInsensitiveString(viewOnly), GROUP_NAME), is(false));
        assertThat(securityService.hasOperatePermissionForPipeline(new CaseInsensitiveString(viewOnly), PIPELINE_NAME), is(false));
        assertThat(securityService.hasViewPermissionForPipeline(Username.valueOf(viewOnly), PIPELINE_NAME), is(true));
        assertThat(securityService.hasOperatePermissionForStage(PIPELINE_NAME, STAGE_NAME, viewOnly), is(false));
    }

    @Test
    public void userShouldHavePermissionToOperateOnAStageIfAGroupAdmin() {
        configHelper.setAdminPermissionForGroup(GROUP_NAME, PIPELINE_ADMIN);
        configHelper.setOperatePermissionForGroup(GROUP_NAME, OPERATOR);
        configHelper.setOperatePermissionForStage(PIPELINE_NAME, STAGE_NAME, OPERATOR);
        assertThat(securityService.hasOperatePermissionForStage(PIPELINE_NAME, STAGE_NAME, PIPELINE_ADMIN), is(true));
    }

    @Test
    public void shouldGiveTheGroupsModifiableByAdmin() {
        configHelper.addAdmins("admin");
        configHelper.addPipelineWithGroup("newGroup", "newPipeline", "newStage", "newJob");
        List<String> groups = securityService.modifiableGroupsForUser(new Username(new CaseInsensitiveString("admin")));
        assertThat(groups, hasItems(GROUP_NAME, "newGroup"));
    }

    @Test
    public void shouldGiveTheGroupsModifiableByAGroupAdmin() {
        configHelper.addPipelineWithGroup("newGroup", "newPipeline", "newStage", "newJob");
        configHelper.setAdminPermissionForGroup("newGroup", "groupAdmin");
        List<String> groups = securityService.modifiableGroupsForUser(new Username(new CaseInsensitiveString("groupAdmin")));
        assertThat(groups, hasItems("newGroup"));
    }

    @Test
    public void shouldGiveAllTheGroupsForAUserWhoIsAnAdminAndAGroupAdmin() {
        configHelper.addAdmins("admin");
        configHelper.addPipelineWithGroup("newGroup", "newPipeline", "newStage", "newJob");
        configHelper.setAdminPermissionForGroup("newGroup", "admin");
        List<String> groups = securityService.modifiableGroupsForUser(new Username(new CaseInsensitiveString("admin")));
        assertThat(groups, hasItems(GROUP_NAME, "newGroup"));
    }

    @Test
    public void shouldNotGiveAnyGroupsForAUserWhoIsNotAnAdminOrAGroupAdmin() {
        configHelper.addAdmins("admin");
        configHelper.addPipelineWithGroup("newGroup", "newPipeline", "newStage", "newJob");
        configHelper.setAdminPermissionForGroup("newGroup", "groupAdmin");
        List<String> groups = securityService.modifiableGroupsForUser(new Username(new CaseInsensitiveString("loser")));
        assertThat(groups.isEmpty(), is(true));
    }

    @Test
    public void shouldReturnTrueIfUserHasViewOrOperatePermissionForPipeline() {
        configHelper.setViewPermissionForGroup(GROUP_NAME, VIEWER);
        assertThat("should have view permission", securityService.hasViewOrOperatePermissionForPipeline(new Username(new CaseInsensitiveString(VIEWER)), PIPELINE_NAME), is(true));

        configHelper.setOperatePermissionForGroup(GROUP_NAME, OPERATOR);
        assertThat("should have operate permission", securityService.hasViewOrOperatePermissionForPipeline(new Username(new CaseInsensitiveString(OPERATOR)), PIPELINE_NAME), is(true));
    }

    @Test
    public void shouldReturnFalseIfUserDoesNotHaveViewPermission() {
        configHelper.setViewPermissionForGroup(GROUP_NAME, VIEWER);
        assertThat(securityService.hasViewPermissionForGroup(HACKER, GROUP_NAME), is(false));
    }

    @Test
    public void shouldNotCheckViewPermissionIfPipelineDoesNotExist() {
        assertThat(securityService.hasViewPermissionForPipeline(Username.valueOf(VIEWER), "noSuchAPipeline"), is(true));
    }

    @Test
    public void shouldNotCheckOperatePermissionIfPipelineDoesNotExist() {
        assertThat(securityService.hasOperatePermissionForPipeline(new CaseInsensitiveString(OPERATOR), "noSuchAPipeline"), is(true));
    }

    @Test
    public void shouldNotCheckViewPermissionIfSecurityIsTurnedOff() {
        configHelper.turnOffSecurity();
        configHelper.setViewPermissionForGroup(GROUP_NAME, VIEWER);
        assertThat(securityService.hasViewPermissionForGroup(HACKER, GROUP_NAME), is(true));
    }

    @Test
    public void shouldNotCheckViewPermissionForNonEnterpriseEdition() {
        assertThat(securityService.hasViewPermissionForGroup(HACKER, GROUP_NAME), is(true));
    }

    @Test
    public void shouldNotCheckOperationPermissionForNonEnterpriseEdition() {
        assertThat(securityService.hasOperatePermissionForGroup(new CaseInsensitiveString(HACKER), GROUP_NAME), is(true));
    }

    @Test
    public void shouldNotCheckOperationPermissionIfSecurityIsTurnedOff() {
        configHelper.turnOffSecurity();
        configHelper.setOperatePermissionForGroup(GROUP_NAME, OPERATOR);
        assertThat(securityService.hasOperatePermissionForGroup(new CaseInsensitiveString(HACKER), GROUP_NAME), is(true));
    }

    @Test
    public void shouldReturnTrueIfUserHasOperatePermission() throws Exception {
        configHelper.setOperatePermissionForGroup(GROUP_NAME, OPERATOR);
        assertThat(securityService.hasOperatePermissionForGroup(new CaseInsensitiveString(OPERATOR), GROUP_NAME), is(true));
    }

    @Test
    public void shouldGrantAdminViewPermissionForAllGroups() {
        configHelper.setViewPermissionForGroup(GROUP_NAME, VIEWER);
        assertThat(securityService.hasViewPermissionForGroup(ADMIN, GROUP_NAME), is(true));
    }

    @Test
    public void shouldGrantAdminOperatePermissionForAllGroups() {
        configHelper.setOperatePermissionForGroup(GROUP_NAME, OPERATOR);
        configHelper.addAuthorizedUserForStage(PIPELINE_NAME, STAGE_NAME, OPERATOR);
        assertThat(securityService.hasOperatePermissionForGroup(new CaseInsensitiveString(ADMIN), GROUP_NAME), is(true));
    }

    @Test
    public void isUserAdminOfGroup_shouldBeTrueForASuperAdmin() {
        configHelper.addAdmins("root");
        assertThat(securityService.isUserAdminOfGroup(new CaseInsensitiveString("root"), GROUP_NAME), is(true));
    }

    @Test
    public void isUserAdminOfGroup_shouldBeTrueForAGroupAdmin() {
        configHelper.setAdminPermissionForGroup(GROUP_NAME, OPERATOR);
        assertThat(securityService.isUserAdminOfGroup(new CaseInsensitiveString(OPERATOR), GROUP_NAME), is(true));
    }

    @Test
    public void isUserAdminOfGroup_shouldBeFalseForANonGroupAdmin() {
        assertThat(securityService.isUserAdminOfGroup(new CaseInsensitiveString("some-unauthorized-user"), GROUP_NAME), is(false));
    }

    @Test
    public void shouldGrantAdminOperatePermissionForAllStages() {
        configHelper.setOperatePermissionForGroup(GROUP_NAME, OPERATOR);
        configHelper.addAuthorizedUserForStage(PIPELINE_NAME, STAGE_NAME, OPERATOR);
        assertThat(securityService.hasOperatePermissionForStage(PIPELINE_NAME, STAGE_NAME, ADMIN), is(true));
    }



    @Test public void shouldReturnAllPipelinesThatUserHasViewPermissionsFor() throws Exception {
        configHelper.onTearDown();
        cachedGoConfig.save(CONFIG_WITH_2_GROUPS, true);
        assertThat(securityService.viewablePipelinesFor(new Username(new CaseInsensitiveString("blah"))).size(), is(0));
        assertThat(securityService.viewablePipelinesFor(new Username(new CaseInsensitiveString("admin"))), is(Arrays.asList(new CaseInsensitiveString("pipeline1"), new CaseInsensitiveString("pipeline2"))));
        assertThat(securityService.viewablePipelinesFor(new Username(new CaseInsensitiveString("pavan"))), is(Arrays.asList(new CaseInsensitiveString("pipeline3"))));
    }

    @Test public void shouldReturnAllPipelinesWithNoSecurity() throws Exception {
        configHelper.onTearDown();
        cachedGoConfig.save(ConfigFileFixture.multipleMaterial("<hg url='http://localhost'/>"), true);
        assertThat(securityService.viewablePipelinesFor(new Username(new CaseInsensitiveString("admin"))),
                is(Arrays.asList(new CaseInsensitiveString("ecl"), new CaseInsensitiveString("ec2"), new CaseInsensitiveString("framework"))));
        assertThat(securityService.viewablePipelinesFor(Username.ANONYMOUS),
                is(Arrays.asList(new CaseInsensitiveString("ecl"), new CaseInsensitiveString("ec2"), new CaseInsensitiveString("framework"))));
    }

    @Test public void shouldNotHaveOperatePermissionsOnStagesThatDoNotExist() throws Exception {
        assertThat(securityService.hasOperatePermissionForStage(PIPELINE_NAME,"doesnt-exist","raghu"),is(false));
    }

    @Test public void shouldNotHaveOperatePermissionsOnPipelinesThatDoNotExist() throws Exception {
        assertThat(securityService.hasOperatePermissionForStage("doesnt-exists","doesnt-exist","raghu"),is(false));
    }

    @Test public void shouldReturnSiteUrlAsCasServiceBaseUrlIfOnlySiteUrlIsDefined() throws Exception {
        configHelper.setBaseUrls(new ServerSiteUrlConfig("http://example.com"), new ServerSiteUrlConfig());
        assertThat(securityService.casServiceBaseUrl(), is("http://example.com"));
    }

    @Test public void shouldReturnSecureSiteUrlAsCasServiceBaseUrlIfBothSiteUrlAndSecureSiteUrlAreDefined() throws Exception {
        configHelper.setBaseUrls(new ServerSiteUrlConfig("http://example.com"), new ServerSiteUrlConfig("https://example.com"));
        assertThat(securityService.casServiceBaseUrl(), is("https://example.com"));
    }

    @Test
    public void shouldReturnTrueIfUserIsAuthorizedToViewAnyOfTheTemplatesListed() {
        String templateAdmin = "template-admin-1";
        Authorization authorization = new Authorization(new AdminsConfig(new AdminUser(new CaseInsensitiveString(templateAdmin))));
        configHelper.addTemplate("pipeline-name", authorization, "stage-name");

        boolean isAuthorized = securityService.isAuthorizedToViewAndEditTemplates(new Username(new CaseInsensitiveString(templateAdmin)));

        assertThat(isAuthorized, is(true));
    }

    @Test
    public void shouldReturnTrueIfUserIsTemplateAdminAndCanEditTemplate() {
        String templateAdmin = "template-admin-1";
        Authorization authorization = new Authorization(new AdminsConfig(new AdminUser(new CaseInsensitiveString(templateAdmin))));
        String templateName = "pipeline-name";
        configHelper.addTemplate(templateName, authorization, "stage-name");

        boolean isAuthorized = securityService.isAuthorizedToEditTemplate(templateName, new Username(new CaseInsensitiveString(templateAdmin)));

        assertThat(isAuthorized, is(true));
    }

    @Test
    public void shouldReturnFalseIfUserIsTemplateAdminAndCannotEditTemplate() {
        String templateAdmin = "template-admin-1";
        String templateAdminNotForThisTemplate = "template-admin-2";
        Authorization authorization = new Authorization(new AdminsConfig(new AdminUser(new CaseInsensitiveString(templateAdmin))));
        String templateName = "pipeline-name";
        configHelper.addTemplate(templateName, authorization, "stage-name");

        boolean isAuthorized = securityService.isAuthorizedToEditTemplate(templateName, new Username(new CaseInsensitiveString(templateAdminNotForThisTemplate)));

        assertThat(isAuthorized, is(false));
    }

    private static final String CONFIG_WITH_2_GROUPS = "<cruise schemaVersion='16'>\n"
            + "<server artifactsdir='artifacts' >"
            +  "<license user=\"Cruise team internal\">E+2WI6OuZ6hQ9wnNZGaiIQzGaLerbJR73qC+4OXlTDhC3Vafq8phXVPjFzUWXzpeBjcyytmQetqKG0TCKSoOhlDKdVrO982jHv7Gal6fz1kD0KbKoNnWo9vwqTEXndOfr+qoVr9KydLtyC3WdpDyjw7fPTBmB/eZmaTHKvZvJHHeYbKsvX8kZPYwhQT6oxbzwylqOPhJAiq6EKxS2S0jk1h0Uy5c07IiE4+y8PmwoElnfl3kpAARHMv40vfxamttp6IljBCuJ2fXQ0rXuukA/jIkv1i78A6dqL0Ii3RAIjRvglVHeI9HT9a0SyOR0eUMorFJJPDoqUnb1TVu/Ij3EQ==</license>\n"
            + "    <security>\n"
            + " <passwordFile path=\"/home/cruise/admins.properties\"/>\n"
            + "      <roles>\n"
            + "        <role name=\"simple\" >\n"
            + "           <user>admin</user>\n"
            + "           <user>pavan</user>\n"
            + "        </role>\n"
            + "      </roles>\n"
            + "      <admins>\n"
            + "           <user>raghu</user>\n"
            + "           <user>jumble</user>\n"
            + "      </admins>\n"
            + "    </security>\n"
            + "</server>\n"
            + "<pipelines group=\"first\">\n"
            +" <authorization>\n"
            + "        <view>\n"
            + "           <user>admin</user>\n"
            + "        </view>\n"
            + "    </authorization>"
            + "<pipeline name='pipeline1'>\n"
            + "    <materials>\n"
            + "      <svn url =\"svnurl\"/>"
            + "    </materials>\n"
            + "  <stage name='mingle'>\n"
            + "    <jobs>\n"
            + "      <job name='cardlist' />\n"
            + "    </jobs>\n"
            + "  </stage>\n"
            + "</pipeline>\n"
            + "<pipeline name='pipeline2'>\n"
            + "    <materials>\n"
            + "      <svn url =\"svnurl\"/>"
            + "    </materials>\n"
            + "  <stage name='mingle'>\n"
            + "    <jobs>\n"
            + "      <job name='cardlist' />\n"
            + "    </jobs>\n"
            + "  </stage>\n"
            + "</pipeline>\n"
            + "</pipelines>\n"
            + "<pipelines group='second'>\n"
            +" <authorization>\n"
            + "        <view>\n"
            + "           <user>pavan</user>\n"
            + "        </view>\n"
            + "    </authorization>"
            + "<pipeline name='pipeline3'>\n"

            + "    <materials>\n"
            + "      <svn url =\"svnurl\"/>"
            + "    </materials>\n"
            + "  <stage name='mingle'>\n"
            + "    <jobs>\n"
            + "      <job name='cardlist' />\n"
            + "    </jobs>\n"
            + "  </stage>\n"
            + "</pipeline>\n"
            + "</pipelines>\n"
            + "</cruise>";
}
