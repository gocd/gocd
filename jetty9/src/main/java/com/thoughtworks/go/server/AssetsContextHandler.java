/*
 * Copyright 2015 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.thoughtworks.go.server;

import com.thoughtworks.go.util.SystemEnvironment;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlets.gzip.GzipHandler;
import org.eclipse.jetty.webapp.WebAppContext;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AssetsContextHandler extends ContextHandler {
    private final AssetsHandler handler;
    private SystemEnvironment systemEnvironment;

    public AssetsContextHandler(SystemEnvironment systemEnvironment) throws IOException {
        super(systemEnvironment.getWebappContextPath() + "/assets");
        this.systemEnvironment = systemEnvironment;
        handler = new AssetsHandler();

        GzipHandler gzipHandler = new GzipHandler();
        gzipHandler.addIncludedMimeTypes("text/html,text/plain,text/xml,application/xhtml+xml,text/css,application/javascript,image/svg+xml,application/vnd.go.cd.v1+json,application/json");
        gzipHandler.setHandler(handler);
        setHandler(gzipHandler);
    }

    public void init(WebAppContext webAppContext) throws IOException {
        String railsRootDirName = webAppContext.getInitParameter("rails.root").replaceAll("/WEB-INF/", "");
        String assetsDir = webAppContext.getWebInf().addPath(String.format("%s/public/assets/", railsRootDirName)).getName();
        handler.setAssetsDir(assetsDir);
    }

    private boolean shouldNotHandle() {
        return !systemEnvironment.useCompressedJs();
    }

    class AssetsHandler extends AbstractHandler {
        private ResourceHandler resourceHandler = new ResourceHandler();

        private AssetsHandler() {
            resourceHandler.setCacheControl("max-age=31536000,public");
            resourceHandler.setEtags(false);
        }

        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            if (shouldNotHandle()) return;
            this.resourceHandler.handle(target, baseRequest, request, response);
        }

        private void setAssetsDir(String assetsDir) {
            resourceHandler.setResourceBase(assetsDir);
        }
    }

}
