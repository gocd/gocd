/*
 * Copyright Thoughtworks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.server.web;

import com.thoughtworks.go.agent.common.ssl.GoAgentServerHttpClientBuilder;
import com.thoughtworks.go.domain.SiteUrl;
import com.thoughtworks.go.server.service.ServerConfigService;
import com.thoughtworks.go.server.service.support.toggle.FeatureToggleService;
import com.thoughtworks.go.server.service.support.toggle.Toggles;
import com.thoughtworks.go.util.SslVerificationMode;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.CloseableHttpClient;
import org.eclipse.jetty.util.resource.Resource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.web.context.WebApplicationContext;
import org.tuckey.web.filters.urlrewrite.UrlRewriteFilter;

import javax.servlet.DispatcherType;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class UrlRewriterIntegrationTest {
    private static final String IP_1 = "127.0.0.1";
    private static final String IP_2 = "127.0.0.2";
    private static final int HTTP = 5197;
    private static final int HTTPS = 9071;

    private static final String HTTP_URL = "http://" + IP_1 + ":" + HTTP;
    private static final String HTTP_SITE_URL = "http://" + IP_2 + ":" + HTTP;
    private static final String HTTPS_SITE_URL = "https://" + IP_2 + ":" + HTTPS;

    private static HttpTestUtil httpUtil;
    private static WebApplicationContext wac;

    private static final ResponseAssertion NO_REWRITE = new ResponseAssertion(HTTP_URL + "/go/quux?hello=world", HTTP_URL + "/go/quux?hello=world");
    private static final ResponseAssertion PIPELINE_GROUP_CREATE = new ResponseAssertion(HTTP_URL + "/go/api/admin/pipeline_groups", HTTP_URL + "/go/spark/api/admin/pipeline_groups", METHOD.POST);


    private static final ResponseAssertion SERVER_BACKUP = new ResponseAssertion(HTTP_URL + "/go/admin/backup", HTTP_URL + "/go/spark/admin/backup", true);

    private static final ResponseAssertion STATIC_PAGES = new ResponseAssertion(HTTP_URL + "/go/static/foo.html?bar=baz", HTTP_URL + "/go/static/foo.html?bar=baz", true);
    private static final ResponseAssertion ASSETS = new ResponseAssertion(HTTP_URL + "/go/assets/some-image.png", HTTP_URL + "/go/rails/assets/some-image.png", true);

    private static final ResponseAssertion CONFIG_FILE_XML = new ResponseAssertion(HTTP_URL + "/go/admin/configuration/file.xml", HTTP_URL + "/go/admin/restful/configuration/file/GET/xml");
    private static final ResponseAssertion CONFIG_PIPELINES_SNIPPET = new ResponseAssertion(HTTP_URL + "/go/admin/pipelines/snippet", HTTP_URL + "/go/rails/admin/pipelines/snippet");
    private static final ResponseAssertion CONFIG_API_FOR_CURRENT = new ResponseAssertion(HTTP_URL + "/go/api/admin/config.xml", HTTP_URL + "/go/admin/restful/configuration/file/GET/xml?version=current");
    private static final ResponseAssertion CONFIG_API_FOR_HISTORICAL = new ResponseAssertion(HTTP_URL + "/go/api/admin/config/some-md5.xml", HTTP_URL + "/go/admin/restful/configuration/file/GET/historical-xml?version=some-md5");

    private static final ResponseAssertion IMAGES_WHILE_BACKUP_IS_IN_PROGRESS = new ResponseAssertion(HTTP_URL + "/go/images/foo.png", HTTP_URL + "/go/images/foo.png");
    private static final ResponseAssertion JAVASCRIPT_WHILE_BACKUP_IS_IN_PROGRESS = new ResponseAssertion(HTTP_URL + "/go/javascripts/foo.js", HTTP_URL + "/go/javascripts/foo.js");
    private static final ResponseAssertion COMPRESSED_JAVASCRIPT_WHILE_BACKUP_IS_IN_PROGRESS = new ResponseAssertion(HTTP_URL + "/go/compressed/all.js", HTTP_URL + "/go/compressed/all.js");
    private static final ResponseAssertion STYLESHEETS_WHILE_BACKUP_IS_IN_PROGRESS = new ResponseAssertion(HTTP_URL + "/go/stylesheets/foo.css", HTTP_URL + "/go/stylesheets/foo.css", true);

    private static final ResponseAssertion PLUGINS_LISTING = new ResponseAssertion(HTTP_URL + "/go/admin/plugins", HTTP_URL + "/go/spark/admin/plugins", true);
    private static final ResponseAssertion PACKAGE_REPOSITORIES_LISTING = new ResponseAssertion(HTTP_URL + "/go/admin/package_repositories", HTTP_URL + "/go/spark/admin/package_repositories", true);
    private static final ResponseAssertion CONFIG_CHANGE = new ResponseAssertion(HTTP_URL + "/go/admin/config_change/md5_value", HTTP_URL + "/go/rails/admin/config_change/md5_value", true);
    private static final ResponseAssertion CONFIG_XML_VIEW = new ResponseAssertion(HTTP_URL + "/go/admin/config_xml", HTTP_URL + "/go/rails/admin/config_xml", true);
    private static final ResponseAssertion CONFIG_XML_EDIT = new ResponseAssertion(HTTP_URL + "/go/admin/config_xml/edit", HTTP_URL + "/go/rails/admin/config_xml/edit", true);

    private static final ResponseAssertion PIPELINE_CONFIG_EDIT = new ResponseAssertion(HTTP_URL + "/go/admin/pipelines/pipeline/edit#!pipeline/materials", HTTP_URL + "/go/spark/admin/pipelines/pipeline/edit", true);

    private static final ResponseAssertion ARTIFACT_API_JSON_LISTING = new ResponseAssertion(HTTP_URL + "/go/files/pipeline/1/stage/2/job.json", HTTP_URL + "/go/repository/restful/artifact/GET/json?pipelineName=pipeline&pipelineCounter=1&stageName=stage&stageCounter=2&buildName=job&filePath=", true);
    private static final ResponseAssertion ARTIFACT_API_GET_FILE = new ResponseAssertion(HTTP_URL + "/go/files/pipeline/1/stage/2/job/tmp/file", HTTP_URL + "/go/repository/restful/artifact/GET/?pipelineName=pipeline&pipelineCounter=1&stageName=stage&stageCounter=2&buildName=job&filePath=tmp%2Ffile", true);
    private static final ResponseAssertion ARTIFACT_API_PUSH_FILE = new ResponseAssertion(HTTP_URL + "/go/files/pipeline/1/stage/2/job/tmp/file", HTTP_URL + "/go/repository/restful/artifact/POST/?pipelineName=pipeline&pipelineCounter=1&stageName=stage&stageCounter=2&buildName=job&filePath=tmp%2Ffile", METHOD.POST, true);
    private static final ResponseAssertion ARTIFACT_API_PUSH_FILE_AGENT_REMOTING = new ResponseAssertion(HTTP_URL + "/go/remoting/files/pipeline/1/stage/2/job/file%25?attempt=100&buildId=1000", HTTP_URL + "/go/repository/restful/artifact/POST/?pipelineName=pipeline&pipelineCounter=1&stageName=stage&stageCounter=2&buildName=job&filePath=file%25&attempt=100&buildId=1000", METHOD.POST, true);
    private static final ResponseAssertion ARTIFACT_API_PUSH_FILE_AGENT_REMOTING_NO_PATH = new ResponseAssertion(HTTP_URL + "/go/remoting/files/pipeline/1/stage/2/job/?attempt=100&buildId=1000", HTTP_URL + "/go/repository/restful/artifact/POST/?pipelineName=pipeline&pipelineCounter=1&stageName=stage&stageCounter=2&buildName=job&filePath=&attempt=100&buildId=1000", METHOD.POST, true);
    private static final ResponseAssertion ARTIFACT_API_CHANGE_FILE = new ResponseAssertion(HTTP_URL + "/go/files/pipeline/1/stage/2/job/file", HTTP_URL + "/go/repository/restful/artifact/PUT/?pipelineName=pipeline&pipelineCounter=1&stageName=stage&stageCounter=2&buildName=job&filePath=file", METHOD.PUT, true);
    private static final ResponseAssertion ARTIFACT_API_CONSOLE_LOG = new ResponseAssertion(HTTP_URL + "/go/files/pipeline/1/stage/2/job/cruise-output/console.log?startLineNumber=1000", HTTP_URL + "/go/consoleout.json?pipelineName=pipeline&pipelineCounter=1&stageName=stage&stageCounter=2&buildName=job&filePath=cruise-output%2Fconsole.log&startLineNumber=1000");

    private static final ResponseAssertion PIPELINES_STAGE_DETAILS = new ResponseAssertion(HTTP_URL + "/go/pipelines/pipeline/1/stage/2", HTTP_URL + "/go/rails/pipelines/pipeline/1/stage/2");
    private static final ResponseAssertion PIPELINES_STAGE_HISTORY_PAGINATION = new ResponseAssertion(HTTP_URL + "/go/history/stage/pipeline/1/stage/2?page=3&tab=jobs", HTTP_URL + "/go/rails/history/stage/pipeline/1/stage/2?page=3&tab=jobs");
    private static final ResponseAssertion BUILD_JOB_DETAILS = new ResponseAssertion(HTTP_URL + "/go/tab/build/detail/pipeline/1/stage/2/job", HTTP_URL + "/go/tab/build/recent?pipelineName=pipeline&pipelineCounter=1&stageName=stage&stageCounter=2&jobName=job");
    private static final ResponseAssertion MATERIALS_VALUE_STREAM_MAP = new ResponseAssertion(HTTP_URL + "/go/materials/value_stream_map/fingerprint/revision", HTTP_URL + "/go/rails/materials/value_stream_map/fingerprint/revision");

    private static final ResponseAssertion LANDING_PAGE_SLASH = new ResponseAssertion(HTTP_URL + "/go/", HTTP_URL + "/go/spark/dashboard", true);

    private static final ResponseAssertion LANDING_PAGE_HOME = new ResponseAssertion(HTTP_URL + "/go/home", HTTP_URL + "/go/spark/dashboard", true);
    private static final ResponseAssertion LANDING_PAGE_PIPELINES = new ResponseAssertion(HTTP_URL + "/go/pipelines", HTTP_URL + "/go/spark/dashboard");

    @SuppressWarnings("unused")
    private static Stream<ResponseAssertion> testResponseAssertions() {
        return Stream.of(
            NO_REWRITE,
            PIPELINE_GROUP_CREATE,
            SERVER_BACKUP,
            STATIC_PAGES,
            ASSETS,
            CONFIG_FILE_XML,
            CONFIG_PIPELINES_SNIPPET,
            CONFIG_API_FOR_CURRENT,
            CONFIG_API_FOR_HISTORICAL,
            IMAGES_WHILE_BACKUP_IS_IN_PROGRESS,
            JAVASCRIPT_WHILE_BACKUP_IS_IN_PROGRESS,
            COMPRESSED_JAVASCRIPT_WHILE_BACKUP_IS_IN_PROGRESS,
            STYLESHEETS_WHILE_BACKUP_IS_IN_PROGRESS,
            PLUGINS_LISTING,
            PACKAGE_REPOSITORIES_LISTING,
            CONFIG_CHANGE,
            CONFIG_XML_VIEW,
            CONFIG_XML_EDIT,
            PIPELINE_CONFIG_EDIT,
            ARTIFACT_API_JSON_LISTING,
            ARTIFACT_API_GET_FILE,
            ARTIFACT_API_PUSH_FILE,
            ARTIFACT_API_PUSH_FILE_AGENT_REMOTING,
            ARTIFACT_API_PUSH_FILE_AGENT_REMOTING_NO_PATH,
            ARTIFACT_API_CHANGE_FILE,
            ARTIFACT_API_CONSOLE_LOG,
            PIPELINES_STAGE_DETAILS,
            PIPELINES_STAGE_HISTORY_PAGINATION,
            BUILD_JOB_DETAILS,
            MATERIALS_VALUE_STREAM_MAP,
            LANDING_PAGE_SLASH,
            LANDING_PAGE_HOME,
            LANDING_PAGE_PIPELINES
        );
    }

    @BeforeAll
    public static void beforeClass() throws Exception {
        httpUtil = new HttpTestUtil(ctx -> {
            wac = mock(WebApplicationContext.class);
            ctx.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);

            ctx.setBaseResource(Resource.newResource(new File("src/main/webapp/WEB-INF/urlrewrite.xml").getParentFile()));
            ctx.addFilter(UrlRewriteFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST)).setInitParameter("confPath", "/urlrewrite.xml");
            ctx.addServlet(HttpTestUtil.EchoServlet.class, "/*");
        });
        httpUtil.httpConnector(HTTP);

        ServerConfigService serverConfigService = mock(ServerConfigService.class);
        when(wac.getBean("serverConfigService")).thenReturn(serverConfigService);
        doAnswer(invocation -> {
            String url = invocation.getArgument(0);
            return new SiteUrl(HTTP_SITE_URL).siteUrlFor(url);
        }).when(serverConfigService).siteUrlFor(anyString());

        httpUtil.start();

        FeatureToggleService featureToggleService = mock(FeatureToggleService.class);
        when(featureToggleService.isToggleOn(anyString())).thenReturn(true);
        Toggles.initializeWith(featureToggleService);
    }

    @AfterAll
    public static void afterClass() {
        httpUtil.stop();
    }


    enum METHOD {
        GET, POST, PUT
    }

    private static class ResponseAssertion {
        private final String requestedUrl;
        private final String servedUrl;
        private boolean useConfiguredUrls = false;
        private final int responseCode = 200;
        private final METHOD method;

        ResponseAssertion(String requestedUrl, String servedUrl, METHOD method) {
            this.requestedUrl = requestedUrl;
            this.servedUrl = servedUrl;
            this.method = method;
        }

        ResponseAssertion(String requestedUrl, String servedUrl) {
            this(requestedUrl, servedUrl, METHOD.GET);
        }

        ResponseAssertion(String requestedUrl, String servedUrl, boolean useConfiguredUrls) {
            this(requestedUrl, servedUrl);
            this.useConfiguredUrls = useConfiguredUrls;
        }

        ResponseAssertion(String requestedUrl, String servedUrl, METHOD method, boolean useConfiguredUrls) {
            this(requestedUrl, servedUrl, method);
            this.useConfiguredUrls = useConfiguredUrls;
        }

        @Override
        public String toString() {
            return String.format("%s %s", method, requestedUrl);
        }
    }

    @ParameterizedTest
    @MethodSource("testResponseAssertions")
    public void shouldRewrite(final ResponseAssertion assertion) throws Exception {
        GoAgentServerHttpClientBuilder builder = new GoAgentServerHttpClientBuilder(null, SslVerificationMode.NONE, null, null, null);
        try (CloseableHttpClient httpClient = builder.build()) {
            HttpRequestBase httpMethod;
            if (assertion.method == METHOD.GET) {
                httpMethod = new HttpGet(assertion.requestedUrl);
            } else if (assertion.method == METHOD.POST) {
                httpMethod = new HttpPost(assertion.requestedUrl);
            } else if (assertion.method == METHOD.PUT) {
                httpMethod = new HttpPut(assertion.requestedUrl);
            } else {
                throw new RuntimeException("Method has to be one of GET, POST and PUT. Was: " + assertion.method);
            }

            try (CloseableHttpResponse response = httpClient.execute(httpMethod)) {
                assertThat(response.getStatusLine().getStatusCode()).isEqualTo(assertion.responseCode);
                assertThat(new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8)).isEqualTo(assertion.servedUrl);
            }
        }
    }
}
