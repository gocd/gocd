/*
 * Copyright 2023 Thoughtworks, Inc.
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
package com.thoughtworks.go.agent.testhelper;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.assertj.core.util.Hexadecimals;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.jupiter.api.extension.ExtensionContext;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.EnumSet;
import java.util.Properties;

import static com.thoughtworks.go.agent.testhelper.FakeGoServer.TestResource.*;

public class FakeGoServer implements ExtensionContext.Store.CloseableResource {
    public enum TestResource {
        TEST_AGENT(Resource.newClassPathResource("testdata/gen/test-agent.jar")),
        TEST_AGENT_LAUNCHER(Resource.newClassPathResource("testdata/gen/agent-launcher.jar")),
        TEST_AGENT_PLUGINS(Resource.newClassPathResource("testdata/agent-plugins.zip")),
        TEST_TFS_IMPL(Resource.newClassPathResource("testdata/gen/tfs-impl-14.jar")),;

        private final Resource source;

        TestResource(Resource source) {
            this.source = source;
        }

        public String getMd5() {
            try (InputStream input = source.getInputStream()) {
                MessageDigest digester = MessageDigest.getInstance("MD5");
                try (DigestInputStream digest = new DigestInputStream(input, digester)) {
                    IOUtils.copy(digest, new NullOutputStream());
                }
                return Hexadecimals.toHexString(digester.digest()).toLowerCase();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public void copyTo(OutputStream outputStream) throws IOException {
            try (InputStream in = source.getInputStream()) {
                IOUtils.copy(in, outputStream);
            }
        }

        // Because the resource can be a jarresource, which extracts to dir instead of a simple copy.
        public void copyTo(File output) throws IOException {
            try (InputStream in = source.getInputStream()) {
                FileUtils.copyToFile(in, output);
            }
        }
    }

    private Server server;
    private int port;
    private int securePort;
    private String extraPropertiesHeaderValue;

    FakeGoServer() {
    }

    public int getPort() {
        return port;
    }

    public int getSecurePort() {
        return securePort;
    }

    @Override
    public void close() throws Throwable {
        stop();
    }

    private void stop() throws Exception {
        if (server != null) {
            server.stop();
            server.join();
        }
    }

    void start() throws Exception {
        server = new Server();
        ServerConnector connector = new ServerConnector(server);
        server.addConnector(connector);

        SslContextFactory sslContextFactory = new SslContextFactory.Server();
        sslContextFactory.setCertAlias("cruise");
        sslContextFactory.setKeyStoreResource(Resource.newClassPathResource("testdata/fake-server-keystore"));
        sslContextFactory.setKeyStorePassword("serverKeystorepa55w0rd");

        ServerConnector secureConnnector = new ServerConnector(server,
                new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
                new HttpConnectionFactory(new HttpConfiguration())
        );
        server.addConnector(secureConnnector);

        WebAppContext wac = new WebAppContext(".", "/go");
        ServletHolder holder = new ServletHolder();
        holder.setServlet(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                resp.getOutputStream().println("Hello");
            }
        });
        wac.addServlet(holder, "/hello");
        addFakeAgentBinaryServlet(wac, "/admin/agent", TEST_AGENT, this);
        addFakeAgentBinaryServlet(wac, "/admin/agent-launcher.jar", TEST_AGENT_LAUNCHER, this);
        addFakeAgentBinaryServlet(wac, "/admin/agent-plugins.zip", TEST_AGENT_PLUGINS, this);
        addFakeAgentBinaryServlet(wac, "/admin/tfs-impl.jar", TEST_TFS_IMPL, this);
        addlatestAgentStatusCall(wac);
        addDefaultServlet(wac);
        server.setHandler(wac);
        server.setStopAtShutdown(true);
        server.start();

        port = connector.getLocalPort();
        securePort = secureConnnector.getLocalPort();
    }

    private static final class AgentStatusApi extends HttpServlet {
        public static String status = "disabled";
        static Properties pluginProps = new Properties();

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setHeader("Plugins-Status", status);
            pluginProps.setProperty("Active Mock Bundle 1", "1.1.1");
            pluginProps.setProperty("Active Mock Bundle 2", "2.2.2");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            pluginProps.store(baos, "Go Plugins for Testing");
            resp.getOutputStream().write(baos.toByteArray());
            baos.close();
        }
    }

    public FakeGoServer setExtraPropertiesHeaderValue(String value) {
        extraPropertiesHeaderValue = value;
        return this;
    }

    String getExtraPropertiesHeaderValue() {
        return extraPropertiesHeaderValue;
    }

    private void addlatestAgentStatusCall(WebAppContext wac) {
        wac.addServlet(AgentStatusApi.class, "/admin/latest-agent.status");
    }

    public static final class BreakpointFriendlyFilter implements Filter {
        @Override
        public void init(FilterConfig filterConfig) {

        }

        @Override
        public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
            filterChain.doFilter(servletRequest, servletResponse);
        }

        @Override
        public void destroy() {

        }
    }

    private void addDefaultServlet(WebAppContext wac) {
        wac.addFilter(BreakpointFriendlyFilter.class, "*", EnumSet.of(DispatcherType.REQUEST));
    }

    private static void addFakeAgentBinaryServlet(WebAppContext wac, final String pathSpec, final TestResource resource, FakeGoServer fakeGoServer) {
        ServletHolder holder = new ServletHolder();
        holder.setServlet(new AgentBinariesServlet(resource, fakeGoServer));
        wac.addServlet(holder, pathSpec);
    }

}
