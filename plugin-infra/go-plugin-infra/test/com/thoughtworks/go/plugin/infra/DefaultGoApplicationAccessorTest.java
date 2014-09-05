package com.thoughtworks.go.plugin.infra;

import com.thoughtworks.go.plugin.api.request.GoApiRequest;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

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

    @Test
    public void shouldHandleExceptionThrownByProcessor() throws Exception {
        String api = "api-uri";
        GoPluginApiRequestProcessor processor = mock(GoPluginApiRequestProcessor.class);
        GoApiRequest goApiRequest = mock(GoApiRequest.class);
        when(goApiRequest.api()).thenReturn(api);
        Throwable cause = new RuntimeException("error");
        when(processor.process(goApiRequest)).thenThrow(cause);

        DefaultGoApplicationAccessor goApplicationAccessor = new DefaultGoApplicationAccessor();
        goApplicationAccessor.registerProcessorFor(api, processor);
        try {
            goApplicationAccessor.submit(goApiRequest);
        } catch (Exception e) {
            assertThat(e.getMessage(), is(String.format("Error while processing request api %s", api)));
            assertThat(e.getCause(), is(cause));
        }
    }
}