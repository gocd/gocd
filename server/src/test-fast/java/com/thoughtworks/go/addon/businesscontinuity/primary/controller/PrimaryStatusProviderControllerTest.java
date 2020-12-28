package com.thoughtworks.go.addon.businesscontinuity.primary.controller;

import com.google.gson.Gson;
import com.thoughtworks.go.addon.businesscontinuity.ConfigFileType;
import com.thoughtworks.go.addon.businesscontinuity.DatabaseStatusProvider;
import com.thoughtworks.go.addon.businesscontinuity.PluginsList;
import com.thoughtworks.go.addon.businesscontinuity.primary.ServerStatusResponse;
import com.thoughtworks.go.addon.businesscontinuity.primary.service.GoFilesStatusProvider;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class PrimaryStatusProviderControllerTest {
    @Mock
    private GoFilesStatusProvider goFilesStatusProvider;
    @Mock
    private PluginsList pluginsList;
    @Mock
    private SystemEnvironment systemEnvironment;
    @Mock
    private DatabaseStatusProvider databaseStatusProvider;

    private PrimaryStatusProviderController primaryStatusProviderController;
    private MockHttpServletResponse httpServletResponse;
    private String configDirectory;
    private String pluginsDirectory;

    @BeforeEach
    void setUp() {
        initMocks(this);
        httpServletResponse = new MockHttpServletResponse();
        configDirectory = getClass().getResource("/config_directory_1").getFile();
        pluginsDirectory = getClass().getResource("/plugins_directory_1").getFile();
        primaryStatusProviderController = new PrimaryStatusProviderController(goFilesStatusProvider, databaseStatusProvider, pluginsList, systemEnvironment);
    }

    @Test
    void shouldRespondWithLatestStatus() throws IOException {
        Map<ConfigFileType, String> latestStatus = new HashMap<>();
        latestStatus.put(ConfigFileType.CRUISE_CONFIG_XML, "a");
        latestStatus.put(ConfigFileType.AES_CIPHER, "AES-CIPHER");
        latestStatus.put(ConfigFileType.JETTY_XML, "c");
        when(goFilesStatusProvider.getLatestStatusMap()).thenReturn(latestStatus);
        when(systemEnvironment.getWebappContextPath()).thenReturn("/go");

        primaryStatusProviderController.latestStatus(httpServletResponse);

        ServerStatusResponse serverStatusResponse = new Gson().fromJson(httpServletResponse.getContentAsString(), ServerStatusResponse.class);
        assertThat(serverStatusResponse.getFileDetailsMap().size(), is(4));
        assertThat(serverStatusResponse.getFileDetailsMap().get(ConfigFileType.CRUISE_CONFIG_XML).getMd5(), is("a"));
        assertThat(serverStatusResponse.getFileDetailsMap().get(ConfigFileType.AES_CIPHER).getMd5(), is("AES-CIPHER"));
        assertThat(serverStatusResponse.getFileDetailsMap().get(ConfigFileType.JETTY_XML).getMd5(), is("c"));
    }

    @Test
    void shouldRespondWithLatestCruiseConfigXML() throws Exception {
        when(systemEnvironment.getCruiseConfigFile()).thenReturn(new File(configDirectory, "cruise-config.xml").getAbsolutePath());

        primaryStatusProviderController.getLatestCruiseConfigXML(httpServletResponse);

        assertThat(httpServletResponse.getContentType(), is("text/xml"));
        assertThat(httpServletResponse.getCharacterEncoding(), is("UTF-8"));
        assertThat(httpServletResponse.getContentAsByteArray(), is(FileUtils.readFileToByteArray(new File(systemEnvironment.getCruiseConfigFile()))));
    }

    @Test
    void shouldRespondWithLatestAESCipher() throws Exception {
        when(systemEnvironment.getAESCipherFile()).thenReturn(new File(configDirectory, "cipher.aes"));

        primaryStatusProviderController.getLatestAESCipher(httpServletResponse);

        assertThat(httpServletResponse.getContentType(), is("text/plain"));
        assertThat(httpServletResponse.getCharacterEncoding(), is("UTF-8"));
        assertThat(httpServletResponse.getContentAsByteArray(), is(FileUtils.readFileToByteArray(systemEnvironment.getAESCipherFile())));
    }

    @Test
    void shouldRespondWithLatestJettyXML() throws Exception {
        when(systemEnvironment.getJettyConfigFile()).thenReturn(new File(configDirectory, "jetty.xml"));

        primaryStatusProviderController.getLatestJettyXML(httpServletResponse);

        assertThat(httpServletResponse.getContentType(), is("text/xml"));
        assertThat(httpServletResponse.getCharacterEncoding(), is("UTF-8"));
        assertThat(httpServletResponse.getContentAsByteArray(), is(FileUtils.readFileToByteArray(systemEnvironment.getJettyConfigFile())));
    }

    @Test
    void shouldProvideLatestDatabaseWalLocation() throws Exception {
        when(databaseStatusProvider.latestWalLocation()).thenReturn("123");
        String databaseWalLocation = primaryStatusProviderController.latestDatabaseWalLocation();
        assertThat(databaseWalLocation, is("123"));
    }

    @Test
    void shouldRespondWithPluginsJSON() throws Exception {
        final String expectedJSON = "some json";
        when(pluginsList.getPluginsJSON()).thenReturn(expectedJSON);

        primaryStatusProviderController.latest(httpServletResponse);

        assertThat(httpServletResponse.getContentType(), is("application/json"));
        assertThat(httpServletResponse.getContentAsByteArray(), is(expectedJSON.getBytes()));
    }

    @Test
    void shouldRespondWithLatestPluginFile() throws Exception {
        when(systemEnvironment.getBundledPluginAbsolutePath()).thenReturn(new File(pluginsDirectory).getAbsolutePath());

        primaryStatusProviderController.getPluginFile("bundled", "plugin-1.jar", httpServletResponse);

        assertThat(httpServletResponse.getContentType(), is("application/octet-stream"));
        assertThat(httpServletResponse.getCharacterEncoding(), is("UTF-8"));
        assertThat(httpServletResponse.getContentAsByteArray(), is(FileUtils.readFileToByteArray(new File(pluginsDirectory, "plugin-1.jar"))));
    }

    @Test
    void shouldNotIncludeFileWithEmptyMd5InLatestStatus() throws Exception {
        Map<ConfigFileType, String> latestStatus = new HashMap<>();
        latestStatus.put(ConfigFileType.CRUISE_CONFIG_XML, "a");
        latestStatus.put(ConfigFileType.USER_FEATURE_TOGGLE, "");
        when(goFilesStatusProvider.getLatestStatusMap()).thenReturn(latestStatus);

        primaryStatusProviderController.latestStatus(httpServletResponse);

        ServerStatusResponse serverStatusResponse = new Gson().fromJson(httpServletResponse.getContentAsString(), ServerStatusResponse.class);
        assertThat(serverStatusResponse.getFileDetailsMap().containsKey(ConfigFileType.USER_FEATURE_TOGGLE), is(false));
    }

    @Test
    void shouldRespondWithLatestUserFeatureToggleFile() throws Exception {
        when(systemEnvironment.getConfigDir()).thenReturn(configDirectory);
        when(systemEnvironment.get(SystemEnvironment.USER_FEATURE_TOGGLES_FILE_PATH_RELATIVE_TO_CONFIG_DIR)).thenReturn("go.feature.toggles");

        primaryStatusProviderController.geUserFeatureToggleFile(httpServletResponse);

        assertThat(httpServletResponse.getContentType(), is("text/json"));
        assertThat(httpServletResponse.getCharacterEncoding(), is("UTF-8"));
        File expectedFile = new File(systemEnvironment.getConfigDir(), "go.feature.toggles");
        assertThat(httpServletResponse.getContentAsByteArray(), is(FileUtils.readFileToByteArray(expectedFile)));
    }

    @Test
    void healthCheckReturns200() {
        assertThat(primaryStatusProviderController.healthCheck(), is("OK!"));
    }
}
