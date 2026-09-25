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
package com.thoughtworks.go.server;

import com.thoughtworks.go.util.SystemEnvironment;
import org.eclipse.jetty.ee8.webapp.WebAppContext;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.util.Callback;

import java.io.IOException;

public class AssetsContextHandler extends ContextHandler {
    private final AssetsHandler assetsHandler = new AssetsHandler();
    private final SystemEnvironment systemEnvironment;

    public AssetsContextHandler(SystemEnvironment systemEnvironment) {
        super(SystemEnvironment.WEBAPP_CONTEXT_PATH + "/assets");
        this.systemEnvironment = systemEnvironment;

        GzipHandler gzipHandler = JettyServer.gzipHandler();
        gzipHandler.setHandler(this.assetsHandler);
        setHandler(gzipHandler);
    }

    public void init(WebAppContext webAppContext) throws IOException {
        String railsRootDirName = webAppContext.getInitParameter("rails.root").replaceAll("/WEB-INF/", "");
        assetsHandler.setAssetsDir(webAppContext.getWebInf().getPath().resolve(railsRootDirName + "/public/assets/").toString());
    }

    AssetsHandler getAssetsHandler() {
        return assetsHandler;
    }

    class AssetsHandler extends Handler.Abstract {
        private final ResourceHandler resourceHandler = new ResourceHandler();

        private AssetsHandler() {
            resourceHandler.setCacheControl("max-age=31536000,public");
            resourceHandler.setEtags(false);
            resourceHandler.setDirAllowed(false);
        }

        @Override
        protected void doStart() throws Exception {
            resourceHandler.doStart();
            super.doStart();
        }

        @Override
        public boolean handle(Request request, Response response, Callback callback) throws Exception {
            if (systemEnvironment.isDevMode()) return false;
            this.resourceHandler.handle(request, response, callback);
            return true;
        }

        private void setAssetsDir(String assetsDir) {
            resourceHandler.setBaseResourceAsString(assetsDir);
        }

        public ResourceHandler getResourceHandler() {
            return resourceHandler;
        }
    }

}
