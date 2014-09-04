package com.thoughtworks.go.plugin.infra;

import com.thoughtworks.go.plugin.api.request.GoApiRequest;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultGoApplicationAccessorTest {

    @Test
    public void shouldAllowProcessorToProcessApiRequestFromPlugin() throws Exception {
        String api = "api-uri";
        GoPluginApiRequestProcessor processor = mock(GoPluginApiRequestProcessor.class);
        GoApiRequest goApiRequest = mock(GoApiRequest.class);
        when(goApiRequest.api()).thenReturn(api);

        DefaultGoApplicationAccessor goApplicationAccessor = new DefaultGoApplicationAccessor();
        goApplicationAccessor.registerProcessorFor(api, processor);
        goApplicationAccessor.submit(goApiRequest);

        verify(processor).process(goApiRequest);
    }
}