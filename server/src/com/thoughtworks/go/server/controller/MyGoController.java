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

import com.google.gson.GsonBuilder;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.domain.NotificationFilter;
import com.thoughtworks.go.domain.StageEvent;
import com.thoughtworks.go.domain.User;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.server.controller.actions.JsonAction;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.presentation.models.PipelineViewModel;
import com.thoughtworks.go.server.service.PipelineConfigService;
import com.thoughtworks.go.server.service.UserService;
import com.thoughtworks.go.server.ui.controller.Redirection;
import com.thoughtworks.go.server.util.UserHelper;
import com.thoughtworks.go.server.web.JsonView;
import com.thoughtworks.go.util.GoConstants;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

import static com.thoughtworks.go.server.controller.Message.error;
import static com.thoughtworks.go.server.controller.Message.info;
import static org.apache.commons.lang.BooleanUtils.toBoolean;

@Controller
public class MyGoController {
    private static final String MESSAGE_KEY = "message";

    private final UserService userService;
    private final PipelineConfigService pipelineConfigService;
    private final Localizer localizer;

    @Autowired
    public MyGoController(UserService userService, PipelineConfigService pipelineConfigService, Localizer localizer) {
        this.userService = userService;
        this.pipelineConfigService = pipelineConfigService;
        this.localizer = localizer;
    }

    @RequestMapping(value = "/tab/mycruise/user", method = RequestMethod.POST)
    public ModelAndView updateUserSetting(@RequestParam("email")String email,
                                          @RequestParam("matchers")String matchers,
                                          @RequestParam(value = "emailme", required = false)Boolean emailMe,
                                          HttpServletRequest request) {
        try {
            User user = userService.load(getUserId(request));
            user.setEmail(email);
            user.setEmailMe(toBoolean(emailMe));
            user.setMatcher(matchers);

            userService.saveOrUpdate(user);
            return new Redirection("/mycruise/user").addParameter(MESSAGE_KEY, "Successfully saved the settings");
        } catch (Exception e) {
            return render(request, error(MESSAGE_KEY, "Failed to save: " + e.getMessage()));
        }
    }

    @RequestMapping(value = "/tab/mycruise/user", method = RequestMethod.GET)
    public ModelAndView handleRequest(@RequestParam(value = MESSAGE_KEY, required = false)String message,
                                      HttpServletRequest request) {
        return render(request, info(MESSAGE_KEY, message));
    }

    @RequestMapping(value = "/tab/mycruise/notification", method = RequestMethod.POST)
    public ModelAndView addNotificationFilter(@RequestParam("pipeline")String pipeline,
                                              @RequestParam("stage")String stage,
                                              @RequestParam("event")String event,
                                              @RequestParam(value = "myCheckin", required = false)Boolean myCheckin,
                                              HttpServletRequest request) {
        try {
            NotificationFilter filter = new NotificationFilter(pipeline, stage, StageEvent.valueOf(event),
                    toBoolean(myCheckin));
            userService.addNotificationFilter(getUserId(request), filter);

            return new Redirection("/mycruise/user")
                    .addParameter(MESSAGE_KEY, "Successfully saved the notification filter.");
        } catch (Exception e) {
            HashMap<String, Object> data = new HashMap<>();
            data.put("pipeline", pipeline);
            data.put("stage", stage);
            data.put("event", event);
            data.put("myCheckin", myCheckin);
            return render(request, error(MESSAGE_KEY, "Failed to save: " + e.getMessage()), data);
        }
    }

    @RequestMapping(value = "/tab/mycruise/notification/delete", method = RequestMethod.POST)
    public ModelAndView removeNotificationFilter(@RequestParam("filterId")long filterId, HttpServletRequest request) {
        try {
            Long userId = getUserId(request);
            userService.removeNotificationFilter(userId, filterId);
            return new Redirection("/mycruise/user")
                    .addParameter(MESSAGE_KEY, "Successfully deleted the notification filter.");
        } catch (Exception e) {
            return render(request, error(MESSAGE_KEY, "Failed to delete: " + e.getMessage()));
        }
    }

    @RequestMapping(value = "/tab/mycruise/user/validate", method = RequestMethod.GET)
    public ModelAndView validate(@RequestParam(value = "email", required = false)String email,
                                 @RequestParam(value = "matchers", required = false)String matchers,
                                 HttpServletRequest request, HttpServletResponse response) {
        User user = new User(CaseInsensitiveString.str(getUserName().getUsername()), new String[]{matchers == null ? "" : matchers},
                email == null ? "" : email, true);
        try {
            userService.validate(user);
            return JsonAction.jsonFound(new LinkedHashMap()).respond(response);
        } catch (Exception e) {
            return JsonAction.jsonConflict(JsonView.getSimpleAjaxResult("message", e.getMessage())).respond(response);
        }
    }

    protected Long getUserId(HttpServletRequest request) {
        return UserHelper.getUserId(request);
    }

    protected Username getUserName() {
        return UserHelper.getUserName();
    }

    private ModelAndView render(HttpServletRequest request, Message message) {
        return render(request, message, new HashMap<String, Object>());
    }

    private ModelAndView render(HttpServletRequest request, Message message, HashMap<String, Object> data) {
        User user = userService.load(getUserId(request));
        user.populateModel(data);

        for (String key : data.keySet()) {
            if (StringUtils.isNotBlank(request.getParameter(key))) {
                data.put(key, request.getParameter(key));
            }
        }

        List<PipelineConfigs> groups = pipelineConfigService.viewableGroupsFor(getUserName());
        data.put("pipelines", new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().toJson(getPipelineModelsSortedByNameFor(groups)));
        data.put("l", localizer);

        message.populateModel(data);
        return new ModelAndView("mycruise/mycruise-tab", data);
    }

    private List<PipelineViewModel> getPipelineModelsSortedByNameFor(List<PipelineConfigs> groups) {
        List<PipelineViewModel> pipelineModels = new ArrayList<>();

        List<PipelineViewModel.StageViewModel> anyPipelineStageModels= new ArrayList<>();
        anyPipelineStageModels.add(new PipelineViewModel.StageViewModel(GoConstants.ANY_STAGE));
        pipelineModels.add(new PipelineViewModel(GoConstants.ANY_PIPELINE, anyPipelineStageModels));

        for (PipelineConfigs group : groups) {
            for (PipelineConfig pipelineConfig : group) {
                pipelineModels.add(new PipelineViewModel(CaseInsensitiveString.str(pipelineConfig.name()), getStagesModelsFor(pipelineConfig)));
            }
        }
        Collections.sort(pipelineModels);
        return pipelineModels;
    }

    private List<PipelineViewModel.StageViewModel> getStagesModelsFor(PipelineConfig pipelineConfig) {
        List<PipelineViewModel.StageViewModel> stageModels = new ArrayList<>();
        stageModels.add(new PipelineViewModel.StageViewModel(GoConstants.ANY_STAGE));
        for (StageConfig stageConfig : pipelineConfig) {
            stageModels.add(new PipelineViewModel.StageViewModel(CaseInsensitiveString.str(stageConfig.name())));
        }
        return stageModels;
    }

}
