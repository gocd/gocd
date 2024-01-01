/*
 * Copyright 2024 Thoughtworks, Inc.
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
package com.thoughtworks.go.server;

import com.thoughtworks.go.util.SystemEnvironment;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.WebAppContext;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AssetsContextHandler extends ContextHandler {
    private final AssetsHandler assetsHandler = new AssetsHandler();
    private final SystemEnvironment systemEnvironment;

    public AssetsContextHandler(SystemEnvironment systemEnvironment) {
        super(systemEnvironment.getWebappContextPath() + "/assets");
        this.systemEnvironment = systemEnvironment;

        GzipHandler gzipHandler = JettyServer.gzipHandler();
        gzipHandler.setHandler(this.assetsHandler);
        setHandler(gzipHandler);
    }

    public void init(WebAppContext webAppContext) throws IOException {
        String railsRootDirName = webAppContext.getInitParameter("rails.root").replaceAll("/WEB-INF/", "");
        try (Resource assetsPathResource = webAppContext.getWebInf().addPath(railsRootDirName + "/public/assets/")) {
            assetsHandler.setAssetsDir(assetsPathResource.getName());
        }
    }

    private boolean shouldNotHandle() {
        return !systemEnvironment.useCompressedJs();
    }

    AssetsHandler getAssetsHandler() {
        return assetsHandler;
    }

    class AssetsHandler extends AbstractHandler {
        private final ResourceHandler resourceHandler = new ResourceHandler();

        private AssetsHandler() {
            resourceHandler.setCacheControl("max-age=31536000,public");
            resourceHandler.setEtags(false);

            resourceHandler.setDirAllowed(false);
            resourceHandler.setDirectoriesListed(false);
        }

        @Override
        protected void doStart() throws Exception {
            resourceHandler.doStart();
            super.doStart();
        }

        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            if (shouldNotHandle()) return;
            this.resourceHandler.handle(target, baseRequest, request, response);
        }

        private void setAssetsDir(String assetsDir) {
            resourceHandler.setResourceBase(assetsDir);
        }

        public ResourceHandler getResourceHandler() {
            return resourceHandler;
        }
    }

}
