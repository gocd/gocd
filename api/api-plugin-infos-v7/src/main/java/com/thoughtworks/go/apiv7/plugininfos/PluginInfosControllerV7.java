/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.apiv7.plugininfos;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.spring.ToggleRegisterLatest;
import com.thoughtworks.go.apiv7.plugininfos.representers.PluginInfoRepresenter;
import com.thoughtworks.go.apiv7.plugininfos.representers.PluginInfosRepresenter;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.config.exceptions.UnprocessableEntityException;
import com.thoughtworks.go.plugin.access.ExtensionsRegistry;
import com.thoughtworks.go.plugin.domain.common.BadPluginInfo;
import com.thoughtworks.go.plugin.domain.common.CombinedPluginInfo;
import com.thoughtworks.go.plugin.infra.DefaultPluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.plugins.builder.DefaultPluginInfoFinder;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.thoughtworks.go.api.util.HaltApiMessages.notFoundMessage;
import static java.util.stream.Collectors.toList;
import static spark.Spark.*;

@ToggleRegisterLatest(controllerPath = Routes.PluginInfoAPI.BASE, apiVersion = ApiVersion.v7, as = "branch_support")
@Component
public class PluginInfosControllerV7 extends ApiController implements SparkSpringController {

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final EntityHashingService entityHashingService;
    private final DefaultPluginManager defaultPluginManager;
    private final ExtensionsRegistry extensionsRegistry;
    private final DefaultPluginInfoFinder pluginInfoFinder;

    @Autowired
    public PluginInfosControllerV7(ApiAuthenticationHelper apiAuthenticationHelper, DefaultPluginInfoFinder pluginInfoFinder, EntityHashingService entityHashingService, DefaultPluginManager defaultPluginManager, ExtensionsRegistry extensionsRegistry) {
        super(ApiVersion.v7);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.pluginInfoFinder = pluginInfoFinder;
        this.entityHashingService = entityHashingService;
        this.defaultPluginManager = defaultPluginManager;
        this.extensionsRegistry = extensionsRegistry;
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
        });
    }

    public String index(Request request, Response response) throws IOException {
        List<CombinedPluginInfo> pluginInfos = new ArrayList<>();
        String pluginType = request.queryParams("type");
        Boolean includeBad = Boolean.valueOf(request.queryParams("include_bad"));

        if (StringUtils.isNotBlank(pluginType) && !extensionsRegistry.allRegisteredExtensions().contains(pluginType)) {
            throw new UnprocessableEntityException(String.format("Invalid plugin type '%s'. It has to be one of '%s'.", pluginType, String.join(", ", extensionsRegistry.allRegisteredExtensions())));
        }

        Collection<CombinedPluginInfo> validPluginInfos = this.pluginInfoFinder.allPluginInfos(pluginType).stream()
                .filter(pluginInfo -> !hasUnsupportedExtensionType(pluginInfo))
                .collect(Collectors.toList());

        pluginInfos.addAll(validPluginInfos);

        if (includeBad) {
            List<BadPluginInfo> badPluginInfos = defaultPluginManager.plugins().stream()
                    .filter(GoPluginDescriptor::isInvalid)
                    .map(BadPluginInfo::new)
                    .collect(toList());

            pluginInfos.addAll(badPluginInfos);
        }

        pluginInfos.sort(Comparator.comparing((CombinedPluginInfo pluginInfos1) -> pluginInfos1.getDescriptor().id()));
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

        if (pluginInfo == null) {
            GoPluginDescriptor pluginDescriptor = defaultPluginManager.getPluginDescriptorFor(pluginId);
            if (pluginDescriptor != null && pluginDescriptor.isInvalid()) {
                pluginInfo = new CombinedPluginInfo(new BadPluginInfo(pluginDescriptor));
            } else {
                throw new RecordNotFoundException(notFoundMessage());
            }
        }

        String etag = etagFor(pluginInfo);
        if (fresh(request, etag)) {
            return notModified(response);
        }

        setEtagHeader(response, etag);
        CombinedPluginInfo finalPluginInfo = pluginInfo;
        return writerForTopLevelObject(request, response, writer -> PluginInfoRepresenter.toJSON(writer, finalPluginInfo));
    }

    private boolean hasUnsupportedExtensionType(CombinedPluginInfo pluginInfo) {
        Set<String> extensionTypes = extensionsRegistry.allRegisteredExtensions();

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
