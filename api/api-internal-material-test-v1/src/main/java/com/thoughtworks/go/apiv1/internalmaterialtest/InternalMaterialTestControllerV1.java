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
package com.thoughtworks.go.apiv1.internalmaterialtest;

import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.abstractmaterialtest.AbstractMaterialTestController;
import com.thoughtworks.go.api.spring.ApiAuthorizationHelper;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.PasswordDeserializer;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.config.preprocessor.ConfigParamPreprocessor;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.MaterialConfigConverter;
import com.thoughtworks.go.server.service.SecretParamResolver;
import com.thoughtworks.go.spark.GlobalExceptionMapper;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import com.thoughtworks.go.util.SystemEnvironment;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;

import java.util.Optional;

import static com.thoughtworks.go.api.util.HaltApiMessages.notFoundMessage;
import static com.thoughtworks.go.api.util.HaltApiResponses.haltBecauseRequiredParamMissing;
import static spark.Spark.*;

@Component
public class InternalMaterialTestControllerV1 extends AbstractMaterialTestController implements SparkSpringController {
    private final ApiAuthorizationHelper apiAuthorizationHelper;
    private final GoConfigService goConfigService;

    @Autowired
    public InternalMaterialTestControllerV1(ApiAuthorizationHelper apiAuthorizationHelper, GoConfigService goConfigService, PasswordDeserializer passwordDeserializer, MaterialConfigConverter materialConfigConverter, SystemEnvironment systemEnvironment, SecretParamResolver secretParamResolver) {
        super(ApiVersion.v1, goConfigService, passwordDeserializer, materialConfigConverter, systemEnvironment, secretParamResolver);
        this.apiAuthorizationHelper = apiAuthorizationHelper;
        this.goConfigService = goConfigService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.InternalMaterialTest.BASE;
    }

    @Override
    public void setupRoutes(GlobalExceptionMapper exceptionMapper) {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);

            before("", mimeType, this.apiAuthorizationHelper::checkPipelineGroupAdminViaNameParamsAnd403);
            post("", mimeType, this::testConnection);
        });
    }

    @Override
    protected Material resolveTestMaterial(Request request, @NotNull ScmMaterialConfig scmMaterialConfig) {
        String pipelineGroupName = Optional.ofNullable(request.queryParams("group_name"))
            .filter(s -> !s.isBlank())
            .orElseThrow(() -> haltBecauseRequiredParamMissing("group_name"));

        Optional<CaseInsensitiveString> pipelineName = Optional.ofNullable(request.queryParams("pipeline_name"))
            .filter(s -> !s.isBlank())
            .map(CaseInsensitiveString::new);

        // If the pipeline name is provided, find the pipeline, ensuring it is in the correct group
        // and add the params to the new pipeline config object
        // Otherwise, pipeline is being created and no parameter expansion is possible.
        if (pipelineName.isPresent()) {
            PipelineConfig pipelineConfig = goConfigService.findGroupByPipelineOptional(pipelineName.get())
                .filter(g -> g.isNamed(pipelineGroupName))
                .map(g -> g.findBy(pipelineName.get()))
                .orElseThrow(() -> new RecordNotFoundException(notFoundMessage()));

            expandParamsIntoMaterialFromExisting(scmMaterialConfig, pipelineConfig);
        }

        // Check permissions for, and resolve secrets. This will work for a config repo material only if there is an Allow/*/* rule.
        return resolveMaterialSecretsFor(scmMaterialConfig, Optional.of(pipelineGroupName));
    }

    private void expandParamsIntoMaterialFromExisting(ScmMaterialConfig scmMaterialConfig, PipelineConfig existingPipeline) {
        PipelineConfig temporaryConfig = new PipelineConfig(
            existingPipeline.name(),
            new MaterialConfigs(scmMaterialConfig));
        temporaryConfig.setParams(existingPipeline.getParams().deepClone());

        new ConfigParamPreprocessor().process(temporaryConfig);
    }
}
