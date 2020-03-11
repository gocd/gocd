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
package com.thoughtworks.go.apiv1.packages;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.CrudController;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.apiv1.packages.representers.PackageDefinitionRepresenter;
import com.thoughtworks.go.apiv1.packages.representers.PackageDefinitionsRepresenter;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.materials.PackageDefinitionService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.spark.DeprecatedAPI;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import static com.thoughtworks.go.api.util.HaltApiResponses.haltBecauseEtagDoesNotMatch;
import static spark.Spark.*;

@Component
@DeprecatedAPI(deprecatedApiVersion = ApiVersion.v1, successorApiVersion = ApiVersion.v2, deprecatedIn = "20.3.0", removalIn = "20.6.0", entityName = "Packages")
public class PackagesControllerV1 extends ApiController implements SparkSpringController, CrudController<PackageDefinition> {

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final EntityHashingService entityHashingService;
    private final PackageDefinitionService packageDefinitionService;

    @Autowired
    public PackagesControllerV1(ApiAuthenticationHelper apiAuthenticationHelper, EntityHashingService entityHashingService,
                                PackageDefinitionService packageDefinitionService) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.entityHashingService = entityHashingService;
        this.packageDefinitionService = packageDefinitionService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.Packages.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);
            before("", mimeType, this.apiAuthenticationHelper::checkAdminUserOrGroupAdminUserAnd403);
            before("/*", mimeType, this.apiAuthenticationHelper::checkAdminUserOrGroupAdminUserAnd403);

            get("", mimeType, this::index);
            get(Routes.Packages.PACKAGE_ID, mimeType, this::show);
            post("", mimeType, this::create);
            put(Routes.Packages.PACKAGE_ID, mimeType, this::update);
            delete(Routes.Packages.PACKAGE_ID, mimeType, this::remove);
        });
    }

    String index(Request request, Response response) throws IOException {
        List<PackageDefinition> packages = packageDefinitionService.getPackages();
        return writerForTopLevelObject(request, response, outputWriter -> PackageDefinitionsRepresenter.toJSON(outputWriter, packages));
    }

    String show(Request request, Response response) throws IOException {
        String packageId = request.params("package_id");

        PackageDefinition packageDefinition = fetchEntityFromConfig(packageId);

        String etag = etagFor(packageDefinition);
        if (fresh(request, etag)) {
            return notModified(response);
        }
        setEtagHeader(packageDefinition, response);

        return writerForTopLevelObject(request, response, outputWriter -> PackageDefinitionRepresenter.toJSON(outputWriter, packageDefinition));
    }

    String create(Request request, Response response) {
        PackageDefinition packageDefinition = buildEntityFromRequestBody(request);
        packageDefinition.ensureIdExists();
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        packageDefinitionService.createPackage(packageDefinition, packageDefinition.getRepository().getId(), currentUsername(), result);
        return handleCreateOrUpdateResponse(request, response, packageDefinition, result);
    }

    String update(Request request, Response response) {
        String oldPackageId = request.params("package_id");
        PackageDefinition newPackageDefinition = buildEntityFromRequestBody(request);
        PackageDefinition oldPackageDefinition = fetchEntityFromConfig(oldPackageId);
        String md5 = entityHashingService.md5ForEntity(oldPackageDefinition);
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        if (isPutRequestStale(request, oldPackageDefinition)) {
            throw haltBecauseEtagDoesNotMatch("packageDefinition", oldPackageId);
        }

        packageDefinitionService.updatePackage(oldPackageId, newPackageDefinition, md5, currentUsername(), result);

        setEtagHeader(newPackageDefinition, response);
        return handleCreateOrUpdateResponse(request, response, newPackageDefinition, result);
    }

    String remove(Request request, Response response) {
        String packageId = request.params("package_id");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        PackageDefinition packageDefinition = fetchEntityFromConfig(packageId);

        packageDefinitionService.deletePackage(packageDefinition, currentUsername(), result);

        return handleSimpleMessageResponse(response, result);
    }

    @Override
    public String etagFor(PackageDefinition entityFromServer) {
        return entityHashingService.md5ForEntity(entityFromServer);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.PackageDefinition;
    }

    @Override
    public PackageDefinition doFetchEntityFromConfig(String packageId) {
        return packageDefinitionService.find(packageId);
    }

    @Override
    public PackageDefinition buildEntityFromRequestBody(Request req) {
        JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(req.body());
        return PackageDefinitionRepresenter.fromJSON(jsonReader);
    }

    @Override
    public Consumer<OutputWriter> jsonWriter(PackageDefinition packages) {
        return outputWriter -> PackageDefinitionRepresenter.toJSON(outputWriter, packages);
    }
}
