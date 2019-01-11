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

package com.thoughtworks.go.server.controller;

import com.thoughtworks.go.config.AgentConfig;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.Tabs;
import com.thoughtworks.go.config.TrackingTool;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.Properties;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentMetadataStore;
import com.thoughtworks.go.plugin.domain.elastic.ElasticAgentPluginInfo;
import com.thoughtworks.go.server.dao.JobAgentMetadataDao;
import com.thoughtworks.go.server.dao.JobInstanceDao;
import com.thoughtworks.go.server.presentation.models.JobDetailPresentationModel;
import com.thoughtworks.go.server.presentation.models.JobStatusJsonPresentationModel;
import com.thoughtworks.go.server.service.*;
import com.thoughtworks.go.server.service.support.toggle.Toggles;
import com.thoughtworks.go.server.util.ErrorHandler;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.lang.StringUtils;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(JobController.class);
    @Autowired
    private JobInstanceService jobInstanceService;
    @Autowired
    private AgentService agentService;
    @Autowired
    private JobInstanceDao jobInstanceDao;
    @Autowired
    private GoConfigService goConfigService;
    @Autowired
    private PipelineService pipelineService;
    @Autowired
    private RestfulService restfulService;
    @Autowired
    private ArtifactsService artifactService;
    @Autowired
    private PropertiesService propertiesService;
    @Autowired
    private StageService stageService;
    @Autowired
    private JobAgentMetadataDao jobAgentMetadataDao;
    @Autowired
    private SystemEnvironment systemEnvironment;

    private ElasticAgentMetadataStore elasticAgentMetadataStore = ElasticAgentMetadataStore.instance();


    public JobController() {
    }

    JobController(
            JobInstanceService jobInstanceService, AgentService agentService, JobInstanceDao jobInstanceDao,
            GoConfigService goConfigService, PipelineService pipelineService, RestfulService restfulService,
            ArtifactsService artifactService, PropertiesService propertiesService, StageService stageService,
            JobAgentMetadataDao jobAgentMetadataDao, SystemEnvironment systemEnvironment) {
        this.jobInstanceService = jobInstanceService;
        this.agentService = agentService;
        this.jobInstanceDao = jobInstanceDao;
        this.goConfigService = goConfigService;
        this.pipelineService = pipelineService;
        this.restfulService = restfulService;
        this.artifactService = artifactService;
        this.propertiesService = propertiesService;
        this.stageService = stageService;
        this.jobAgentMetadataDao = jobAgentMetadataDao;
        this.systemEnvironment = systemEnvironment;
    }

    @RequestMapping(value = "/tab/build/recent", method = RequestMethod.GET)
    public ModelAndView jobDetail(@RequestParam("pipelineName") String pipelineName,
                                  @RequestParam("pipelineCounter") String pipelineCounter,
                                  @RequestParam("stageName") String stageName,
                                  @RequestParam("stageCounter") String stageCounter,
                                  @RequestParam("jobName") String jobName) throws Exception {
        if (!StringUtils.isNumeric(pipelineCounter) && !JobIdentifier.LATEST.equalsIgnoreCase(pipelineCounter)) {
            throw bomb(String.format("Expected numeric pipelineCounter, but received '%s' for [%s/%s/%s/%s/%s]", pipelineCounter, pipelineName, pipelineCounter, stageName,
                    stageCounter, jobName));

        }
        if (!StringUtils.isNumeric(stageCounter) && !JobIdentifier.LATEST.equalsIgnoreCase(stageCounter)) {
            throw bomb(String.format("Expected numeric stageCounter, but received '%s' for [%s/%s/%s/%s/%s]", stageCounter, pipelineName, pipelineCounter, stageName,
                    stageCounter, jobName));

        }

        Pipeline pipeline = pipelineService.findPipelineByCounterOrLatestKeyword(pipelineName, pipelineCounter);
        if (pipeline == null) {
            throw bomb(String.format("Job %s/%s/%s/%s/%s not found", pipelineName, pipelineCounter, stageName,
                    stageCounter, jobName));
        }

        StageIdentifier stageIdentifier = restfulService.translateStageCounter(pipeline.getIdentifier(), stageName, stageCounter);

        JobInstance instance = jobInstanceDao.mostRecentJobWithTransitions(new JobIdentifier(stageIdentifier, jobName));
        return getModelAndView(instance);
    }

    private ModelAndView getModelAndView(JobInstance jobDetail) {
        final JobDetailPresentationModel presenter = presenter(jobDetail);
        Map data = new HashMap();
        data.put("presenter", presenter);
        data.put("websocketEnabled", Toggles.isToggleOn(Toggles.BROWSER_CONSOLE_LOG_WS));
        data.put("useIframeSandbox", systemEnvironment.useIframeSandbox());
        data.put("isEditableViaUI", goConfigService.isPipelineEditable(jobDetail.getPipelineName()));
        data.put("isAgentAlive", goConfigService.hasAgent(jobDetail.getAgentUuid()));
        addElasticAgentInfo(jobDetail, data);
        return new ModelAndView("build_detail/build_detail_page", data);
    }

    private void addElasticAgentInfo(JobInstance jobInstance, Map data) {
        if (!jobInstance.currentStatus().isActive()) {
            return;
        }

        final JobAgentMetadata jobAgentMetadata = jobAgentMetadataDao.load(jobInstance.getId());
        if (jobAgentMetadata == null) {
            return;
        }

        final String pluginId = jobAgentMetadata.elasticProfile().getPluginId();
        final ElasticAgentPluginInfo pluginInfo = elasticAgentMetadataStore.getPluginInfo(pluginId);

        if (pluginInfo != null && pluginInfo.getCapabilities().supportsAgentStatusReport()) {
            final AgentConfig agentConfig = goConfigService.agentByUuid(jobInstance.getAgentUuid());

            if (agentConfig != null && agentConfig.isElastic()) {
                data.put("elasticAgentPluginId", agentConfig.getElasticPluginId());
                data.put("elasticAgentId", agentConfig.getElasticAgentId());
                return;
            }

            data.put("elasticAgentPluginId", pluginId);
        }
    }

    @ErrorHandler
    public ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Exception e) {
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
    public ModelAndView handleRequest(@RequestParam(value = "pipelineName") String pipelineName,
                                      @RequestParam(value = "stageName") String stageName,
                                      @RequestParam(value = "jobId") long jobId,
                                      HttpServletResponse response) {
        Object json;
        try {
            JobInstance requestedInstance = jobInstanceService.buildByIdWithTransitions(jobId);
            JobInstance mostRecentJobInstance = jobInstanceDao.mostRecentJobWithTransitions(requestedInstance.getIdentifier());

            JobStatusJsonPresentationModel presenter = new JobStatusJsonPresentationModel(mostRecentJobInstance,
                    agentService.findAgentObjectByUuid(mostRecentJobInstance.getAgentUuid()),
                    stageService.getBuildDuration(pipelineName, stageName, mostRecentJobInstance));
            json = createBuildInfo(presenter);
        } catch (Exception e) {
            LOGGER.warn(null, e);
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
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("building_info", presenter.toJsonHash());
        List jsonList = new ArrayList();
        jsonList.add(info);
        return jsonList;
    }
}
