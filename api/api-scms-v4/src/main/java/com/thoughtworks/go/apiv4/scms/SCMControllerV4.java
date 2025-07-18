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
package com.thoughtworks.go.apiv4.scms;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.CrudController;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.apiv4.scms.representers.SCMRepresenter;
import com.thoughtworks.go.apiv4.scms.representers.SCMsRepresenter;
import com.thoughtworks.go.apiv4.scms.representers.ScmUsageRepresenter;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.domain.scm.SCMs;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.materials.PluggableScmService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import com.thoughtworks.go.util.Pair;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.thoughtworks.go.api.util.HaltApiResponses.*;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static spark.Spark.*;

@Component
public class SCMControllerV4 extends ApiController implements SparkSpringController, CrudController<SCM> {

    public static final String MATERIAL_NAME = "material_name";
    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final PluggableScmService pluggableScmService;
    private final EntityHashingService entityHashingService;
    private final GoConfigService goConfigService;

    @Autowired
    public SCMControllerV4(ApiAuthenticationHelper apiAuthenticationHelper, PluggableScmService pluggableScmService, EntityHashingService entityHashingService, GoConfigService goConfigService) {
        super(ApiVersion.v4);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.pluggableScmService = pluggableScmService;
        this.entityHashingService = entityHashingService;
        this.goConfigService = goConfigService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.SCM.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);
            before("", mimeType, this::verifyContentType);
            before("/*", mimeType, this::verifyContentType);

            before("", this.mimeType, this.apiAuthenticationHelper::checkAdminUserOrGroupAdminUserAnd403);
            before("/*", this.mimeType, this.apiAuthenticationHelper::checkAdminUserOrGroupAdminUserAnd403);

            get("", mimeType, this::index);
            post("", mimeType, this::create);
            get(Routes.SCM.ID, mimeType, this::show);
            put(Routes.SCM.ID, mimeType, this::update);
            delete(Routes.SCM.ID, mimeType, this::destroy);
            get(Routes.SCM.USAGES, mimeType, this::getUsages);
        });
    }

    public String index(Request request, Response response) throws IOException {
        SCMs scms = pluggableScmService.listAllScms();

        String etag = entityHashingService.hashForEntity(scms);
        if (fresh(request, etag)) {
            return notModified(response);
        }
        setEtagHeader(response, etag);
        return writerForTopLevelObject(request, response, writer -> SCMsRepresenter.toJSON(writer, scms));
    }

    public String show(Request request, Response response) throws IOException {
        String materialName = request.params(MATERIAL_NAME);

        SCM scm = fetchEntityFromConfig(materialName);

        String etag = entityHashingService.hashForEntity(scm);
        if (fresh(request, etag)) {
            return notModified(response);
        }
        setEtagHeader(response, etag);
        return writerForTopLevelObject(request, response, writer -> SCMRepresenter.toJSON(writer, scm));
    }

    public String create(Request request, Response response) {
        SCM scmFromRequest = buildEntityFromRequestBody(request, false);

        scmFromRequest.ensureIdExists();

        haltIfEntityWithSameNameExists(scmFromRequest);
        haltIfEntityWithSameIDExists(scmFromRequest);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        pluggableScmService.createPluggableScmMaterial(currentUsername(), scmFromRequest, result);

        return handleCreateOrUpdateResponse(request, response, scmFromRequest, result);
    }

    public String update(Request request, Response response) {
        final String materialName = request.params(MATERIAL_NAME);
        final SCM existingSCM = fetchEntityFromConfig(materialName);
        final SCM scmFromRequest = buildEntityFromRequestBody(request);

        if (isRenameAttempt(existingSCM.getId(), scmFromRequest.getId())
                || isRenameAttempt(existingSCM.getName(), scmFromRequest.getName())) {
            throw haltBecauseRenameOfEntityIsNotSupported(getEntityType().getEntityNameLowerCase());
        }

        haltIfEntityIsDefinedRemotely(existingSCM);
        if (isPutRequestStale(request, existingSCM)) {
            throw haltBecauseEtagDoesNotMatch(getEntityType().getEntityNameLowerCase(), existingSCM.getId());
        }

        final HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
        pluggableScmService.updatePluggableScmMaterial(currentUsername(), scmFromRequest, operationResult, getIfMatch(request));

        return handleCreateOrUpdateResponse(request, response, scmFromRequest, operationResult);
    }

    public String destroy(Request request, Response response) throws IOException {
        final String materialName = request.params(MATERIAL_NAME);
        SCM scm = fetchEntityFromConfig(materialName);
        haltIfEntityIsDefinedRemotely(scm);
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        pluggableScmService.deletePluggableSCM(currentUsername(), scm, result);

        return renderHTTPOperationResult(result, request, response);
    }

    public String getUsages(Request request, Response response) throws IOException {
        String materialName = request.params(MATERIAL_NAME);
        SCM scm = fetchEntityFromConfig(materialName);
        Map<String, List<Pair<PipelineConfig, PipelineConfigs>>> allUsages = goConfigService.getCurrentConfig().getGroups().getPluggableSCMMaterialUsageInPipelines();
        List<Pair<PipelineConfig, PipelineConfigs>> scmUsageInPipelines = allUsages.getOrDefault(scm.getId(), emptyList());

        return writerForTopLevelObject(request, response, outputWriter -> ScmUsageRepresenter.toJSON(outputWriter, materialName, scmUsageInPipelines));
    }

    @Override
    public String etagFor(SCM entityFromServer) {
        return entityHashingService.hashForEntity(entityFromServer);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.SCM;
    }

    @Override
    public SCM doFetchEntityFromConfig(String name) {
        return pluggableScmService.findPluggableScmMaterial(name);
    }

    @Override
    public SCM buildEntityFromRequestBody(Request req) {
        JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(req.body());
        return SCMRepresenter.fromJSON(jsonReader);
    }

    public SCM buildEntityFromRequestBody(Request req, boolean mustHaveId) {
        JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(req.body());
        return SCMRepresenter.fromJSON(jsonReader, mustHaveId);
    }

    @Override
    public Consumer<OutputWriter> jsonWriter(SCM scm) {
        return writer -> SCMRepresenter.toJSON(writer, scm);
    }

    private void haltIfEntityWithSameNameExists(SCM scm) {
        if (pluggableScmService.findPluggableScmMaterial(scm.getName()) == null) {
            return;
        }

        scm.addError("name", format("SCM name should be unique. SCM with name '%s' already exists.", scm.getName()));
        throw haltBecauseEntityAlreadyExists(jsonWriter(scm), EntityType.SCM.getEntityNameLowerCase(), scm.getName());
    }

    private void haltIfEntityWithSameIDExists(SCM scm) {
        boolean scmWithSameIdDoesNotExist = pluggableScmService
                .listAllScms()
                .stream()
                .noneMatch(s -> s.getId().equals(scm.getId()));
        if (scmWithSameIdDoesNotExist) {
            return;
        }

        scm.addError("id", format("SCM id should be unique. SCM with id '%s' already exists.", scm.getId()));
        throw haltBecauseEntityAlreadyExists(jsonWriter(scm), EntityType.SCM.getEntityNameLowerCase(), scm.getId());
    }

    private boolean isRenameAttempt(String profileIdFromRequestParam, String profileIdFromRequestBody) {
        return !Strings.CS.equals(profileIdFromRequestBody, profileIdFromRequestParam);
    }

    private void haltIfEntityIsDefinedRemotely(SCM existingSCM) {
        if (!existingSCM.isLocal()) {
            throw haltBecauseOfReason(format("Can not operate on SCM '%s' as it is defined remotely in '%s'.", existingSCM.getName(), existingSCM.getOrigin().displayName()));
        }
    }

}
