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

import com.thoughtworks.go.server.service.support.toggle.FeatureToggleService;
import com.thoughtworks.go.server.service.support.toggle.Toggles;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.jetty.util.resource.Resource;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.web.context.WebApplicationContext;
import org.tuckey.web.filters.urlrewrite.UrlRewriteFilter;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.stream.Stream;

import static com.thoughtworks.go.server.web.UrlRewriterIntegrationTest.ResponseAssertion.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UrlRewriterIntegrationTest {
    private static final String IP_1 = "127.0.0.1";
    private static final int HTTP = 5197;

    private static final String HTTP_URL = "http://" + IP_1 + ":" + HTTP;

    private static HttpTestUtil httpUtil;
    private static WebApplicationContext wac;

    private static final ResponseAssertion NO_REWRITE = of(
        "/go/quux?hello=world",
        "/go/quux?hello=world");
    private static final ResponseAssertion PIPELINE_GROUP_CREATE = of(
        "/go/api/admin/pipeline_groups",
        "/go/spark/api/admin/pipeline_groups", METHOD.POST);

    private static final ResponseAssertion SERVER_BACKUP = of(
        "/go/admin/backup",
        "/go/spark/admin/backup");

    private static final ResponseAssertion STATIC_PAGES = of(
        "/go/static/foo.html?bar=baz",
        "/go/static/foo.html?bar=baz");
    private static final ResponseAssertion ASSETS = of(
        "/go/assets/some-image.png",
        "/go/rails/assets/some-image.png");

    private static final ResponseAssertion CONFIG_PIPELINES_SNIPPET = of(
        "/go/admin/pipelines/snippet",
        "/go/rails/admin/pipelines/snippet");
    private static final ResponseAssertion CONFIG_API_CURRENT = of(
        "/go/api/admin/config.xml",
        "/go/spring-internal/admin/configuration/file/GET/xml");
    private static final ResponseAssertion CONFIG_API_CURRENT_EDIT = of(
        "/go/api/admin/config.xml",
        "/go/spring-internal/admin/configuration/file/POST/xml", METHOD.POST);

    private static final ResponseAssertion IMAGES_WHILE_BACKUP_IS_IN_PROGRESS = of(
        "/go/images/foo.png",
        "/go/images/foo.png");
    private static final ResponseAssertion JAVASCRIPT_WHILE_BACKUP_IS_IN_PROGRESS = of(
        "/go/javascripts/foo.js",
        "/go/javascripts/foo.js");
    private static final ResponseAssertion COMPRESSED_JAVASCRIPT_WHILE_BACKUP_IS_IN_PROGRESS = of(
        "/go/compressed/all.js",
        "/go/compressed/all.js");
    private static final ResponseAssertion STYLESHEETS_WHILE_BACKUP_IS_IN_PROGRESS = of(
        "/go/stylesheets/foo.css",
        "/go/stylesheets/foo.css");

    private static final ResponseAssertion PLUGINS_LISTING = of(
        "/go/admin/plugins",
        "/go/spark/admin/plugins");
    private static final ResponseAssertion PACKAGE_REPOSITORIES_LISTING = of(
        "/go/admin/package_repositories",
        "/go/spark/admin/package_repositories");
    private static final ResponseAssertion CONFIG_CHANGE = of(
        "/go/admin/config_change/md5_value",
        "/go/rails/admin/config_change/md5_value");
    private static final ResponseAssertion CONFIG_XML_VIEW = of(
        "/go/admin/config_xml",
        "/go/rails/admin/config_xml");
    private static final ResponseAssertion CONFIG_XML_EDIT = of(
        "/go/admin/config_xml/edit",
        "/go/rails/admin/config_xml/edit");

    private static final ResponseAssertion PIPELINE_CONFIG_EDIT = of(
        "/go/admin/pipelines/pipeline/edit#!pipeline/materials",
        "/go/spark/admin/pipelines/pipeline/edit");

    private static final ResponseAssertion ARTIFACT_API_JSON_LISTING = of(
        "/go/files/pipeline/1/stage/2/job.json",
        "/go/spring-internal/artifact/GET/json?pipelineName=pipeline&pipelineCounter=1&stageName=stage&stageCounter=2&buildName=job&filePath=");
    private static final ResponseAssertion ARTIFACT_API_GET_FILE = of(
        "/go/files/pipeline/1/stage/2/job/tmp/file",
        "/go/spring-internal/artifact/GET/?pipelineName=pipeline&pipelineCounter=1&stageName=stage&stageCounter=2&buildName=job&filePath=tmp%2Ffile");
    private static final ResponseAssertion ARTIFACT_API_PUSH_FILE = of(
        "/go/files/pipeline/1/stage/2/job/tmp/file",
        "/go/spring-internal/artifact/POST/?pipelineName=pipeline&pipelineCounter=1&stageName=stage&stageCounter=2&buildName=job&filePath=tmp%2Ffile", METHOD.POST);
    private static final ResponseAssertion ARTIFACT_API_PUSH_FILE_AGENT_REMOTING = of(
        "/go/remoting/files/pipeline/1/stage/2/job/file%25?attempt=100&buildId=1000",
        "/go/spring-internal/artifact/POST/?pipelineName=pipeline&pipelineCounter=1&stageName=stage&stageCounter=2&buildName=job&filePath=file%25&attempt=100&buildId=1000", METHOD.POST);
    private static final ResponseAssertion ARTIFACT_API_PUSH_FILE_AGENT_REMOTING_NO_PATH = of(
        "/go/remoting/files/pipeline/1/stage/2/job/?attempt=100&buildId=1000",
        "/go/spring-internal/artifact/POST/?pipelineName=pipeline&pipelineCounter=1&stageName=stage&stageCounter=2&buildName=job&filePath=&attempt=100&buildId=1000", METHOD.POST);
    private static final ResponseAssertion ARTIFACT_API_CHANGE_FILE = of(
        "/go/files/pipeline/1/stage/2/job/file",
        "/go/spring-internal/artifact/PUT/?pipelineName=pipeline&pipelineCounter=1&stageName=stage&stageCounter=2&buildName=job&filePath=file", METHOD.PUT);
    private static final ResponseAssertion ARTIFACT_API_CONSOLE_LOG = of(
        "/go/files/pipeline/1/stage/2/job/cruise-output/console.log?startLineNumber=1000",
        "/go/spring-internal/consoleout.json?pipelineName=pipeline&pipelineCounter=1&stageName=stage&stageCounter=2&buildName=job&filePath=cruise-output%2Fconsole.log&startLineNumber=1000");

    private static final ResponseAssertion PIPELINES_STAGE_DETAILS = of(
        "/go/pipelines/pipeline/1/stage/2",
        "/go/rails/pipelines/pipeline/1/stage/2");
    private static final ResponseAssertion PIPELINES_STAGE_HISTORY_PAGINATION = of(
        "/go/history/stage/pipeline/1/stage/2?page=3&tab=jobs",
        "/go/rails/history/stage/pipeline/1/stage/2?page=3&tab=jobs");
    private static final ResponseAssertion BUILD_JOB_DETAILS = of(
        "/go/tab/build/detail/pipeline/1/stage/2/job",
        "/go/tab/build/recent?pipelineName=pipeline&pipelineCounter=1&stageName=stage&stageCounter=2&jobName=job");
    private static final ResponseAssertion MATERIALS_VALUE_STREAM_MAP = of(
        "/go/materials/value_stream_map/fingerprint/revision",
        "/go/rails/materials/value_stream_map/fingerprint/revision");

    private static final ResponseAssertion LANDING_PAGE_SLASH = of(
        "/go/",
        "/go/spark/dashboard");

    private static final ResponseAssertion LANDING_PAGE_HOME = of(
        "/go/home",
        "/go/spark/dashboard");
    private static final ResponseAssertion LANDING_PAGE_PIPELINES = of(
        "/go/pipelines",
        "/go/spark/dashboard");

    @SuppressWarnings("unused")
    private static Stream<ResponseAssertion> testResponseAssertions() {
        return Stream.of(
            NO_REWRITE,
            PIPELINE_GROUP_CREATE,
            SERVER_BACKUP,
            STATIC_PAGES,
            ASSETS,
            CONFIG_PIPELINES_SNIPPET,
            CONFIG_API_CURRENT,
            CONFIG_API_CURRENT_EDIT,
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

    public record ResponseAssertion(String requestedUrl, String servedUrl, METHOD method) {
        static ResponseAssertion of(String requestedUrl, String servedUrl) {
            return of(requestedUrl, servedUrl, METHOD.GET);
        }

        static ResponseAssertion of(String requestedUrl, String servedUrl, METHOD method) {
            return new ResponseAssertion(HTTP_URL + requestedUrl, HTTP_URL + servedUrl, method);
        }
    }

    @ParameterizedTest
    @MethodSource("testResponseAssertions")
    public void shouldRewrite(final ResponseAssertion assertion) throws Exception {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build();
             CloseableHttpResponse response = httpClient.execute(httpMethodFor(assertion))) {
            assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpServletResponse.SC_OK);
            assertThat(new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8)).isEqualTo(assertion.servedUrl);
        }
    }

    private static @NonNull HttpRequestBase httpMethodFor(ResponseAssertion assertion) {
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
        return httpMethod;
    }
}
