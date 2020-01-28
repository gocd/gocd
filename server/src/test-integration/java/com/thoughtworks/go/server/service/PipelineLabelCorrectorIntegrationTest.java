/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.GoPartialConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.helper.PartialConfigMother;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.PipelineSqlMapDao;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.sql.SQLException;

import static com.thoughtworks.go.helper.MaterialConfigsMother.git;
import static com.thoughtworks.go.util.IBatisUtil.arguments;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class PipelineLabelCorrectorIntegrationTest {
    @Autowired
    private DatabaseAccessHelper dbHelper;
    @Autowired
    private GoConfigDao goConfigDao;
    @Autowired
    private PipelineSqlMapDao pipelineSqlMapDao;
    @Autowired
    private PipelineLabelCorrector pipelineLabelCorrector;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private MaterialRepository materialRepository;
    @Autowired
    private GoPartialConfig goPartialConfig;
    private ScheduleTestUtil scheduleUtil;
    private ConfigRepoConfig repoConfig;
    private GoConfigFileHelper configHelper = new GoConfigFileHelper();

    @Before
    public void setUp() throws Exception {
        configHelper.usingCruiseConfigDao(goConfigDao);
        configHelper.onSetUp();
        dbHelper.onSetUp();
        scheduleUtil = new ScheduleTestUtil(transactionTemplate, materialRepository, dbHelper, configHelper);
        repoConfig = ConfigRepoConfig.createConfigRepoConfig(git("url1"), "plugin");
        configHelper.addConfigRepo(repoConfig);
    }

    @After
    public void teardown() throws Exception {
        dbHelper.onTearDown();
        configHelper.onTearDown();
    }

    @Test
    public void shouldRemoveDuplicateEntriesForPipelineCounterFromDbAndKeepTheOneMatchingPipelineNameCaseInConfig() throws SQLException {
        String pipelineName = "Pipeline-Name";
        configHelper.addPipeline(pipelineName, "stage-name");
        pipelineSqlMapDao.getSqlMapClientTemplate().insert("insertPipelineLabelCounter", arguments("pipelineName", pipelineName.toLowerCase()).and("count", 10).asMap());
        pipelineSqlMapDao.getSqlMapClientTemplate().insert("insertPipelineLabelCounter", arguments("pipelineName", pipelineName.toUpperCase()).and("count", 20).asMap());
        pipelineSqlMapDao.getSqlMapClientTemplate().insert("insertPipelineLabelCounter", arguments("pipelineName", pipelineName).and("count", 30).asMap());
        assertThat(pipelineSqlMapDao.getPipelineNamesWithMultipleEntriesForLabelCount().size(), is(1));
        assertThat(pipelineSqlMapDao.getPipelineNamesWithMultipleEntriesForLabelCount().get(0).equalsIgnoreCase(pipelineName), is(true));

        pipelineLabelCorrector.correctPipelineLabelCountEntries();
        assertThat(pipelineSqlMapDao.getPipelineNamesWithMultipleEntriesForLabelCount().isEmpty(), is(true));
        assertThat(pipelineSqlMapDao.getCounterForPipeline(pipelineName), is(30));
        assertThat(pipelineSqlMapDao.getCounterForPipeline(pipelineName.toLowerCase()), is(30));
        assertThat(pipelineSqlMapDao.getCounterForPipeline(pipelineName.toUpperCase()), is(30));
    }

    @Test
    public void shouldRemoveAllEntriesForPipelineCounterFromDbIfThePipelineDoesnotBelongToConfigAnymoreAndThereWereNoPipelineRunsForThatPipelineButDuplicatesWereFound() throws SQLException {
        // Such a scenario could be created in pre 18.4 world when the pipeline was recreated with different cases via the API or the UI which causes the pipeline to be paused by default. Pipeline pause information is stored in the same table.
        String pipelineName = "Pipeline-Name";
        configHelper.addPipeline(pipelineName, "stage-name");
        pipelineSqlMapDao.getSqlMapClientTemplate().insert("insertPipelineLabelCounter", arguments("pipelineName", pipelineName.toLowerCase()).and("count", 10).asMap());
        pipelineSqlMapDao.getSqlMapClientTemplate().insert("insertPipelineLabelCounter", arguments("pipelineName", pipelineName.toUpperCase()).and("count", 20).asMap());
        pipelineSqlMapDao.getSqlMapClientTemplate().insert("insertPipelineLabelCounter", arguments("pipelineName", pipelineName).and("count", 30).asMap());
        assertThat(pipelineSqlMapDao.getPipelineNamesWithMultipleEntriesForLabelCount().size(), is(1));
        assertThat(pipelineSqlMapDao.getPipelineNamesWithMultipleEntriesForLabelCount().get(0).equalsIgnoreCase(pipelineName), is(true));
        configHelper.removePipeline(pipelineName);

        pipelineLabelCorrector.correctPipelineLabelCountEntries();

        assertThat(pipelineSqlMapDao.getPipelineNamesWithMultipleEntriesForLabelCount(), hasSize(0));
        assertThat(pipelineSqlMapDao.getCounterForPipeline(pipelineName), is(0));
    }

    @Test
    public void shouldRemoveDuplicateEntriesForPipelineCounterFromDbIfThePipelineDoesnotBelongToConfigAnymoreAndLeaveOutTheOneThatMatchesTheCaseOfLatestPipelineRun() throws SQLException {
        // Such a scenario could be created in pre 18.4 world when the pipeline was recreated with different cases, and has had a few runs after which it was deleted
        String pipelineName = "Pipeline-Name";
        SvnMaterial svn = scheduleUtil.wf(new SvnMaterial("svn", "username", "password", false), "folder1");
        scheduleUtil.checkinInOrder(svn, "s1");
        ScheduleTestUtil.AddedPipeline pipeline = scheduleUtil.saveConfigWith(pipelineName.toUpperCase(), scheduleUtil.m(svn));
        scheduleUtil.runAndPass(pipeline, "s1");
        configHelper.removePipeline(pipelineName);
        scheduleUtil.checkinInOrder(svn, "s2");
        pipeline = scheduleUtil.saveConfigWith(pipelineName.toLowerCase(), scheduleUtil.m(svn));
        scheduleUtil.runAndPass(pipeline, "s2");
        configHelper.removePipeline(pipelineName);
        scheduleUtil.checkinInOrder(svn, "s3");
        pipeline = scheduleUtil.saveConfigWith(pipelineName, scheduleUtil.m(svn));
        scheduleUtil.runAndPass(pipeline, "s3");

        pipelineSqlMapDao.getSqlMapClientTemplate().insert("insertPipelineLabelCounter", arguments("pipelineName", pipelineName.toLowerCase()).and("count", 10).asMap());
        pipelineSqlMapDao.getSqlMapClientTemplate().insert("insertPipelineLabelCounter", arguments("pipelineName", pipelineName).and("count", 20).asMap());

        assertThat(pipelineSqlMapDao.getPipelineNamesWithMultipleEntriesForLabelCount().size(), is(1));
        assertThat(pipelineSqlMapDao.getPipelineNamesWithMultipleEntriesForLabelCount().get(0).equalsIgnoreCase(pipelineName), is(true));
        configHelper.removePipeline(pipelineName);

        pipelineLabelCorrector.correctPipelineLabelCountEntries();

        assertThat(pipelineSqlMapDao.getPipelineNamesWithMultipleEntriesForLabelCount(), hasSize(0));
        assertThat(pipelineSqlMapDao.getCounterForPipeline(pipelineName), is(20));
    }

    @Test
    public void shouldRemoveDuplicateEntriesForPipelineCounterFromDbIfTheConfigRepoPipelineHasNotBeenLoadedUpYetLeavingBehindTheOneWhichMatchesTheCaseOfTheLastRunPipeline() throws SQLException {
        // Such a scenario could be created in pre 18.4 world when the pipeline defined in a config-repo was created/renamed with different cases, and has had a few runs. After this the server is upgraded to a version >= 18.4
        String pipelineName = "Pipeline-Name";
        ConfigRepoConfig repoConfig = ConfigRepoConfig.createConfigRepoConfig(git("url2"), "plugin");
        configHelper.addConfigRepo(repoConfig);
        PipelineConfig pipelineConfig = addConfigRepoPipeline(repoConfig, pipelineName);
        scheduleUtil.runAndPass(new ScheduleTestUtil.AddedPipeline(pipelineConfig, new DependencyMaterial(pipelineConfig.name(), pipelineConfig.first().name())), "svn1r11");

        pipelineConfig = addConfigRepoPipeline(repoConfig, pipelineName.toUpperCase());
        scheduleUtil.runAndPass(new ScheduleTestUtil.AddedPipeline(pipelineConfig, new DependencyMaterial(pipelineConfig.name(), pipelineConfig.first().name())), "svn1r11");
        pipelineConfig = addConfigRepoPipeline(repoConfig, pipelineName.toLowerCase());
        scheduleUtil.runAndPass(new ScheduleTestUtil.AddedPipeline(pipelineConfig,
                new DependencyMaterial(pipelineConfig.name(), pipelineConfig.first().name())), "svn1r11");
        pipelineSqlMapDao.getSqlMapClientTemplate().insert("insertPipelineLabelCounter", arguments("pipelineName", pipelineName.toLowerCase()).and("count", 10).asMap());
        pipelineSqlMapDao.getSqlMapClientTemplate().insert("insertPipelineLabelCounter", arguments("pipelineName", pipelineName.toUpperCase()).and("count", 20).asMap());
        addConfigRepoPipeline(repoConfig, "something-else");

        pipelineLabelCorrector.correctPipelineLabelCountEntries();

        addConfigRepoPipeline(repoConfig, pipelineName.toUpperCase());

        assertThat(pipelineSqlMapDao.getPipelineNamesWithMultipleEntriesForLabelCount(), hasSize(0));
        assertThat(pipelineSqlMapDao.getCounterForPipeline(pipelineName), is(10));
        assertThat(pipelineSqlMapDao.getCounterForPipeline(pipelineName.toUpperCase()), is(10));
        assertThat(pipelineSqlMapDao.getCounterForPipeline(pipelineName.toLowerCase()), is(10));
    }

    private PipelineConfig addConfigRepoPipeline(ConfigRepoConfig repoConfig, String pipeline) {
        PartialConfig partialConfig = PartialConfigMother.withPipeline(pipeline, new RepoConfigOrigin(repoConfig, "4567"));
        PipelineConfig remotePipeline = partialConfig.getGroups().first().getPipelines().get(0);
        SvnMaterial svn = scheduleUtil.wf((SvnMaterial) new MaterialConfigConverter().toMaterial(remotePipeline.materialConfigs().getSvnMaterial()), "svn");
        scheduleUtil.checkinInOrder(svn, scheduleUtil.d(1), "svn1r11");
        GitMaterial configRepoMaterial = scheduleUtil.wf((GitMaterial) new MaterialConfigConverter().toMaterial(repoConfig.getRepo()), "git");
        scheduleUtil.checkinInOrder(configRepoMaterial, scheduleUtil.d(1), "s1");
        goPartialConfig.onSuccessPartialConfig(repoConfig, partialConfig);
        return remotePipeline;
    }
}
