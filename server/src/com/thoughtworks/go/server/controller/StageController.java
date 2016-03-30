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

package com.thoughtworks.go.server.controller;

import com.thoughtworks.go.config.StageNotFoundException;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.server.GoUnauthorizedException;
import com.thoughtworks.go.server.security.HeaderConstraint;
import com.thoughtworks.go.server.service.ScheduleService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.util.ErrorHandler;
import com.thoughtworks.go.server.util.UserHelper;
import com.thoughtworks.go.server.web.ResponseCodeView;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.log4j.Logger;
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

import static com.thoughtworks.go.server.controller.actions.JsonAction.jsonNotAcceptable;
import static com.thoughtworks.go.server.controller.actions.JsonAction.jsonOK;
import static com.thoughtworks.go.util.json.JsonHelper.addFriendlyErrorMessage;
import static java.lang.String.format;

@Controller
public class StageController {

    private static final Logger LOGGER = Logger.getLogger(StageController.class);

    private ScheduleService scheduleService;
    private Localizer localizer;
    private HeaderConstraint headerConstraint;

    protected StageController() {
    }

    @Autowired
    public StageController(ScheduleService scheduleService, Localizer localizer, SystemEnvironment systemEnvironment) {
        this.scheduleService = scheduleService;
        this.localizer = localizer;
        this.headerConstraint = new HeaderConstraint(systemEnvironment);
    }

    @RequestMapping(value = "/admin/rerun", method = RequestMethod.POST)
    public ModelAndView rerunStage(@RequestParam(value = "pipelineName") String pipelineName,
                                   @RequestParam(value = "pipelineLabel") String counterOrLabel,
                                   @RequestParam(value = "stageName") String stageName,
                                   HttpServletResponse response, HttpServletRequest request) {

        if(!headerConstraint.isSatisfied(request)) {
            return ResponseCodeView.create(HttpServletResponse.SC_BAD_REQUEST, "Missing required header 'Confirm'");
        }

        try {
            scheduleService.rerunStage(pipelineName, counterOrLabel, stageName);
            return ResponseCodeView.create(HttpServletResponse.SC_OK, "");

        } catch (GoUnauthorizedException e) {
            return ResponseCodeView.create(HttpServletResponse.SC_UNAUTHORIZED, "");
        } catch (StageNotFoundException e) {
            LOGGER.error(String.format("Error while rerunning %s/%s/%s", pipelineName, counterOrLabel, stageName), e);
            return ResponseCodeView.create(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            LOGGER.error(String.format("Error while rerunning %s/%s/%s", pipelineName, counterOrLabel, stageName), e);
            return ResponseCodeView.create(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    @RequestMapping(value = "/**/cancel.json", method = RequestMethod.POST)
    public ModelAndView cancelViaPost(@RequestParam(value = "id") Long stageId, HttpServletResponse response,
                                      HttpServletRequest request) {
        if(!headerConstraint.isSatisfied(request)) {
            return ResponseCodeView.create(HttpServletResponse.SC_BAD_REQUEST, "Missing required header 'Confirm'");
        }

        try {
            HttpLocalizedOperationResult cancelResult = new HttpLocalizedOperationResult();
            scheduleService.cancelAndTriggerRelevantStages(stageId, UserHelper.getUserName(), cancelResult);
            return handleResult(cancelResult, response);
        } catch (GoUnauthorizedException e) {
            return ResponseCodeView.create(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
        } catch (Exception e) {
            return ResponseCodeView.create(HttpServletResponse.SC_NOT_ACCEPTABLE, e.getMessage());
        }
    }

    private ModelAndView handleResult(HttpLocalizedOperationResult cancelResult, HttpServletResponse response) {
        if (cancelResult.httpCode() == HttpServletResponse.SC_UNAUTHORIZED) {
            return ResponseCodeView.create(HttpServletResponse.SC_UNAUTHORIZED, cancelResult.message(localizer));
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
