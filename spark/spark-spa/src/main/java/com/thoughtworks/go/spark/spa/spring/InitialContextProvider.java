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

package com.thoughtworks.go.spark.spa.spring;

import com.google.common.base.CaseFormat;
import com.thoughtworks.go.CurrentGoCDVersion;
import com.thoughtworks.go.server.service.RailsAssetsService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.VersionInfoService;
import com.thoughtworks.go.server.service.WebpackAssetsService;
import com.thoughtworks.go.server.service.support.toggle.Toggles;
import com.thoughtworks.go.server.util.UserHelper;
import com.thoughtworks.go.spark.SparkController;
import org.apache.velocity.VelocityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class InitialContextProvider {

    private final RailsAssetsService railsAssetsService;
    private final WebpackAssetsService webpackAssetsService;
    private final SecurityService securityService;
    private final VersionInfoService versionInfoService;

    @Autowired
    public InitialContextProvider(RailsAssetsService railsAssetsService, WebpackAssetsService webpackAssetsService, SecurityService securityService, VersionInfoService versionInfoService) {
        this.railsAssetsService = railsAssetsService;
        this.webpackAssetsService = webpackAssetsService;
        this.securityService = securityService;
        this.versionInfoService = versionInfoService;
    }

    public VelocityContext getVelocityContext(Map<String, Object> modelMap, Class<? extends SparkController> controller, String viewName) {
        HashMap<String, Object> context = new HashMap<>(modelMap);
        context.put("railsAssetsService", railsAssetsService);
        context.put("webpackAssetsService", webpackAssetsService);
        context.put("securityService", securityService);
        context.put("currentUser", UserHelper.getUserName());
        context.put("controllerName", humanizedControllerName(controller));
        context.put("viewName", viewName);
        context.put("currentVersion", CurrentGoCDVersion.getInstance());
        context.put("toggles", Toggles.class);
        context.put("goUpdate", versionInfoService.getGoUpdate());
        context.put("goUpdateCheckEnabled", versionInfoService.isGOUpdateCheckEnabled());

        return new VelocityContext(context);
    }

    private String humanizedControllerName(Class<? extends SparkController> controller) {
        return CaseFormat.UPPER_CAMEL.converterTo(CaseFormat.LOWER_UNDERSCORE).convert(controller.getSimpleName().replaceAll("(Delegate|Controller)", ""));
    }


}
