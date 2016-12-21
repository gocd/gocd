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

package com.thoughtworks.go.agent.testhelper;

import org.apache.commons.io.FileUtils;
import org.apache.poi.util.IOUtils;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.EnumSet;
import java.util.Properties;
import java.util.UUID;

import static com.thoughtworks.go.agent.testhelper.FakeBootstrapperServer.TestResource.*;
import static com.thoughtworks.go.util.FileDigester.md5DigestOfStream;

public class FakeBootstrapperServer extends BlockJUnit4ClassRunner {
    public enum TestResource {
        TEST_AGENT(Resource.newClassPathResource("testdata/gen/test-agent.jar")),
        TEST_AGENT_LAUNCHER(Resource.newClassPathResource("testdata/gen/agent-launcher.jar")),
        TEST_AGENT_PLUGINS(Resource.newClassPathResource("testdata/agent-plugins.zip")),
        TEST_TFS_IMPL(Resource.newClassPathResource("testdata/gen/tfs-impl-14.jar")),;

        private final Resource source;

        TestResource(Resource source) {
            this.source = source;
        }

        public String getMd5() throws IOException {
            try (InputStream in = source.getInputStream()) {
                return md5DigestOfStream(in);
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

    public FakeBootstrapperServer(Class<?> testClass) throws InitializationError {
        super(testClass);
    }

    public void run(RunNotifier runNotifier) {
        try {
            // could be smarter if this is too slow, start only if not started already
            // shut down on JVM shut down instead
            start();
        } catch (Exception e) {
            runNotifier.fireTestFailure(new Failure(getDescription(), e));
        }
        try {
            super.run(runNotifier);
        } finally {
            try {
                stop();
            } catch (Exception e) {
                runNotifier.fireTestFailure(new Failure(getDescription(), e));
            }
        }
    }

    public void start() throws Exception {
        server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(9090);
        server.addConnector(connector);

        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setCertAlias("cruise");
        sslContextFactory.setKeyStoreResource(Resource.newClassPathResource("testdata/fake-server-keystore"));
        sslContextFactory.setKeyStorePassword("serverKeystorepa55w0rd");

        ServerConnector secureConnnector = new ServerConnector(server,
                new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
                new HttpConnectionFactory(new HttpConfiguration())
        );
        secureConnnector.setPort(9091);
        server.addConnector(secureConnnector);

        WebAppContext wac = new WebAppContext(".", "/go");
        ServletHolder holder = new ServletHolder();
        holder.setServlet(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                resp.getOutputStream().println("Hello");
            }
        });
        wac.addServlet(holder, "/hello");
        addFakeAgentBinaryServlet(wac, "/admin/agent", TEST_AGENT);
        addFakeAgentBinaryServlet(wac, "/admin/agent-launcher.jar", TEST_AGENT_LAUNCHER);
        addFakeAgentBinaryServlet(wac, "/admin/agent-plugins.zip", TEST_AGENT_PLUGINS);
        addFakeAgentBinaryServlet(wac, "/admin/tfs-impl.jar", TEST_TFS_IMPL);
        addHeadOnlyServlet(wac, "/broken-jar-download-with-no-response-body");
        addlatestAgentStatusCall(wac);
        addDefaultServlet(wac);
        server.setHandler(wac);
        server.setStopAtShutdown(true);
        server.start();
    }

    private void addHeadOnlyServlet(WebAppContext wac, String pathSpec) {
        ServletHolder holder = new ServletHolder();
        holder.setServlet(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                doHead(req, resp);
            }

            @Override
            protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                resp.setHeader("Content-MD5", UUID.randomUUID().toString());
                resp.setHeader("Cruise-Server-Ssl-Port", "9091");
            }
        });
        wac.addServlet(holder, pathSpec);
    }

    public static final class AgentStatusApi extends HttpServlet {
        public static String status = "disabled";
        public static Properties pluginProps = new Properties();

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setHeader("Plugins-Status", status);
            pluginProps.setProperty("Active Mock Bundle 1", "1.1.1");
            pluginProps.setProperty("Active Mock Bundle 2", "2.2.2");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            pluginProps.store(baos, "Go Plugins for Testing");
            resp.getOutputStream().write(baos.toByteArray());
            baos.close();
        }
    }

    private void addlatestAgentStatusCall(WebAppContext wac) {
        wac.addServlet(AgentStatusApi.class, "/admin/latest-agent.status");
    }

    public static final class BreakpointFriendlyFilter implements Filter {
        public void init(FilterConfig filterConfig) throws ServletException {

        }

        public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
            String requestURI = ((HttpServletRequest) servletRequest).getRequestURI();
            filterChain.doFilter(servletRequest, servletResponse);
        }

        public void destroy() {

        }
    }

    private void addDefaultServlet(WebAppContext wac) {
        wac.addFilter(BreakpointFriendlyFilter.class, "*", EnumSet.of(DispatcherType.REQUEST));
    }

    private static void addFakeAgentBinaryServlet(WebAppContext wac, final String pathSpec, final TestResource resource) {
        ServletHolder holder = new ServletHolder();
        holder.setServlet(new AgentBinariesServlet(resource));
        wac.addServlet(holder, pathSpec);
    }

    public void stop() throws Exception {
        server.stop();
        server.join();
    }

}
