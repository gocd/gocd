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
package com.thoughtworks.go.apiv1.currentuser;


import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.apiv1.user.representers.UserRepresenter;
import com.thoughtworks.go.domain.User;
import com.thoughtworks.go.server.service.UserService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import com.thoughtworks.go.util.TriState;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;
import spark.Spark;

import java.util.Collection;
import java.util.Map;

import static spark.Spark.*;

@Component
public class CurrentUserController extends ApiController implements SparkSpringController {
    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final UserService userService;

    @Autowired
    public CurrentUserController(ApiAuthenticationHelper apiAuthenticationHelper, UserService userService) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.userService = userService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.CurrentUser.BASE;
    }

    @Override
    public void setupRoutes() {
        Spark.path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);
            before("", mimeType, this::verifyContentType);
            before("/*", mimeType, this::verifyContentType);

            before("", mimeType, apiAuthenticationHelper::checkNonAnonymousUser);
            before("/*", mimeType, apiAuthenticationHelper::checkNonAnonymousUser);

            get("", mimeType, this::show);
            head("", mimeType, this::show);

            patch("", mimeType, this::update);
        });
    }

    public String show(Request req, Response res) {
        User user = userService.findUserByName(currentUserLoginName().toString());
        String json = jsonizeAsTopLevelObject(req, writer -> UserRepresenter.toJSON(writer, user));
        String etag = etagFor(json);

        if (fresh(req, etag)) {
            return notModified(res);
        }

        setEtagHeader(res, etag);
        return json;
    }

    public String update(Request req, Response res) {
        User user = userService.findUserByName(currentUserLoginName().toString());

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        Map map = readRequestBodyAsJSON(req);
        String checkinAliases = null;

        if (map.containsKey("checkin_aliases")) {
            Object newAliases = map.get("checkin_aliases");

            if (newAliases instanceof Collection) {
                checkinAliases = StringUtils.join((Collection) newAliases, ", ");
            } else if (newAliases instanceof String) {
                checkinAliases = (String) newAliases;
            }
        }

        TriState emailMe = TriState.from(String.valueOf(map.get("email_me")));
        String email = (String) map.get("email");

        User serializedUser = userService.save(user, TriState.from(null), emailMe, email, checkinAliases, result);

        res.status(result.httpCode());
        String json = jsonizeAsTopLevelObject(req, writer -> UserRepresenter.toJSON(writer, serializedUser, result));
        String etag = etagFor(json);
        setEtagHeader(res, etag);
        return json;
    }
}
