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

package com.thoughtworks.go.server.api;

import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import spark.Request;
import spark.Response;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public interface CrudController<Entity> extends ControllerMethods {
    String etagFor(Entity entityFromServer);

    default boolean isGetOrHeadRequestFresh(Request req, Entity entity) {
        return fresh(req, etagFor(entity));
    }

    default boolean fresh(Request req, String etagFromServer) {
        String etagFromClient = getIfNoneMatch(req);
        if (etagFromClient == null) {
            return false;
        }
        return Objects.equals(etagFromClient, etagFromServer);
    }

    default boolean isPutRequestFresh(Request req, Entity entity) {
        String etagFromClient = getIfMatch(req);
        if (etagFromClient == null) {
            return false;
        }
        String etagFromServer = etagFor(entity);
        return Objects.equals(etagFromClient, etagFromServer);
    }

    boolean isRenameAttempt(Entity fromServer, Entity fromRequest);

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

    default Map handleCreateOrUpdateResponse(Request req, Response res, Entity entity, HttpLocalizedOperationResult result) {
        if (result.isSuccessful()) {
            setEtagHeader(entity, res);
            return jsonize(req, entity);
        } else {
            res.status(result.httpCode());
            return Collections.singletonMap("message", result.message(getLocalizer()));
        }
    }

    Map jsonize(Request req, Entity entity);

    default void setEtagHeader(Entity entity, Response res) {
        setEtagHeader(res, etagFor(entity));
    }

    default void setEtagHeader(Response res, String value) {
        if (value == null) {
            return;
        }
        res.header("ETag", '"' + value + '"');
    }

}
