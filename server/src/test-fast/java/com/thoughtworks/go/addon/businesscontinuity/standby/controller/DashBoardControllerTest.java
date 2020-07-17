package com.thoughtworks.go.addon.businesscontinuity.standby.controller;

import com.github.paweladamski.httpclientmock.HttpClientMock;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.go.addon.businesscontinuity.*;
import com.thoughtworks.go.addon.businesscontinuity.primary.ServerStatusResponse;
import com.thoughtworks.go.addon.businesscontinuity.standby.service.PrimaryServerCommunicationService;
import com.thoughtworks.go.addon.businesscontinuity.standby.service.PrimaryServerEndPoint;
import com.thoughtworks.go.addon.businesscontinuity.standby.service.StandbyFileSyncService;
import com.thoughtworks.go.http.mocks.HttpRequestBuilder;
import com.thoughtworks.go.http.mocks.MockHttpServletResponse;
import com.thoughtworks.go.http.mocks.MockHttpServletResponseAssert;
import com.thoughtworks.go.server.newsecurity.models.UsernamePassword;
import com.thoughtworks.go.server.service.RailsAssetsService;
import com.thoughtworks.go.util.SystemEnvironment;
import net.javacrumbs.jsonunit.fluent.JsonFluentAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

class DashBoardControllerTest {
    @Mock
    private AddOnConfiguration addOnConfiguration;

    private PrimaryServerCommunicationService primaryServerCommunicationService;
    @Mock
    private StandbyFileSyncService standbyFileSyncService;

    @Mock
    private ViewResolver viewResolver;
    @Mock
    private DatabaseStatusProvider databaseStatusProvider;
    @Mock
    private RailsAssetsService railsAssetsService;
    @Mock
    private AuthToken authToken;

    private static final String USERNAME = "bob";
    private static final String PASSWORD = "s3cr3t";
    private static final String CREDENTIALS = USERNAME + ":" + PASSWORD;
    private static final String CREDENTIALS_AS_BASE64 = Base64.getEncoder().encodeToString(CREDENTIALS.getBytes(UTF_8));
    private static final String AUTHORIZATION_HEADER_VALUE = "Basic " + CREDENTIALS_AS_BASE64;

    private DashBoardController controller;
    private Gson gson;
    private HttpClientMock httpClientMock;

    @BeforeEach
    void setUp() {
        initMocks(this);
        gson = new GsonBuilder().setDateFormat("MMM d, YYYY HH:mm:ss").create();
        SystemEnvironment systemEnvironment = new SystemEnvironment();
        httpClientMock = new HttpClientMock();
        primaryServerCommunicationService = spy(new PrimaryServerCommunicationService(httpClientMock, new PrimaryServerEndPoint(systemEnvironment), authToken));
        controller = new DashBoardController(addOnConfiguration, primaryServerCommunicationService, standbyFileSyncService, viewResolver, databaseStatusProvider, railsAssetsService, authToken);
    }

    @Test
    void shouldProvideDashboardContentsForStandby() {
        when(authToken.isValid()).thenReturn(true);
        when(authToken.toUsernamePassword()).thenReturn(new UsernamePassword(USERNAME, PASSWORD));
        when(authToken.forHttp()).thenReturn(CREDENTIALS);

        HttpServletRequest request = HttpRequestBuilder.GET("")
                .withBasicAuth(USERNAME, PASSWORD)
                .build();

        httpClientMock.onGet("https://localhost:8154/go/add-on/business-continuity/api/health-check")
                .withHeader("Authorization", AUTHORIZATION_HEADER_VALUE)
                .doReturnStatus(200);
        httpClientMock.onGet("https://localhost:8154/go/add-on/business-continuity/api/latest_database_wal_location")
                .withHeader("Authorization", AUTHORIZATION_HEADER_VALUE)
                .doReturn(200, "/logs/location");
        httpClientMock.onGet("https://localhost:8154/go/add-on/business-continuity/api/config_files_status")
                .withHeader("Authorization", AUTHORIZATION_HEADER_VALUE)
                .doReturn(200, "{\"configFilesUpdateInterval\":10,\"fileDetailsMap\":{\"CRUISE_CONFIG_XML\":{\"md5\":\"a\"}}}");
        httpClientMock.onGet("https://localhost:8154/go/add-on/business-continuity/api/plugin_files_status")
                .withHeader("Authorization", AUTHORIZATION_HEADER_VALUE)
                .doReturn(200, "{\"bundled\":[{\"name\":\"yum.jar\",\"md5\":\"LAVBbwaDykricDnAP57klg\\u003d\\u003d\"}],\"external\":[{\"name\":\"external1.jar\",\"md5\":\"+yWDK4+tYQtfqyh3tmT95A\\u003d\\u003d\"},{\"name\":\"external2.jar\",\"md5\":\"DS/Oa0vv5URteXfzSU7mvQ\\u003d\\u003d\"}]}");

        when(addOnConfiguration.isServerInStandby()).thenReturn(true);

        MockHttpServletResponse response = new MockHttpServletResponse();
        String dashboardData = controller.dashboardData(request, response);

        MockHttpServletResponseAssert.assertThat(response).hasStatus(200);

        JsonFluentAssert.assertThatJson(dashboardData)
                .isEqualTo("{\n" +
                        "  \"setupStatus\": \"success\",\n" +
                        "  \"userName\": \"bob\",\n" +
                        "  \"standbyServerDetails\": {\n" +
                        "    \"primaryStatusCheckInterval\": 0,\n" +
                        "    \"pluginStatus\": \"\",\n" +
                        "    \"lastUpdateTime\": " + new GsonBuilder().setDateFormat("MMM d, YYYY HH:mm:ss").create().toJson(new Date(0)) + "\n" +
                        "  },\n" +
                        "  \"primaryServerDetails\": {\n" +
                        "    \"latestDatabaseWalLocation\": \"/logs/location\",\n" +
                        "    \"configFilesUpdateInterval\": 10,\n" +
                        "    \"lastConfigUpdateTime\": " + new GsonBuilder().setDateFormat("MMM d, YYYY HH:mm:ss").create().toJson(new Date(0)) + ",\n" +
                        "    \"CRUISE_CONFIG_XML\": {\n" +
                        "      \"md5\": \"a\"\n" +
                        "    },\n" +
                        "    \"pluginStatus\": \"external1.jar\\u003d+yWDK4+tYQtfqyh3tmT95A\\u003d\\u003d, external2.jar\\u003dDS/Oa0vv5URteXfzSU7mvQ\\u003d\\u003d\",\n" +
                        "    \"url\": \"https://localhost:8154\"\n" +
                        "  },\n" +
                        "  \"syncErrors\": []\n" +
                        "}");
    }

    @Test
    void shouldResolveDashboardViewForStandby() {
        when(authToken.isValid()).thenReturn(true);
        when(authToken.toUsernamePassword()).thenReturn(new UsernamePassword(USERNAME, PASSWORD));
        when(authToken.forHttp()).thenReturn(CREDENTIALS);

        when(addOnConfiguration.isServerInStandby()).thenReturn(true);
        when(railsAssetsService.getAssetPath("application.css")).thenReturn("application.css");
        when(railsAssetsService.getAssetPath("patterns/application.css")).thenReturn("patterns/application.css");
        when(railsAssetsService.getAssetPath("application.js")).thenReturn("application.js");
        when(railsAssetsService.getAssetPath("cruise.ico")).thenReturn("cruise.ico");

        Map<String, String> expectedModelMap = new HashMap<>();
        expectedModelMap.put("REPLACED_BY_GO:application.css", "application.css");
        expectedModelMap.put("REPLACED_BY_GO:patterns/application.css", "patterns/application.css");
        expectedModelMap.put("REPLACED_BY_GO:application.js", "application.js");
        expectedModelMap.put("REPLACED_BY_GO:cruise.ico", "cruise.ico");

        String template = "<html></html>";
        when(viewResolver.resolveView("standby_dashboard", expectedModelMap)).thenReturn(template);

        HttpServletRequest request = HttpRequestBuilder.GET("")
                .withBasicAuth(USERNAME, PASSWORD)
                .build();
        String view = controller.dashboard(request, null);
        assertThat(view).isEqualTo(template);
    }

    @Test
    void shouldResolveDashboardViewNonStandbyServer() {
        when(authToken.isValid()).thenReturn(true);
        when(authToken.toUsernamePassword()).thenReturn(new UsernamePassword(USERNAME, PASSWORD));
        when(authToken.forHttp()).thenReturn(CREDENTIALS);

        when(addOnConfiguration.isServerInStandby()).thenReturn(false);
        when(railsAssetsService.getAssetPath("application.css")).thenReturn("application.css");
        when(railsAssetsService.getAssetPath("patterns/application.css")).thenReturn("patterns/application.css");
        when(railsAssetsService.getAssetPath("application.js")).thenReturn("application.js");
        when(railsAssetsService.getAssetPath("cruise.ico")).thenReturn("cruise.ico");

        Map<String, String> expectedModelMap = new HashMap<>();
        expectedModelMap.put("REPLACED_BY_GO:application.css", "application.css");
        expectedModelMap.put("REPLACED_BY_GO:patterns/application.css", "patterns/application.css");
        expectedModelMap.put("REPLACED_BY_GO:application.js", "application.js");
        expectedModelMap.put("REPLACED_BY_GO:cruise.ico", "cruise.ico");

        String template = "<html></html>";
        when(viewResolver.resolveView("error", expectedModelMap)).thenReturn(template);

        HttpServletRequest request = HttpRequestBuilder.GET("")
                .withBasicAuth(USERNAME, PASSWORD)
                .build();
        String view = controller.dashboard(request, null);
        assertThat(view).isEqualTo(template);
    }

    @Test
    void shouldGetStandbyServerDetails() {
        HashMap<ConfigFileType, String> fileMd5Map = new HashMap<>();
        fileMd5Map.put(ConfigFileType.CRUISE_CONFIG_XML, "md51");
        fileMd5Map.put(ConfigFileType.DES_CIPHER, "md52");
        fileMd5Map.put(ConfigFileType.AES_CIPHER, "md53");

        Map<String, String> plugins = new HashMap<>();
        plugins.put("plugin-one", "md51");
        plugins.put("plugin-two", "md52");

        long time = 1428051875504L;
        when(standbyFileSyncService.lastUpdateTime()).thenReturn(time);
        when(standbyFileSyncService.primaryStatusCheckInterval()).thenReturn(60000);
        when(standbyFileSyncService.getCurrentFileStatus()).thenReturn(fileMd5Map);
        when(databaseStatusProvider.latestReceivedWalLocation()).thenReturn("12345");
        when(standbyFileSyncService.getCurrentExternalPluginsStatus()).thenReturn(plugins);

        Map<String, Object> standbyDetails = controller.standbyServerDetails();

        JsonFluentAssert.assertThatJson(gson.toJson(standbyDetails)).isEqualTo("{\"CRUISE_CONFIG_XML\":\"md51\",\"lastUpdateTime\":\"" +
                new SimpleDateFormat("MMM d, YYYY HH:mm:ss").format(new Date(time)) +
                "\",\"primaryStatusCheckInterval\":60000,\"pluginStatus\":\"plugin-one=md51, plugin-two=md52\",\"DES_CIPHER\":\"md52\",\"AES_CIPHER\":\"md53\",\"latestReceivedDatabaseWalLocation\":\"12345\"}");
    }

    @Test
    void shouldGetPrimaryServerDetails() {
        when(authToken.isValid()).thenReturn(true);
        when(authToken.toUsernamePassword()).thenReturn(new UsernamePassword(USERNAME, PASSWORD));
        when(authToken.forHttp()).thenReturn(CREDENTIALS);

        HashMap<ConfigFileType, FileDetails> fileDetailsMap = new HashMap<>();
        fileDetailsMap.put(ConfigFileType.CRUISE_CONFIG_XML, new FileDetails("md51"));
        fileDetailsMap.put(ConfigFileType.DES_CIPHER, new FileDetails("md52"));
        fileDetailsMap.put(ConfigFileType.AES_CIPHER, new FileDetails("md53"));

        Map<String, String> pluginOne = new HashMap<>();
        pluginOne.put("name", "plugin-one");
        pluginOne.put("md5", "md51");

        Map<String, String> pluginTwo = new HashMap<>();
        pluginTwo.put("name", "plugin-two");
        pluginTwo.put("md5", "md52");

        Map<String, Object> plugins = new HashMap<>();
        plugins.put("external", asList(pluginOne, pluginTwo));

        long time = 1428051875504L;
        doReturn(new ServerStatusResponse(60000, time, fileDetailsMap)).when(primaryServerCommunicationService).getLatestFileStatus();
        doReturn("https://localhost:8154").when(primaryServerCommunicationService).primaryServerUrl();
        doReturn("12345").when(primaryServerCommunicationService).latestDatabaseWalLocation();
        doReturn(plugins).when(primaryServerCommunicationService).getLatestPluginsStatus();

        Map<String, Object> primaryServerDetails = controller.primaryServerDetails();
        JsonFluentAssert.assertThatJson(gson.toJson(primaryServerDetails)).isEqualTo("{\"CRUISE_CONFIG_XML\":{\"md5\":\"md51\"},\"configFilesUpdateInterval\":60000,\"latestDatabaseWalLocation\":\"12345\",\"pluginStatus\":\"plugin-one=md51, plugin-two=md52\",\"DES_CIPHER\":{\"md5\":\"md52\"},\"AES_CIPHER\":{\"md5\":\"md53\"},\"url\":\"https://localhost:8154\",\"lastConfigUpdateTime\":\"" +
                        new SimpleDateFormat("MMM d, YYYY HH:mm:ss").format(new Date(time)) + "\"}");
    }

    @Test
    public void shouldHandleExceptionWhenRetrievingPrimaryServerDetails() {
        when(authToken.isValid()).thenReturn(true);
        when(authToken.toUsernamePassword()).thenReturn(new UsernamePassword(USERNAME, PASSWORD));
        when(authToken.forHttp()).thenReturn(CREDENTIALS);

        doThrow(new RuntimeException("with this message")).when(primaryServerCommunicationService).getLatestFileStatus();
        Map<String, Object> primaryServerDetails = controller.primaryServerDetails();
        assertThat(((String) primaryServerDetails.get("error"))).isEqualTo("Could not fetch latest file status from master, Reason, with this message");
    }

    @Test
    void shouldErrorWhenStandbyNotAddedAsOAuthClient() {
        when(authToken.isValid()).thenReturn(true);
        when(authToken.toUsernamePassword()).thenReturn(new UsernamePassword(USERNAME, PASSWORD));
        when(authToken.forHttp()).thenReturn(CREDENTIALS);

        when(addOnConfiguration.isServerInStandby()).thenReturn(true);

        HttpServletRequest request = HttpRequestBuilder.GET("")
                .withBasicAuth(USERNAME, PASSWORD)
                .build();
        MockHttpServletResponse response = new MockHttpServletResponse();
        String dashboardData = controller.dashboardData(request, response);
        MockHttpServletResponseAssert.assertThat(response).hasStatus(200);
        JsonFluentAssert.assertThatJson(dashboardData).isEqualTo("{\"syncErrors\":[\"Unable to connect to primary, please check that the business-continuity-token file is identical on primary and secondary, and that this server can connect to the primary server.\"],\"setupStatus\":\"incomplete\", \"userName\": \"bob\"}");
    }

}
