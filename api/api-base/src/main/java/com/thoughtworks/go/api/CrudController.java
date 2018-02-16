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

import com.fasterxml.jackson.databind.JsonNode;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.Objects;

public interface CrudController<Entity> extends ControllerMethods {
    String etagFor(Entity entityFromServer);

    default boolean isGetOrHeadRequestFresh(Request req, Entity entity) {
        return fresh(req, etagFor(entity));
    }

    default boolean isPutRequestFresh(Request req, Entity entity) {
        String etagFromClient = getIfMatch(req);
        if (etagFromClient == null) {
            return false;
        }
        String etagFromServer = etagFor(entity);
        return Objects.equals(etagFromClient, etagFromServer);
    }

    default Entity getEntityFromConfig(String name) {
        Entity entity = doGetEntityFromConfig(name);
        if (entity == null) {
            throw new RecordNotFoundException();
        }
        return entity;
    }

    Localizer getLocalizer();

    Entity doGetEntityFromConfig(String name);

    Entity getEntityFromRequestBody(Request req);

    default String handleCreateOrUpdateResponse(Request req, Response res, Entity entity, HttpLocalizedOperationResult result) throws IOException {
        if (result.isSuccessful()) {
            setEtagHeader(entity, res);
            return jsonize(req, entity);
        } else {
            res.status(result.httpCode());

            JsonNode jsonNode = entity == null ? null : jsonNode(req, entity);
            return writerForTopLevelObject(req, res, writer -> {
                    writer.add("message", result.message(getLocalizer()));

                    if (jsonNode != null) {
                        writer.add("data", jsonNode);
                    }
                }

            );
        }
    }

    String jsonize(Request req, Entity entity);

    JsonNode jsonNode(Request req, Entity entity) throws IOException;

    default void setEtagHeader(Entity entity, Response res) {
        setEtagHeader(res, etagFor(entity));
    }


}
