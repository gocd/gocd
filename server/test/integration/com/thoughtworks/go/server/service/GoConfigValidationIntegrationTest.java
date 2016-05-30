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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.User;
import com.thoughtworks.go.domain.exception.ValidationException;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.helper.StageConfigMother;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.transaction.TransactionCallbackWithoutResult;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionStatus;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class GoConfigValidationIntegrationTest {
    private static final String ADMIN_1 = "admin-1";
    private static final String NON_ADMIN = "non-admin";
    private static final String MY_GROUP = "my-group";
    private static final String ADMIN_ROLE = "admin-role";
    private static final String PIPELINE_FOO = "foo";
    private static final String STAGE_BAR = "bar";
    private static final String MEMBER_OF_GO_ROLE = "loser";
    private static final String USER_WITH_PERMISSION_TO_OPERATE_ON_GROUP = "boozer";
    private GoConfigFileHelper configHelper = new GoConfigFileHelper();

    @Autowired private GoConfigDao goConfigDao;
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private GoCache goCache;
    @Autowired private UserService userService;
    @Autowired private TransactionTemplate transactionTemplate;

    @Before
    public void setUp() throws Exception {
        goCache.clear();

        configHelper.usingCruiseConfigDao(goConfigDao);
        configHelper.onSetUp();
        configHelper.addSecurityWithAdminConfig();

        dbHelper.onSetUp();
        saveAdminAndNonAdminGroups(transactionTemplate, userService);

        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig(PIPELINE_FOO, StageConfigMother.manualStage(STAGE_BAR));
        configHelper.addPipelineToGroup(pipelineConfig, MY_GROUP);
        configHelper.addRole(new Role(new CaseInsensitiveString(ADMIN_ROLE), new RoleUser(new CaseInsensitiveString(MEMBER_OF_GO_ROLE))));
        configHelper.blockPipelineGroupExceptFor(MY_GROUP, ADMIN_ROLE);
        configHelper.setOperatePermissionForGroup(MY_GROUP, USER_WITH_PERMISSION_TO_OPERATE_ON_GROUP);
    }

    @After
    public void teardown() throws Exception {
        dbHelper.onTearDown();
        configHelper.onTearDown();
    }

    @Test
    public void shouldAllow_aUserThatIsMemberOfARoleThatCanOperateOnPipelineGroup() {
        try {
            addApproverToStage(MEMBER_OF_GO_ROLE);
        } catch (Exception e) {
            fail("should allow loser to operate on stage, as he is a member of " + ADMIN_ROLE);
        }

        PipelineConfig pipelineConfig = configHelper.load().pipelineConfigByName(new CaseInsensitiveString(PIPELINE_FOO));
        StageConfig stage = pipelineConfig.getStage(new CaseInsensitiveString(STAGE_BAR));
        assertThat(stage.getApproval().getAuthConfig().getUsers(), hasItem(new AdminUser(new CaseInsensitiveString(MEMBER_OF_GO_ROLE))));
    }

    @Test
    public void shouldAllow_aUserThatHasDirectPermissionToOperateOnPipelineGroup() {

        try {
            addApproverToStage(USER_WITH_PERMISSION_TO_OPERATE_ON_GROUP);
        } catch (Exception e) {
            fail("should allow boozer to operate on stage, as he has operate permission on the group");
        }

        PipelineConfig pipelineConfig = configHelper.load().pipelineConfigByName(new CaseInsensitiveString(PIPELINE_FOO));
        StageConfig stage = pipelineConfig.getStage(new CaseInsensitiveString(STAGE_BAR));
        assertThat(stage.getApproval().getAuthConfig().getUsers(), hasItem(new AdminUser(new CaseInsensitiveString(USER_WITH_PERMISSION_TO_OPERATE_ON_GROUP))));
    }

    private void addApproverToStage(final String userName) {
        goConfigDao.updateConfig(new UpdateConfigCommand() {
            public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
                PipelineConfig pConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString(PIPELINE_FOO));
                StageConfig stage = pConfig.getStage(new CaseInsensitiveString(STAGE_BAR));
                stage.getApproval().addAdmin(new AdminUser(new CaseInsensitiveString(userName)));
                return cruiseConfig;
            }
        });
    }

    private static void saveAdminAndNonAdminGroups(final TransactionTemplate transactionTemplate, final UserService userService) throws Exception {
        transactionTemplate.executeWithExceptionHandling(new TransactionCallbackWithoutResult() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) throws Exception {
                addUser(ADMIN_1, "Admin One", "admin@admin.one", userService);
                addUser("admin-2", "Admin Two", "admin@admin.two", userService);
                addUser(NON_ADMIN, "Non Admin", "no@admin.no", userService);
            }
        });
    }

    private static User addUser(final String name, final String displayName, final String email, final UserService userService) throws ValidationException {
        User admin = new User(name, displayName, email);
        userService.saveOrUpdate(admin);
        return admin;
    }
}
