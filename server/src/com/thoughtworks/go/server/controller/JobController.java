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

import com.thoughtworks.go.config.AgentConfig;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.Tabs;
import com.thoughtworks.go.config.TrackingTool;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.Properties;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.server.presentation.models.JobDetailPresentationModel;
import com.thoughtworks.go.server.presentation.models.JobStatusJsonPresentationModel;
import com.thoughtworks.go.server.service.*;
import com.thoughtworks.go.server.util.ErrorHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

import static com.thoughtworks.go.server.controller.actions.JsonAction.jsonFound;
import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static com.thoughtworks.go.util.GoConstants.ERROR_FOR_PAGE;
import static com.thoughtworks.go.util.json.JsonHelper.addDeveloperErrorMessage;

/*
 * Handles requests for Build Details: See urlrewrite.xml.
 */
@Controller
public class JobController {

    private static final Log LOGGER = LogFactory.getLog(JobController.class);
    @Autowired private JobInstanceService jobInstanceService;
    @Autowired private JobDetailService jobDetailService;
    @Autowired private GoConfigService goConfigService;
    @Autowired private PipelineService pipelineService;
    @Autowired private RestfulService restfulService;
    @Autowired private ArtifactsService artifactService;
    @Autowired private PropertiesService propertiesService;
    @Autowired private StageService stageService;
    @Autowired private Localizer localizer;

    public JobController() {
    }

    JobController(
            JobInstanceService jobInstanceService, JobDetailService jobDetailService,
            GoConfigService goConfigService, PipelineService pipelineService, RestfulService restfulService,
            ArtifactsService artifactService, PropertiesService propertiesService, StageService stageService, Localizer localizer) {
        this.jobInstanceService = jobInstanceService;
        this.jobDetailService = jobDetailService;
        this.goConfigService = goConfigService;
        this.pipelineService = pipelineService;
        this.restfulService = restfulService;
        this.artifactService = artifactService;
        this.propertiesService = propertiesService;
        this.stageService = stageService;
        this.localizer = localizer;
    }

    @RequestMapping(value = "/tab/build/recent", method = RequestMethod.GET)
    public ModelAndView jobDetail(@RequestParam("pipelineName") String pipelineName,
                                  @RequestParam("label") String counterOrLabel,
                                  @RequestParam("stageName") String stageName,
                                  @RequestParam("stageCounter") String stageCounter,
                                  @RequestParam("jobName") String jobName) throws Exception {

        Pipeline pipeline = pipelineService.findPipelineByCounterOrLabel(pipelineName, counterOrLabel);
        if (pipeline == null) {
            throw bomb(String.format("Job %s/%s/%s/%s/%s not found", pipelineName, counterOrLabel, stageName,
                    stageCounter, jobName));
        }

        StageIdentifier stageIdentifier = restfulService.translateStageCounter(pipeline.getIdentifier(), stageName, stageCounter);

        JobInstance instance = jobDetailService.findMostRecentBuild(new JobIdentifier(stageIdentifier, jobName));
        return getModelAndView(instance);
    }

    private ModelAndView getModelAndView(JobInstance jobDetail) throws Exception {
        final JobDetailPresentationModel presenter = presenter(jobDetail);
        Map data = new HashMap();
        data.put("presenter", presenter);
        data.put("l", localizer);
        return new ModelAndView("build_detail/build_detail_page", data);
    }

    @ErrorHandler
    public ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Exception e) throws Exception {
        LOGGER.error("Job detail page error: ", e);
        Map model = new HashMap();
        model.put(ERROR_FOR_PAGE, e.getMessage());
        return new ModelAndView("exceptions_page", model);
    }

    private JobDetailPresentationModel presenter(JobInstance current) {
        String pipelineName = current.getIdentifier().getPipelineName();
        String stageName = current.getIdentifier().getStageName();
        JobInstances recent25 = jobInstanceService.latestCompletedJobs(pipelineName, stageName, current.getName());
        AgentConfig agentConfig = goConfigService.agentByUuid(current.getAgentUuid());
        Pipeline pipelineWithOneBuild = pipelineService.wrapBuildDetails(current);
        Tabs customizedTabs = goConfigService.getCustomizedTabs(pipelineWithOneBuild.getName(),
                pipelineWithOneBuild.getFirstStage().getName(), current.getName());
        TrackingTool trackingTool = goConfigService.pipelineConfigNamed(
                new CaseInsensitiveString(pipelineWithOneBuild.getName())).trackingTool();
        Properties properties = propertiesService.getPropertiesForJob(current.getId());
        Stage stage = stageService.getStageByBuild(current);
        return new JobDetailPresentationModel(current, recent25, agentConfig, pipelineWithOneBuild, customizedTabs, trackingTool, artifactService, properties, stage);
    }


    @RequestMapping(value = "/**/jobStatus.json", method = RequestMethod.GET)
    public ModelAndView handleRequest(@RequestParam(value = "pipelineName")String pipelineName,
                                      @RequestParam(value = "stageName")String stageName,
                                      @RequestParam(value = "jobId")long jobId,
                                      HttpServletResponse response) {
        Object json;
        try {
            JobInstance requestedInstance = jobInstanceService.buildByIdWithTransitions(jobId);
            JobInstance mostRecentJobInstance = jobDetailService.findMostRecentBuild(requestedInstance.getIdentifier());

            JobStatusJsonPresentationModel presenter = new JobStatusJsonPresentationModel(mostRecentJobInstance,
                    goConfigService.agentByUuid(mostRecentJobInstance.getAgentUuid()),
                    stageService.getBuildDuration(pipelineName, stageName, mostRecentJobInstance));
            json = createBuildInfo(presenter);
        } catch (Exception e) {
            LOGGER.warn(e);
            json = errorJsonMap(e);
        }
        return jsonFound(json).respond(response);
    }

    private Map errorJsonMap(Exception e) {
        Map<String, Object> jsonMap = new LinkedHashMap<>();
        addDeveloperErrorMessage(jsonMap, e);
        return jsonMap;
    }

    private List createBuildInfo(JobStatusJsonPresentationModel presenter) {
        // TODO: Hucking Fack alert. We shouldn't need "building_info"
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("building_info", presenter.toJsonHash());
        List jsonList = new ArrayList();
        jsonList.add(info);
        return jsonList;
    }
}
