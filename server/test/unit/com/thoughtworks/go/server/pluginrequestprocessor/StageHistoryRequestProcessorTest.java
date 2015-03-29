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
package com.thoughtworks.go.server.pluginrequestprocessor;

import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageConfigIdentifier;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.request.DefaultGoApiRequest;
import com.thoughtworks.go.plugin.api.response.GoApiResponse;
import com.thoughtworks.go.plugin.infra.DefaultGoApplicationAccessor;
import com.thoughtworks.go.server.service.StageService;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class StageHistoryRequestProcessorTest {

    private StageHistoryRequestProcessor requestProcessor;
    private DefaultGoApplicationAccessor goApplicationAccessor;
    private StageService stageService;

    @Before
    public void setUp() throws Exception {
        goApplicationAccessor = mock(DefaultGoApplicationAccessor.class);
        stageService = mock(StageService.class);
        requestProcessor = new StageHistoryRequestProcessor(goApplicationAccessor, stageService);

    }

    @Test
    public void shouldRegisterWithGoApplicationAccessor() throws Exception {
        StageHistoryRequestProcessor requestProcessor = new StageHistoryRequestProcessor(goApplicationAccessor, null);
        verify(goApplicationAccessor).registerProcessorFor("stage-history", requestProcessor);
    }

    @Test
    public void shouldGetStageHistoryForGivenRequest() throws Exception {
        GoPluginIdentifier pluginIdentifier = mock(GoPluginIdentifier.class);
        DefaultGoApiRequest request = new DefaultGoApiRequest("stage-history", "1.0", pluginIdentifier);

        request.setRequestBody("{\n" +
                "\t\"includeStages\" : [{\"pipeline\":\"p\",\"stage\":\"s1\"},{\"pipeline\":\"p\",\"stage\":\"s2\"}],\n" +
                "\t\"excludeStages\" : [{\"pipeline\":\"p\",\"stage\":\"s3\"},{\"pipeline\":\"p\",\"stage\":\"s4\"}],\n" +
                "\t\"fromId\":\"1\",\n" +
                "\t\"orderBy\":\"ASC\"\n" +
                "}");


        List<StageConfigIdentifier> includeStages = asList(new StageConfigIdentifier("p", "s1"), new StageConfigIdentifier("p", "s2"));
        List<StageConfigIdentifier> excludeStages = asList(new StageConfigIdentifier("p", "s3"), new StageConfigIdentifier("p", "s4"));
        List<Stage> stageInstanceList = asList(
                createStageInstance(1, "p", 1, "s1", "1", false),
                createStageInstance(2, "p", 1, "s2", "1", false),
                createStageInstance(3, "p", 1, "s1", "2", false),
                createStageInstance(4, "p", 1, "s2", "2", false),
                createStageInstance(5, "p", 2, "s1", "1", false),
                createStageInstance(6, "p", 2, "s2", "1", false),
                createStageInstance(7, "p", 2, "s1", "2", true),
                createStageInstance(8, "p", 2, "s2", "2", true)
        );
        when(stageService.getStagesWithArtifacts(includeStages, excludeStages, 1L, null, true)).thenReturn(stageInstanceList);


        Map<StageConfigIdentifier, Long> uncleanedArtifactCount = new HashMap<StageConfigIdentifier, Long>();
        uncleanedArtifactCount.put(new StageConfigIdentifier("p", "s1"), 2L);
        uncleanedArtifactCount.put(new StageConfigIdentifier("p", "s2"), 3L);
        when(stageService.getStagesInstanceCount(asList(new StageConfigIdentifier("p", "s1"), new StageConfigIdentifier("p", "s2")), true)).thenReturn(uncleanedArtifactCount);


        GoApiResponse response = requestProcessor.process(request);

        assertThat(response.responseBody(), Is.is("[{\"uncleanedArtifactsInstanceCount\":2,\"pipeline\":\"p\",\"name\":\"s1\",\"instances\":[{\"id\":1,\"result\":\"Unknown\",\"locator\":\"p/1/s1/1\",\"lastTransitionedTime\":\"Mar 4, 2015 12:24:15 PM\",\"latestRun\":false},{\"id\":3,\"result\":\"Unknown\",\"locator\":\"p/1/s1/2\",\"lastTransitionedTime\":\"Mar 4, 2015 12:24:15 PM\",\"latestRun\":false},{\"id\":5,\"result\":\"Unknown\",\"locator\":\"p/2/s1/1\",\"lastTransitionedTime\":\"Mar 4, 2015 12:24:15 PM\",\"latestRun\":false},{\"id\":7,\"result\":\"Unknown\",\"locator\":\"p/2/s1/2\",\"lastTransitionedTime\":\"Mar 4, 2015 12:24:15 PM\",\"latestRun\":true}]},{\"uncleanedArtifactsInstanceCount\":3,\"pipeline\":\"p\",\"name\":\"s2\",\"instances\":[{\"id\":2,\"result\":\"Unknown\",\"locator\":\"p/1/s2/1\",\"lastTransitionedTime\":\"Mar 4, 2015 12:24:15 PM\",\"latestRun\":false},{\"id\":4,\"result\":\"Unknown\",\"locator\":\"p/1/s2/2\",\"lastTransitionedTime\":\"Mar 4, 2015 12:24:15 PM\",\"latestRun\":false},{\"id\":6,\"result\":\"Unknown\",\"locator\":\"p/2/s2/1\",\"lastTransitionedTime\":\"Mar 4, 2015 12:24:15 PM\",\"latestRun\":false},{\"id\":8,\"result\":\"Unknown\",\"locator\":\"p/2/s2/2\",\"lastTransitionedTime\":\"Mar 4, 2015 12:24:15 PM\",\"latestRun\":true}]}]"));
    }

    private Stage createStageInstance(int id, String pipelineName, int pipelineCounter, String stageName, String stageCounter, boolean latestRun) {
        Stage stage = new Stage();
        stage.setId(id);
        stage.setCreatedTime(new Timestamp(1425452055169L));
        stage.setLatestRun(latestRun);
        stage.setIdentifier(new StageIdentifier(pipelineName, pipelineCounter, stageName, stageCounter));
        return stage;
    }
}