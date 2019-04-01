/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import com.thoughtworks.go.server.service.BackupService;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.SparkController;
import com.thoughtworks.go.spark.spring.SPAAuthenticationHelper;
import org.joda.time.DateTime;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.TemplateEngine;

import java.util.HashMap;

import static spark.Spark.*;

public class BackupsController implements SparkController {

    private final SPAAuthenticationHelper authenticationHelper;
    private final TemplateEngine engine;
    private BackupService backupService;

    public BackupsController(SPAAuthenticationHelper authenticationHelper, TemplateEngine engine, BackupService backupService) {
        this.authenticationHelper = authenticationHelper;
        this.engine = engine;
        this.backupService = backupService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.BackupsSPA.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", authenticationHelper::checkAdminUserOrGroupAdminUserAnd403);
            get("", this::index, engine);
        });
    }

    public ModelAndView index(Request request, Response response) {
        HashMap<Object, Object> object = new HashMap<Object, Object>() {{
            put("viewTitle", "Backup");
            put("meta", lastBackupData());
        }};

        return new ModelAndView(object, null);
    }

    private HashMap<String, String> lastBackupData() {
        HashMap<String, String> lastBackupData = new HashMap<>();
        lastBackupData.put("lastBackupTime", new DateTime(backupService.lastBackupTime()).toString());
        lastBackupData.put("lastBackupUser", backupService.lastBackupUser());
        lastBackupData.put("availableDiskSpace", backupService.availableDiskSpace());
        return lastBackupData;
    }
}
