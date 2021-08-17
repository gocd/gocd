package com.thoughtworks.go.addon.businesscontinuity.standby.service;

import com.github.paweladamski.httpclientmock.HttpClientMock;
import com.thoughtworks.go.addon.businesscontinuity.AuthToken;
import com.thoughtworks.go.addon.businesscontinuity.ConfigFileType;
import com.thoughtworks.go.addon.businesscontinuity.FileDetails;
import com.thoughtworks.go.addon.businesscontinuity.primary.ServerStatusResponse;
import com.thoughtworks.go.util.SystemEnvironment;
import net.javacrumbs.jsonunit.fluent.JsonFluentAssert;
import org.apache.commons.io.FileUtils;
import org.apache.http.client.methods.HttpGet;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.properties.SystemProperties;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(SystemStubsExtension.class)
@EnableRuleMigrationSupport
public class PrimaryServerCommunicationServiceTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();
    @SystemStub
    private SystemProperties systemProperties;

    private static final String CREDENTIALS = "bob:s3cr3t";
    private static final String CREDENTIALS_AS_BASE64 = Base64.getEncoder().encodeToString(CREDENTIALS.getBytes(UTF_8));
    private static final String AUTHORIZATION_HEADER_VALUE = "Basic " + CREDENTIALS_AS_BASE64;

    @Spy
    private SystemEnvironment systemEnvironment = new SystemEnvironment();

    private PrimaryServerCommunicationService primaryServerCommunicationService;

    private HttpClientMock httpClientMock;

    private PrimaryServerEndPoint primaryServerEndPoint;
    @Mock(lenient = true)
    private AuthToken authToken;

    @BeforeEach
    void setUp() {
        httpClientMock = new HttpClientMock();

        System.setProperty("bc.primary.url", "https://localhost:1234");
        primaryServerEndPoint = new PrimaryServerEndPoint(systemEnvironment);
        when(authToken.forHttp()).thenReturn(CREDENTIALS);

        this.primaryServerCommunicationService = spy(new PrimaryServerCommunicationService(httpClientMock, primaryServerEndPoint, authToken));
    }

    @Test
    void shouldRespondWithLatestStatus() {
        String responseBody = "{\"configFilesUpdateInterval\":10,\"fileDetailsMap\":{\"CRUISE_CONFIG_XML\":{\"md5\":\"a\"}}}";
        httpClientMock.onGet("https://localhost:1234/go/add-on/business-continuity/api/config_files_status")
                .withHeader("Authorization", AUTHORIZATION_HEADER_VALUE)
                .doReturn(200, responseBody);

        ServerStatusResponse serverStatusResponse = primaryServerCommunicationService.getLatestFileStatus();

        assertThat(serverStatusResponse.getConfigFilesUpdateInterval()).isEqualTo(10L);
        Map<ConfigFileType, FileDetails> fileDetailsMap = serverStatusResponse.getFileDetailsMap();
        assertThat(fileDetailsMap.size()).isEqualTo(1);
        assertThat(fileDetailsMap.get(ConfigFileType.CRUISE_CONFIG_XML).getMd5()).isEqualTo("a");
    }

    @Test
    void shouldThrowExceptionIfFetchingConfigStatusUnSuccessfulBecauseOfBadHttpStatus() {
        httpClientMock.onGet("https://localhost:1234")
                .doReturnStatus(400);
        assertThatCode(() -> primaryServerCommunicationService.invokeHttp(new HttpGet("https://localhost:1234"), "blah", inputStream -> null))
                .hasMessageContaining("blah, expected primary server to respond with the HTTP status code 200 but got 400");
    }

    @Test
    void shouldThrowExceptionIfFetchingConfigStatusUnSuccessfulBecauseOfIOError() {
        httpClientMock.onGet("https://localhost:1234")
                .doThrowException(new IOException("something blew up!"));
        assertThatCode(() -> primaryServerCommunicationService.invokeHttp(new HttpGet("https://localhost:1234"), "blah", inputStream -> null))
                .hasMessageContaining("something blew up!");
    }

    @Test
    void shouldDownloadSpecifiedFile() throws Exception {
        String fileContent = "cruise-config.xml contents";
        httpClientMock.onGet("https://localhost:1234/go/add-on/business-continuity/api/cruise_config")
                .withHeader("Authorization", AUTHORIZATION_HEADER_VALUE)
                .doReturn(200, fileContent);
        File fileOnStandby = temporaryFolder.newFile("cruise-config.xml");

        primaryServerCommunicationService.downloadConfigFile(ConfigFileType.CRUISE_CONFIG_XML, fileOnStandby);

        assertThat(FileUtils.readFileToString(fileOnStandby, UTF_8)).isEqualTo(fileContent);
    }

    @Test
    void shouldThrowExceptionIfDownloadingFileUnSuccessfulBecauseOfBadStatusCode() throws Exception {
        httpClientMock.onGet("https://localhost:1234/go/add-on/business-continuity/api/cruise_config")
                .withHeader("Authorization", AUTHORIZATION_HEADER_VALUE)
                .doReturnStatus(400);
        File fileOnStandby = temporaryFolder.newFile("cruise-config.xml");
        fileOnStandby.delete();

        assertThat(fileOnStandby).doesNotExist();
        assertThatCode(() -> primaryServerCommunicationService.downloadConfigFile(ConfigFileType.CRUISE_CONFIG_XML, fileOnStandby))
                .hasMessage("Could not download file 'https://localhost:1234/go/add-on/business-continuity/api/cruise_config' from primary server, expected primary server to respond with the HTTP status code 200 but got 400");

        assertThat(fileOnStandby).doesNotExist();
    }

    @Test
    void shouldThrowExceptionIfDownloadingFileUnSuccessfulBecauseOfNetworkError() throws Exception {
        httpClientMock.onGet("https://localhost:1234/go/add-on/business-continuity/api/cruise_config")
                .withHeader("Authorization", AUTHORIZATION_HEADER_VALUE)
                .doThrowException(new IOException("Unable to resolve IP address."));

        File fileOnStandby = temporaryFolder.newFile("cruise-config.xml");
        fileOnStandby.delete();

        assertThat(fileOnStandby).doesNotExist();
        assertThatCode(() -> primaryServerCommunicationService.downloadConfigFile(ConfigFileType.CRUISE_CONFIG_XML, fileOnStandby))
                .hasMessageContaining("Unable to resolve IP address.");

        assertThat(fileOnStandby).doesNotExist();
    }

    @Test
    void shouldGetLatestDatabaseWalLocation() {
        httpClientMock.onGet("https://localhost:1234/go/add-on/business-continuity/api/latest_database_wal_location")
                .withHeader("Authorization", AUTHORIZATION_HEADER_VALUE)
                .doReturn(200, "/foo/bar.txt");
        String databaseWalLocation = primaryServerCommunicationService.latestDatabaseWalLocation();
        assertThat(databaseWalLocation).isEqualTo("/foo/bar.txt");
    }

    @Test
    void shouldRespondWithLatestPluginListing() {
        String responseBody = "{\"bundled\":[{\"name\":\"yum.jar\",\"md5\":\"LAVBbwaDykricDnAP57klg\\u003d\\u003d\"}],\"external\":[{\"name\":\"external1.jar\",\"md5\":\"+yWDK4+tYQtfqyh3tmT95A\\u003d\\u003d\"},{\"name\":\"external2.jar\",\"md5\":\"DS/Oa0vv5URteXfzSU7mvQ\\u003d\\u003d\"}]}";
        httpClientMock.onGet("https://localhost:1234/go/add-on/business-continuity/api/plugin_files_status")
                .withHeader("Authorization", AUTHORIZATION_HEADER_VALUE)
                .doReturn(200, responseBody);
        Map pluginsStatus = primaryServerCommunicationService.getLatestPluginsStatus();
        JsonFluentAssert.assertThatJson(responseBody).isEqualTo(pluginsStatus);
    }

    @Test
    void shouldDownloadSpecifiedPlugin() throws Exception {
        String fileContent = "some plugin jar";
        httpClientMock.onGet("https://localhost:1234/go/add-on/business-continuity/api/plugin?folderName=foo&pluginName=plugin.jar")
                .withHeader("Authorization", AUTHORIZATION_HEADER_VALUE)
                .doReturn(200, fileContent);
        File fileOnStandby = temporaryFolder.newFile("plugin.jar");

        primaryServerCommunicationService.downloadPlugin("foo", "plugin.jar", fileOnStandby);

        assertThat(FileUtils.readFileToString(fileOnStandby, UTF_8)).isEqualTo(fileContent);
    }


    @Nested
    class AbleToConnect {
        @Rule
        public final TemporaryFolder temporaryFolder = new TemporaryFolder();
        @SystemStub
        private SystemProperties systemProperties;

        @Test
        void shouldReturnFalseIfUnableToConnect() {
            httpClientMock.onGet("https://localhost:1234/go/add-on/business-continuity/api/health-check")
                    .withHeader("Authorization", AUTHORIZATION_HEADER_VALUE)
                    .doThrowException(new IOException("Unable to connect"));

            assertThat(primaryServerCommunicationService.ableToConnect()).isFalse();
        }

        @Test
        void shouldReturnTrueIfStatusCodeWas200() {
            httpClientMock.onGet("https://localhost:1234/go/add-on/business-continuity/api/health-check")
                    .withHeader("Authorization", AUTHORIZATION_HEADER_VALUE)
                    .doReturn(200, "OK");

            assertThat(primaryServerCommunicationService.ableToConnect()).isTrue();
        }

        @Test
        void shouldReturnFalseIfStatusCodeWasNot200() {
            httpClientMock.onGet("https://localhost:1234/go/add-on/business-continuity/api/health-check")
                    .withHeader("Authorization", AUTHORIZATION_HEADER_VALUE)
                    .doReturnStatus(502);

            assertThat(primaryServerCommunicationService.ableToConnect()).isFalse();
        }
    }
}
