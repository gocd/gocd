/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.Filter;
import com.thoughtworks.go.config.materials.IgnoredFiles;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.git.GitTestRepo;
import com.thoughtworks.go.domain.materials.svn.Subversion;
import com.thoughtworks.go.domain.materials.svn.SvnCommand;
import com.thoughtworks.go.helper.PipelineMother;
import com.thoughtworks.go.helper.SvnTestRepo;
import com.thoughtworks.go.helper.TestRepo;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.PipelineDao;
import com.thoughtworks.go.server.domain.PipelineTimeline;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.materials.DependencyMaterialUpdateNotifier;
import com.thoughtworks.go.server.materials.MaterialDatabaseUpdater;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.scheduling.ScheduleHelper;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.TemporaryFolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
@EnableRuleMigrationSupport
public class BuildCauseProducerServiceDependencyIntegrationTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static final String STAGE_NAME = "dev";

    @Autowired
    private GoConfigDao goConfigDao;
    @Autowired
    private GoConfigService goConfigService;
    @Autowired
    private PipelineDao pipelineDao;
    @Autowired
    private PipelineScheduleQueue pipelineScheduleQueue;
    @Autowired
    private ScheduleHelper scheduleHelper;
    @Autowired
    private DatabaseAccessHelper dbHelper;
    @Autowired
    private MaterialDatabaseUpdater materialDatabaseUpdater;
    @Autowired
    private MaterialRepository materialRepository;
    @Autowired
    private SubprocessExecutionContext subprocessExecutionContext;
    @Autowired
    private PipelineTimeline pipelineTimeline;
    @Autowired
    private DependencyMaterialUpdateNotifier dependencyMaterialUpdateNotifier;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private static GoConfigFileHelper configHelper = new GoConfigFileHelper();
    public Subversion repository;
    public GitTestRepo gitTestRepo;
    public static SvnTestRepo svnRepository;

    private MinglePipeline minglePipeline;
    private GoPipeline goPipeline;

    private static final String MINGLE_PIPELINE_NAME = "mingle";
    private static final String GO_PIPELINE_NAME = "go";
    private MaterialRevisions svnMaterialRevs;
    private HttpOperationResult result;
    private SvnMaterial svnMaterial;
    private GitMaterial gitMaterial;

    //contains stuff related to the "Mingle" pipeline
    class MinglePipeline {
        PipelineConfig config;
        Pipeline latest;

        void setup(BuildCause buildCause) throws Exception {
            config = configHelper.addPipeline(MINGLE_PIPELINE_NAME, STAGE_NAME, repository, new Filter(new IgnoredFiles("**/*.doc")), "unit", "functional");
            latest = PipelineMother.schedule(this.config, buildCause);
            latest = pipelineDao.saveWithStages(latest);
            dbHelper.passStage(latest.getStages().first());
            pipelineScheduleQueue.clear();
        }

        MaterialRevisions runAndPassWith(MaterialRevisions newRevs) throws Exception {
            return runAndPassWith(newRevs, null);
        }

        MaterialRevisions runAndPassWith(MaterialRevisions newRevs, MaterialRevisions revsAfterFoo) throws Exception {
            if (revsAfterFoo != null) {
                for (MaterialRevision newRev : newRevs) {
                    newRev.addModifications(revsAfterFoo.getModifications(newRev.getMaterial()));
                }
            }
            runAndPass(newRevs);
            return newRevs;
        }

        void runAndPass(MaterialRevisions mingleRev) {
            BuildCause buildCause = BuildCause.createWithModifications(mingleRev, "boozer");
            latest = PipelineMother.schedule(config, buildCause);
            latest = pipelineDao.saveWithStages(latest);
            dbHelper.passStage(latest.getStages().first());
        }
    }

    //contains stuff related to the "Go" pipeline
    class GoPipeline {
        PipelineConfig config;
        Pipeline latest;

        void setup(BuildCause buildCause) throws Exception {
            config = configHelper.addPipeline(GO_PIPELINE_NAME, STAGE_NAME, repository, "unit");
            latest = PipelineMother.schedule(this.config, buildCause);
            latest = pipelineDao.saveWithStages(latest);
            dbHelper.passStage(latest.getStages().first());
            pipelineScheduleQueue.clear();
        }

        MaterialRevisions runAndPassWith(MaterialRevisions newRevs) throws Exception {
            return runAndPassWith(newRevs, null);
        }

        MaterialRevisions runAndPassWith(MaterialRevisions newRevs, MaterialRevisions revsAfterFoo) throws Exception {
            if (revsAfterFoo != null) {
                for (MaterialRevision newRev : newRevs) {
                    newRev.addModifications(revsAfterFoo.getModifications(newRev.getMaterial()));
                }
            }
            runAndPass(newRevs);
            return newRevs;
        }

        void runAndPass(MaterialRevisions rev) {
            BuildCause buildCause = BuildCause.createWithModifications(rev, "boozer");
            latest = PipelineMother.schedule(config, buildCause);
            latest = pipelineDao.saveWithStages(latest);
            dbHelper.passStage(latest.getStages().first());
        }
    }

    @BeforeEach
    public void setup() throws Exception {
        dbHelper.onSetUp();
        configHelper.onSetUp();
        configHelper.usingCruiseConfigDao(goConfigDao).initializeConfigFile();
        dependencyMaterialUpdateNotifier.disableUpdates();

        minglePipeline = new MinglePipeline();
        goPipeline = new GoPipeline();

        svnRepository = new SvnTestRepo(temporaryFolder);
        repository = new SvnCommand(null, svnRepository.projectRepositoryUrl());

        svnMaterialRevs = new MaterialRevisions();
        svnMaterial = new SvnMaterial(repository);
        svnMaterialRevs.addRevision(svnMaterial, svnMaterial.latestModification(null, new ServerSubprocessExecutionContext(goConfigService, new SystemEnvironment())));

        final MaterialRevisions materialRevisions = new MaterialRevisions();
        SvnMaterial anotherSvnMaterial = new SvnMaterial(repository);
        materialRevisions.addRevision(anotherSvnMaterial, anotherSvnMaterial.latestModification(null, subprocessExecutionContext));

        gitTestRepo = new GitTestRepo(temporaryFolder);
        MaterialRevisions gitMaterialRevs = new MaterialRevisions();
        gitMaterial = gitTestRepo.createMaterial();
        gitMaterialRevs.addRevision(gitMaterial, gitTestRepo.latestModifications());

        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                materialRepository.save(svnMaterialRevs);
                materialRepository.save(gitMaterialRevs);
            }
        });

        BuildCause buildCause = BuildCause.createWithModifications(svnMaterialRevs, "");

        minglePipeline.setup(buildCause);
        goPipeline.setup(buildCause);
        result = new HttpOperationResult();
    }

    @AfterEach
    public void teardown() throws Exception {
        TestRepo.internalTearDown();
        dbHelper.onTearDown();
        pipelineScheduleQueue.clear();
        configHelper.onTearDown();
        dependencyMaterialUpdateNotifier.enableUpdates();
    }

    @Test
    public void shouldNotScheduleDownstreamPipeline_whenIgnoreForSchedulingIsTrue() throws Exception {
        //set up a pipeline downstream to "mingle", run and save once instance of the pipelines.
        String mingleDownstreamPipelineName = "down_of_mingle";
        DependencyMaterialConfig mingleMaterialConfig = new DependencyMaterialConfig(new CaseInsensitiveString(MINGLE_PIPELINE_NAME), new CaseInsensitiveString(STAGE_NAME));
        mingleMaterialConfig.ignoreForScheduling(true);

        PipelineConfig downstreamPipelineConfig = configHelper.addPipeline(mingleDownstreamPipelineName, STAGE_NAME, new MaterialConfigs(mingleMaterialConfig), "unit");

        Pipeline latestMinglePipeline = minglePipeline.latest;
        String revision = String.format("%s/%s/%s/%s", latestMinglePipeline.getName(), latestMinglePipeline.getCounter(), STAGE_NAME, latestMinglePipeline.getStages().last().getCounter());
        MaterialRevision dependencyMaterialRevision = new MaterialRevision(new DependencyMaterial(mingleMaterialConfig), true, new Modification(latestMinglePipeline.getModifiedDate(), revision, latestMinglePipeline.getLabel(), latestMinglePipeline.getId()));
        MaterialRevisions dependencyMaterialRevisions = new MaterialRevisions(dependencyMaterialRevision);
        dbHelper.saveRevs(dependencyMaterialRevisions);

        Pipeline latestDownstreamInstance = PipelineMother.schedule(downstreamPipelineConfig, BuildCause.createManualForced(dependencyMaterialRevisions, new Username(new CaseInsensitiveString("loser"))));
        latestDownstreamInstance = pipelineDao.saveWithStages(latestDownstreamInstance);
        dbHelper.passStage(latestDownstreamInstance.getStages().first());

        //trigger pipeline
        MaterialRevisions newRevs = checkinFile(svnMaterial, "bar.c", svnRepository);
        minglePipeline.runAndPassWith(newRevs);
        pipelineTimeline.update();
        scheduleHelper.autoSchedulePipelinesWithRealMaterials(mingleDownstreamPipelineName);
        assertThat(pipelineScheduleQueue.toBeScheduled().keySet()).doesNotContain(new CaseInsensitiveString(mingleDownstreamPipelineName));
    }

    @Test
    public void shouldNotScheduleDownStreamPipeline_withTwoUpstreamMaterials_bothSkippedForScheduling() throws Exception {
        String downstreamPipelineName = "downstream_pipeline";
        //first upstream pipeline - "mingle"
        DependencyMaterialConfig mingleMaterialConfig = new DependencyMaterialConfig(new CaseInsensitiveString(MINGLE_PIPELINE_NAME), new CaseInsensitiveString(STAGE_NAME));
        mingleMaterialConfig.ignoreForScheduling(true);

        //second upstream pipeline - "go"
        DependencyMaterialConfig goMaterialConfig = new DependencyMaterialConfig(new CaseInsensitiveString(GO_PIPELINE_NAME), new CaseInsensitiveString(STAGE_NAME));
        goMaterialConfig.ignoreForScheduling(true);

        PipelineConfig downstreamPipelineConfig = configHelper.addPipeline(downstreamPipelineName, STAGE_NAME, new MaterialConfigs(mingleMaterialConfig, goMaterialConfig), "unit");

        Pipeline latestMinglePipeline = minglePipeline.latest;
        String revision = String.format("%s/%s/%s/%s", latestMinglePipeline.getName(), latestMinglePipeline.getCounter(), STAGE_NAME, latestMinglePipeline.getStages().last().getCounter());
        MaterialRevision mingleMaterialRevision = new MaterialRevision(new DependencyMaterial(mingleMaterialConfig), true, new Modification(latestMinglePipeline.getModifiedDate(), revision, latestMinglePipeline.getLabel(), latestMinglePipeline.getId()));

        Pipeline latestGoPipeline = goPipeline.latest;
        revision = String.format("%s/%s/%s/%s", latestGoPipeline.getName(), latestGoPipeline.getCounter(), STAGE_NAME, latestGoPipeline.getStages().last().getCounter());
        MaterialRevision goMaterialRevision = new MaterialRevision(new DependencyMaterial(goMaterialConfig), true, new Modification(latestGoPipeline.getModifiedDate(), revision, latestGoPipeline.getLabel(), latestGoPipeline.getId()));

        MaterialRevisions dependencyMaterialRevisions = new MaterialRevisions(mingleMaterialRevision, goMaterialRevision);
        dbHelper.saveRevs(dependencyMaterialRevisions);

        Pipeline latestDownstreamInstance = PipelineMother.schedule(downstreamPipelineConfig, BuildCause.createManualForced(dependencyMaterialRevisions, new Username(new CaseInsensitiveString("loser"))));
        latestDownstreamInstance = pipelineDao.saveWithStages(latestDownstreamInstance);
        dbHelper.passStage(latestDownstreamInstance.getStages().first());

        //trigger upstream pipelines
        MaterialRevisions newRevs = checkinFile(svnMaterial, "bar.c", svnRepository);
        minglePipeline.runAndPassWith(newRevs);
        goPipeline.runAndPassWith(newRevs);
        pipelineTimeline.update();
        scheduleHelper.autoSchedulePipelinesWithRealMaterials(downstreamPipelineName);
        assertThat(pipelineScheduleQueue.toBeScheduled().keySet()).doesNotContain(new CaseInsensitiveString(downstreamPipelineName));
    }

    @Test
    public void shouldScheduleDownStreamPipeline_withTwoUpstreamMaterials_oneOneIsSkippedForScheduling() throws Exception {
        String downstreamPipelineName = "downstream_pipeline";
        //first upstream pipeline - "mingle"
        DependencyMaterialConfig mingleMaterialConfig = new DependencyMaterialConfig(new CaseInsensitiveString(MINGLE_PIPELINE_NAME), new CaseInsensitiveString(STAGE_NAME));
        mingleMaterialConfig.ignoreForScheduling(true);

        //second upstream pipeline - "go"
        DependencyMaterialConfig goMaterialConfig = new DependencyMaterialConfig(new CaseInsensitiveString(GO_PIPELINE_NAME), new CaseInsensitiveString(STAGE_NAME));
        goMaterialConfig.ignoreForScheduling(false);

        PipelineConfig downstreamPipelineConfig = configHelper.addPipeline(downstreamPipelineName, STAGE_NAME, new MaterialConfigs(mingleMaterialConfig, goMaterialConfig), "unit");

        Pipeline latestMinglePipeline = minglePipeline.latest;
        String revision = String.format("%s/%s/%s/%s", latestMinglePipeline.getName(), latestMinglePipeline.getCounter(), STAGE_NAME, latestMinglePipeline.getStages().last().getCounter());
        MaterialRevision mingleMaterialRevision = new MaterialRevision(new DependencyMaterial(mingleMaterialConfig), true, new Modification(latestMinglePipeline.getModifiedDate(), revision, latestMinglePipeline.getLabel(), latestMinglePipeline.getId()));

        Pipeline latestGoPipeline = goPipeline.latest;
        revision = String.format("%s/%s/%s/%s", latestGoPipeline.getName(), latestGoPipeline.getCounter(), STAGE_NAME, latestGoPipeline.getStages().last().getCounter());
        MaterialRevision goMaterialRevision = new MaterialRevision(new DependencyMaterial(goMaterialConfig), true, new Modification(latestGoPipeline.getModifiedDate(), revision, latestGoPipeline.getLabel(), latestGoPipeline.getId()));

        MaterialRevisions dependencyMaterialRevisions = new MaterialRevisions(mingleMaterialRevision, goMaterialRevision);
        dbHelper.saveRevs(dependencyMaterialRevisions);

        Pipeline latestDownstreamInstance = PipelineMother.schedule(downstreamPipelineConfig, BuildCause.createManualForced(dependencyMaterialRevisions, new Username(new CaseInsensitiveString("loser"))));
        latestDownstreamInstance = pipelineDao.saveWithStages(latestDownstreamInstance);
        dbHelper.passStage(latestDownstreamInstance.getStages().first());

        //trigger upstream pipelines
        MaterialRevisions newRevs = checkinFile(svnMaterial, "bar.c", svnRepository);
        minglePipeline.runAndPassWith(newRevs);
        goPipeline.runAndPassWith(newRevs);
        pipelineTimeline.update();
        scheduleHelper.autoSchedulePipelinesWithRealMaterials(downstreamPipelineName);
        assertThat(pipelineScheduleQueue.toBeScheduled().keySet()).contains(new CaseInsensitiveString(downstreamPipelineName));
    }

    @Test
    public void shouldScheduleDownStreamPipeline_withSCMAndDependencyMaterials_whenSCMMaterialHasChanges() throws Exception {
        String downstreamPipelineName = "downstream_pipeline";
        //first upstream pipeline - "mingle"
        DependencyMaterialConfig mingleMaterialConfig = new DependencyMaterialConfig(new CaseInsensitiveString(MINGLE_PIPELINE_NAME), new CaseInsensitiveString(STAGE_NAME));
        mingleMaterialConfig.ignoreForScheduling(true);

        //setup the pipeline
        PipelineConfig downstreamPipelineConfig = configHelper.addPipeline(downstreamPipelineName, STAGE_NAME, new MaterialConfigs(mingleMaterialConfig, gitMaterial.config()), "unit");
        Pipeline latestMinglePipeline = minglePipeline.latest;
        String revision = String.format("%s/%s/%s/%s", latestMinglePipeline.getName(), latestMinglePipeline.getCounter(), STAGE_NAME, latestMinglePipeline.getStages().last().getCounter());
        MaterialRevision mingleMaterialRevision = new MaterialRevision(new DependencyMaterial(mingleMaterialConfig), true, new Modification(latestMinglePipeline.getModifiedDate(), revision, latestMinglePipeline.getLabel(), latestMinglePipeline.getId()));

        MaterialRevision gitMaterialRevision = new MaterialRevision(gitMaterial, gitTestRepo.checkInOneFile("new_file.c", "Adding a new file"));
        MaterialRevisions initialMaterialRevisions = new MaterialRevisions(mingleMaterialRevision, gitMaterialRevision);
        dbHelper.saveRevs(initialMaterialRevisions);
        Pipeline latestDownstreamInstance = PipelineMother.schedule(downstreamPipelineConfig, BuildCause.createManualForced(initialMaterialRevisions, new Username(new CaseInsensitiveString("loser"))));
        latestDownstreamInstance = pipelineDao.saveWithStages(latestDownstreamInstance);
        dbHelper.passStage(latestDownstreamInstance.getStages().first());

        //make a commit on the git repo
        List<Modification> newGitModifications = gitTestRepo.checkInOneFile("another_file.c", "Adding a new file");
        MaterialRevision materialRevision = new MaterialRevision(gitMaterial, newGitModifications);
        MaterialRevisions gitMaterialRevisions = new MaterialRevisions(materialRevision);
        dbHelper.saveRevs(gitMaterialRevisions);

        //schedule the pipeline
        pipelineTimeline.update();
        scheduleHelper.autoSchedulePipelinesWithRealMaterials(downstreamPipelineName);
        assertThat(pipelineScheduleQueue.toBeScheduled().keySet()).contains(new CaseInsensitiveString(downstreamPipelineName));
    }

    @Test
    public void shouldNotScheduleDownStreamPipeline_withSCMAndDependencyMaterials_whenDependencyMaterialHasChanges() throws Exception {
        String downstreamPipelineName = "downstream_pipeline";
        //first upstream pipeline - "mingle"
        DependencyMaterialConfig mingleMaterialConfig = new DependencyMaterialConfig(new CaseInsensitiveString(MINGLE_PIPELINE_NAME), new CaseInsensitiveString(STAGE_NAME));
        mingleMaterialConfig.ignoreForScheduling(true);

        //setup the pipeline
        PipelineConfig downstreamPipelineConfig = configHelper.addPipeline(downstreamPipelineName, STAGE_NAME, new MaterialConfigs(mingleMaterialConfig, gitMaterial.config()), "unit");
        Pipeline latestMinglePipeline = minglePipeline.latest;
        String revision = String.format("%s/%s/%s/%s", latestMinglePipeline.getName(), latestMinglePipeline.getCounter(), STAGE_NAME, latestMinglePipeline.getStages().last().getCounter());
        MaterialRevision mingleMaterialRevision = new MaterialRevision(new DependencyMaterial(mingleMaterialConfig), true, new Modification(latestMinglePipeline.getModifiedDate(), revision, latestMinglePipeline.getLabel(), latestMinglePipeline.getId()));

        MaterialRevision gitMaterialRevision = new MaterialRevision(gitMaterial, gitTestRepo.checkInOneFile("new_file.c", "Adding a new file"));
        MaterialRevisions initialMaterialRevisions = new MaterialRevisions(mingleMaterialRevision, gitMaterialRevision);
        dbHelper.saveRevs(initialMaterialRevisions);
        Pipeline latestDownstreamInstance = PipelineMother.schedule(downstreamPipelineConfig, BuildCause.createManualForced(initialMaterialRevisions, new Username(new CaseInsensitiveString("loser"))));
        latestDownstreamInstance = pipelineDao.saveWithStages(latestDownstreamInstance);
        dbHelper.passStage(latestDownstreamInstance.getStages().first());

        //trigger upstream pipelines
        MaterialRevisions newRevs = checkinFile(svnMaterial, "bar.c", svnRepository);
        minglePipeline.runAndPassWith(newRevs);
        pipelineTimeline.update();
        scheduleHelper.autoSchedulePipelinesWithRealMaterials(downstreamPipelineName);
        assertThat(pipelineScheduleQueue.toBeScheduled().keySet()).doesNotContain(new CaseInsensitiveString(downstreamPipelineName));
    }

    private MaterialRevisions checkinFile(SvnMaterial svn, String checkinFile, final SvnTestRepo svnRepository) throws Exception {
        svnRepository.checkInOneFile(checkinFile);
        materialDatabaseUpdater.updateMaterial(svn);
        return materialRepository.findLatestModification(svn);
    }

}
