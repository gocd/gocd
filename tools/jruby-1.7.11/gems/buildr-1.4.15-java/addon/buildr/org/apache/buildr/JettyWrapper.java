/* Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership.  The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */


package org.apache.buildr;
 
import org.mortbay.jetty.Server;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.handler.AbstractHandler;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.jetty.webapp.WebAppClassLoader;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.HashMap;

/**
 * @author Matthieu Riou <mriou at apache dot org>
 */
public class JettyWrapper {

    private Server _server;
    private ContextHandlerCollection _handlerColl;

    public JettyWrapper(int port) throws Exception {
        _server = new Server(port);
        // Adding the buildr handler to control our server lifecycle
        ContextHandler context = new ContextHandler();
        context.setContextPath("/buildr");
        Handler handler = new BuildrHandler();
        context.setHandler(handler);

        _handlerColl = new ContextHandlerCollection();
        _handlerColl.setHandlers(new Handler[] {context});

        _server.addHandler(_handlerColl);
        _server.start();
    }

/*
    public void join() {
        try {
            _server.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
*/

    private class BuildrHandler extends AbstractHandler {

        private HashMap _apps = new HashMap();

        public void handle(String string, HttpServletRequest request,
                           HttpServletResponse response, int i) throws IOException, ServletException {
            response.setContentType("text/html");
            if (request.getPathInfo().equals("/")) {
                response.getWriter().println("Alive");
                ((Request)request).setHandled(true);
                return;
            } else if (request.getPathInfo().equals("/deploy")) {
                try {
                    String webapp = request.getParameter("webapp");
                    String path = request.getParameter("path");
                    System.out.println("Deploying " + webapp + " in " + path);
                    WebAppContext context;
            
                    context = (WebAppContext) _apps.get(path);
                    if (context != null) {
                        context.stop();
                        _handlerColl.removeHandler(context);
                        _apps.remove(path);
                    }

                    context = new WebAppContext(webapp, path);
                    context.setConfigurationClasses(new String[] {
                        "org.mortbay.jetty.webapp.WebInfConfiguration",
                        "org.mortbay.jetty.webapp.WebXmlConfiguration"});
                    context.setClassLoader(new WebAppClassLoader(context));
                    
                    _handlerColl.addHandler(context);
                    context.start();
                    _apps.put(path, context);
                    response.getWriter().println("Deployed");
                    response.getWriter().println(context.getTempDirectory());
                    ((Request)request).setHandled(true);
                } catch (Throwable e) {
                    e.printStackTrace();
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    ((Request)request).setHandled(true);
                    return;
                }
            } else if (request.getPathInfo().equals("/undeploy")) {
                try {
                    String path = request.getParameter("path");
                    WebAppContext context = (WebAppContext) _apps.get(path);
                    if (context != null) {
                        System.out.println("Undeploying app at " + path);
                        context.stop();
                        _handlerColl.removeHandler(context);
                        _apps.remove(path);
                    }
                    response.getWriter().println("Undeployed");
                    ((Request)request).setHandled(true);
                } catch (Throwable e) {
                    e.printStackTrace();
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    ((Request)request).setHandled(true);
                    return;
                }
            } else if (request.getPathInfo().equals("/stop")) {
                try {
                    _server.stop();
                    _server.destroy();
                    // Brute force
                    System.exit(0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            response.getWriter().println("OK " + request.getPathInfo());
            ((Request)request).setHandled(true);
        }

    }
}
