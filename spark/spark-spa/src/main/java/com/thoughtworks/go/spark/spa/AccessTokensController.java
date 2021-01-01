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
package com.thoughtworks.go.spark.spa;

import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.server.newsecurity.models.AuthenticationToken;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.service.AuthorizationExtensionCacheService;
import com.thoughtworks.go.server.service.SecurityAuthConfigService;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.SparkController;
import com.thoughtworks.go.spark.spring.SPAAuthenticationHelper;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.TemplateEngine;

import java.util.HashMap;
import java.util.Map;

import static spark.Spark.*;

public class AccessTokensController implements SparkController {
    private final SPAAuthenticationHelper authenticationHelper;
    private final AuthorizationExtensionCacheService authorizationExtensionCacheService;
    private final SecurityAuthConfigService securityAuthConfigService;
    private final TemplateEngine engine;

    public AccessTokensController(SPAAuthenticationHelper authenticationHelper,
                                  AuthorizationExtensionCacheService authorizationExtensionCacheService,
                                  SecurityAuthConfigService securityAuthConfigService,
                                  TemplateEngine engine) {
        this.authenticationHelper = authenticationHelper;
        this.authorizationExtensionCacheService = authorizationExtensionCacheService;
        this.securityAuthConfigService = securityAuthConfigService;
        this.engine = engine;
    }

    @Override
    public String controllerBasePath() {
        return Routes.AccessTokens.SPA_BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", authenticationHelper::checkUserAnd403);
            get("", this::index, engine);
        });
    }

    public ModelAndView index(Request request, Response response) {
        Map<Object, Object> object = new HashMap<Object, Object>() {{
            put("viewTitle", "Access Tokens");
            put("meta", getMeta(request));
        }};
        return new ModelAndView(object, null);
    }

    private Map<String, Object> getMeta(Request request) {
        final AuthenticationToken<?> authenticationToken = SessionUtils.getAuthenticationToken(request.raw());
        final Map<String, Object> meta = new HashMap<>();
        meta.put("pluginId", authenticationToken.getPluginId());
        meta.put("supportsAccessToken", supportsAccessToken(authenticationToken));
        return meta;
    }

    private boolean supportsAccessToken(AuthenticationToken<?> authenticationToken) {
        final SecurityAuthConfig profile = securityAuthConfigService.findProfile(authenticationToken.getAuthConfigId());
        return authorizationExtensionCacheService.isValidUser(authenticationToken.getPluginId(), authenticationToken.getUser().getUsername(), profile);
    }
}
