package com.thoughtworks.go.addon.businesscontinuity.standby.service;

import com.thoughtworks.go.addon.businesscontinuity.*;
import com.thoughtworks.go.addon.businesscontinuity.primary.ServerStatusResponse;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@EnableRuleMigrationSupport
public class StandbyFileSyncServiceTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();
    @Rule
    public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();
    @Mock
    private SystemEnvironment systemEnvironment;
    @Mock
    private PrimaryServerCommunicationService primaryServerCommunicationService;
    @Mock
    private AddOnConfiguration addOnConfiguration;
    @Mock
    private AuthToken authToken;


    @BeforeEach
    void setUp() throws Exception {
        initMocks(this);
        when(authToken.forHttp()).thenReturn("foo:bar");

        File tempFolder = temporaryFolder.newFolder();
        when(systemEnvironment.getWebappContextPath()).thenReturn("/go");
        when(systemEnvironment.getCruiseConfigFile()).thenReturn(new File(tempFolder, "cruise-config.xml").getAbsolutePath());
        when(systemEnvironment.getDESCipherFile()).thenReturn(new File(tempFolder, "cipher"));
        when(systemEnvironment.getAESCipherFile()).thenReturn(new File(tempFolder, "cipher.aes"));
        when(systemEnvironment.getJettyConfigFile()).thenReturn(new File(tempFolder, "jetty.xml"));
        File externalPluginsDirectory = new File(tempFolder, "external");
        FileUtils.forceMkdir(externalPluginsDirectory);
        when(systemEnvironment.getExternalPluginAbsolutePath()).thenReturn(externalPluginsDirectory.getAbsolutePath());

        Answer answerWithFile = invocationOnMock -> {
            Object[] arguments = invocationOnMock.getArguments();
            ConfigFileType type = (ConfigFileType) arguments[0];
            File file = (File) arguments[1];
            FileUtils.writeStringToFile(file, type + " contents", UTF_8);
            return null;
        };
        Answer answerWithPlugin = invocationOnMock -> {
            Object[] arguments = invocationOnMock.getArguments();
            String pluginName = (String) arguments[1];
            File file = (File) arguments[2];
            FileUtils.writeStringToFile(file, pluginName + " contents", UTF_8);
            return null;
        };
        doAnswer(answerWithFile).when(primaryServerCommunicationService).downloadConfigFile(eq(ConfigFileType.CRUISE_CONFIG_XML), any(File.class));
        doAnswer(answerWithFile).when(primaryServerCommunicationService).downloadConfigFile(eq(ConfigFileType.DES_CIPHER), any(File.class));
        doAnswer(answerWithFile).when(primaryServerCommunicationService).downloadConfigFile(eq(ConfigFileType.AES_CIPHER), any(File.class));
        doAnswer(answerWithFile).when(primaryServerCommunicationService).downloadConfigFile(eq(ConfigFileType.JETTY_XML), any(File.class));

        doAnswer(answerWithPlugin).when(primaryServerCommunicationService).downloadPlugin(eq("external"), eq("external-1.jar"), any(File.class));
        doAnswer(answerWithPlugin).when(primaryServerCommunicationService).downloadPlugin(eq("external"), eq("external-2.jar"), any(File.class));

        when(addOnConfiguration.isServerInStandby()).thenReturn(true);
        when(primaryServerCommunicationService.ableToConnect()).thenReturn(true);
    }

    @Test
    void shouldSyncConfigFiles() throws Exception {
        Map<ConfigFileType, FileDetails> latestStatusMap = new HashMap<ConfigFileType, FileDetails>() {{
            put(ConfigFileType.CRUISE_CONFIG_XML, new FileDetails("new-md5"));
            put(ConfigFileType.DES_CIPHER, new FileDetails("new-md5"));
            put(ConfigFileType.AES_CIPHER, new FileDetails("new-md5"));
            put(ConfigFileType.JETTY_XML, new FileDetails("new-md5"));
        }};

        when(primaryServerCommunicationService.getLatestFileStatus()).thenReturn(new ServerStatusResponse(60, 0L, latestStatusMap));

        Map<String, Object> pluginsList = new HashMap<>();
        pluginsList.put("external", new ArrayList<Map>());
        when(primaryServerCommunicationService.getLatestPluginsStatus()).thenReturn(pluginsList);

        new StandbyFileSyncService(systemEnvironment, primaryServerCommunicationService, new MockScheduledExecutorService(), addOnConfiguration);

        assertThat(FileUtils.readFileToString(new File(systemEnvironment.getCruiseConfigFile()), UTF_8), is("CRUISE_CONFIG_XML contents"));
        assertThat(FileUtils.readFileToString(systemEnvironment.getDESCipherFile(), UTF_8), is("DES_CIPHER contents"));
        assertThat(FileUtils.readFileToString(systemEnvironment.getAESCipherFile(), UTF_8), is("AES_CIPHER contents"));
        assertThat(FileUtils.readFileToString(systemEnvironment.getJettyConfigFile(), UTF_8), is("JETTY_XML contents"));
        verify(primaryServerCommunicationService).getLatestFileStatus();
    }

    @Test
    void shouldSyncPlugins() throws Exception {

        Map<ConfigFileType, FileDetails> latestStatusMap = new HashMap<>();
        when(primaryServerCommunicationService.getLatestFileStatus()).thenReturn(new ServerStatusResponse(60, 0L, latestStatusMap));

        Map<String, String> pluginMap1 = new HashMap<String, String>() {{
            put("name", "external-1.jar");
            put("md5", "md5-1");
        }};
        Map<String, String> pluginMap2 = new HashMap<String, String>() {{
            put("name", "external-2.jar");
            put("md5", "md5-2");
        }};
        Map<String, Object> pluginsList = new HashMap<>();
        pluginsList.put("external", Arrays.asList(pluginMap1, pluginMap2));

        when(primaryServerCommunicationService.getLatestPluginsStatus()).thenReturn(pluginsList);

        StandbyFileSyncService syncService = new StandbyFileSyncService(systemEnvironment, primaryServerCommunicationService, new MockScheduledExecutorService(), addOnConfiguration);

        assertThat(FileUtils.readFileToString(new File(systemEnvironment.getExternalPluginAbsolutePath(), "external-1.jar"), UTF_8), is("external-1.jar contents"));
        assertThat(FileUtils.readFileToString(new File(systemEnvironment.getExternalPluginAbsolutePath(), "external-2.jar"), UTF_8), is("external-2.jar contents"));
        verify(primaryServerCommunicationService).getLatestFileStatus();

        File external3 = new File(systemEnvironment.getExternalPluginAbsolutePath(), "external-3.jar");
        FileUtils.writeStringToFile(external3, "external-3.jar", UTF_8);
        syncService.currentExternalPluginsStatus.put("external-3.jar", "md5-3");

        syncService.syncPlugins();

        assertThat("external-3.jar should have got deleted.", external3.exists(), is(false));
    }

    @Test
    void shouldNotStartStandbyFileSyncWhenGoServerNotInStandbyMode() {
        when(addOnConfiguration.isServerInStandby()).thenReturn(false);
        new StandbyFileSyncService(systemEnvironment, primaryServerCommunicationService, new MockScheduledExecutorService(), addOnConfiguration);
        verify(primaryServerCommunicationService, never()).getLatestFileStatus();
    }

    @Test
    void shouldNotStartStandbyFileSyncWhenOauthSetupIncomplete() {
        when(primaryServerCommunicationService.ableToConnect()).thenReturn(false);
        new StandbyFileSyncService(systemEnvironment, primaryServerCommunicationService, new MockScheduledExecutorService(), addOnConfiguration);
        verify(primaryServerCommunicationService, never()).getLatestFileStatus();
    }

    @Test
    void shouldEnqueueSyncErrors() {
        when(primaryServerCommunicationService.getLatestFileStatus())
                .thenThrow(new RuntimeException("could not connect"))
                .thenThrow(new RuntimeException("could not connect"))
                .thenThrow(new RuntimeException("could not connect"))
                .thenThrow(new RuntimeException("connection refused"));

        MockScheduledExecutorService executorService = new MockScheduledExecutorService(6);
        StandbyFileSyncService standbyFileSyncService = new StandbyFileSyncService(systemEnvironment, primaryServerCommunicationService, executorService, addOnConfiguration);
        List<String> errors = standbyFileSyncService.syncErrors();

        assertThat(errors.size(), is(5));
        assertThat(errors.get(0).contains("Error while syncing files. Reason, could not connect"), is(true));
        assertThat(errors.get(1).contains("Error while syncing files. Reason, could not connect"), is(true));
        assertThat(errors.get(2).contains("Error while syncing files. Reason, connection refused"), is(true));
        assertThat(errors.get(3).contains("Error while syncing files. Reason, connection refused"), is(true));
        assertThat(errors.get(4).contains("Error while syncing files. Reason, connection refused"), is(true));
    }

    @Test
    void shouldClearErrorsAfterSuccess() {
        when(primaryServerCommunicationService.getLatestFileStatus())
                .thenThrow(new RuntimeException("could not connect"))
                .thenReturn(new ServerStatusResponse(0, 0, new HashMap<>()));
        Map<String, Object> pluginsList = new HashMap<>();
        pluginsList.put("external", new ArrayList<Map>());
        when(primaryServerCommunicationService.getLatestPluginsStatus()).thenReturn(pluginsList);

        MockScheduledExecutorService executorService = new MockScheduledExecutorService(2);
        StandbyFileSyncService standbyFileSyncService = new StandbyFileSyncService(systemEnvironment, primaryServerCommunicationService, executorService, addOnConfiguration);
        List<String> errors = standbyFileSyncService.syncErrors();

        assertThat(errors.size(), is(0));
    }
}
