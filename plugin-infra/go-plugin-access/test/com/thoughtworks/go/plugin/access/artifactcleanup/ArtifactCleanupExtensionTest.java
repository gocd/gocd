package com.thoughtworks.go.plugin.access.artifactcleanup;

import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.request.DefaultGoPluginApiRequest;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.go.plugin.infra.DefaultPluginManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.OngoingStubbing;

import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class ArtifactCleanupExtensionTest {

    private ArtifactCleanupExtension cleanupExtension;
    private DefaultPluginManager pluginManager;


    @Before
    public void setUp() throws Exception {
        pluginManager = mock(DefaultPluginManager.class);
        cleanupExtension = new ArtifactCleanupExtension(pluginManager);
    }

    @Test
    public void shouldGetListOfStagesAHandledByArtifactCleanupExtensionPlugins() throws Exception {
        String pluginId = "plugin-id";
        mockPluginManagerCalls(pluginId, "[{\"stageName\":\"stage-one\",\"pipelineName\":\"pipeline-one\"},{\"stageName\":\"stage-two\",\"pipelineName\":\"pipeline-one\"}]");

        List<StageConfigDetailsArtifactCleanup> stageConfigDetailsArtifactCleanups = cleanupExtension.listOfStagesHandledByExtension();

        assertThat(stageConfigDetailsArtifactCleanups.size(), is(2));

        assertThat(stageConfigDetailsArtifactCleanups.get(0).getStageName(), is("stage-one"));
        assertThat(stageConfigDetailsArtifactCleanups.get(0).getPipelineName(), is("pipeline-one"));

        assertThat(stageConfigDetailsArtifactCleanups.get(1).getStageName(), is("stage-two"));
        assertThat(stageConfigDetailsArtifactCleanups.get(1).getPipelineName(), is("pipeline-one"));

        verifyPluginManagerSubmitCall(pluginId, ArtifactCleanupExtension.REQUEST_LIST_OF_STAGES_HANDLED);
    }

    @Test
    public void shouldContinueToOtherPluginsIfOneOfThePluginReturnUnSuccessfulResponseWhileGettingStageConfigDetails() throws Exception {
        GoPluginIdentifier pluginOne = new GoPluginIdentifier("one", ArtifactCleanupExtension.NAME, asList("1.0"));
        GoPluginIdentifier pluginTwo = new GoPluginIdentifier("two", ArtifactCleanupExtension.NAME, asList("1.0"));
        when(pluginManager.allPluginsOfType(ArtifactCleanupExtension.NAME)).thenReturn(asList(pluginOne, pluginTwo));
        when(pluginManager.submitTo(eq("one"), any(GoPluginApiRequest.class))).thenReturn(DefaultGoPluginApiResponse.badRequest(""));
        when(pluginManager.submitTo(eq("two"), any(GoPluginApiRequest.class))).thenReturn(DefaultGoPluginApiResponse.success("[{\"stageName\":\"stage-one\",\"pipelineName\":\"pipeline-one\"}]"));

        List<StageConfigDetailsArtifactCleanup> list = cleanupExtension.listOfStagesHandledByExtension();

        assertThat(list.size(), is(1));
        assertThat(list.get(0).getPipelineName(), is("pipeline-one"));
        assertThat(list.get(0).getStageName(), is("stage-one"));
    }

    @Test
    public void shouldContinueToOtherPluginsIfExceptionRaisedWhenProcessingOnOfThemWhileGettingStageConfigDetails() throws Exception {
        GoPluginIdentifier pluginOne = new GoPluginIdentifier("one", ArtifactCleanupExtension.NAME, asList("1.0"));
        GoPluginIdentifier pluginTwo = new GoPluginIdentifier("two", ArtifactCleanupExtension.NAME, asList("1.0"));
        when(pluginManager.allPluginsOfType(ArtifactCleanupExtension.NAME)).thenReturn(asList(pluginOne, pluginTwo));
        when(pluginManager.submitTo(eq("one"), any(GoPluginApiRequest.class))).thenThrow(new RuntimeException("plugin throws exception"));
        when(pluginManager.submitTo(eq("two"), any(GoPluginApiRequest.class))).thenReturn(DefaultGoPluginApiResponse.success("[{\"stageName\":\"stage-one\",\"pipelineName\":\"pipeline-one\"}]"));

        List<StageConfigDetailsArtifactCleanup> list = cleanupExtension.listOfStagesHandledByExtension();

        assertThat(list.size(), is(1));
        assertThat(list.get(0).getPipelineName(), is("pipeline-one"));
        assertThat(list.get(0).getStageName(), is("stage-one"));
    }

    @Test
    public void shouldGetStageInstanceDetailsForArtifactCleanup() throws Exception {
        String pluginId = "plugin-id";
        mockPluginManagerCalls(pluginId, "[{\"stageId\":\"1\", \"stageName\":\"stage\",\"stageCounter\":\"1\",\"pipelineName\":\"pipeline\",\"pipelineCounter\":\"1\"}]");

        List<StageDetailsArtifactCleanup> stageDetailsArtifactCleanups = cleanupExtension.listOfStageInstanceIdsForArtifactDeletion();

        assertThat(stageDetailsArtifactCleanups.size(), is(1));
        assertStageDetailsArtifactCleanup(stageDetailsArtifactCleanups.get(0), 1L, "pipeline", 1, "stage", 1);
        verifyPluginManagerSubmitCall(pluginId, ArtifactCleanupExtension.REQUEST_LIST_OF_STAGES_INSTANCES);

    }


    @Test
    public void shouldGetStageInstanceDetailsForArtifactCleanupWithIncludePaths() throws Exception {
        String pluginId = "plugin-id";
        mockPluginManagerCalls(pluginId, "[{\"stageId\":\"1\", \"stageName\":\"stage\",\"stageCounter\":\"1\",\"pipelineName\":\"pipeline\",\"pipelineCounter\":\"1\",\"includePaths\":[\"a.txt\",\"b.txt\"]}]");

        List<StageDetailsArtifactCleanup> stageDetailsArtifactCleanups = cleanupExtension.listOfStageInstanceIdsForArtifactDeletion();

        assertThat(stageDetailsArtifactCleanups.size(), is(1));
        assertStageDetailsArtifactCleanup(stageDetailsArtifactCleanups.get(0), 1L, "pipeline", 1, "stage", 1);
        assertThat(stageDetailsArtifactCleanups.get(0).getArtifactsPathsToBeDeleted(), hasItems("a.txt", "b.txt"));
        verifyPluginManagerSubmitCall(pluginId, ArtifactCleanupExtension.REQUEST_LIST_OF_STAGES_INSTANCES);
    }


    @Test
    public void shouldGetStageInstanceDetailsForArtifactCleanupWithExcludePaths() throws Exception {
        String pluginId = "plugin-id";
        mockPluginManagerCalls(pluginId, "[{\"stageId\":\"1\", \"stageName\":\"stage\",\"stageCounter\":\"1\",\"pipelineName\":\"pipeline\",\"pipelineCounter\":\"1\",\"excludePaths\":[\"a.txt\",\"b.txt\"]}]");

        List<StageDetailsArtifactCleanup> stageDetailsArtifactCleanups = cleanupExtension.listOfStageInstanceIdsForArtifactDeletion();

        assertThat(stageDetailsArtifactCleanups.size(), is(1));
        assertStageDetailsArtifactCleanup(stageDetailsArtifactCleanups.get(0), 1L, "pipeline", 1, "stage", 1);
        assertThat(stageDetailsArtifactCleanups.get(0).getArtifactsPathsToBeRetained(), hasItems("a.txt", "b.txt"));
        verifyPluginManagerSubmitCall(pluginId, ArtifactCleanupExtension.REQUEST_LIST_OF_STAGES_INSTANCES);
    }

    @Test
    public void shouldUseExcludePathsWhenBothIncludeAndExcludePathProvided() throws Exception {
        String pluginId = "plugin-id";
        mockPluginManagerCalls(pluginId, "[{\"stageId\":\"1\", \"stageName\":\"stage\",\"stageCounter\":\"1\",\"pipelineName\":\"pipeline\",\"pipelineCounter\":\"1\",\"includePaths\":[\"a.txt\",\"b.txt\"],\"excludePaths\":[\"a.txt\",\"b.txt\"]}]");

        List<StageDetailsArtifactCleanup> stageDetailsArtifactCleanups = cleanupExtension.listOfStageInstanceIdsForArtifactDeletion();

        assertThat(stageDetailsArtifactCleanups.size(), is(1));
        assertStageDetailsArtifactCleanup(stageDetailsArtifactCleanups.get(0), 1L, "pipeline", 1, "stage", 1);
        assertThat(stageDetailsArtifactCleanups.get(0).getArtifactsPathsToBeRetained(), hasItems("a.txt", "b.txt"));
        verifyPluginManagerSubmitCall(pluginId, ArtifactCleanupExtension.REQUEST_LIST_OF_STAGES_INSTANCES);
    }

    @Test
    public void shouldContinueToOtherPluginsIfOneOfThePluginReturnUnSuccessfulResponseWhileGettingStageInstanceDetails() throws Exception {
        GoPluginIdentifier pluginOne = new GoPluginIdentifier("one", ArtifactCleanupExtension.NAME, asList("1.0"));
        GoPluginIdentifier pluginTwo = new GoPluginIdentifier("two", ArtifactCleanupExtension.NAME, asList("1.0"));
        when(pluginManager.allPluginsOfType(ArtifactCleanupExtension.NAME)).thenReturn(asList(pluginOne, pluginTwo));
        when(pluginManager.submitTo(eq("one"), any(GoPluginApiRequest.class))).thenReturn(DefaultGoPluginApiResponse.badRequest(""));
        when(pluginManager.submitTo(eq("two"), any(GoPluginApiRequest.class))).thenReturn(
                DefaultGoPluginApiResponse.success("[{\"stageId\":\"1\", \"stageName\":\"stage\",\"stageCounter\":\"1\",\"pipelineName\":\"pipeline\",\"pipelineCounter\":\"1\"}]"));

        List<StageDetailsArtifactCleanup> list = cleanupExtension.listOfStageInstanceIdsForArtifactDeletion();

        assertThat(list.size(), is(1));
        assertThat(list.get(0).getId(), is(1L));
        assertThat(list.get(0).getPipelineName(), is("pipeline"));
        assertThat(list.get(0).getStageName(), is("stage"));
    }

    @Test
    public void shouldContinueToOtherPluginsIfExceptionRaisedWhenProcessingOnOfThemWhileGettingStageInstanceDetails() throws Exception {
        GoPluginIdentifier pluginOne = new GoPluginIdentifier("one", ArtifactCleanupExtension.NAME, asList("1.0"));
        GoPluginIdentifier pluginTwo = new GoPluginIdentifier("two", ArtifactCleanupExtension.NAME, asList("1.0"));
        when(pluginManager.allPluginsOfType(ArtifactCleanupExtension.NAME)).thenReturn(asList(pluginOne, pluginTwo));
        when(pluginManager.submitTo(eq("one"), any(GoPluginApiRequest.class))).thenThrow(new RuntimeException("plugin throws exception"));
        when(pluginManager.submitTo(eq("two"), any(GoPluginApiRequest.class))).thenReturn(
                DefaultGoPluginApiResponse.success("[{\"stageId\":\"1\", \"stageName\":\"stage\",\"stageCounter\":\"1\",\"pipelineName\":\"pipeline\",\"pipelineCounter\":\"1\"}]"));

        List<StageDetailsArtifactCleanup> list = cleanupExtension.listOfStageInstanceIdsForArtifactDeletion();

        assertThat(list.size(), is(1));
        assertThat(list.get(0).getId(), is(1L));
        assertThat(list.get(0).getPipelineName(), is("pipeline"));
        assertThat(list.get(0).getStageName(), is("stage"));
    }

    private void mockPluginManagerCalls(String pluginId, String expectedPluginResponse) {
        OngoingStubbing<List<GoPluginIdentifier>> listOngoingStubbing = when(pluginManager.allPluginsOfType(ArtifactCleanupExtension.NAME)).thenReturn(asList(new GoPluginIdentifier(pluginId, ArtifactCleanupExtension.NAME, asList("1.0"))));
        GoPluginApiResponse response = mock(GoPluginApiResponse.class);
        when(pluginManager.submitTo(eq(pluginId), any(DefaultGoPluginApiRequest.class))).thenReturn(response);
        when(response.responseBody()).thenReturn(expectedPluginResponse);
        when(response.responseCode()).thenReturn(200);
    }


    private void assertStageDetailsArtifactCleanup(StageDetailsArtifactCleanup stageDetailsArtifactCleanup, long stageId, String pipelineName, int pipelineCounter, String stageName, int stageCounter) {
        assertThat(stageDetailsArtifactCleanup.getId(), is(stageId));
        assertThat(stageDetailsArtifactCleanup.getPipelineName(), is(pipelineName));
        assertThat(stageDetailsArtifactCleanup.getPipelineCounter(), is(pipelineCounter));
        assertThat(stageDetailsArtifactCleanup.getStageName(), is(stageName));
        assertThat(stageDetailsArtifactCleanup.getStageCounter(), is(stageCounter));
    }


    private void verifyPluginManagerSubmitCall(String pluginId, String requestName) {
        ArgumentCaptor<DefaultGoPluginApiRequest> pluginApiRequestArgumentCaptor = ArgumentCaptor.forClass(DefaultGoPluginApiRequest.class);

        verify(pluginManager).submitTo(eq(pluginId), pluginApiRequestArgumentCaptor.capture());
        assertThat(pluginApiRequestArgumentCaptor.getValue().extension(), is(ArtifactCleanupExtension.NAME));
        assertThat(pluginApiRequestArgumentCaptor.getValue().requestName(), is(requestName));
        assertThat(pluginApiRequestArgumentCaptor.getValue().extensionVersion(), is("1.0"));
    }
}