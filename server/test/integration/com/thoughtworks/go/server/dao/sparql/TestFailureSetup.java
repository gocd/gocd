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

package com.thoughtworks.go.server.dao.sparql;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobStateTransition;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.helper.PipelineMother;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.domain.PipelineTimeline;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.studios.shine.cruise.builder.JunitXML;
import com.thoughtworks.studios.shine.net.StubGoURLRepository;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import static com.thoughtworks.studios.shine.cruise.builder.JunitXML.junitXML;

public class TestFailureSetup {
    private final MaterialRepository materialRepository;
    private final TransactionTemplate transactionTemplate;
    private final HgMaterial hgMaterial;
    private final PipelineConfig pipelineConfig;
    private final DatabaseAccessHelper dbHelper;
    private final PipelineTimeline pipelineTimeline;

    public TestFailureSetup(MaterialRepository materialRepository, DatabaseAccessHelper dbHelper, PipelineTimeline pipelineTimeline, GoConfigFileHelper configHelper,
                            TransactionTemplate transactionTemplate) {
        this.materialRepository = materialRepository;
        this.transactionTemplate = transactionTemplate;
        this.hgMaterial = new HgMaterial("http://google.com", null);
        this.pipelineConfig = PipelineMother.createPipelineConfig(UUID.randomUUID().toString(), new MaterialConfigs(hgMaterial.config()), "bar-stage");
        configHelper.addPipeline(pipelineConfig);
        this.dbHelper = dbHelper;
        this.pipelineTimeline = pipelineTimeline;
    }

    public String pipelineName(){
        return pipelineConfig.name().toString();
    }

    public SavedStage setupPipelineInstance(final boolean failStage, final String overriddenLabel, final StubGoURLRepository goURLRepository) {
        TestResultsStubbing resultStubbing = new TestResultsStubbing() {
            public void stub(Stage stage) {
                JunitXML junit1 = junitXML("testSuite1", 2).errored(2).failed(1);
                junit1.registerStubContent(goURLRepository, "pipelines/" + stage.getJobInstances().get(0).getIdentifier().artifactLocator("junit") + "/junit/");

                JunitXML junit2 = junitXML("testSuite1", 1).failed(1);
                junit2.registerStubContent(goURLRepository, "pipelines/" + stage.getJobInstances().get(1).getIdentifier().artifactLocator("junit") + "/junit/");
            }
        };
        return setupPipelineInstance(failStage, overriddenLabel, resultStubbing, new Date());
    }

    private SavedStage setupPipelineInstance(boolean failStage, String overriddenLabel, TestResultsStubbing resultStubbing, final Date latestTransitionDate) {
        Modification modification = new Modification("user", "comment", "foo@bar.com", latestTransitionDate, UUID.randomUUID().toString());
        return setupPipelineInstance(failStage, overriddenLabel, Arrays.asList(modification), resultStubbing, latestTransitionDate);
    }

    public SavedStage setupPipelineInstanceWithoutTestXmlStubbing(boolean failStage, String overriddenLabel, final Date latestTransitionDate) {
        return setupPipelineInstance(failStage, overriddenLabel, new TestResultsStubbing() {
            public void stub(Stage stage) {}
        }, latestTransitionDate);
    }

    public SavedStage setupPipelineInstance(boolean failStage, String overriddenLabel, List<Modification> modifications, TestResultsStubbing test, final Date latestTransitionDate) {
        return setupPipelineInstnace(failStage, overriddenLabel, modifications, test, latestTransitionDate);
    }

    private SavedStage setupPipelineInstnace(final boolean failStage, final String overriddenLabel, final List<Modification> modifications, final TestResultsStubbing test,
                                             final Date latestTransitionDate) {
        return (SavedStage) transactionTemplate.execute(new TransactionCallback() {
            public Object doInTransaction(TransactionStatus status) {
                MaterialInstance materialInstance = materialRepository.findOrCreateFrom(hgMaterial);

                for (Modification mod : modifications) {
                    mod.setMaterialInstance(materialInstance);
                }
                MaterialRevision rev = new MaterialRevision(hgMaterial, modifications);
                materialRepository.saveMaterialRevision(rev);
                Pipeline pipeline = PipelineMother.schedule(pipelineConfig, BuildCause.createManualForced(new MaterialRevisions(rev), new Username(new CaseInsensitiveString("loser"))));
                if (overriddenLabel != null) {
                    pipeline.setLabel(overriddenLabel);
                }


                for (JobInstance instance : pipeline.getStages().get(0).getJobInstances()) {
                    for (JobStateTransition jobStateTransition : instance.getTransitions()) {
                        jobStateTransition.setStateChangeTime(latestTransitionDate);
                    }
                }

                dbHelper.save(pipeline);
                Stage barStage = pipeline.getFirstStage();
                if (failStage) {
                    dbHelper.failStage(barStage, latestTransitionDate);
                }

                test.stub(barStage);

                pipelineTimeline.update();

                return new SavedStage(pipeline);
            }
        });
    }

    public static interface TestResultsStubbing {
        void stub(Stage stage);
    }

    public static final class SavedStage {
        public final Pipeline pipeline;
        public final StageIdentifier stageId;
        public final Stage stage;

        public SavedStage(Pipeline pipeline) {
            this.pipeline = pipeline;
            this.stage = pipeline.getStages().get(0);
            this.stageId = stage.getIdentifier();
        }
    }
}
