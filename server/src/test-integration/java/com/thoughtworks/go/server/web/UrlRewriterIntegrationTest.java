/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import com.thoughtworks.go.domain.SecureSiteUrl;
import com.thoughtworks.go.domain.ServerSiteUrlConfig;
import com.thoughtworks.go.domain.SiteUrl;
import com.thoughtworks.go.server.service.support.toggle.FeatureToggleService;
import com.thoughtworks.go.server.service.support.toggle.Toggles;
import com.thoughtworks.go.server.util.HttpTestUtil;
import com.thoughtworks.go.server.util.ServletHelper;
import com.thoughtworks.go.util.SslVerificationMode;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.CloseableHttpClient;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.web.context.WebApplicationContext;
import org.tuckey.web.filters.urlrewrite.UrlRewriteFilter;

import javax.servlet.DispatcherType;
import java.io.File;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UrlRewriterIntegrationTest {
    private static final String IP_1 = "127.0.0.1";
    private static final String IP_2 = "127.0.0.2";
    private static final int HTTP = 5197;
    private static final int HTTPS = 9071;

    private static final String HTTP_URL = "http://" + IP_1 + ":" + HTTP;
    private static final String HTTPS_URL = "https://" + IP_1 + ":" + HTTPS;
    private static final String HTTP_SITE_URL = "http://" + IP_2 + ":" + HTTP;
    private static final String HTTPS_SITE_URL = "https://" + IP_2 + ":" + HTTPS;

    private static HttpTestUtil httpUtil;
    private static WebApplicationContext wac;
    private static boolean useConfiguredUrls;

    private static ResponseAssertion NO_REWRITE = new ResponseAssertion(HTTP_URL + "/go/quux?hello=world", HTTP_URL + "/go/quux?hello=world");
    private static ResponseAssertion NO_REWRITE_SSL = new ResponseAssertion(HTTPS_URL + "/go/quux?hello=world", HTTPS_URL + "/go/quux?hello=world");
    private static ResponseAssertion PIPELINE_GROUP_CREATE = new ResponseAssertion(HTTP_URL + "/go/api/admin/pipeline_groups", HTTP_URL + "/go/spark/api/admin/pipeline_groups", METHOD.POST);

    private static ResponseAssertion CONFIG_VIEW = new ResponseAssertion(HTTP_URL + "/go/config_view/templates/template_name", HTTP_URL + "/go/rails/config_view/templates/template_name");

    private static ResponseAssertion SERVER_BACKUP = new ResponseAssertion(HTTP_URL + "/go/admin/backup", HTTP_URL + "/go/spark/admin/backup", true);

    private static ResponseAssertion STATIC_PAGES = new ResponseAssertion(HTTP_URL + "/go/static/foo.html?bar=baz", HTTP_URL + "/go/static/foo.html?bar=baz", true);

    private static final ResponseAssertion CONFIG_FILE_XML = new ResponseAssertion(HTTP_URL + "/go/admin/configuration/file.xml", HTTP_URL + "/go/admin/restful/configuration/file/GET/xml");
    private static final ResponseAssertion CONFIG_API_FOR_CURRENT = new ResponseAssertion(HTTP_URL + "/go/api/admin/config.xml", HTTP_URL + "/go/admin/restful/configuration/file/GET/xml?version=current");
    private static final ResponseAssertion CONFIG_API_FOR_HISTORICAL = new ResponseAssertion(HTTP_URL + "/go/api/admin/config/some-md5.xml", HTTP_URL + "/go/admin/restful/configuration/file/GET/historical-xml?version=some-md5");

    private static ResponseAssertion IMAGES_WHILE_BACKUP_IS_IN_PROGRESS = new ResponseAssertion(HTTP_URL + "/go/images/foo.png", HTTP_URL + "/go/images/foo.png");
    private static ResponseAssertion JAVASCRIPT_WHILE_BACKUP_IS_IN_PROGRESS = new ResponseAssertion(HTTP_URL + "/go/javascripts/foo.js", HTTP_URL + "/go/javascripts/foo.js");
    private static ResponseAssertion COMPRESSED_JAVASCRIPT_WHILE_BACKUP_IS_IN_PROGRESS = new ResponseAssertion(HTTP_URL + "/go/compressed/all.js", HTTP_URL + "/go/compressed/all.js");
    private static ResponseAssertion STYLESHEETS_WHILE_BACKUP_IS_IN_PROGRESS = new ResponseAssertion(HTTP_URL + "/go/stylesheets/foo.css", HTTP_URL + "/go/stylesheets/foo.css", true);

    private static ResponseAssertion TASKS_LOOKUP_LISTING = new ResponseAssertion(HTTP_URL + "/go/admin/commands", HTTP_URL + "/go/rails/admin/commands", true);
    private static ResponseAssertion TASKS_LOOKUP_SHOW = new ResponseAssertion(HTTP_URL + "/go/admin/commands/show", HTTP_URL + "/go/rails/admin/commands/show", true);
    private static ResponseAssertion PLUGINS_LISTING = new ResponseAssertion(HTTP_URL + "/go/admin/plugins", HTTP_URL + "/go/spark/admin/plugins", true);
    private static ResponseAssertion PACKAGE_REPOSITORIES_LISTING = new ResponseAssertion(HTTP_URL + "/go/admin/package_repositories", HTTP_URL + "/go/spark/admin/package_repositories", true);
    private static ResponseAssertion PACKAGE_DEFINITIONS = new ResponseAssertion(HTTP_URL + "/go/admin/package_definitions", HTTP_URL + "/go/rails/admin/package_definitions", true);
    private static ResponseAssertion PLUGGABLE_SCM = new ResponseAssertion(HTTP_URL + "/go/admin/materials/pluggable_scm/check_connection/plugin_id", HTTP_URL + "/go/rails/admin/materials/pluggable_scm/check_connection/plugin_id", true);
    private static ResponseAssertion CONFIG_CHANGE = new ResponseAssertion(HTTP_URL + "/go/admin/config_change/md5_value", HTTP_URL + "/go/rails/admin/config_change/md5_value", true);
    private static ResponseAssertion CONFIG_XML_VIEW = new ResponseAssertion(HTTP_URL + "/go/admin/config_xml", HTTP_URL + "/go/rails/admin/config_xml", METHOD.GET, true);
    private static ResponseAssertion CONFIG_XML_EDIT = new ResponseAssertion(HTTP_URL + "/go/admin/config_xml/edit", HTTP_URL + "/go/rails/admin/config_xml/edit", METHOD.GET, true);

    private static ResponseAssertion ARTIFACT_API_HTML_LISTING = new ResponseAssertion(HTTP_URL + "/go/files/pipeline/1/stage/1/job.html", HTTP_URL + "/go/repository/restful/artifact/GET/html?pipelineName=pipeline&pipelineCounter=1&stageName=stage&stageCounter=1&buildName=job&filePath=", true);
    private static ResponseAssertion ARTIFACT_API_HTML_LISTING_FILENAME = new ResponseAssertion(HTTP_URL + "/go/files/pipeline/1/stage/1/target/abc%2Bfoo.txt", HTTP_URL + "/go/repository/restful/artifact/GET/?pipelineName=pipeline&pipelineCounter=1&stageName=stage&stageCounter=1&buildName=target&filePath=abc%2Bfoo.txt", true);
    private static ResponseAssertion ARTIFACT_API_JSON_LISTING = new ResponseAssertion(HTTP_URL + "/go/files/pipeline/1/stage/1/job.json", HTTP_URL + "/go/repository/restful/artifact/GET/json?pipelineName=pipeline&pipelineCounter=1&stageName=stage&stageCounter=1&buildName=job&filePath=", true);
    private static ResponseAssertion ARTIFACT_API_GET_FILE = new ResponseAssertion(HTTP_URL + "/go/files/pipeline/1/stage/1/job/tmp/file", HTTP_URL + "/go/repository/restful/artifact/GET/?pipelineName=pipeline&pipelineCounter=1&stageName=stage&stageCounter=1&buildName=job&filePath=tmp%2Ffile", true);
    private static ResponseAssertion ARTIFACT_API_PUSH_FILE = new ResponseAssertion(HTTP_URL + "/go/files/pipeline/1/stage/1/job/tmp/file", HTTP_URL + "/go/repository/restful/artifact/POST/?pipelineName=pipeline&pipelineCounter=1&stageName=stage&stageCounter=1&buildName=job&filePath=tmp%2Ffile", METHOD.POST, true);
    private static ResponseAssertion ARTIFACT_API_CHANGE_FILE = new ResponseAssertion(HTTP_URL + "/go/files/pipeline/1/stage/1/job/file", HTTP_URL + "/go/repository/restful/artifact/PUT/?pipelineName=pipeline&pipelineCounter=1&stageName=stage&stageCounter=1&buildName=job&filePath=file", METHOD.PUT, true);

    private static ResponseAssertion PIPELINE_DASHBOARD_JSON = new ResponseAssertion(HTTP_URL + "/go/pipelines.json", HTTP_URL + "/go/rails/pipelines.json", METHOD.GET);
    private static ResponseAssertion MATERIALS_VALUE_STREAM_MAP = new ResponseAssertion(HTTP_URL + "/go/materials/value_stream_map/fingerprint/revision", HTTP_URL + "/go/rails/materials/value_stream_map/fingerprint/revision", METHOD.GET);

    private static ResponseAssertion LANDING_PAGE_SLASH = new ResponseAssertion(HTTP_URL + "/go/", HTTP_URL + "/go/spark/dashboard", true);

    private static ResponseAssertion LANDING_PAGE_HOME = new ResponseAssertion(HTTP_URL + "/go/home", HTTP_URL + "/go/spark/dashboard", true);

    @SuppressWarnings("unused")
    private static Stream<ResponseAssertion> testResponseAssertions() {
        return Stream.of(
                NO_REWRITE,
                NO_REWRITE_SSL,
                PIPELINE_GROUP_CREATE,
                CONFIG_VIEW,
                SERVER_BACKUP,
                STATIC_PAGES,
                CONFIG_FILE_XML,
                CONFIG_API_FOR_CURRENT,
                CONFIG_API_FOR_HISTORICAL,
                IMAGES_WHILE_BACKUP_IS_IN_PROGRESS,
                JAVASCRIPT_WHILE_BACKUP_IS_IN_PROGRESS,
                COMPRESSED_JAVASCRIPT_WHILE_BACKUP_IS_IN_PROGRESS,
                STYLESHEETS_WHILE_BACKUP_IS_IN_PROGRESS,
                TASKS_LOOKUP_LISTING,
                TASKS_LOOKUP_SHOW,
                PLUGINS_LISTING,
                PACKAGE_REPOSITORIES_LISTING,
                PACKAGE_DEFINITIONS,
                PLUGGABLE_SCM,
                CONFIG_CHANGE,
                CONFIG_XML_VIEW,
                CONFIG_XML_EDIT,
                ARTIFACT_API_HTML_LISTING,
                ARTIFACT_API_HTML_LISTING_FILENAME,
                ARTIFACT_API_JSON_LISTING,
                ARTIFACT_API_GET_FILE,
                ARTIFACT_API_PUSH_FILE,
                ARTIFACT_API_CHANGE_FILE,
                PIPELINE_DASHBOARD_JSON,
                MATERIALS_VALUE_STREAM_MAP,
                LANDING_PAGE_SLASH,
                LANDING_PAGE_HOME
        );
    }

    @BeforeAll
    public static void beforeClass() throws Exception {
        ServletHelper.init();
        httpUtil = new HttpTestUtil(new HttpTestUtil.ContextCustomizer() {
            @Override
            public void customize(WebAppContext ctx) throws Exception {
                wac = mock(WebApplicationContext.class);
                ctx.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);

                ctx.setBaseResource(Resource.newResource(new File("src/main/webapp/WEB-INF/urlrewrite.xml").getParentFile()));
                ctx.addFilter(UrlRewriteFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST)).setInitParameter("confPath", "/urlrewrite.xml");
                ctx.addServlet(HttpTestUtil.EchoServlet.class, "/*");
            }
        });
        httpUtil.httpConnector(HTTP);
        httpUtil.httpsConnector(HTTPS);
        when(wac.getBean("serverConfigService")).thenReturn(new BaseUrlProvider() {
            @Override
            public boolean hasAnyUrlConfigured() {
                return useConfiguredUrls;
            }

            @Override
            public String siteUrlFor(String url, boolean forceSsl) throws URISyntaxException {
                ServerSiteUrlConfig siteUrl = forceSsl ? new SecureSiteUrl(HTTPS_SITE_URL) : new SiteUrl(HTTP_SITE_URL);
                return siteUrl.siteUrlFor(url);
            }
        });

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
        private int responseCode = 200;
        private METHOD method;

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
    }

    @ParameterizedTest
    @MethodSource("testResponseAssertions")
    public void shouldRewrite(final ResponseAssertion assertion) throws Exception {
        useConfiguredUrls = assertion.useConfiguredUrls;
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
                assertThat("status code match failed", response.getStatusLine().getStatusCode(), is(assertion.responseCode));
                assertThat("handler url match failed", IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8), is(assertion.servedUrl));
            }
        }
    }
}
