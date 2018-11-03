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

package com.thoughtworks.go.api;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.util.MessageJson;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import spark.Request;
import spark.Response;

import java.util.Objects;
import java.util.function.Consumer;

public interface CrudController<Entity> extends ControllerMethods {
    String etagFor(Entity entityFromServer);

    default boolean isGetOrHeadRequestFresh(Request req, Entity entity) {
        return fresh(req, etagFor(entity));
    }

    default boolean isPutRequestStale(Request req, Entity entity) {
        String etagFromClient = getIfMatch(req);
        if (etagFromClient == null) {
            return true;
        }
        String etagFromServer = etagFor(entity);
        return !Objects.equals(etagFromClient, etagFromServer);
    }

    default Entity fetchEntityFromConfig(String name) {
        Entity entity = doFetchEntityFromConfig(name);
        if (entity == null) {
            throw new RecordNotFoundException();
        }
        return entity;
    }

    Entity doFetchEntityFromConfig(String name);

    Entity buildEntityFromRequestBody(Request req);

    default String handleCreateOrUpdateResponse(Request req, Response res, Entity entity, HttpLocalizedOperationResult result) {
        if (result.isSuccessful()) {
            setEtagHeader(entity, res);
            return jsonize(req, entity);
        } else {
            res.status(result.httpCode());

            String errorMessage = result.message();

            return null == entity ? MessageJson.create(errorMessage) : MessageJson.create(errorMessage, jsonWriter(entity));
        }
    }

    default String jsonize(Request req, Entity entity) {
        return jsonizeAsTopLevelObject(req, jsonWriter(entity));
    }

    Consumer<OutputWriter> jsonWriter(Entity entity);

    default void setEtagHeader(Entity entity, Response res) {
        setEtagHeader(res, etagFor(entity));
    }


}
