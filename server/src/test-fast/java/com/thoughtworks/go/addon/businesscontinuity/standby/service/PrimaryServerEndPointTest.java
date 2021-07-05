package com.thoughtworks.go.addon.businesscontinuity.standby.service;

import com.thoughtworks.go.addon.businesscontinuity.ConfigFileType;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.http.client.methods.HttpRequestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.properties.SystemProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SystemStubsExtension.class)
class PrimaryServerEndPointTest {

    private PrimaryServerEndPoint primaryServerEndPoint;
    private SystemEnvironment systemEnvironment;
    @SystemStub
    private SystemProperties systemProperties;

    @BeforeEach
    void setUp() {
        systemEnvironment = mock(SystemEnvironment.class);
        when(systemEnvironment.getWebappContextPath()).thenReturn("/go");
        primaryServerEndPoint = new PrimaryServerEndPoint(systemEnvironment);
    }

    @Test
    void shouldPointToTheConfigFileStatusGetURL() {
        HttpRequestBase getMethod = primaryServerEndPoint.configFileStatus("token");
        assertThat(getMethod.getURI().toString()).isEqualTo("https://localhost:8154/go/add-on/business-continuity/api/config_files_status");
    }

    @Test
    void shouldAddAuthorizationTokenForConfigFileStatusEndpoint() {
        HttpRequestBase getMethod = primaryServerEndPoint.configFileStatus("foo:bar");
        assertThat(getMethod.getFirstHeader("Authorization").getValue()).isEqualTo("Basic Zm9vOmJhcg==");
    }

    @Test
    void shouldPointToTheDownloadConfigFileStatusGetURL() {
        HttpRequestBase request = primaryServerEndPoint.downloadConfigFile(ConfigFileType.CRUISE_CONFIG_XML, "foo:bar");
        assertThat(request.getURI().toString()).isEqualTo("https://localhost:8154/go/add-on/business-continuity/api/cruise_config");
    }

    @Test
    void shouldPointToTheUserFeatureToggleGetURL() {
        HttpRequestBase request = primaryServerEndPoint.downloadConfigFile(ConfigFileType.USER_FEATURE_TOGGLE, "foo:bar");
        assertThat(request.getURI().toString()).isEqualTo("https://localhost:8154/go/add-on/business-continuity/api/user_feature_toggle");
    }

    @Test
    void shouldAddAuthorizationTokenForDownloadConfigFileEndpoint() {
        HttpRequestBase getMethod = primaryServerEndPoint.downloadConfigFile(ConfigFileType.CRUISE_CONFIG_XML, "foo:bar");
        assertThat(getMethod.getFirstHeader("Authorization").getValue()).isEqualTo("Basic Zm9vOmJhcg==");
    }

    @Test
    void shouldGetDefaultPrimaryServerUrl() {
        assertThat(primaryServerEndPoint.primaryServerUrl()).isEqualTo("https://localhost:8154");
    }

    @Test
    void shouldGetEffectivePrimaryServerUrl() {
        try {
            System.setProperty("bc.primary.url", "https://localhost:8254");
            assertThat(primaryServerEndPoint.primaryServerUrl()).isEqualTo("https://localhost:8254");
        } finally {
            System.clearProperty("bc.primary.url");
        }
    }

    @Test
    void shouldTrimTrailingForwardSlashPrimaryServerUrl() {
        try {
            System.setProperty("bc.primary.url", "https://localhost:8254/");
            assertThat(primaryServerEndPoint.primaryServerUrl()).isEqualTo("https://localhost:8254");
        } finally {
            System.clearProperty("bc.primary.url");
        }
    }

    @Test
    void shouldPointToLatestDatabaseWalLocationURL() {
        HttpRequestBase getMethod = primaryServerEndPoint.latestDatabaseWalLocation("foo:bar");
        assertThat(getMethod.getURI().toString()).isEqualTo("https://localhost:8154/go/add-on/business-continuity/api/latest_database_wal_location");
        assertThat(getMethod.getFirstHeader("Authorization").getValue()).isEqualTo("Basic Zm9vOmJhcg==");
    }

    @Test
    void shouldPointToThePluginListingGetURL() {
        HttpRequestBase getMethod = primaryServerEndPoint.pluginsListing("token");
        assertThat(getMethod.getURI().toString()).isEqualTo("https://localhost:8154/go/add-on/business-continuity/api/plugin_files_status");
    }

    @Test
    void shouldPointToTheDownloadPluginGetURL() {
        HttpRequestBase request = primaryServerEndPoint.downloadPlugin("external", "external1.jar", "foo:bar");
        assertThat(request.getURI().toString()).isEqualTo("https://localhost:8154/go/add-on/business-continuity/api/plugin?folderName=external&pluginName=external1.jar");
    }
}
