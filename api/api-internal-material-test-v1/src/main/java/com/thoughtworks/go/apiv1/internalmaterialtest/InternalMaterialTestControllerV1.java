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

package com.thoughtworks.go.apiv1.internalmaterialtest;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.api.util.MessageJson;
import com.thoughtworks.go.apiv10.admin.shared.representers.materials.MaterialsRepresenter;
import com.thoughtworks.go.apiv10.admin.shared.representers.stages.ConfigHelperOptions;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.GoConfigCloner;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineConfigSaveValidationContext;
import com.thoughtworks.go.config.exceptions.UnprocessableEntityException;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.PasswordDeserializer;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.config.preprocessor.ConfigParamPreprocessor;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.ValidationBean;
import com.thoughtworks.go.server.service.CheckConnectionSubprocessExecutionContext;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.MaterialConfigConverter;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static spark.Spark.*;

@Component
public class InternalMaterialTestControllerV1 extends ApiController implements SparkSpringController {

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final GoConfigService goConfigService;
    private final PasswordDeserializer passwordDeserializer;
    private final MaterialConfigConverter materialConfigConverter;
    private final SystemEnvironment systemEnvironment;

    @Autowired
    public InternalMaterialTestControllerV1(ApiAuthenticationHelper apiAuthenticationHelper, GoConfigService goConfigService, PasswordDeserializer passwordDeserializer, MaterialConfigConverter materialConfigConverter, SystemEnvironment systemEnvironment) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.goConfigService = goConfigService;
        this.passwordDeserializer = passwordDeserializer;
        this.materialConfigConverter = materialConfigConverter;
        this.systemEnvironment = systemEnvironment;
    }

    @Override
    public String controllerBasePath() {
        return Routes.InternalMaterialTest.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);

            before("", mimeType, this.apiAuthenticationHelper::checkUserAnd403);

            post("", mimeType, this::testConnection);
        });
    }

    public String testConnection(Request request, Response response) {
        JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(request.body());
        String type = jsonReader.getString("type");
        String pipelineName = jsonReader.getStringOrDefault("pipeline_name", "");
        String pipelineGroupName = jsonReader.getStringOrDefault("pipeline_group", "");

        haltIfMaterialTypeIsInvalid(type);
        haltIfMaterialTypeDoesNotSupportsCheckConnection(type);

        ScmMaterialConfig scmMaterialConfig = buildEntityFromRequestBody(request);

        validateMaterialConfig(scmMaterialConfig, pipelineName, pipelineGroupName);

        if (!scmMaterialConfig.errors().isEmpty()) {
            List<String> errorsList = new ArrayList<>();
            scmMaterialConfig.errors().forEach((key, errors) -> errorsList.add(String.format("- %s: %s", key, StringUtils.join(errors, ", "))));
            response.status(422);
            return MessageJson.create(String.format("There was an error with the material configuration.\n%s", StringUtils.join(errorsList, "\n")), jsonWriter(scmMaterialConfig));
        }

        if (isNotBlank(pipelineName)) {
            performParamExpansion(scmMaterialConfig, pipelineName);
        }
        Material material = materialConfigConverter.toMaterial(scmMaterialConfig);
        ValidationBean validationBean = material.checkConnection(new CheckConnectionSubprocessExecutionContext(systemEnvironment));
        return handleValidationBeanResponse(validationBean, response);
    }

    public ScmMaterialConfig buildEntityFromRequestBody(Request req) {
        JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(req.body());
        ConfigHelperOptions options = new ConfigHelperOptions(goConfigService.getCurrentConfig(), passwordDeserializer);
        return (ScmMaterialConfig) MaterialsRepresenter.fromJSON(jsonReader, options);
    }

    public Consumer<OutputWriter> jsonWriter(ScmMaterialConfig materialConfig) {
        return outputWriter -> MaterialsRepresenter.toJSON(outputWriter, materialConfig);
    }

    private String handleValidationBeanResponse(ValidationBean validationBean, Response response) {
        if (validationBean.isValid()) {
            response.status(200);
            return MessageJson.create("Connection OK.");
        } else {
            response.status(422);
            return MessageJson.create(validationBean.getError());
        }
    }

    private void performParamExpansion(ScmMaterialConfig scmMaterialConfig, String pipelineName) {
        PipelineConfig existingPipeline = goConfigService.pipelineConfigNamed(new CaseInsensitiveString(pipelineName));
        PipelineConfig pipelineConfig = new PipelineConfig(existingPipeline.name(), new MaterialConfigs());

        GoConfigCloner goConfigCloner = new GoConfigCloner();
        pipelineConfig.setParams(goConfigCloner.deepClone(existingPipeline.getParams()));

        pipelineConfig.addMaterialConfig(scmMaterialConfig);

        ConfigParamPreprocessor configParamPreprocessor = new ConfigParamPreprocessor();
        configParamPreprocessor.process(pipelineConfig);
    }

    private void haltIfMaterialTypeIsInvalid(String type) {
        List materialTypes = Arrays.asList("git", "hg", "svn", "p4", "tfs", "dependency", "package", "plugin");
        if (!materialTypes.contains(type)) {
            throw new UnprocessableEntityException(String.format("Invalid material type '%s'. It has to be one of %s.", type, materialTypes));
        }
    }

    private void haltIfMaterialTypeDoesNotSupportsCheckConnection(String type) {
        List scmMaterialTypes = Arrays.asList("git", "hg", "svn", "p4", "tfs");
        if (!scmMaterialTypes.contains(type)) {
            throw new UnprocessableEntityException(String.format("The material of type '%s' does not support connection testing.", type));
        }
    }

    private void validateMaterialConfig(ScmMaterialConfig scmMaterialConfig, String pipelineName, String pipelineGrpName) {
        if (isBlank(pipelineGrpName)) {
            pipelineGrpName = goConfigService.findGroupNameByPipeline(new CaseInsensitiveString(pipelineName));
        }
        scmMaterialConfig.validateConcreteScmMaterial(PipelineConfigSaveValidationContext.forChain(false, pipelineGrpName, goConfigService.getCurrentConfig(), scmMaterialConfig));
    }

}
