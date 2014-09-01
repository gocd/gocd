package com.thoughtworks.go.server.pluginrequestprocessor;

import com.thoughtworks.go.domain.StageConfigIdentifier;
import com.thoughtworks.go.plugin.api.request.GoApiRequest;
import com.thoughtworks.go.plugin.api.response.GoApiResponse;
import com.thoughtworks.go.plugin.infra.DefaultGoApplicationAccessor;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.StageService;
import org.junit.Before;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class StageListRequestProcessorTest {

    private StageListRequestProcessor requestProcessor;
    private StageService stageService;
    private DefaultGoApplicationAccessor defaultGoApplicationAccessor;
    private GoConfigService goConfigService;

    @Before
    public void setUp() throws Exception {
        defaultGoApplicationAccessor = mock(DefaultGoApplicationAccessor.class);
        stageService = mock(StageService.class);
        goConfigService = mock(GoConfigService.class);
        requestProcessor = new StageListRequestProcessor(defaultGoApplicationAccessor, stageService, goConfigService);
    }

    @Test
    public void shouldRegisterWithGoApplicationAccessor() throws Exception {
        verify(defaultGoApplicationAccessor).registerProcessorFor(StageListRequestProcessor.REQUEST, requestProcessor);
    }

    @Test
    public void shouldReturnSuccessResponse() throws Exception {
        GoApiRequest goApiRequest = mock(GoApiRequest.class);
        when(stageService.getAllDistinctStages()).thenReturn(asList(new StageConfigIdentifier("pipeline-one", "stage"), new StageConfigIdentifier("pipeline-two", "stage")));
        when(goConfigService.stageExists("pipeline-one", "stage")).thenReturn(true);
        when(goConfigService.stageExists("pipeline-two", "false")).thenReturn(false);

        GoApiResponse response = requestProcessor.process(goApiRequest);

        assertThat(response.responseCode(), is(200));
        assertThat(response.responseBody(),
                is("[{\"stageName\":\"stage\",\"existInConfig\":true,\"pipelineName\":\"pipeline-one\"},{\"stageName\":\"stage\",\"existInConfig\":false,\"pipelineName\":\"pipeline-two\"}]"));
    }
}