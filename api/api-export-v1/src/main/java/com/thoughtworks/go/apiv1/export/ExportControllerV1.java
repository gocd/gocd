/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv1.export;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.HaltApiResponses;
import com.thoughtworks.go.config.ConfigRepoPlugin;
import com.thoughtworks.go.config.GoConfigPluginService;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.plugin.access.configrepo.ExportedConfig;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.spark.Routes.Export;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import static com.thoughtworks.go.api.util.HaltApiResponses.haltBecauseOfReason;
import static java.lang.String.format;
import static spark.Spark.*;

@Component
public class ExportControllerV1 extends ApiController implements SparkSpringController {

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final GoConfigPluginService crPluginService;
    private final GoConfigService configService;
    private final SecurityService securityService;

    @Autowired
    public ExportControllerV1(ApiAuthenticationHelper apiAuthenticationHelper, GoConfigPluginService crPluginService, GoConfigService configService, SecurityService securityService) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.crPluginService = crPluginService;
        this.configService = configService;
        this.securityService = securityService;
    }

    @Override
    public String controllerBasePath() {
        return Export.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);

            before("", this::verifyContentType);
            before("/*", this::verifyContentType);

            before("", this.mimeType, this.apiAuthenticationHelper::checkAdminUserAnd403);
            before(Export.PIPELINES_PATH, mimeType, this.apiAuthenticationHelper::checkAdminUserOrGroupAdminUserAnd403);

            get(Export.PIPELINES_PATH, mimeType, this::exportPipeline);

            exception(RecordNotFoundException.class, this::notFound);
        });
    }

    public String exportPipeline(Request req, Response res) {
        String pluginId = requiredQueryParam(req, "pluginId");
        PipelineConfig pipelineConfig = pipelineConfigFromRequest(req);
        String groupName = configService.findGroupNameByPipeline(pipelineConfig.name());

        if (!securityService.isUserAdminOfGroup(SessionUtils.currentUsername(), groupName)) {
            throw HaltApiResponses.haltBecauseForbidden();
        }

        if (pipelineConfig.hasTemplate()) {
            throw haltBecauseOfReason("Pipeline `%s` cannot be exported because pipelines defined by templates are not yet supported by config-repo plugins.", pipelineConfig.name());
        }

        if (!crPluginService.isConfigRepoPlugin(pluginId)) {
            throw haltBecauseOfReason("Plugin `%s` is not a config-repo plugin.", pluginId);
        }

        if (!crPluginService.supportsPipelineExport(pluginId)) {
            throw haltBecauseOfReason("Plugin `%s` does not support pipeline config export.", pluginId);
        }

        ConfigRepoPlugin repoPlugin = crPlugin(pluginId);
        String etag = repoPlugin.etagForExport(pipelineConfig, groupName);

        if (fresh(req, etag)) {
            return notModified(res);
        } else {
            setEtagHeader(res, etag);

            ExportedConfig export = repoPlugin.pipelineExport(pipelineConfig, groupName);

            res.header("Content-Type", export.getContentType());
            res.header("Content-Disposition", format("attachment; filename=\"%s\"", export.getFilename()));
            return export.getContent();
        }
    }

    private ConfigRepoPlugin crPlugin(String pluginId) {
        return (ConfigRepoPlugin) crPluginService.partialConfigProviderFor(pluginId);
    }

    private PipelineConfig pipelineConfigFromRequest(Request req) {
        final String pipelineName = req.params("pipeline_name");
        PipelineConfig pipeline = configService.pipelineConfigNamed(pipelineName);

        if (null == pipeline) {
            throw new RecordNotFoundException(format("Cannot locate pipeline config with name: %s", pipelineName));
        }

        return pipeline;
    }

    private String requiredQueryParam(final Request req, final String name) {
        String value = req.queryParams(name);

        if (StringUtils.isBlank(value)) {
            throw HaltApiResponses.haltBecauseRequiredParamMissing(name);
        }

        return value;
    }
}
