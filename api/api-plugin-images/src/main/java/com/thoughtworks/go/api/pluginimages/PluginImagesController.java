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
package com.thoughtworks.go.api.pluginimages;

import com.thoughtworks.go.api.ControllerMethods;
import com.thoughtworks.go.plugin.domain.common.CombinedPluginInfo;
import com.thoughtworks.go.plugin.domain.common.Image;
import com.thoughtworks.go.server.service.plugins.builder.DefaultPluginInfoFinder;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.SparkController;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import static spark.Spark.*;

@Component
public class PluginImagesController implements SparkController, ControllerMethods, SparkSpringController {
    private final DefaultPluginInfoFinder pluginInfoFinder;

    @Autowired
    public PluginImagesController(DefaultPluginInfoFinder pluginInfoFinder) {
        this.pluginInfoFinder = pluginInfoFinder;
    }

    @Override
    public String controllerBasePath() {
        return Routes.PluginImages.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            get(Routes.PluginImages.PLUGIN_ID_HASH_PATH, this::show);
            head(Routes.PluginImages.PLUGIN_ID_HASH_PATH, this::show);
        });
    }

    public byte[] show(Request request, Response response) {
        String pluginId = request.params("plugin_id");
        String hash = request.params("hash");

        CombinedPluginInfo pluginInfo = pluginInfoFinder.pluginInfoFor(pluginId);
        if (pluginInfo == null) {
            throw halt(404, "");
        }

        Image image = pluginInfo.getImage();
        if (image == null || !image.getHash().equals(hash)) {
            throw halt(404, "");
        }

        response.raw().setHeader("Cache-Control", "max-age=31557600, public");

        if (fresh(request, image.getHash())) {
            notModified(response);
            return new byte[0];
        }

        response.status(200);
        response.header("Content-Type", image.getContentType());
        this.setEtagHeader(response, image.getHash());
        if (request.requestMethod().equals("head")) {
            return new byte[0];
        } else {
            return image.getDataAsBytes();
        }
    }
}
