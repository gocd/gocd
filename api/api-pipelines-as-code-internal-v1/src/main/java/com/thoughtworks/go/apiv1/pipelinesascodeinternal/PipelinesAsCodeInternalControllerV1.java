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

package com.thoughtworks.go.apiv1.pipelinesascodeinternal;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.api.util.HaltApiResponses;
import com.thoughtworks.go.api.util.MessageJson;
import com.thoughtworks.go.apiv1.pipelinesascodeinternal.representers.ConfigFileListsRepresenter;
import com.thoughtworks.go.apiv8.admin.shared.representers.PipelineConfigRepresenter;
import com.thoughtworks.go.apiv8.admin.shared.representers.materials.MaterialsRepresenter;
import com.thoughtworks.go.apiv8.admin.shared.representers.stages.ConfigHelperOptions;
import com.thoughtworks.go.config.ConfigRepoPlugin;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.GoConfigPluginService;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.PasswordDeserializer;
import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.config.update.CreatePipelineConfigCommand;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.plugin.access.configrepo.ConfigFileList;
import com.thoughtworks.go.plugin.access.configrepo.ExportedConfig;
import com.thoughtworks.go.server.service.*;
import com.thoughtworks.go.server.service.plugins.builder.DefaultPluginInfoFinder;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import spark.HaltException;
import spark.Request;
import spark.Response;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static com.thoughtworks.go.api.util.HaltApiResponses.haltBecauseOfReason;
import static com.thoughtworks.go.plugin.domain.common.PluginConstants.CONFIG_REPO_EXTENSION;
import static com.thoughtworks.go.spark.Routes.PaC.*;
import static java.lang.String.format;
import static spark.Spark.*;

@Component
public class PipelinesAsCodeInternalControllerV1 extends ApiController implements SparkSpringController {

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final PasswordDeserializer passwordDeserializer;
    private final GoConfigService goConfigService;
    private final GoConfigPluginService pluginService;
    private final PipelineConfigService pipelineService;
    private DefaultPluginInfoFinder defaultPluginInfoFinder;
    private MaterialService materialService;
    private final MaterialConfigConverter materialConfigConverter;
    private final SubprocessExecutionContext subprocessExecutionContext;
    private SystemEnvironment systemEnvironment;
    private ConfigRepoService configRepoService;

    @Autowired
    public PipelinesAsCodeInternalControllerV1(
            ApiAuthenticationHelper apiAuthenticationHelper,
            PasswordDeserializer passwordDeserializer,
            GoConfigService goConfigService,
            GoConfigPluginService pluginService,
            PipelineConfigService pipelineService,
            DefaultPluginInfoFinder defaultPluginInfoFinder,
            MaterialService materialService,
            MaterialConfigConverter materialConfigConverter,
            SubprocessExecutionContext subprocessExecutionContext,
            SystemEnvironment systemEnvironment,
            ConfigRepoService configRepoService) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.passwordDeserializer = passwordDeserializer;
        this.goConfigService = goConfigService;
        this.pluginService = pluginService;
        this.pipelineService = pipelineService;
        this.defaultPluginInfoFinder = defaultPluginInfoFinder;
        this.materialService = materialService;
        this.materialConfigConverter = materialConfigConverter;
        this.subprocessExecutionContext = subprocessExecutionContext;
        this.systemEnvironment = systemEnvironment;
        this.configRepoService = configRepoService;
    }

    @Override
    public String controllerBasePath() {
        return BASE_INTERNAL_API;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before(PREVIEW, this.mimeType, this::setContentType, this::verifyContentType, this.apiAuthenticationHelper::checkAdminUserAnd403);
            before(CONFIG_FILES, this.mimeType, this::setContentType, this::verifyContentType, this.apiAuthenticationHelper::checkAdminUserAnd403);

            post(PREVIEW, this.mimeType, this::preview);

            post(CONFIG_FILES, this.mimeType, this::configFiles);
        });
    }

    String configFiles(Request req, Response res) {
        File folder = FileUtil.createTempFolder();
        String responseText;
        try {
            JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(req.body());

            ConfigHelperOptions options = new ConfigHelperOptions(goConfigService.getCurrentConfig(), passwordDeserializer);
            MaterialConfig materialConfig = MaterialsRepresenter.fromJSON(jsonReader, options);

            if (configRepoService.hasConfigRepoByFingerprint(materialConfig.getFingerprint())) {
                res.status(409);
                responseText = MessageJson.create("Material is already being used as a config repository");
            } else {
                Map<String, ConfigFileList> pacPluginFiles = new HashMap<>();
                checkoutFromMaterialConfig(materialConfig, folder);
                defaultPluginInfoFinder.pluginDisplayNameToPluginId(CONFIG_REPO_EXTENSION).forEach((k, pluginId) -> {
                    ConfigRepoPlugin crPlugin = (ConfigRepoPlugin) pluginService.partialConfigProviderFor(pluginId);
                    pacPluginFiles.put(pluginId, crPlugin.getConfigFiles(folder, new ArrayList<>()));
                });
                responseText = jsonizeAsTopLevelObject(req, w -> ConfigFileListsRepresenter.toJSON(w, pacPluginFiles));
            }
        } catch (Exception ex) {
            res.status(500);
            responseText = MessageJson.create(ex.getMessage());
        } finally {
            FileUtils.deleteQuietly(folder);
        }

        return responseText;
    }

    protected void checkoutFromMaterialConfig(MaterialConfig materialConfig, File folder) throws ExecutionException, InterruptedException {
        Material material = materialConfigConverter.toMaterial(materialConfig);
        subprocessExecutionContext.setGitShallowClone(true);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future future = executor.submit(() -> {
            List<Modification> modifications = materialService.latestModification(material, folder, subprocessExecutionContext);
            materialService.checkout(material, folder, Modification.latestRevision(modifications), subprocessExecutionContext);
        });
        try {
            future.get(systemEnvironment.getPacCloneTimeout(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new RuntimeException("Request couldn't be completed because we failed to clone the repo within 2 minutes");
        }
    }

    String preview(Request req, Response res) {
        String pluginId = requiredParam(req, ":plugin_id");
        String groupName = requiredQueryParam(req, "group");

        if (!pluginService.isConfigRepoPlugin(pluginId)) {
            throw haltBecauseOfReason("Plugin `%s` is not a Pipelines-as-Code plugin.", pluginId);
        }

        if (!pluginService.supportsPipelineExport(pluginId)) {
            throw haltBecauseOfReason("Plugin `%s` does not support pipeline config export.", pluginId);
        }

        ConfigHelperOptions options = new ConfigHelperOptions(goConfigService.getCurrentConfig(), passwordDeserializer);
        JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(req.body());

        validateNamePresent(jsonReader);

        PipelineConfig pipeline = PipelineConfigRepresenter.fromJSON(jsonReader, options);

        if (requiresFullValidation(req) && !isValidPipelineConfig(pipeline, groupName)) {
            res.status(HttpStatus.UNPROCESSABLE_ENTITY.value());
            return MessageJson.create(format("Please fix the validation errors for pipeline %s.", pipeline.name()), jsonWriter(pipeline));
        }

        ConfigRepoPlugin repoPlugin = plugin(pluginId);
        String etag = repoPlugin.etagForExport(pipeline, groupName);

        if (fresh(req, etag)) {
            return notModified(res);
        } else {
            setEtagHeader(res, etag);

            ExportedConfig export = repoPlugin.pipelineExport(pipeline, groupName);

            res.header("Content-Type", export.getContentType());
            res.header("Content-Disposition", format("attachment; filename=\"%s\"", export.getFilename()));
            return export.getContent();
        }

    }

    private boolean requiresFullValidation(Request req) {
        return "true".equalsIgnoreCase(req.queryParams("validate"));
    }

    private boolean isValidPipelineConfig(final PipelineConfig pipeline, final String group) throws HaltException {
        CreatePipelineConfigCommand create = pipelineService.createPipelineConfigCommand(currentUsername(), pipeline, null, group);
        CruiseConfig config;

        try {
            config = goConfigService.preprocessedCruiseConfigForPipelineUpdate(create);
        } catch (Exception e) {
            throw HaltApiResponses.haltBecauseOfReason(e.getMessage());
        }

        return create.isValid(config);
    }

    /**
     * @throws spark.HaltException when "name" is missing from JSON body
     */
    private void validateNamePresent(JsonReader jsonReader) {
        jsonReader.getString("name");
    }

    private Consumer<OutputWriter> jsonWriter(PipelineConfig pipelineConfig) {
        return writer -> PipelineConfigRepresenter.toJSON(writer, pipelineConfig);
    }

    private ConfigRepoPlugin plugin(String pluginId) {
        return (ConfigRepoPlugin) pluginService.partialConfigProviderFor(pluginId);
    }

    private String requiredParam(final Request req, final String name) {
        String value = req.params(name);

        if (StringUtils.isBlank(value)) {
            throw HaltApiResponses.haltBecauseRequiredParamMissing(name);
        }

        return value;
    }

    private String requiredQueryParam(final Request req, final String name) {
        String value = req.queryParams(name);

        if (StringUtils.isBlank(value)) {
            throw HaltApiResponses.haltBecauseRequiredParamMissing(name);
        }

        return value;
    }
}
