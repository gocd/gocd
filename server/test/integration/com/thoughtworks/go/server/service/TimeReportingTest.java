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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.config.update.ConfigUpdateResponse;
import com.thoughtworks.go.config.update.UpdateConfigFromUI;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.domain.user.PipelineSelections;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.TimeReportingUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class TimeReportingTest {
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private GoConfigService goConfigService;
    @Autowired private DatabaseAccessHelper dbHelper;
    private GoConfigFileHelper configHelper;

    @Before
    public void setup() throws Exception {
        configHelper = new GoConfigFileHelper();
        dbHelper.onSetUp();
        configHelper.usingCruiseConfigDao(goConfigDao).initializeConfigFile();
        configHelper.onSetUp();
        goConfigService.forceNotifyListeners();
    }

    @After
    public void tearDown() throws Exception {
        configHelper.onTearDown();
        dbHelper.onTearDown();
    }

    @Test
    @Ignore("Not really testing anything.")
    public void shouldBeAbleToShortlistPipelinesQuicklyWithPipelineSelections() throws Exception {
        CruiseConfig cruiseConfig = new BasicCruiseConfig();
        final CruiseConfig afterUpdate = getConfigWith(cruiseConfig, 60, 40);
        List<String> allNames = new ArrayList<String>();
        for (CaseInsensitiveString name : afterUpdate.getAllPipelineNames()) {
            allNames.add(name.toString());
        }
        final PipelineSelections oneIgnored = new PipelineSelections(allNames.subList(0, allNames.size() / 2));

        TimeReportingUtil.print(new TimeReportingUtil.TestAction() {
            @Override
            public void perform() throws Exception {
                for (PipelineConfigs group : afterUpdate.getGroups()) {
                    oneIgnored.includesGroup(group);
                }
            }
        });
    }

    @Test
    public void shouldSaveAndLoad2000pipelines() throws Exception {
        final CruiseConfig oldConfig = goConfigDao.load();
        final int numberOfNewPipelines = 2000;
        TimeReportingUtil.report(TimeReportingUtil.Key.CONFIG_WRITE_2000, new TimeReportingUtil.TestAction() {
            @Override
            public void perform() throws Exception {
                write(numberOfNewPipelines, oldConfig.getMd5());
            }
        });
        TimeReportingUtil.report(TimeReportingUtil.Key.CONFIG_READ_2000, new TimeReportingUtil.TestAction() {
            @Override
            public void perform() throws Exception {
                read(numberOfNewPipelines);
            }
        });
    }

    private CruiseConfig read(int numberOfPipelines) {
        Date a = new Date();
        goConfigDao.forceReload();
        CruiseConfig cruiseConfig = goConfigDao.load();
        Date b = new Date();
        assertThat(cruiseConfig.allPipelines().size(), is(numberOfPipelines));
//        System.out.println("READ = " + (b.getTime() - a.getTime()) / 1000.0 + "s");
        return cruiseConfig;
    }

    private ConfigUpdateResponse write(int numberOfNewPipelines, String oldMd5) throws InterruptedException {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        Date a = new Date();
        ConfigUpdateResponse res = goConfigService.updateConfigFromUI(new ReplaceConfigCommand(numberOfNewPipelines), oldMd5, null, result);
        Date b = new Date();
        assertThat(result.isSuccessful(), is(true));
//        System.out.println("WRITE = " + (b.getTime() - a.getTime()) / 1000.0 + "s");
        return res;
    }

    private static class ReplaceConfigCommand implements UpdateConfigFromUI {

        private final int numberOfNewPipelines;

        public ReplaceConfigCommand(int numberOfNewPipelines) {
            this.numberOfNewPipelines = numberOfNewPipelines;
        }

        public void checkPermission(CruiseConfig cruiseConfig, LocalizedOperationResult result) {
        }

        public Validatable node(CruiseConfig cruiseConfig) {
            return getConfigWith(cruiseConfig);
        }

        public Validatable updatedNode(CruiseConfig cruiseConfig) {
            return node(cruiseConfig);
        }

        public void update(Validatable node) {
        }

        public Validatable subject(Validatable node) {
            return node;
        }

        public Validatable updatedSubject(Validatable updatedNode) {
            return subject(updatedNode);
        }

        private CruiseConfig getConfigWith(CruiseConfig cruiseConfig) {
            int numberOfJobs = 10;
            int numberOfMaterials = 5;
            int numberOfTasks = 2;
            for (int i = 0; i < numberOfNewPipelines; i++) {
                PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfigWithStage("pipeline" + i, "stage" + i);
                for (int j = 0; j < numberOfJobs; j++) {
                    JobConfig jobConfig = new JobConfig("job" + j);
                    for (int k = 0; k < numberOfTasks; k++) {
                        jobConfig.addTask(new ExecTask("command" + k, "args", "workingdir"));
                    }
                    pipelineConfig.get(0).getJobs().add(jobConfig);
                }
                for (int j = 0; j < numberOfMaterials; j++) {
                    pipelineConfig.addMaterialConfig(new HgMaterialConfig("url" + j, "dest" + j));
                }
                cruiseConfig.addPipeline("group" + i, pipelineConfig);
            }
            return cruiseConfig;
        }
    }

    private CruiseConfig getConfigWith(CruiseConfig cruiseConfig, int numberOfGroups, int numberOfNewPipelinesInEachGroup) {
        int numberOfJobs = 10;
        int numberOfMaterials = 5;
        int numberOfTasks = 2;
        for (int z = 0; z < numberOfGroups; z++) {
            for (int i = 0; i < numberOfNewPipelinesInEachGroup; i++) {
                PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfigWithStage("pipeline" + i, "stage" + i);
                for (int j = 0; j < numberOfJobs; j++) {
                    JobConfig jobConfig = new JobConfig("job" + j);
                    for (int k = 0; k < numberOfTasks; k++) {
                        jobConfig.addTask(new ExecTask("command" + k, "args", "workingdir"));
                    }
                    pipelineConfig.get(0).getJobs().add(jobConfig);
                }
                for (int j = 0; j < numberOfMaterials; j++) {
                    pipelineConfig.addMaterialConfig(new HgMaterialConfig("url" + j, "dest" + j));
                }
                cruiseConfig.addPipeline("group" + z, pipelineConfig);
            }
        }

        return cruiseConfig;
    }
}
