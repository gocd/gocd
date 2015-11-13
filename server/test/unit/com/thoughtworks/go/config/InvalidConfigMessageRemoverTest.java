package com.thoughtworks.go.config;

import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class InvalidConfigMessageRemoverTest {
    @Test
    public void shouldRemoveServerHealthServiceMessageAboutStartedWithInvalidConfigurationOnPipelineConfigSave() {
        ServerHealthService serverHealthService = mock(ServerHealthService.class);
        InvalidConfigMessageRemover invalidConfigMessageRemover = new InvalidConfigMessageRemover(mock(GoConfigService.class), serverHealthService);

        invalidConfigMessageRemover.onPipelineConfigChange(mock(PipelineConfig.class), "g1");

        ArgumentCaptor<HealthStateScope> captor = ArgumentCaptor.forClass(HealthStateScope.class);
        verify(serverHealthService).removeByScope(captor.capture());
        assertThat(captor.getValue().compareTo(HealthStateScope.forInvalidConfig()), is(0));
    }
}