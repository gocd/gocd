package com.thoughtworks.go.server.pluginrequestprocessor;

import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.plugin.api.request.GoApiRequest;
import com.thoughtworks.go.plugin.api.response.GoApiResponse;
import com.thoughtworks.go.plugin.infra.DefaultGoApplicationAccessor;
import com.thoughtworks.go.server.service.StageService;
import org.junit.Before;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;

import static com.thoughtworks.go.helper.StageMother.custom;
import static com.thoughtworks.go.util.json.JsonHelper.DATE_FORMAT;
import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class StageHistoryRequestProcessorTest {

    private StageService stageService;
    private DefaultGoApplicationAccessor defaultGoApplicationAccessor;
    private StageHistoryRequestProcessor requestProcessor;

    @Before
    public void setUp() throws Exception {
        stageService = mock(StageService.class);
        defaultGoApplicationAccessor = mock(DefaultGoApplicationAccessor.class);
        requestProcessor = new StageHistoryRequestProcessor(defaultGoApplicationAccessor, stageService);
    }

    @Test
    public void shouldRegisterWithGoApplicationAccessor() throws Exception {
        verify(defaultGoApplicationAccessor).registerProcessorFor(StageHistoryRequestProcessor.REQUEST, requestProcessor);
    }

    @Test
    public void shouldReturnResponseWithIncompleteRequestMessageWhenStageNameIsMissing() throws Exception {
        HashMap<String, String> requestParams = new HashMap<String, String>();
        requestParams.put(StageHistoryRequestProcessor.PIPELINE_NAME, "pipeline");
        GoApiRequest goApiRequest = mock(GoApiRequest.class);
        when(goApiRequest.requestParameters()).thenReturn(requestParams);

        GoApiResponse response = requestProcessor.process(goApiRequest);

        assertThat(response.responseCode(), is(412));
        assertThat(response.responseBody(), is("Expected to provide both pipeline name and stage name"));
        verify(stageService, never()).getStagesWithArtifactsGivenPipelineAndStage(anyString(), anyString(), anyLong());
    }

    @Test
    public void shouldReturnResponseWithIncompleteRequestMessageWhenPipelineNameIsMissing() throws Exception {
        HashMap<String, String> requestParams = new HashMap<String, String>();
        requestParams.put(StageHistoryRequestProcessor.STAGE_NAME, "stage");
        GoApiRequest goApiRequest = mock(GoApiRequest.class);
        when(goApiRequest.requestParameters()).thenReturn(requestParams);

        GoApiResponse response = requestProcessor.process(goApiRequest);

        assertThat(response.responseCode(), is(412));
        assertThat(response.responseBody(), is("Expected to provide both pipeline name and stage name"));
        verify(stageService, never()).getStagesWithArtifactsGivenPipelineAndStage(anyString(), anyString(), anyLong());
    }

    @Test
    public void shouldReturnResponseWithIncompleteRequestMessageWhenInvalidFromIdIsProvided() throws Exception {
        HashMap<String, String> requestParams = new HashMap<String, String>();
        requestParams.put(StageHistoryRequestProcessor.PIPELINE_NAME, "pipeline");
        requestParams.put(StageHistoryRequestProcessor.STAGE_NAME, "stage");
        requestParams.put(StageHistoryRequestProcessor.FROM_ID, "junk");
        GoApiRequest goApiRequest = mock(GoApiRequest.class);
        when(goApiRequest.requestParameters()).thenReturn(requestParams);

        GoApiResponse response = requestProcessor.process(goApiRequest);

        assertThat(response.responseCode(), is(412));
        assertThat(response.responseBody(), is("Invalid from-id"));
        verify(stageService, never()).getStagesWithArtifactsGivenPipelineAndStage(anyString(), anyString(), anyLong());
    }

    @Test
    public void shouldReturnSuccessResponseWhenPipelineNameAndStageNameProvided() throws Exception {
        HashMap<String, String> requestParams = new HashMap<String, String>();
        requestParams.put(StageHistoryRequestProcessor.PIPELINE_NAME, "pipeline");
        requestParams.put(StageHistoryRequestProcessor.STAGE_NAME, "stage");
        GoApiRequest goApiRequest = mock(GoApiRequest.class);
        when(goApiRequest.requestParameters()).thenReturn(requestParams);
        when(stageService.getStagesWithArtifactsGivenPipelineAndStage("pipeline", "stage")).thenReturn(new ArrayList<Stage>());

        GoApiResponse response = requestProcessor.process(goApiRequest);

        assertThat(response.responseCode(), is(200));
        assertThat(response.responseBody(), is("[]"));
    }

    @Test
    public void shouldReturnSuccessResponseWhenPipelineNameAndStageNameAndFromIdProvided() throws Exception {
        HashMap<String, String> requestParams = new HashMap<String, String>();
        requestParams.put(StageHistoryRequestProcessor.PIPELINE_NAME, "pipeline");
        requestParams.put(StageHistoryRequestProcessor.STAGE_NAME, "stage");
        requestParams.put(StageHistoryRequestProcessor.FROM_ID, "100");
        GoApiRequest goApiRequest = mock(GoApiRequest.class);
        when(goApiRequest.requestParameters()).thenReturn(requestParams);
        Stage stage = custom("stage");
        stage.setPipelineId(1L);
        when(stageService.getStagesWithArtifactsGivenPipelineAndStage("pipeline", "stage", 100L)).thenReturn(asList(stage));

        GoApiResponse response = requestProcessor.process(goApiRequest);

        assertThat(response.responseCode(), is(200));
        String expectedResponseBody = String.format(
                "[{\"stageId\":\"%s\",\"pipelineCounter\":\"1\",\"stageName\":\"stage\",\"stageCounter\":\"1\",\"pipelineId\":\"1\",\"lastTransitionTime\":\"%s\",\"pipelineName\":\"pipeline-name\",\"stageResult\":\"Passed\"}]",
                stage.getId(),new SimpleDateFormat(DATE_FORMAT).format(stage.getLastTransitionedTime()));
        assertThat(response.responseBody(), is(expectedResponseBody));
    }
}