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

package com.thoughtworks.go.server.controller;

import com.thoughtworks.go.config.exceptions.NotAuthorizedException;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.config.exceptions.StageNotFoundException;
import com.thoughtworks.go.server.security.HeaderConstraint;
import com.thoughtworks.go.server.service.PipelineService;
import com.thoughtworks.go.server.service.ScheduleService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.util.ErrorHandler;
import com.thoughtworks.go.server.web.ResponseCodeView;
import com.thoughtworks.go.util.SystemEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static com.thoughtworks.go.server.controller.actions.JsonAction.jsonNotAcceptable;
import static com.thoughtworks.go.server.controller.actions.JsonAction.jsonOK;
import static com.thoughtworks.go.util.json.JsonHelper.addFriendlyErrorMessage;
import static java.lang.String.format;

@Controller
public class StageController {

    private static final Logger LOGGER = LoggerFactory.getLogger(StageController.class);

    private ScheduleService scheduleService;
    private HeaderConstraint headerConstraint;
    private PipelineService pipelineService;

    protected StageController() {
    }

    @Autowired
    public StageController(ScheduleService scheduleService, SystemEnvironment systemEnvironment, PipelineService pipelineService) {
        this.scheduleService = scheduleService;
        this.headerConstraint = new HeaderConstraint(systemEnvironment);
        this.pipelineService = pipelineService;
    }

    @RequestMapping(value = "/admin/rerun", method = RequestMethod.POST)
    public ModelAndView rerunStage(@RequestParam(value = "pipelineName") String pipelineName,
                                   @RequestParam(value = "pipelineCounter") String pipelineCounter,
                                   @RequestParam(value = "stageName") String stageName,
                                   HttpServletResponse response, HttpServletRequest request) {

        if (!headerConstraint.isSatisfied(request)) {
            return ResponseCodeView.create(HttpServletResponse.SC_BAD_REQUEST, "Missing required header 'Confirm'");
        }
        Optional<Integer> pipelineCounterValue = pipelineService.resolvePipelineCounter(pipelineName, pipelineCounter);

        if (!pipelineCounterValue.isPresent()) {
            String errorMessage = String.format("Error while rerunning [%s/%s/%s]. Received non-numeric pipeline counter '%s'.", pipelineName, pipelineCounter, stageName, pipelineCounter);
            LOGGER.error(errorMessage);
            return ResponseCodeView.create(HttpServletResponse.SC_BAD_REQUEST, errorMessage);
        }
        try {
            scheduleService.rerunStage(pipelineName, pipelineCounterValue.get(), stageName);
            return ResponseCodeView.create(HttpServletResponse.SC_OK, "");

        } catch (NotAuthorizedException e) {
            return ResponseCodeView.create(HttpServletResponse.SC_FORBIDDEN, "");
        } catch (RecordNotFoundException e) {
            LOGGER.error("Error while rerunning {}/{}/{}", pipelineName, pipelineCounter, stageName, e);
            return ResponseCodeView.create(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Error while rerunning {}/{}/{}", pipelineName, pipelineCounter, stageName, e);
            return ResponseCodeView.create(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private ModelAndView handleResult(HttpLocalizedOperationResult cancelResult, HttpServletResponse response) {
        if (cancelResult.httpCode() == HttpServletResponse.SC_FORBIDDEN) {
            return ResponseCodeView.create(HttpServletResponse.SC_FORBIDDEN, cancelResult.message());
        }
        return jsonOK().respond(response);
    }

    @ErrorHandler
    public ModelAndView handleError(HttpServletRequest request, HttpServletResponse response, Exception e) {
        Map<String, Object> json = new LinkedHashMap<>();
        String message = e.getMessage();
        if (e instanceof StageNotFoundException) {
            StageNotFoundException stageNotFoundException = (StageNotFoundException) e;
            message = format(
                    "Stage '%s' of pipeline '%s' does not exist in current configuration. You can not rerun it.",
                    stageNotFoundException.getStageName(), stageNotFoundException.getPipelineName());
        }
        addFriendlyErrorMessage(json, message);
        return jsonNotAcceptable(json).respond(response);
    }
}
