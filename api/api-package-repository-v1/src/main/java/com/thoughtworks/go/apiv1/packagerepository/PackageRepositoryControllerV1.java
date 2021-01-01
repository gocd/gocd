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
package com.thoughtworks.go.apiv1.packagerepository;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.CrudController;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.apiv1.packagerepository.representers.PackageRepositoriesRepresenter;
import com.thoughtworks.go.apiv1.packagerepository.representers.PackageRepositoryRepresenter;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.domain.packagerepository.PackageRepositories;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.materials.PackageRepositoryService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.function.Consumer;

import static com.thoughtworks.go.api.util.HaltApiResponses.haltBecauseEtagDoesNotMatch;
import static spark.Spark.*;

@Component
public class PackageRepositoryControllerV1 extends ApiController implements SparkSpringController, CrudController<PackageRepository> {

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final EntityHashingService entityHashingService;
    private final PackageRepositoryService packageRepositoryService;

    @Autowired
    public PackageRepositoryControllerV1(ApiAuthenticationHelper apiAuthenticationHelper, EntityHashingService entityHashingService, PackageRepositoryService packageRepositoryService) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.entityHashingService = entityHashingService;
        this.packageRepositoryService = packageRepositoryService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.PackageRepository.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);
            before("", mimeType, this.apiAuthenticationHelper::checkAdminUserOrGroupAdminUserAnd403);
            before("/*", mimeType, this.apiAuthenticationHelper::checkAdminUserOrGroupAdminUserAnd403);

            get("", mimeType, this::index);
            get(Routes.PackageRepository.REPO_ID, mimeType, this::show);
            post("", mimeType, this::create);
            put(Routes.PackageRepository.REPO_ID, mimeType, this::update);
            delete(Routes.PackageRepository.REPO_ID, mimeType, this::remove);
        });
    }

    String index(Request request, Response response) throws IOException {
        PackageRepositories packageRepositories = packageRepositoryService.getPackageRepositories();
        return writerForTopLevelObject(request, response, outputWriter -> PackageRepositoriesRepresenter.toJSON(outputWriter, packageRepositories));
    }

    String show(Request request, Response response) throws IOException {
        String repoId = request.params("repo_id");
        PackageRepository packageRepository = fetchEntityFromConfig(repoId);

        String etag = etagFor(packageRepository);
        if (fresh(request, etag)) {
            return notModified(response);
        }
        setEtagHeader(response, etag);

        return writerForTopLevelObject(request, response, outputWriter -> PackageRepositoryRepresenter.toJSON(outputWriter, packageRepository));
    }

    String create(Request request, Response response) {
        PackageRepository packageRepository = buildEntityFromRequestBody(request);
        packageRepository.ensureIdExists();
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        packageRepositoryService.createPackageRepository(packageRepository, currentUsername(), result);

        return handleCreateOrUpdateResponse(request, response, packageRepository, result);
    }

    String update(Request request, Response response) {
        PackageRepository packageRepository = buildEntityFromRequestBody(request);
        String repoId = request.params("repo_id");
        PackageRepository oldPackageRepository = fetchEntityFromConfig(repoId);
        String etag = etagFor(oldPackageRepository);
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        if (isPutRequestStale(request, oldPackageRepository)) {
            throw haltBecauseEtagDoesNotMatch("package repository", repoId);
        }

        packageRepositoryService.updatePackageRepository(packageRepository, currentUsername(), etag, result, repoId);

        setEtagHeader(packageRepository, response);

        return handleCreateOrUpdateResponse(request, response, packageRepository, result);
    }

    String remove(Request request, Response response) {
        String repoId = request.params("repo_id");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        PackageRepository packageRepository = fetchEntityFromConfig(repoId);

        packageRepositoryService.deleteRepository(currentUsername(), packageRepository, result);

        return handleSimpleMessageResponse(response, result);
    }

    @Override
    public String etagFor(PackageRepository entityFromServer) {
        return entityHashingService.hashForEntity(entityFromServer);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.PackageRepository;
    }

    @Override
    public PackageRepository doFetchEntityFromConfig(String name) {
        return packageRepositoryService.getPackageRepository(name);
    }

    @Override
    public PackageRepository buildEntityFromRequestBody(Request req) {
        JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(req.body());
        return PackageRepositoryRepresenter.fromJSON(jsonReader);
    }

    @Override
    public Consumer<OutputWriter> jsonWriter(PackageRepository packageRepository) {
        return outputWriter -> PackageRepositoryRepresenter.toJSON(outputWriter, packageRepository);
    }
}
