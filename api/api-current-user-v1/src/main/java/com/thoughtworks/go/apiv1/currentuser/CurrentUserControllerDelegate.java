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

package com.thoughtworks.go.apiv1.currentuser;


import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.apiv1.user.representers.UserRepresenter;
import com.thoughtworks.go.domain.User;
import com.thoughtworks.go.server.service.UserService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.spark.RequestContext;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.util.TriState;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import spark.Request;
import spark.Response;
import spark.Spark;

import java.util.Collection;
import java.util.Map;

import static spark.Spark.*;

public class CurrentUserControllerDelegate extends ApiController {
    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final UserService userService;

    public CurrentUserControllerDelegate(ApiAuthenticationHelper apiAuthenticationHelper, UserService userService) {
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
            before("", this::verifyContentType);
            before("/*", this::verifyContentType);

            before("", mimeType, apiAuthenticationHelper::checkNonAnonymousUser);
            before("/*", mimeType, apiAuthenticationHelper::checkNonAnonymousUser);

            get("", mimeType, this::show, GsonTransformer.getInstance());
            head("", mimeType, this::show, GsonTransformer.getInstance());

            patch("", mimeType, this::update, GsonTransformer.getInstance());
        });
    }

    public Object show(Request req, Response res) {
        User user = userService.findUserByName(currentUserLoginName().toString());
        Map map = UserRepresenter.toJSON(user, RequestContext.requestContext(req));
        String etag = etagFor(map);

        if (fresh(req, etag)) {
            return notModified(res);
        }

        setEtagHeader(res, etag);
        return map;
    }

    public Object update(Request req, Response res) {
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

        user = userService.save(user, TriState.from(null), emailMe, email, checkinAliases, result);

        Map responseMap = UserRepresenter.toJSON(user, RequestContext.requestContext(req));
        String etag = etagFor(responseMap);
        setEtagHeader(res, etag);
        return responseMap;
    }


    String etagFor(Map map) {
        return DigestUtils.md5Hex(GsonTransformer.getInstance().render(map));
    }
}
