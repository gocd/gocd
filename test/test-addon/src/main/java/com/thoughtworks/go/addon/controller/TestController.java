/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.addon.controller;

import com.thoughtworks.go.server.service.BackupService;
import com.thoughtworks.go.server.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(value = "/add-on/test-addon/admin")
public class TestController {
    private final BackupService backupService;
    private final UserService userService;

    @Autowired
    public TestController(BackupService backupService, UserService userService) {
        this.backupService = backupService;
        this.userService = userService;
    }

    @RequestMapping(value = "/backups/delete")
    @ResponseBody
    public String deleteAllBackups() {
        backupService.deleteAll();
        return "Deleted";
    }

    @RequestMapping(value = "/users/delete")
    @ResponseBody
    public String deleteAllUsers() {
        userService.deleteAll();
        return "Deleted";
    }
}
