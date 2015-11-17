package com.thoughtworks.go.config;

import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.listener.EntityConfigChangedListener;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Type;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class InvalidConfigMessageRemoverTest {
    @Test
    public void shouldRemoveServerHealthServiceMessageAboutStartedWithInvalidConfigurationOnPipelineConfigSave() {
        GoConfigService goConfigService = mock(GoConfigService.class);
        ServerHealthService serverHealthService = mock(ServerHealthService.class);
        ArgumentCaptor<ConfigChangedListener> configChangedListenerArgumentCaptor = ArgumentCaptor.forClass(ConfigChangedListener.class);
        doNothing().when(goConfigService).register(configChangedListenerArgumentCaptor.capture());
        InvalidConfigMessageRemover invalidConfigMessageRemover = new InvalidConfigMessageRemover(goConfigService, serverHealthService);
        invalidConfigMessageRemover.initialize();
        invalidConfigMessageRemover.onConfigChange(null);
        List<ConfigChangedListener> listeners = configChangedListenerArgumentCaptor.getAllValues();
        assertThat(listeners.get(1) instanceof EntityConfigChangedListener, is(true));
        EntityConfigChangedListener<PipelineConfig> pipelineConfigChangeListener= (EntityConfigChangedListener<PipelineConfig>) listeners.get(1);


        pipelineConfigChangeListener.onEntityConfigChange(mock(PipelineConfig.class));

        ArgumentCaptor<HealthStateScope> captor = ArgumentCaptor.forClass(HealthStateScope.class);
        verify(serverHealthService).removeByScope(captor.capture());
        assertThat(captor.getValue().compareTo(HealthStateScope.forInvalidConfig()), is(0));
    }

}