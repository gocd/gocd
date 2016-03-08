/*
 * Copyright 2016 ThoughtWorks, Inc.
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

import com.thoughtworks.go.domain.ServerSiteUrlConfig;
import com.thoughtworks.go.server.util.HttpTestUtil;
import com.thoughtworks.go.server.util.ServletHelper;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.springframework.web.context.WebApplicationContext;
import org.tuckey.web.filters.urlrewrite.UrlRewriteFilter;

import javax.servlet.DispatcherType;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Theories.class)
public class UrlRewriterIntegrationTest {
    public static HttpTestUtil httpUtil;
    public static final int HTTP = 5197;
    public static final int HTTPS = 9071;
    public static WebApplicationContext wac;
    public static boolean useConfiguredUrls;
    public static String originalSslPort;

    @BeforeClass
    public static void beforeClass() throws Exception {
        ServletHelper.init();
        httpUtil = new HttpTestUtil(new HttpTestUtil.ContextCustomizer() {
            public void customize(WebAppContext ctx) throws Exception {
                wac = mock(WebApplicationContext.class);
                ctx.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);

                URL resource = getClass().getClassLoader().getResource("WEB-INF/urlrewrite.xml");
                if (resource == null) {
                    throw new RuntimeException("Cannot load WEB-INF/urlrewrite.xml");
                }

                ctx.setBaseResource(Resource.newResource(new File(resource.getFile()).getParent()));
                ctx.addFilter(UrlRewriteFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST)).setInitParameter("confPath", "/urlrewrite.xml");
                ctx.addServlet(HttpTestUtil.EchoServlet.class, "/*");
            }
        });
        httpUtil.httpConnector(HTTP);
        httpUtil.httpsConnector(HTTPS);
        when(wac.getBean("serverConfigService")).thenReturn(new BaseUrlProvider() {
            public boolean hasAnyUrlConfigured() {
                return useConfiguredUrls;
            }

            public String siteUrlFor(String url, boolean forceSsl) throws URISyntaxException {
                ServerSiteUrlConfig siteUrl = forceSsl ? new ServerSiteUrlConfig("https://127.2.2.2:" + 9071) : new ServerSiteUrlConfig("http://127.2.2.2:" + 5197);
                return siteUrl.siteUrlFor(url);
            }
        });

        httpUtil.start();
        Protocol.registerProtocol("https", new Protocol("https", (ProtocolSocketFactory) new PermissiveSSLSocketFactory(), HTTPS));
        originalSslPort = System.getProperty(SystemEnvironment.CRUISE_SERVER_SSL_PORT);
        System.setProperty(SystemEnvironment.CRUISE_SERVER_SSL_PORT, String.valueOf(9071));
    }

    @AfterClass
    public static void afterClass() {
        if (originalSslPort == null) {
            System.getProperties().remove(SystemEnvironment.CRUISE_SERVER_SSL_PORT);
        } else {
            System.setProperty(SystemEnvironment.CRUISE_SERVER_SSL_PORT, originalSslPort);
        }
        httpUtil.stop();
    }

    enum METHOD {
        GET, POST, PUT
    }

    private static class ResponseAssertion {
        private final String requestedUrl;
        private final String servedUrl;
        private boolean useConfiguredUrls = false;
        private Map<String, String> responseHeaders = new HashMap<String, String>();
        private int responseCode = 200;
        private METHOD method;

        public ResponseAssertion(String requestedUrl, String servedUrl, METHOD method) {
            this.requestedUrl = requestedUrl;
            this.servedUrl = servedUrl;
            this.method = method;
        }

        public ResponseAssertion(String requestedUrl, String servedUrl) {
            this(requestedUrl, servedUrl, METHOD.GET);
        }

        public ResponseAssertion(String requestedUrl, String servedUrl, boolean useConfiguredUrls) {
            this(requestedUrl, servedUrl);
            this.useConfiguredUrls = useConfiguredUrls;
        }

        public ResponseAssertion(String requestedUrl, String servedUrl, METHOD method, boolean useConfiguredUrls) {
            this(requestedUrl, servedUrl, method);
            this.useConfiguredUrls = useConfiguredUrls;
        }
    }

    @DataPoint public static ResponseAssertion NO_REWRITE = new ResponseAssertion("http://127.1.1.1:" + HTTP + "/go/quux?hello=world", "http://127.1.1.1:" + HTTP + "/go/quux?hello=world");
    @DataPoint public static ResponseAssertion NO_REWRITE_SSL = new ResponseAssertion("https://127.1.1.1:" + HTTPS +"/go/quux?hello=world", "https://127.1.1.1:" + HTTPS + "/go/quux?hello=world");
    @DataPoint public static ResponseAssertion OAUTH = new ResponseAssertion("http://127.1.1.1:" + HTTP +"/go/foo/oauth/bar?hello=world", "http://127.1.1.1:" + HTTP + "/go/foo/oauth/bar?hello=world");//error handled in ssh_helper
    @DataPoint public static ResponseAssertion RAILS_BOUND = new ResponseAssertion("http://127.1.1.1:" + HTTP +"/go/agents/foo?hello=world", "http://127.1.1.1:" + HTTP + "/go/rails/agents/foo?hello=world");
    @DataPoint public static ResponseAssertion RAILS_BOUND_SSL = new ResponseAssertion("https://127.1.1.1:" + HTTPS +"/go/agents/foo?hello=world", "https://127.1.1.1:" + HTTPS + "/go/rails/agents/foo?hello=world");
    @DataPoint public static ResponseAssertion GADGET_RENDERING_IFRAME = new ResponseAssertion("http://127.1.1.1:" + HTTP +"/go/gadgets/ifr?hello=world", "http://127.1.1.1:" + HTTP + "/go/rails/gadgets/ifr?hello=world");
    @DataPoint public static ResponseAssertion UNDER_GADGET_RENDERING_IFRAME = new ResponseAssertion("http://127.1.1.1:" + HTTP +"/go/gadgets/ifr/foo?hello=world", "http://127.1.1.1:" + HTTP + "/go/rails/gadgets/ifr/foo?hello=world");//error handled in ssh_helper
    @DataPoint public static ResponseAssertion GADGET_REQUEST = new ResponseAssertion("http://127.1.1.1:" + HTTP +"/go/gadgets/makeRequest?hello=world", "http://127.1.1.1:" + HTTP + "/go/rails/gadgets/makeRequest?hello=world");
    @DataPoint public static ResponseAssertion UNDER_GADGET_REQUEST = new ResponseAssertion("http://127.1.1.1:" + HTTP +"/go/gadgets/makeRequest/junk?hello=world", "http://127.1.1.1:" + HTTP + "/go/rails/gadgets/makeRequest/junk?hello=world");//error handled in ssh_helper
    @DataPoint public static ResponseAssertion GADGET_JS = new ResponseAssertion("http://127.1.1.1:" + HTTP +"/go/gadgets/js/foo.js?version=24", "http://127.1.1.1:" + HTTP + "/go/rails/gadgets/js/foo.js?version=24");
    @DataPoint public static ResponseAssertion GADGET_JS_NESTED = new ResponseAssertion("http://127.1.1.1:" + HTTP +"/go/gadgets/js/foo/bar/baz/quux.js?version=24", "http://127.1.1.1:" + HTTP + "/go/rails/gadgets/js/foo/bar/baz/quux.js?version=24");
    @DataPoint public static ResponseAssertion GADGET_CONCAT = new ResponseAssertion("http://127.1.1.1:" + HTTP +"/go/gadgets/concat?foo=bar&baz=quux", "http://127.1.1.1:" + HTTP + "/go/rails/gadgets/concat?foo=bar&baz=quux");
    @DataPoint public static ResponseAssertion GADGET_PROXY = new ResponseAssertion("http://127.1.1.1:" + HTTP +"/go/gadgets/proxy?foo=bar&baz=quux", "http://127.1.1.1:" + HTTP + "/go/rails/gadgets/proxy?foo=bar&baz=quux");
    @DataPoint public static ResponseAssertion GADGET_ADMIN = new ResponseAssertion("http://127.1.1.1:" + HTTP +"/go/admin/gadgets/bar?foo=bar&baz=quux", "http://127.1.1.1:" + HTTP + "/go/rails/admin/gadgets/bar?foo=bar&baz=quux");//error handled in ssh_helper
    @DataPoint public static ResponseAssertion GADGET_ADMIN_SSL = new ResponseAssertion("https://127.1.1.1:" + HTTPS +"/go/admin/gadgets/bar?foo=bar&baz=quux", "https://127.1.1.1:" + HTTPS + "/go/rails/admin/gadgets/bar?foo=bar&baz=quux");//error handled in ssh_helper
    @DataPoint public static ResponseAssertion GADGET_ADMIN_WITH_URLS_CONFIGURED = new ResponseAssertion("http://127.1.1.1:" + HTTP +"/go/admin/gadgets/bar?foo=bar&baz=quux", "https://127.2.2.2:" + HTTPS + "/go/rails/admin/gadgets/bar?foo=bar&baz=quux", true);
    @DataPoint public static ResponseAssertion PIPELINE_GROUPS_LISTING = new ResponseAssertion("http://127.1.1.1:" + HTTP +"/go/admin/pipelines?foo=bar&baz=quux", "http://127.1.1.1:" + HTTP + "/go/rails/admin/pipelines?foo=bar&baz=quux", true);
    @DataPoint public static ResponseAssertion PIPELINE_GROUP_EDIT = new ResponseAssertion("http://127.1.1.1:" + HTTP +"/go/admin/pipeline_group/group.name?foo=bar&baz=quux", "http://127.1.1.1:" + HTTP + "/go/rails/admin/pipeline_group/group.name?foo=bar&baz=quux", true);
    @DataPoint public static ResponseAssertion PIPELINE_GROUP_CREATE = new ResponseAssertion("http://127.1.1.1:" + HTTP +"/go/admin/pipeline_group", "http://127.1.1.1:" + HTTP + "/go/rails/admin/pipeline_group", true);
    @DataPoint public static ResponseAssertion TEMPLATES_LISTING = new ResponseAssertion("http://127.1.1.1:" + HTTP +"/go/admin/templates?foo=bar&baz=quux", "http://127.1.1.1:" + HTTP + "/go/rails/admin/templates?foo=bar&baz=quux", true);
    @DataPoint public static ResponseAssertion CONFIG_VIEW = new ResponseAssertion("http://127.1.1.1:" + HTTP +"/go/config_view/templates/template_name", "http://127.1.1.1:" + HTTP + "/go/rails/config_view/templates/template_name");

    @DataPoint public static ResponseAssertion PIPELINE_NEW = new ResponseAssertion("http://127.1.1.1:" + HTTP +"/go/admin/pipeline/new", "http://127.1.1.1:" + HTTP + "/go/rails/admin/pipeline/new", true);
    @DataPoint public static ResponseAssertion PIPELINE_CREATE = new ResponseAssertion("http://127.1.1.1:" + HTTP +"/go/admin/pipelines", "http://127.1.1.1:" + HTTP + "/go/rails/admin/pipelines", METHOD.POST);
    @DataPoint public static ResponseAssertion SERVER_BACKUP = new ResponseAssertion("http://127.1.1.1:" + HTTP +"/go/admin/backup", "http://127.1.1.1:" + HTTP + "/go/rails/admin/backup", true);

    @DataPoint public static ResponseAssertion SERVER_BACKUP_OTHER_ACTIONS = new ResponseAssertion("http://127.1.1.1:" + HTTP +"/go/admin/backup/foo?abc=dfx", "http://127.1.1.1:" + HTTP + "/go/rails/admin/backup/foo?abc=dfx", true);

    @DataPoint public static ResponseAssertion STATIC_PAGES = new ResponseAssertion("http://127.1.1.1:" + HTTP +"/go/static/foo.html?bar=baz", "http://127.1.1.1:" + HTTP + "/go/static/foo.html?bar=baz", true);

    @DataPoint public static final ResponseAssertion CONFIG_FILE_XML = new ResponseAssertion("http://127.1.1.1:" + HTTP +"/go/admin/configuration/file.xml", "http://127.1.1.1:" + HTTP +"/go/admin/restful/configuration/file/GET/xml");
    @DataPoint public static final ResponseAssertion CONFIG_API_FOR_CURRENT = new ResponseAssertion("http://127.1.1.1:" + HTTP +"/go/api/admin/config.xml", "http://127.1.1.1:" + HTTP + "/go/admin/restful/configuration/file/GET/xml?version=current");
    @DataPoint public static final ResponseAssertion CONFIG_API_FOR_HISTORICAL = new ResponseAssertion("http://127.1.1.1:" + HTTP +"/go/api/admin/config/some-md5.xml", "http://127.1.1.1:" + HTTP +"/go/admin/restful/configuration/file/GET/historical-xml?version=some-md5");

    @DataPoint public static ResponseAssertion IMAGES_WHILE_BACKUP_IS_IN_PROGRESS = new ResponseAssertion("http://127.1.1.1:" + HTTP +"/go/images/foo.png", "http://127.1.1.1:" + HTTP +"/go/images/foo.png");
    @DataPoint public static ResponseAssertion JAVASCRIPT_WHILE_BACKUP_IS_IN_PROGRESS = new ResponseAssertion("http://127.1.1.1:" + HTTP +"/go/javascripts/foo.js", "http://127.1.1.1:" + HTTP +"/go/javascripts/foo.js");
    @DataPoint public static ResponseAssertion COMPRESSED_JAVASCRIPT_WHILE_BACKUP_IS_IN_PROGRESS = new ResponseAssertion("http://127.1.1.1:" + HTTP +"/go/compressed/all.js", "http://127.1.1.1:" + HTTP +"/go/compressed/all.js");
    @DataPoint public static ResponseAssertion STYLESHEETS_WHILE_BACKUP_IS_IN_PROGRESS = new ResponseAssertion("http://127.1.1.1:" + HTTP +"/go/stylesheets/foo.css", "http://127.1.1.1:" + HTTP +"/go/stylesheets/foo.css", true);

    @DataPoint public static ResponseAssertion TASKS_LOOKUP_LISTING = new ResponseAssertion("http://127.1.1.1:" + HTTP +"/go/admin/commands", "http://127.1.1.1:" + HTTP + "/go/rails/admin/commands", true);
    @DataPoint public static ResponseAssertion TASKS_LOOKUP_SHOW = new ResponseAssertion("http://127.1.1.1:" + HTTP +"/go/admin/commands/show", "http://127.1.1.1:" + HTTP + "/go/rails/admin/commands/show", true);
    @DataPoint public static ResponseAssertion PLUGINS_LISTING = new ResponseAssertion("http://127.1.1.1:" + HTTP +"/go/admin/plugins", "http://127.1.1.1:" + HTTP + "/go/rails/admin/plugins", true);
    @DataPoint public static ResponseAssertion PACKAGE_REPOSITORIES_LISTING = new ResponseAssertion("http://127.1.1.1:" + HTTP +"/go/admin/package_repositories", "http://127.1.1.1:" + HTTP + "/go/rails/admin/package_repositories", true);
    @DataPoint public static ResponseAssertion PACKAGE_DEFINITIONS = new ResponseAssertion("http://127.1.1.1:" + HTTP +"/go/admin/package_definitions", "http://127.1.1.1:" + HTTP + "/go/rails/admin/package_definitions", true);
    @DataPoint public static ResponseAssertion PLUGGABLE_SCM = new ResponseAssertion("http://127.1.1.1:" + HTTP +"/go/admin/materials/pluggable_scm/check_connection/plugin_id", "http://127.1.1.1:" + HTTP + "/go/rails/admin/materials/pluggable_scm/check_connection/plugin_id", true);
    @DataPoint public static ResponseAssertion CONFIG_CHANGE = new ResponseAssertion("http://127.1.1.1:" + HTTP +"/go/config_change/md5_value", "http://127.1.1.1:" + HTTP + "/go/rails/config_change/md5_value", true);
    @DataPoint public static ResponseAssertion CONFIG_XML_VIEW = new ResponseAssertion("http://127.1.1.1:" + HTTP +"/go/admin/config_xml", "http://127.1.1.1:" + HTTP + "/go/rails/admin/config_xml", METHOD.GET, true);
    @DataPoint public static ResponseAssertion CONFIG_XML_EDIT = new ResponseAssertion("http://127.1.1.1:" + HTTP +"/go/admin/config_xml/edit", "http://127.1.1.1:" + HTTP + "/go/rails/admin/config_xml/edit", METHOD.GET, true);

    @DataPoint public static ResponseAssertion ARTIFACT_API_HTML_LISTING = new ResponseAssertion("http://127.1.1.1:" + HTTP +"/go/files/pipeline/1/stage/1/job.html", "http://127.1.1.1:" + HTTP + "/go/repository/restful/artifact/GET/html?pipelineName=pipeline&pipelineLabel=1&stageName=stage&stageCounter=1&buildName=job&filePath=", true);
    @DataPoint public static ResponseAssertion ARTIFACT_API_HTML_LISTING_FILENAME = new ResponseAssertion("http://127.1.1.1:" + HTTP +"/go/files/pipeline/1/stage/1/target/abc%2Bfoo.txt", "http://127.1.1.1:" + HTTP + "/go/repository/restful/artifact/GET/?pipelineName=pipeline&pipelineLabel=1&stageName=stage&stageCounter=1&buildName=target&filePath=abc%2Bfoo.txt", true);
    @DataPoint public static ResponseAssertion ARTIFACT_API_JSON_LISTING = new ResponseAssertion("http://127.1.1.1:" + HTTP +"/go/files/pipeline/1/stage/1/job.json", "http://127.1.1.1:" + HTTP + "/go/repository/restful/artifact/GET/json?pipelineName=pipeline&pipelineLabel=1&stageName=stage&stageCounter=1&buildName=job&filePath=", true);
    @DataPoint public static ResponseAssertion ARTIFACT_API_GET_FILE = new ResponseAssertion("http://127.1.1.1:" + HTTP +"/go/files/pipeline/1/stage/1/job//tmp/file", "http://127.1.1.1:" + HTTP + "/go/repository/restful/artifact/GET/?pipelineName=pipeline&pipelineLabel=1&stageName=stage&stageCounter=1&buildName=job&filePath=%2Ftmp%2Ffile", true);
    @DataPoint public static ResponseAssertion ARTIFACT_API_PUSH_FILE = new ResponseAssertion("http://127.1.1.1:" + HTTP +"/go/files/pipeline/1/stage/1/job//tmp/file", "http://127.1.1.1:" + HTTP + "/go/repository/restful/artifact/POST/?pipelineName=pipeline&pipelineLabel=1&stageName=stage&stageCounter=1&buildName=job&filePath=%2Ftmp%2Ffile", METHOD.POST, true);
    @DataPoint public static ResponseAssertion ARTIFACT_API_CHANGE_FILE = new ResponseAssertion("http://127.1.1.1:" + HTTP +"/go/files/pipeline/1/stage/1/job/file", "http://127.1.1.1:" + HTTP + "/go/repository/restful/artifact/PUT/?pipelineName=pipeline&pipelineLabel=1&stageName=stage&stageCounter=1&buildName=job&filePath=file", METHOD.PUT, true);

    @DataPoint public static ResponseAssertion ADMIN_GARAGE_INDEX = new ResponseAssertion("http://127.1.1.1:" + HTTP +"/go/admin/garage", "http://127.1.1.1:" + HTTP + "/go/rails/admin/garage");
    @DataPoint public static ResponseAssertion ADMIN_GARAGE_GC = new ResponseAssertion("http://127.1.1.1:" + HTTP +"/go/admin/garage/gc", "http://127.1.1.1:" + HTTP + "/go/rails/admin/garage/gc", METHOD.POST);
    @DataPoint public static ResponseAssertion PIPELINE_DASHBOARD_JSON = new ResponseAssertion("http://127.1.1.1:" + HTTP +"/go/pipelines.json", "http://127.1.1.1:" + HTTP + "/go/rails/pipelines.json", METHOD.GET);
    @DataPoint public static ResponseAssertion MATERIALS_VALUE_STREAM_MAP = new ResponseAssertion("http://127.1.1.1:" + HTTP +"/go/materials/value_stream_map/fingerprint/revision", "http://127.1.1.1:" + HTTP + "/go/rails/materials/value_stream_map/fingerprint/revision", METHOD.GET);

    @DataPoint public static ResponseAssertion RAILS_INTERNAL_API = new ResponseAssertion("http://127.1.1.1:" + HTTP +"/go/api/config/internal/pluggable_task/indix.s3fetch", "http://127.1.1.1:" + HTTP + "/go/rails/api/config/internal/pluggable_task/indix.s3fetch", METHOD.GET);

    @DataPoint public static ResponseAssertion USERS_INDEX_API = new ResponseAssertion("http://127.1.1.1:" + HTTP +"/go/api/users", "http://127.1.1.1:" + HTTP + "/go/rails/api/users", METHOD.GET);
    @DataPoint public static ResponseAssertion USERS_SHOW_API = new ResponseAssertion("http://127.1.1.1:" + HTTP +"/go/api/users/some.one", "http://127.1.1.1:" + HTTP + "/go/rails/api/users/some.one", METHOD.GET);

    public static String enc(String str) {
        try {
            return URLEncoder.encode(str, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Theory
    public void shouldRewrite(final ResponseAssertion assertion) throws IOException {
        useConfiguredUrls = assertion.useConfiguredUrls;
        HttpClient httpClient = new HttpClient(new HttpClientParams());

        HttpMethod httpMethod;
        if (assertion.method == METHOD.GET) {
            httpMethod = new GetMethod(assertion.requestedUrl);
        } else if (assertion.method == METHOD.POST) {
            httpMethod = new PostMethod(assertion.requestedUrl);
        } else if (assertion.method == METHOD.PUT) {
            httpMethod = new PutMethod(assertion.requestedUrl);
        } else {
            throw new RuntimeException("Method has to be one of GET, POST and PUT. Was: " + assertion.method);
        }

        int resp = httpClient.executeMethod(httpMethod);
        assertThat("status code match failed", resp, is(assertion.responseCode));
        assertThat("handler url match failed", httpMethod.getResponseBodyAsString(), is(assertion.servedUrl));
        for (Map.Entry<String, String> headerValPair : assertion.responseHeaders.entrySet()) {
            Header responseHeader = httpMethod.getResponseHeader(headerValPair.getKey());
            if (headerValPair.getValue() == null) {
                assertThat("header match failed", responseHeader, is(nullValue()));
            } else {
                assertThat("header match failed", responseHeader.getValue(), is(headerValPair.getValue()));
            }
        }
    }
}
