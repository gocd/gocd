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
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.update.ConfigUpdateResponse;
import com.thoughtworks.go.config.update.UpdateConfigFromUI;
import com.thoughtworks.go.helper.PipelineTemplateConfigMother;
import com.thoughtworks.go.helper.StageConfigMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class GoConfigServiceConfigSaveTest {
    @Autowired
    GoConfigService goConfigService;
    @Autowired private GoConfigDao goConfigDao;

    private GoConfigFileHelper configFileHelper;

    @Before
    public void setUp() throws Exception {
        configFileHelper = new GoConfigFileHelper(goConfigDao);
    }

    @Test
    public void shouldSaveTemplateWhenUserIsATemplateAdmin() throws Exception {
        String adminUser = "admin1";
        String templateName = "template-name";
        CruiseConfig configForEdit = addTemplateWithTemplateAdminUser(adminUser, templateName);

        ConfigUpdateResponse response = goConfigService.updateConfigFromUI(new AddStageToTemplate(templateName, "stage2"), configForEdit.getMd5(), new Username(new CaseInsensitiveString(adminUser)),
                new HttpLocalizedOperationResult());
        CruiseConfig configAfterUpdate = response.configAfterUpdate();

        assertThat(configAfterUpdate.getTemplateByName(new CaseInsensitiveString(templateName)).size(), is(2));
    }

    private CruiseConfig addTemplateWithTemplateAdminUser(final String userName, final String templateName) {
        AdminUser adminUser = new AdminUser(new CaseInsensitiveString(userName));
        PipelineTemplateConfig template = PipelineTemplateConfigMother.createTemplate(templateName, new Authorization(new AdminsConfig(adminUser)), StageConfigMother.manualStage("stage1"));
        CruiseConfig configForEdit = goConfigDao.loadForEditing();
        configForEdit.addTemplate(template);
        configFileHelper.writeConfigFile(configForEdit);
        return goConfigDao.loadForEditing();
    }

    private class AddStageToTemplate implements UpdateConfigFromUI {
        private final String templateName;
        private final String stageName;

        public AddStageToTemplate(String templateName, String stageName) {
            this.templateName = templateName;
            this.stageName = stageName;
        }

        public void checkPermission(CruiseConfig cruiseConfig, LocalizedOperationResult result) {
        }

        public Validatable node(CruiseConfig cruiseConfig) {
            return cruiseConfig.getTemplateByName(new CaseInsensitiveString(templateName));
        }

        public Validatable updatedNode(CruiseConfig cruiseConfig) {
            return node(cruiseConfig);
        }

        public void update(Validatable node) {
            PipelineTemplateConfig template = (PipelineTemplateConfig) node;
            template.addStageWithoutValidityAssertion(StageConfigMother.manualStage(stageName));
        }

        public Validatable subject(Validatable node) {
            return node;
        }

        public Validatable updatedSubject(Validatable updatedNode) {
            return subject(updatedNode);
        }
    }
}
