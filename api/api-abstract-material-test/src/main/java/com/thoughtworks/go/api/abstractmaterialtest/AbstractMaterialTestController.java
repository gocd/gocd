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

package com.thoughtworks.go.api.abstractmaterialtest;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.api.util.MessageJson;
import com.thoughtworks.go.apiv11.admin.shared.representers.materials.MaterialsRepresenter;
import com.thoughtworks.go.apiv11.admin.shared.representers.stages.ConfigHelperOptions;
import com.thoughtworks.go.config.exceptions.UnprocessableEntityException;
import com.thoughtworks.go.config.materials.PasswordDeserializer;
import com.thoughtworks.go.config.materials.ScmMaterial;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.ValidationBean;
import com.thoughtworks.go.server.service.CheckConnectionSubprocessExecutionContext;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.MaterialConfigConverter;
import com.thoughtworks.go.server.service.SecretParamResolver;
import com.thoughtworks.go.util.SystemEnvironment;
import org.jetbrains.annotations.NotNull;
import spark.Request;
import spark.Response;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.thoughtworks.go.api.util.HaltApiResponses.haltBecauseEntityFailedValidation;
import static java.lang.String.join;

public abstract class AbstractMaterialTestController extends ApiController {
    private final GoConfigService goConfigService;
    private final PasswordDeserializer passwordDeserializer;
    private final MaterialConfigConverter materialConfigConverter;
    private final SystemEnvironment systemEnvironment;
    private final SecretParamResolver secretParamResolver;

    public AbstractMaterialTestController(ApiVersion version, GoConfigService goConfigService, PasswordDeserializer passwordDeserializer, MaterialConfigConverter materialConfigConverter, SystemEnvironment systemEnvironment, SecretParamResolver secretParamResolver) {
        super(version);
        this.goConfigService = goConfigService;
        this.passwordDeserializer = passwordDeserializer;
        this.materialConfigConverter = materialConfigConverter;
        this.systemEnvironment = systemEnvironment;
        this.secretParamResolver = secretParamResolver;
    }

    /**
     * Tests the connection for the passed request. It is assumed that the caller has validated permissions as correct
     * for the user to access or edit pipeline's/materials within the relevant context or will do so within
     * {@link #resolveTestMaterial}
     */
    public String testConnection(Request request, Response response) {
        JsonReader requestReader = GsonTransformer.getInstance().jsonReaderFrom(request.body());

        String type = requestReader.getString("type");
        haltIfMaterialTypeIsInvalid(type);
        haltIfMaterialTypeDoesNotSupportsCheckConnection(type);

        ScmMaterialConfig scmMaterialConfig = parseValidatedScmMaterialFromRequest(requestReader);

        Material material = resolveTestMaterial(request, scmMaterialConfig);

        ValidationBean checkConnectionResult = material.checkConnection(new CheckConnectionSubprocessExecutionContext(systemEnvironment));
        if (!checkConnectionResult.isValid()) {
            throw haltBecauseEntityFailedValidation(checkConnectionResult);
        }

        response.status(200);
        return MessageJson.create("Connection OK.");
    }

    abstract protected Material resolveTestMaterial(Request request, ScmMaterialConfig materialConfig);

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    protected Material resolveMaterialSecretsFor(ScmMaterialConfig scmMaterialConfig, Optional<String> pipelineGroupName) {
        Material material = materialConfigConverter.toMaterial(scmMaterialConfig);
        if (material instanceof ScmMaterial scmMaterial) {
            secretParamResolver.resolve(scmMaterial, pipelineGroupName);
        }
        return material;
    }

    private @NotNull ScmMaterialConfig parseValidatedScmMaterialFromRequest(JsonReader requestBody) {
        ScmMaterialConfig scmMaterialConfig = buildScmMaterialFromRequestBody(requestBody);
        scmMaterialConfig.validateConcreteScmMaterial();

        if (!scmMaterialConfig.errors().isEmpty()) {
            String errorMessage = scmMaterialConfig.errors().entrySet().stream()
                .map(e -> String.format("- %s: %s", e.getKey(), join(", ", e.getValue())))
                .collect(Collectors.joining("\n", "There was an error with the material configuration.\n", ""));
            throw haltBecauseEntityFailedValidation(errorMessage, jsonWriter(scmMaterialConfig));
        }
        return scmMaterialConfig;
    }

    private ScmMaterialConfig buildScmMaterialFromRequestBody(JsonReader requestBody) {
        ConfigHelperOptions options = new ConfigHelperOptions(goConfigService.getCurrentConfig(), passwordDeserializer);
        return (ScmMaterialConfig) MaterialsRepresenter.fromJSON(requestBody, options);
    }

    private Consumer<OutputWriter> jsonWriter(ScmMaterialConfig materialConfig) {
        return outputWriter -> MaterialsRepresenter.toJSON(outputWriter, materialConfig);
    }

    private void haltIfMaterialTypeIsInvalid(String type) {
        List<String> materialTypes = List.of("git", "hg", "svn", "p4", "tfs", "dependency", "package", "plugin");
        if (!materialTypes.contains(type)) {
            throw new UnprocessableEntityException(String.format("Invalid material type '%s'. It has to be one of %s.", type, materialTypes));
        }
    }

    private void haltIfMaterialTypeDoesNotSupportsCheckConnection(String type) {
        List<String> scmMaterialTypes = List.of("git", "hg", "svn", "p4", "tfs");
        if (!scmMaterialTypes.contains(type)) {
            throw new UnprocessableEntityException(String.format("The material of type '%s' does not support connection testing.", type));
        }
    }
}
