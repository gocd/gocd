/*
 * Copyright 2015 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineNotFoundException;
import com.thoughtworks.go.domain.PipelinePauseInfo;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModels;
import com.thoughtworks.go.server.presentation.models.PipelineHistoryJsonPresentationModel;
import com.thoughtworks.go.server.service.*;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.ServerHealthStateOperationResult;
import com.thoughtworks.go.server.util.Pagination;
import com.thoughtworks.go.server.util.UserHelper;
import com.thoughtworks.go.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.thoughtworks.go.server.controller.actions.JsonAction.jsonFound;
import static com.thoughtworks.go.server.controller.actions.JsonAction.jsonNotAcceptable;
import static com.thoughtworks.go.util.json.JsonHelper.addDeveloperErrorMessage;

@Controller
public class PipelineHistoryController {
    private PipelineHistoryService pipelineHistoryService;
    private GoConfigService goConfigService;
    private PipelineScheduleQueue pipelineScheduleQueue;
    private SchedulingCheckerService schedulingCheckerService;
    private SecurityService securityService;
    private Localizer localizer;
    private PipelinePauseService pipelinePauseService;

    protected PipelineHistoryController() {
    }

    @Autowired
    PipelineHistoryController(PipelineHistoryService pipelineHistoryService,
                              GoConfigService goConfigService,
                              PipelineScheduleQueue pipelineScheduleQueue,
                              SchedulingCheckerService schedulingCheckerService, SecurityService securityService,
                              Localizer localizer, PipelinePauseService pipelinePauseService) {
        this.pipelineHistoryService = pipelineHistoryService;
        this.goConfigService = goConfigService;
        this.pipelineScheduleQueue = pipelineScheduleQueue;
        this.schedulingCheckerService = schedulingCheckerService;
        this.securityService = securityService;
        this.localizer = localizer;
        this.pipelinePauseService = pipelinePauseService;
    }

    @RequestMapping(value = "/tab/pipeline/history", method = RequestMethod.GET)
    public ModelAndView list(@RequestParam("pipelineName") String pipelineName) throws Exception {
        Map model = new HashMap();
        try {
            PipelineConfig pipelineConfig = goConfigService.pipelineConfigNamed(new CaseInsensitiveString(pipelineName));
            model.put("pipelineName", pipelineConfig.name());
            model.put("l", localizer);
            return new ModelAndView("pipeline/pipeline_history", model);
        } catch (PipelineNotFoundException e) {
            model.put("errorMessage", e.getMessage());
            return new ModelAndView("exceptions_page", model);
        }
    }

    @RequestMapping(value = "/**/pipelineHistory.json", method = RequestMethod.GET)
    public ModelAndView list(@RequestParam("pipelineName")String pipelineName,
                             @RequestParam(value = "perPage", required = false)Integer perPageParam,
                             @RequestParam(value = "start", required = false)Integer startParam,
                             @RequestParam(value = "labelFilter", required = false) String labelFilter,
                             HttpServletResponse response, HttpServletRequest request) throws NamingException {
        PipelineConfig pipelineConfig = goConfigService.pipelineConfigNamed(new CaseInsensitiveString(pipelineName));
        String username = CaseInsensitiveString.str(UserHelper.getUserName().getUsername());

        Pagination pagination;
        try {
            pagination = Pagination.pageStartingAt(startParam, pipelineHistoryService.totalCount(pipelineName), perPageParam);
        } catch (Exception e) {
            Map<String, Object> json = new LinkedHashMap<>();
            addDeveloperErrorMessage(json, e);
            return jsonNotAcceptable(json).respond(response);
        }

        PipelinePauseInfo pauseInfo = pipelinePauseService.pipelinePauseInfo(pipelineName);
        boolean hasBuildCauseInBuffer = pipelineScheduleQueue.hasBuildCause(CaseInsensitiveString.str(pipelineConfig.name()));
        PipelineInstanceModels pipelineHistory = StringUtil.isBlank(labelFilter) ?
                pipelineHistoryService.load(pipelineName, pagination, username, true) :
                pipelineHistoryService.findMatchingPipelineInstances(pipelineName, labelFilter, perPageParam, UserHelper.getUserName(), new HttpLocalizedOperationResult());


        boolean hasForcedBuildCause = pipelineScheduleQueue.hasForcedBuildCause(pipelineName);

        PipelineHistoryJsonPresentationModel historyJsonPresenter = new PipelineHistoryJsonPresentationModel(
                pauseInfo,
                pipelineHistory,
                pipelineConfig,
                pagination, canForce(pipelineConfig, username),
                hasForcedBuildCause, hasBuildCauseInBuffer, canPause(pipelineConfig, username));
        return jsonFound(historyJsonPresenter.toJson()).respond(response);
    }

    private boolean canPause(PipelineConfig pipelineConfig, String username) {
        return securityService.hasOperatePermissionForPipeline(new CaseInsensitiveString(username), CaseInsensitiveString.str(pipelineConfig.name()));
    }

    private boolean canForce(PipelineConfig pipelineConfig, String username) {
        return schedulingCheckerService.canManuallyTrigger(pipelineConfig, username,
                new ServerHealthStateOperationResult());
    }

}
