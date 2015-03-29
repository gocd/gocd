package com.thoughtworks.go.plugin.access.artifactcleanup;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class JsonMessageHandler1_0Test {

    private JsonMessageHandler1_0 messageHandler;

    @Before
    public void setUp() throws Exception {
        messageHandler = new JsonMessageHandler1_0();
    }

    @Test
    public void shouldSerializeToJsonString() throws Exception {
        ArtifactExtensionStageConfiguration stage = new ArtifactExtensionStageConfiguration("p", "s1");
        ArtifactExtensionStageConfiguration anotherStage = new ArtifactExtensionStageConfiguration("p", "s2");
        String response = messageHandler.requestGetStageInstancesForArtifactCleanup(asList(stage, anotherStage));
        assertThat(response, is("[{\"pipeline\":\"p\",\"stage\":\"s1\"},{\"pipeline\":\"p\",\"stage\":\"s2\"}]"));
    }

    @Test
    public void shouldDeSerializeResponse() throws Exception {
        String responseBody =
                "[{\"id\":\"1\",\"pipeline\":\"p\",\"stage\":\"s1\",\"pipeline-counter\":\"1\",\"stage-counter\":\"1\",\"include-paths\":[\"p/a\",\"p/b\"]}," +
                        "{\"id\":\"2\",\"pipeline\":\"p\",\"stage\":\"s2\",\"pipeline-counter\":\"1\",\"stage-counter\":\"1\",\"exclude-paths\":[\"p/c\",\"p/d\"]}]";

        List<ArtifactExtensionStageInstance> artifactExtensionStageInstances = messageHandler.responseGetStageInstancesForArtifactCleanup(responseBody);
        assertArtifactExtensionStageInstance(artifactExtensionStageInstances.get(0), 1L, "p", 1, "s1", "1");
        assertThat(artifactExtensionStageInstances.get(0).getIncludePaths(), is(asList("p/a", "p/b")));
        assertThat(artifactExtensionStageInstances.get(0).getExcludePaths().isEmpty(), is(true));
        assertArtifactExtensionStageInstance(artifactExtensionStageInstances.get(1), 2L, "p", 1, "s2", "1");
        assertThat(artifactExtensionStageInstances.get(1).getIncludePaths().isEmpty(), is(true));
        assertThat(artifactExtensionStageInstances.get(1).getExcludePaths(), is(asList("p/c", "p/d")));
    }

    private void assertArtifactExtensionStageInstance(ArtifactExtensionStageInstance artifactExtensionStageInstance, long id, String pipeline, int pipelineCounter, String stage, String stageCounter) {
        assertThat(artifactExtensionStageInstance.getId(), is(id));
        assertThat(artifactExtensionStageInstance.getPipelineName(), is(pipeline));
        assertThat(artifactExtensionStageInstance.getPipelineCounter(), is(pipelineCounter));
        assertThat(artifactExtensionStageInstance.getStageName(), is(stage));
        assertThat(artifactExtensionStageInstance.getStageCounter(), is(stageCounter));
    }
}