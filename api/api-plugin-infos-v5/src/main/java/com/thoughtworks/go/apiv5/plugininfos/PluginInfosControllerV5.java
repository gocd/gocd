/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv5.plugininfos;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.apiv5.plugininfos.representers.ExtensionType;
import com.thoughtworks.go.apiv5.plugininfos.representers.PluginInfoRepresenter;
import com.thoughtworks.go.apiv5.plugininfos.representers.PluginInfosRepresenter;
import com.thoughtworks.go.config.exceptions.HttpException;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.config.exceptions.UnprocessableEntityException;
import com.thoughtworks.go.plugin.domain.common.BadPluginInfo;
import com.thoughtworks.go.plugin.domain.common.CombinedPluginInfo;
import com.thoughtworks.go.plugin.infra.DefaultPluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.plugins.InvalidPluginTypeException;
import com.thoughtworks.go.server.service.plugins.builder.DefaultPluginInfoFinder;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.thoughtworks.go.api.util.HaltApiMessages.notFoundMessage;
import static java.util.stream.Collectors.toList;
import static spark.Spark.*;

@Component
public class PluginInfosControllerV5 extends ApiController implements SparkSpringController {

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final EntityHashingService entityHashingService;
    private DefaultPluginManager defaultPluginManager;
    private DefaultPluginInfoFinder pluginInfoFinder;

    @Autowired
    public PluginInfosControllerV5(ApiAuthenticationHelper apiAuthenticationHelper, DefaultPluginInfoFinder pluginInfoFinder, EntityHashingService entityHashingService, DefaultPluginManager defaultPluginManager) {
        super(ApiVersion.v5);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.pluginInfoFinder = pluginInfoFinder;
        this.entityHashingService = entityHashingService;
        this.defaultPluginManager = defaultPluginManager;
    }

    @Override
    public String controllerBasePath() {
        return Routes.PluginInfoAPI.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);

            before("", this.mimeType, this.apiAuthenticationHelper::checkUserAnd403);
            before(Routes.PluginInfoAPI.ID, this.mimeType, this.apiAuthenticationHelper::checkUserAnd403);

            get("", mimeType, this::index);
            get(Routes.PluginInfoAPI.ID, mimeType, this::show);

            exception(HttpException.class, this::httpException);
        });
    }

    public String index(Request request, Response response) throws IOException {
        List<CombinedPluginInfo> pluginInfos = new ArrayList<>();
        String pluginType = request.queryParams("type");
        Boolean includeBad = Boolean.valueOf(request.queryParams("include_bad"));

        try {
            Collection<CombinedPluginInfo> validPluginInfos = this.pluginInfoFinder.allPluginInfos(pluginType).stream()
                    .filter(pluginInfo -> !hasUnsupportedExtensionType(pluginInfo))
                    .collect(Collectors.toList());

            pluginInfos.addAll(validPluginInfos);
        } catch (InvalidPluginTypeException exception) {
            List<String> collect = Arrays.stream(ExtensionType.values()).map(ExtensionType::getExtensionType).collect(toList());
            throw new UnprocessableEntityException(String.format("Invalid plugin type '%s'. It has to be one of '%s'.", pluginType, String.join(", ", collect)));
        }

        if (includeBad) {
            List<BadPluginInfo> badPluginInfos = defaultPluginManager.plugins().stream()
                    .filter(GoPluginDescriptor::isInvalid)
                    .map(BadPluginInfo::new)
                    .collect(toList());

            pluginInfos.addAll(badPluginInfos);
        }

        String etag = etagFor(pluginInfos);

        if (fresh(request, etag)) {
            return notModified(response);
        }
        setEtagHeader(response, etag);
        return writerForTopLevelObject(request, response, writer -> PluginInfosRepresenter.toJSON(writer, pluginInfos));

    }

    public String show(Request request, Response response) throws IOException {
        String pluginId = request.params("id");
        CombinedPluginInfo pluginInfo = this.pluginInfoFinder.pluginInfoFor(pluginId);
        CombinedPluginInfo badPluginInfo = null;


        if (pluginInfo != null && hasUnsupportedExtensionType(pluginInfo)) {
            throw new RecordNotFoundException(notFoundMessage());
        }

        if (pluginInfo == null) {
            GoPluginDescriptor pluginDescriptor = defaultPluginManager.getPluginDescriptorFor(pluginId);
            if (pluginDescriptor != null && pluginDescriptor.isInvalid()) {
                badPluginInfo = new CombinedPluginInfo(new BadPluginInfo(pluginDescriptor));
            }
        }

        if (pluginInfo == null && badPluginInfo == null) {
            throw new RecordNotFoundException(notFoundMessage());
        }

        CombinedPluginInfo extractedPluginInfo = badPluginInfo != null ? badPluginInfo : pluginInfo;

        String etag = etagFor(extractedPluginInfo);
        if (fresh(request, etag)) {
            return notModified(response);
        }

        setEtagHeader(response, etag);
        return writerForTopLevelObject(request, response, writer -> PluginInfoRepresenter.toJSON(writer, extractedPluginInfo));
    }

    private boolean hasUnsupportedExtensionType(CombinedPluginInfo pluginInfo) {
        List<String> extensionTypes = Arrays.stream(ExtensionType.values()).map(ExtensionType::getExtensionType).collect(toList());

        List<String> invalidExtensions = pluginInfo.extensionNames().stream().filter(extensionName -> !extensionTypes.contains(extensionName)).collect(toList());
        return !invalidExtensions.isEmpty();

    }

    private String etagFor(CombinedPluginInfo pluginInfo) {
        return entityHashingService.md5ForEntity(pluginInfo);
    }

    private String etagFor(Collection<CombinedPluginInfo> pluginInfos) {
        return entityHashingService.md5ForEntity(pluginInfos);
    }
}
