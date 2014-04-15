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

import java.sql.SQLException;
import java.util.HashMap;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineNotFoundException;
import com.thoughtworks.go.util.json.JsonMap;
import com.thoughtworks.go.server.controller.actions.JsonAction;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.scheduling.ScheduleOptions;
import com.thoughtworks.go.server.service.CcTrayStatusService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.JsonCurrentActivityService;
import com.thoughtworks.go.server.service.PipelinePauseService;
import com.thoughtworks.go.server.service.PipelineScheduler;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.result.ServerHealthStateOperationResult;
import com.thoughtworks.go.server.util.ErrorHandler;
import com.thoughtworks.go.server.util.UserHelper;
import com.thoughtworks.go.server.web.XmlModelAndView;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import static com.thoughtworks.go.util.json.JsonHelper.addFriendlyErrorMessage;
import static com.thoughtworks.go.server.controller.actions.JsonAction.jsonFound;
import static com.thoughtworks.go.server.controller.actions.JsonAction.jsonNotAcceptable;
import static com.thoughtworks.go.server.controller.actions.JsonAction.jsonNotFound;
import static com.thoughtworks.go.util.GoConstants.ERROR_FOR_JSON;
import static org.apache.commons.lang.StringUtils.isEmpty;

@Controller
public class PipelineStatusController {
    private static final Logger LOGGER = Logger.getLogger(PipelineStatusController.class);

    private static final String FORCE_URI = "/force";

    private final GoConfigService goConfigService;
    private final PipelineScheduler buildCauseProducer;
    private final CcTrayStatusService ccTrayStatusService;
    private final SecurityService securityService;
    private final JsonCurrentActivityService jsonCurrentActivityService;
    private PipelinePauseService pipelinePauseService;

    @Autowired
    public PipelineStatusController(GoConfigService goConfigService,
                                    PipelineScheduler buildCauseProducer,
                                    CcTrayStatusService ccTrayStatusService,
                                    SecurityService securityService,
                                    JsonCurrentActivityService jsonCurrentActivityService,
                                    PipelinePauseService pipelinePauseService) {
        this.goConfigService = goConfigService;
        this.buildCauseProducer = buildCauseProducer;
        this.ccTrayStatusService = ccTrayStatusService;
        this.securityService = securityService;
        this.jsonCurrentActivityService = jsonCurrentActivityService;
        this.pipelinePauseService = pipelinePauseService;
    }

    @RequestMapping(value = FORCE_URI, method = RequestMethod.POST)
    public ModelAndView triggerPipeline(@RequestParam(value = "pipelineName", required = true)String pipelineName,
                                        HttpServletResponse response) throws Exception {
        Username username = UserHelper.getUserName();
        PipelineConfig pipelineConfig = goConfigService.pipelineConfigNamed(new CaseInsensitiveString(pipelineName));
        boolean isAuthorized = securityService.hasOperatePermissionForStage(CaseInsensitiveString.str(pipelineConfig.name()), CaseInsensitiveString.str(pipelineConfig.first().name()),
                CaseInsensitiveString.str(username.getUsername()));
        if (isAuthorized) {
            LOGGER.debug("start producing manual build cause");
            ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
            final HashMap<String, String> revisions = new HashMap<String, String>();
            final HashMap<String, String> environmentVariables = new HashMap<String, String>();
            final HashMap<String, String> secureEnvironmentVariables = new HashMap<String, String>();
            buildCauseProducer.manualProduceBuildCauseAndSave(pipelineName, username, new ScheduleOptions(revisions, environmentVariables, secureEnvironmentVariables), result);
            return JsonAction.from(result.getServerHealthState()).respond(response);
        } else {
            LOGGER.error(username + " is not authorized to force this pipeline");
            return JsonAction.jsonUnauthorized().respond(response);
        }
    }

    /**
     * @deprecated #5839 - unsupported since 12.3 sriki/sachin
     */
    @RequestMapping(value = "/**/pausePipeline.json", method = RequestMethod.POST)
    public void pauseViaPost(@RequestParam(value = "pipelineName")String pipelineName,
                             @RequestParam(value = "pauseCause")String pauseCause,
                             HttpServletRequest request,
                             HttpServletResponse response) throws SQLException, NamingException {
        if (isEmpty(pipelineName)) {
            response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
            return;
        }
        Username userName = UserHelper.getUserName();
        pipelinePauseService.pause(pipelineName, pauseCause, userName);
        LOGGER.info(String.format("[JSON API] Pipeline[%s] is paused by [%s] because [%s]", pipelineName, userName.getDisplayName(), pauseCause));
    }

    /**
     * @deprecated #5839 - unsupported since 12.3 sriki/sachin
     */
    @RequestMapping(value = "/**/unpausePipeline.json", method = RequestMethod.POST)
    public void unpauseViaPost(@RequestParam(value = "pipelineName")String pipelineName,
                               HttpServletRequest request,
                               HttpServletResponse response) throws SQLException, NamingException {
        if (isEmpty(pipelineName)) {
            response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
            return;
        }
        pipelinePauseService.unpause(pipelineName);

        String userName = UserHelper.getUserName().getDisplayName();
        LOGGER.info(String.format("[JSON API] Pipeline [%s] is unpaused by [%s]", pipelineName, userName));
    }

    @RequestMapping(value = "/**/pipelineStatus.json", method = RequestMethod.GET)
    public ModelAndView list(@RequestParam(value = "pipelineName", required = false)String pipelineName,
                             @RequestParam(value = "useCache", required = false)Boolean useCache,
                             HttpServletResponse response, HttpServletRequest request) throws NamingException {
        String username = CaseInsensitiveString.str(UserHelper.getUserName().getUsername());

        JsonMap json = new JsonMap();
        try {
            json.put("pipelines", jsonCurrentActivityService.pipelinesActivityAsJson(username, pipelineName));
            return jsonFound(json).respond(response);
        } catch (PipelineNotFoundException e) {
            JsonMap jsonLog = new JsonMap();
            jsonLog.put(ERROR_FOR_JSON, e.getMessage());
            return jsonNotFound(jsonLog).respond(response);
        }
    }

    @RequestMapping(value = "/**/cctray.xml", method = RequestMethod.GET)
    public ModelAndView cctray(HttpServletRequest request) throws Exception {
        Document document = ccTrayStatusService.createCctrayXmlDocument(getFullContextPath(request));
        return new XmlModelAndView(document);
    }

    @ErrorHandler
    public ModelAndView handleError(HttpServletRequest request, HttpServletResponse response, Exception e) {
        LOGGER.error("Error happend", e);
        JsonMap json = new JsonMap();
        String pipelineName = request.getParameter("pipelineName");
        if (isEmpty(pipelineName) && isForceBuildRequest(request)) {
            addFriendlyErrorMessage(json, "Cannot schedule: missed pipeline name");
        } else {
            addFriendlyErrorMessage(json, e.getMessage());
        }
        return jsonNotAcceptable(json).respond(response);
    }

    private boolean isForceBuildRequest(HttpServletRequest request) {
        return request.getRequestURI().contains(FORCE_URI);
    }

    String getFullContextPath(HttpServletRequest request) throws URIException {
        String contextPath = request.getContextPath();
        StringBuffer url = request.getRequestURL();
        URI uri = new URI(url.toString());
        uri.setPath(contextPath);
        return uri.toString();
    }
}
