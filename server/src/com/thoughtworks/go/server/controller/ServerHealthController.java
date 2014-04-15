/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.controller;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.thoughtworks.go.util.json.JsonList;
import com.thoughtworks.go.util.json.JsonMap;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.util.ErrorHandler;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import static com.thoughtworks.go.server.controller.actions.JsonAction.jsonFound;
import static com.thoughtworks.go.server.controller.actions.JsonAction.jsonNotAcceptable;

@Controller
public class ServerHealthController {
    private ServerHealthService serverHealthService;
    private GoConfigService goConfigService;

    public static final Logger LOGGER = Logger.getLogger(ServerHealthController.class);

    public ServerHealthController() {
    }

    @Autowired
    public ServerHealthController(ServerHealthService serverHealthService, GoConfigService goConfigService) {
        this.serverHealthService = serverHealthService;
        this.goConfigService = goConfigService;
    }

    @RequestMapping(value = "/**/log.json", method = RequestMethod.GET)
    public ModelAndView logs(HttpServletResponse response) {
        List<ServerHealthState> logEntries = serverHealthService.getAllValidLogs(goConfigService.currentCruiseConfig());
        JsonList logList = new JsonList();
        for (ServerHealthState serverHealthState : logEntries) {
            logList.add(serverHealthState.asJson());
        }
        return jsonFound(logList).respond(response);
    }


    @ErrorHandler
    public ModelAndView errors(HttpServletRequest request, HttpServletResponse response, Exception e) {
        LOGGER.error("Failed to get error logs", e);
        JsonMap json = new JsonMap();
        return jsonNotAcceptable(json).respond(response);
    }

}
