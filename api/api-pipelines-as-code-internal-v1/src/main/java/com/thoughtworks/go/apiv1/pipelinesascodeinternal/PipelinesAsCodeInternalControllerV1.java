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
import com.thoughtworks.go.apiv10.admin.shared.representers.PipelineConfigRepresenter;
import com.thoughtworks.go.apiv10.admin.shared.representers.materials.MaterialsRepresenter;
import com.thoughtworks.go.apiv10.admin.shared.representers.stages.ConfigHelperOptions;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.PasswordDeserializer;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.config.update.CreatePipelineConfigCommand;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.plugin.access.configrepo.ConfigFileList;
import com.thoughtworks.go.plugin.access.configrepo.ExportedConfig;
import com.thoughtworks.go.server.service.*;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import spark.HaltException;
import spark.Request;
import spark.Response;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static com.thoughtworks.go.api.util.HaltApiResponses.haltBecauseOfReason;
import static com.thoughtworks.go.spark.Routes.PaC.*;
import static java.lang.String.format;
import static spark.Spark.*;

@Component
public class PipelinesAsCodeInternalControllerV1 extends ApiController implements SparkSpringController {
    private static final Logger LOGGER = LoggerFactory.getLogger(PipelinesAsCodeInternalControllerV1.class);

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final PasswordDeserializer passwordDeserializer;
    private final GoConfigService goConfigService;
    private final GoConfigPluginService pluginService;
    private final PipelineConfigService pipelineService;
    private MaterialService materialService;
    private final MaterialConfigConverter materialConfigConverter;
    private final SubprocessExecutionContext subprocessExecutionContext;
    private SystemEnvironment systemEnvironment;
    private ConfigRepoService configRepoService;
    private EntityHashingService entityHashingService;

    @Autowired
    public PipelinesAsCodeInternalControllerV1(
            ApiAuthenticationHelper apiAuthenticationHelper,
            PasswordDeserializer passwordDeserializer,
            GoConfigService goConfigService,
            GoConfigPluginService pluginService,
            PipelineConfigService pipelineService,
            MaterialService materialService,
            MaterialConfigConverter materialConfigConverter,
            SubprocessExecutionContext subprocessExecutionContext,
            SystemEnvironment systemEnvironment,
            ConfigRepoService configRepoService,
            EntityHashingService entityHashingService) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.passwordDeserializer = passwordDeserializer;
        this.goConfigService = goConfigService;
        this.pluginService = pluginService;
        this.pipelineService = pipelineService;
        this.materialService = materialService;
        this.materialConfigConverter = materialConfigConverter;
        this.subprocessExecutionContext = subprocessExecutionContext;
        this.systemEnvironment = systemEnvironment;
        this.configRepoService = configRepoService;
        this.entityHashingService = entityHashingService;
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
        ConfigRepoPlugin repoPlugin = pluginFromRequest(req);

        File folder = null;

        try {
            JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(req.body());

            ConfigHelperOptions options = new ConfigHelperOptions(goConfigService.getCurrentConfig(), passwordDeserializer);
            MaterialConfig materialConfig = MaterialsRepresenter.fromJSON(jsonReader, options);

            if (!(materialConfig instanceof ScmMaterialConfig)) {
                res.status(HttpStatus.UNPROCESSABLE_ENTITY.value());
                return MessageJson.create(format("This material check requires an SCM repository; instead, supplied material was of type: %s", materialConfig.getType()));
            }

            validateMaterial(materialConfig);

            if (materialConfig.errors().present()) {
                res.status(HttpStatus.UNPROCESSABLE_ENTITY.value());
                return MessageJson.create(format("Please fix the following SCM configuration errors: %s", materialConfig.errors().asString()));
            }

            if (configRepoService.hasConfigRepoByFingerprint(materialConfig.getFingerprint())) {
                res.status(HttpStatus.CONFLICT.value());
                return MessageJson.create("Material is already being used as a config repository");
            }

            folder = FileUtil.createTempFolder();
            checkoutFromMaterialConfig(materialConfig, folder);

            final Map<String, ConfigFileList> pacPluginFiles = Collections.singletonMap(repoPlugin.id(), repoPlugin.getConfigFiles(folder, new ArrayList<>()));
            return jsonizeAsTopLevelObject(req, w -> ConfigFileListsRepresenter.toJSON(w, pacPluginFiles));
        } catch (TimeoutException e) {
            res.status(HttpStatus.PAYLOAD_TOO_LARGE.value());
            return MessageJson.create("Aborted check because cloning the SCM repository took too long");
        } catch (ExecutionException e) {
            res.status(HttpStatus.INTERNAL_SERVER_ERROR.value());
            return MessageJson.create(e.getCause().getMessage()); // unwrap these exceptions thrown by the future
        } catch (Exception e) {
            res.status(HttpStatus.INTERNAL_SERVER_ERROR.value());
            return MessageJson.create(e.getMessage());
        } finally {
            if (null != folder) {
                FileUtils.deleteQuietly(folder);
            }
        }
    }

    String preview(Request req, Response res) {
        ConfigRepoPlugin repoPlugin = pluginFromRequest(req);
        String pluginId = repoPlugin.id();
        String groupName = requiredQueryParam(req, "group");

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

        String etag = entityHashingService.hashForEntity(pipeline, groupName, pluginId);

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

    protected void validateMaterial(MaterialConfig materialConfig) {
        PipelineConfigSaveValidationContext vctx = PipelineConfigSaveValidationContext.forChain(false, null, goConfigService.getCurrentConfig(), materialConfig);
        ((ScmMaterialConfig) materialConfig).validateConcreteScmMaterial(vctx);
    }

    protected void checkoutFromMaterialConfig(MaterialConfig materialConfig, File folder) throws ExecutionException, InterruptedException, TimeoutException {
        Material material = materialConfigConverter.toMaterial(materialConfig);
        long timeout = systemEnvironment.getPacCloneTimeout();

        subprocessExecutionContext.setGitShallowClone(true);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future future = executor.submit(() -> {
            List<Modification> modifications = materialService.latestModification(material, folder, subprocessExecutionContext);
            materialService.checkout(material, folder, Modification.latestRevision(modifications), subprocessExecutionContext);
        });
        try {
            future.get(timeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            LOGGER.debug(format("Failed to clone material %s in %d ms", material.getDescription(), timeout), e);
            future.cancel(true);
            throw e;
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
        String groupName = goConfigService.findGroupNameByPipeline(pipelineConfig.name());
        return writer -> PipelineConfigRepresenter.toJSON(writer, pipelineConfig, groupName);
    }

    private ConfigRepoPlugin pluginFromRequest(Request req) {
        String pluginId = requiredParam(req, ":plugin_id");

        if (!pluginService.isConfigRepoPlugin(pluginId)) {
            throw haltBecauseOfReason("Plugin `%s` is not a Pipelines-as-Code plugin.", pluginId);
        }

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
