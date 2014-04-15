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
import java.util.List;

import com.googlecode.junit.ext.RunIf;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.GoConfigFileDao;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.ScmMaterial;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.domain.valuestreammap.Node;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helper.PipelineMother;
import com.thoughtworks.go.junitext.DatabaseChecker;
import com.thoughtworks.go.junitext.GoJUnitExtSpringRunner;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.presentation.models.ValueStreamMapPresentationModel;
import com.thoughtworks.go.server.service.result.DefaultLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(GoJUnitExtSpringRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class ValueStreamMapPerformanceTest {
    @Autowired private GoConfigFileDao goConfigFileDao;
    @Autowired private GoConfigService goConfigService;
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private ValueStreamMapService valueStreamMapService;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private TransactionTemplate transactionTemplate;

    private GoConfigFileHelper configHelper;
    private ScheduleTestUtil u;


    @Before
    public void setup() throws Exception {
        configHelper = new GoConfigFileHelper();
        dbHelper.onSetUp();
        configHelper.usingCruiseConfigDao(goConfigFileDao).initializeConfigFile();
        configHelper.onSetUp();
        goConfigService.forceNotifyListeners();
        u = new ScheduleTestUtil(transactionTemplate, materialRepository, dbHelper, configHelper);
    }

    @After
    public void tearDown() throws Exception {
        configHelper.onTearDown();
        dbHelper.onTearDown();
    }

    @Test
    public void shouldTestVSMForNPipelines() throws Exception {
        final int numberOfDownstreamPipelines = 5;
        final CruiseConfig cruiseConfig = setupVSM(numberOfDownstreamPipelines);
        ArrayList<Thread> ts = new ArrayList<Thread>();
        int numberOfParallelRequests = 10;
        for (int i = 0; i < numberOfParallelRequests; i++) {
            final int finalI = i;
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(5000);
                        doRun(numberOfDownstreamPipelines, cruiseConfig, "Thread" + finalI);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }, "Thread" + i);
            ts.add(t);
        }
        for (Thread t : ts) {
            t.start();
        }
        for (Thread t : ts) {
            t.join();
        }
    }

    @Test(timeout = 240000)
    @RunIf(value = DatabaseChecker.class, arguments = {DatabaseChecker.H2})
    public void shouldTestVSMForMeshInUpstreamAndDownstream() throws Exception {
        int numberOfNodesPerLevel = 5;
        int numberOfLevels = 5;
        int numberOfInstancesForUpstream = 1;
        int numberOfInstancesForDownstream = 10;

        ScmMaterial svn = u.wf((ScmMaterial) MaterialsMother.defaultMaterials().get(0), "folder1");
        String[] svn_revs = {"svn_1"};
        u.checkinInOrder(svn, svn_revs);

        PipelineConfig upstreamConfig = createPipelineWithInstances("upstream", new ArrayList<PipelineConfig>(), 1);
        PipelineConfig currentConfig = createMesh(upstreamConfig, "current", "up", numberOfInstancesForUpstream, numberOfNodesPerLevel, numberOfLevels);
        createMesh(currentConfig, "downstream", "down", numberOfInstancesForDownstream, numberOfNodesPerLevel, numberOfLevels);

        long start = System.currentTimeMillis();
        valueStreamMapService.getValueStreamMap("current", 1, Username.ANONYMOUS, new DefaultLocalizedOperationResult());
        long timeTaken = (System.currentTimeMillis() - start) / 1000;
        assertThat(String.format("VSM took %ds. Should have been generated in 30s.", timeTaken), timeTaken, Matchers.lessThan(30l));
    }

    private PipelineConfig createMesh(PipelineConfig startNode, String endNodeName, String pipelineNameSuffix, int numberOfInstances, int numberOfNodesPerLevel, int numberOfLevels) {
        List<PipelineConfig> previousNodes = new ArrayList<PipelineConfig>();
        previousNodes.add(startNode);
        List<PipelineConfig> currentNodes = new ArrayList<PipelineConfig>();
        for (int i = 1; i <= numberOfLevels; i++) {
            for (int j = 1; j <= numberOfNodesPerLevel; j++) {
                String pipelineName = String.format("pipeline_%s_%d_%d", pipelineNameSuffix, i, j);
                PipelineConfig pipelineConfig = createPipelineWithInstances(pipelineName, previousNodes, numberOfInstances);
                currentNodes.add(pipelineConfig);
            }
            previousNodes = currentNodes;
            currentNodes = new ArrayList<PipelineConfig>();
        }
        return createPipelineWithInstances(endNodeName, previousNodes, numberOfInstances);
    }

    private PipelineConfig createPipelineWithInstances(String pipelineName, List<PipelineConfig> previousNodes, int numberOfInstances) {
        PipelineConfig pipelineConfig = getPipelineWithName(pipelineName, previousNodes);
        configHelper.addPipeline(pipelineConfig);

        createInstances(numberOfInstances, previousNodes, pipelineConfig);
        return pipelineConfig;
    }

    private void createInstances(int numberOfInstances, List<PipelineConfig> previousNodes, PipelineConfig pipelineConfig) {
        for (int k = 1; k <= numberOfInstances; k++) {
            List<String> previousRevisions = new ArrayList<String>();
            previousRevisions.add("svn_1");
            int instanceCount = previousNodes.size() == 1 ? 1 : k;
            for (int x = 0; x < previousNodes.size(); x++) {
                previousRevisions.add(String.format("%s/%d/stage/1", previousNodes.get(x).name().toString(), instanceCount));
            }
            u.runAndPass(new ScheduleTestUtil.AddedPipeline(pipelineConfig, new DependencyMaterial(pipelineConfig.name(), new CaseInsensitiveString("stage"))),
                    previousRevisions.toArray(new String[previousRevisions.size()]));
        }
    }

    private void addDependencyMaterials(PipelineConfig pipelineConfig, List<PipelineConfig> pipelineDependencies) {
        for (PipelineConfig pipelineDependency : pipelineDependencies) {
            pipelineConfig.addMaterialConfig(new DependencyMaterialConfig(pipelineDependency.name(), new CaseInsensitiveString("stage")));
        }
    }

    private PipelineConfig getPipelineWithName(String startNode) {
        return PipelineMother.createPipelineConfig(startNode, MaterialConfigsMother.defaultMaterialConfigs(), "stage");
    }

    private PipelineConfig getPipelineWithName(String startNode, List<PipelineConfig> materials) {
        PipelineConfig pipelineConfig = PipelineMother.createPipelineConfig(startNode, MaterialConfigsMother.defaultMaterialConfigs(), "stage");
        addDependencyMaterials(pipelineConfig, materials);
        return pipelineConfig;
    }

    private void doRun(int numberOfDownstreamPipelines, CruiseConfig cruiseConfig, String threadName) throws InterruptedException {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        for (PipelineConfig pipelineConfig : cruiseConfig.allPipelines()) {
            ValueStreamMapPresentationModel map = valueStreamMapService.getValueStreamMap(pipelineConfig.name().toString(), 1, Username.ANONYMOUS, result);
            assertThat(getAllNodes(map).size(), is(numberOfDownstreamPipelines + 2));
        }
    }

    private List<Node> getAllNodes(ValueStreamMapPresentationModel presentationModel) {
        ArrayList<Node> nodes = new ArrayList<Node>();
        List<List<Node>> nodesAtEachLevel = presentationModel.getNodesAtEachLevel();
        for (List<Node> nodesAtLevel : nodesAtEachLevel) {
            nodes.addAll(nodesAtLevel);
        }
        return nodes;
    }

    private CruiseConfig setupVSM(int numberOfDownstreamPipelines) {
        HgMaterial hg = new HgMaterial("hgurl", "folder");
        String hg_revs = "hg1";
        u.checkinInOrder(hg, hg_revs);
        ScheduleTestUtil.AddedPipeline up = u.saveConfigWith("u", new ScheduleTestUtil.MaterialDeclaration(hg, "dest"));
        String up_r = u.runAndPass(up, hg_revs);
        ScheduleTestUtil.AddedPipeline previouslyCreatedPipeline = up;
        String previousRun = up_r;

        for (int i = 0; i < numberOfDownstreamPipelines; i++) {
            DependencyMaterial dep = new DependencyMaterial(previouslyCreatedPipeline.config.name(), previouslyCreatedPipeline.config.get(0).name());
            ScheduleTestUtil.AddedPipeline d = u.saveConfigWith("d" + i, new ScheduleTestUtil.MaterialDeclaration(dep, "random"));
            String currentRun = u.runAndPass(d, previousRun);
            previouslyCreatedPipeline = d;
            previousRun = currentRun;
        }
        return goConfigFileDao.load();
    }


}
