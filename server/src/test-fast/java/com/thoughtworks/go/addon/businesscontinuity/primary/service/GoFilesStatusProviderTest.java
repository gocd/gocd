package com.thoughtworks.go.addon.businesscontinuity.primary.service;

import com.thoughtworks.go.addon.businesscontinuity.AddOnConfiguration;
import com.thoughtworks.go.addon.businesscontinuity.ConfigFileType;
import com.thoughtworks.go.addon.businesscontinuity.MockScheduledExecutorService;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoFilesStatusProviderTest {
    @Mock(lenient = true)
    private GoConfigService goConfigService;
    @Mock(lenient = true)
    private SystemEnvironment systemEnvironment;
    @Mock(lenient = true)
    private CruiseConfig cruiseConfig;
    @Mock
    private ScheduledExecutorService scheduledExecutorService;
    @Mock
    private AddOnConfiguration addOnConfiguration;

    private GoFilesStatusProvider goFilesStatusProvider;

    @BeforeEach
    void setUp() {
        String configDirForTest = getClass().getResource("/config_directory_1").getFile();
        when(systemEnvironment.getConfigDir()).thenReturn(configDirForTest);
        when(systemEnvironment.getCruiseConfigFile()).thenReturn(new File(configDirForTest, "cruise-config.xml").getAbsolutePath());
        when(systemEnvironment.getAESCipherFile()).thenReturn(new File(configDirForTest, "cipher.aes"));
        when(systemEnvironment.getJettyConfigFile()).thenReturn(new File(configDirForTest, "jetty.xml"));
        when(cruiseConfig.getMd5()).thenReturn("cruise-config-md5");
        when(goConfigService.getCurrentConfig()).thenReturn(cruiseConfig);

        goFilesStatusProvider = new GoFilesStatusProvider(goConfigService, systemEnvironment, scheduledExecutorService, addOnConfiguration);
    }

    @Test
    void shouldInitializeFileStatusMapWithRequiredFilesAndInitializeUpdateThread() {
        assertThat(getFileStatus(ConfigFileType.CRUISE_CONFIG_XML), is(EMPTY));
        assertThat(getFileStatus(ConfigFileType.AES_CIPHER), is(EMPTY));
        assertThat(getFileStatus(ConfigFileType.JETTY_XML), is(EMPTY));
        assertThat(getFileStatus(ConfigFileType.USER_FEATURE_TOGGLE), is(EMPTY));

        verify(scheduledExecutorService).scheduleAtFixedRate(any(Runnable.class), eq(1L), eq(60L * 1000L), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void shouldExecuteRunnableToUpdateStatus() {
        when(systemEnvironment.get(SystemEnvironment.USER_FEATURE_TOGGLES_FILE_PATH_RELATIVE_TO_CONFIG_DIR)).thenReturn("go.feature.toggles");
        goFilesStatusProvider = new GoFilesStatusProvider(goConfigService, systemEnvironment, new MockScheduledExecutorService(), addOnConfiguration);

        assertThat(getFileStatus(ConfigFileType.CRUISE_CONFIG_XML), is("cruise-config-md5"));
        assertThat(getFileStatus(ConfigFileType.AES_CIPHER), is("0b4335edfaeacf4d61c0db1ae0820ac3"));
        assertThat(getFileStatus(ConfigFileType.JETTY_XML), is("ee710ce0aa67ef28f22210c659deb6d2"));
        assertThat(getFileStatus(ConfigFileType.USER_FEATURE_TOGGLE), is("c08744f9efd2574c8f93a75bef98de1f"));
    }

    @Test
    void shouldNotUpdateStatusWhenRunningInStandbyMode() {
        when(addOnConfiguration.isServerInStandby()).thenReturn(true);
        goFilesStatusProvider = new GoFilesStatusProvider(goConfigService, systemEnvironment, new MockScheduledExecutorService(), addOnConfiguration);
        Map<ConfigFileType, String> latestStatusMap = goFilesStatusProvider.getLatestStatusMap();
        assertThat(latestStatusMap.get(ConfigFileType.CRUISE_CONFIG_XML), is(EMPTY));
    }

    private String getFileStatus(ConfigFileType fileName) {
        return goFilesStatusProvider.getLatestStatusMap().get(fileName);
    }
}
