/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.Agent;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.Tabs;
import com.thoughtworks.go.config.TrackingTool;
import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.config.policy.SupportedAction;
import com.thoughtworks.go.config.policy.SupportedEntity;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentMetadataStore;
import com.thoughtworks.go.plugin.domain.elastic.ElasticAgentPluginInfo;
import com.thoughtworks.go.server.dao.JobAgentMetadataDao;
import com.thoughtworks.go.server.dao.JobInstanceDao;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
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
import java.io.IOException;
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
    private JobInstanceService jobInstanceService;
    private AgentService agentService;
    private JobInstanceDao jobInstanceDao;
    private GoConfigService goConfigService;
    private PipelineService pipelineService;
    private RestfulService restfulService;
    private ArtifactsService artifactService;
    private StageService stageService;
    private JobAgentMetadataDao jobAgentMetadataDao;
    private SystemEnvironment systemEnvironment;
    private SecurityService securityService;

    private ElasticAgentMetadataStore elasticAgentMetadataStore = ElasticAgentMetadataStore.instance();
    private Boolean disallowPropertiesAccess;

    public JobController() {
    }

    @Autowired
    public JobController(
            JobInstanceService jobInstanceService, AgentService agentService, JobInstanceDao jobInstanceDao,
            GoConfigService goConfigService, PipelineService pipelineService, RestfulService restfulService,
            ArtifactsService artifactService, StageService stageService,
            JobAgentMetadataDao jobAgentMetadataDao, SystemEnvironment systemEnvironment, SecurityService securityService) {
        this.jobInstanceService = jobInstanceService;
        this.agentService = agentService;
        this.jobInstanceDao = jobInstanceDao;
        this.goConfigService = goConfigService;
        this.pipelineService = pipelineService;
        this.restfulService = restfulService;
        this.artifactService = artifactService;
        this.stageService = stageService;
        this.jobAgentMetadataDao = jobAgentMetadataDao;
        this.systemEnvironment = systemEnvironment;
        this.securityService = securityService;
        this.disallowPropertiesAccess = Boolean.valueOf(System.getenv().getOrDefault("GO_DISALLOW_PROPERTIES_ACCESS", "true"));
    }

    @RequestMapping(value = "/tab/build/recent", method = RequestMethod.GET)
    public ModelAndView jobDetail(@RequestParam("pipelineName") String pipelineName,
                                  @RequestParam("pipelineCounter") String pipelineCounter,
                                  @RequestParam("stageName") String stageName,
                                  @RequestParam("stageCounter") String stageCounter,
                                  @RequestParam("jobName") String jobName) throws Exception {
        Optional<Integer> pipelineCounterValue = pipelineService.resolvePipelineCounter(pipelineName, pipelineCounter);

        if (!pipelineCounterValue.isPresent()) {
            throw bomb(String.format("Expected numeric pipelineCounter or latest keyword, but received '%s' for [%s/%s/%s/%s/%s]", pipelineCounter, pipelineName, pipelineCounter, stageName,
                    stageCounter, jobName));
        }

        if (!isValidCounter(stageCounter)) {
            throw bomb(String.format("Expected numeric stageCounter or latest keyword, but received '%s' for [%s/%s/%s/%s/%s]", stageCounter, pipelineName, pipelineCounter, stageName,
                    stageCounter, jobName));

        }

        Pipeline pipeline = pipelineService.findPipelineByNameAndCounter(pipelineName, pipelineCounterValue.get());

        if (pipeline == null) {
            throw bomb(String.format("Job %s/%s/%s/%s/%s not found", pipelineName, pipelineCounter, stageName,
                    stageCounter, jobName));
        }

        StageIdentifier stageIdentifier = restfulService.translateStageCounter(pipeline.getIdentifier(), stageName, stageCounter);

        JobInstance instance = jobInstanceDao.mostRecentJobWithTransitions(new JobIdentifier(stageIdentifier, jobName));
        return getModelAndView(instance);
    }

    @ErrorHandler
    public ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Exception e) {
        LOGGER.error("Job detail page error: ", e);
        Map model = new HashMap();
        model.put(ERROR_FOR_PAGE, e.getMessage());
        return new ModelAndView("exceptions_page", model);
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
                    agentService.findAgentByUUID(mostRecentJobInstance.getAgentUuid()),
                    stageService.getBuildDuration(pipelineName, stageName, mostRecentJobInstance));
            json = createBuildInfo(presenter);
        } catch (Exception e) {
            LOGGER.warn(null, e);
            json = errorJsonMap(e);
        }
        return jsonFound(json).respond(response);
    }

    private JobDetailPresentationModel presenter(JobInstance current) {
        String pipelineName = current.getIdentifier().getPipelineName();
        String stageName = current.getIdentifier().getStageName();
        JobInstances recent25 = jobInstanceService.latestCompletedJobs(pipelineName, stageName, current.getName());
        Agent agent = agentService.getAgentByUUID(current.getAgentUuid());
        Pipeline pipelineWithOneBuild = pipelineService.wrapBuildDetails(current);
        Tabs customizedTabs = goConfigService.getCustomizedTabs(pipelineWithOneBuild.getName(),
                pipelineWithOneBuild.getFirstStage().getName(), current.getName());
        TrackingTool trackingTool = goConfigService.pipelineConfigNamed(
                new CaseInsensitiveString(pipelineWithOneBuild.getName())).trackingTool();
        Stage stage = stageService.getStageByBuild(current);
        return new JobDetailPresentationModel(current, recent25, agent, pipelineWithOneBuild, customizedTabs, trackingTool, artifactService, stage);
    }

    private boolean isValidCounter(String pipelineCounter) {
        return StringUtils.isNumeric(pipelineCounter) || JobIdentifier.LATEST.equalsIgnoreCase(pipelineCounter);
    }

    private ModelAndView getModelAndView(JobInstance jobDetail) throws IOException {
        final JobDetailPresentationModel presenter = presenter(jobDetail);
        Map data = new HashMap();
        data.put("presenter", presenter);
        data.put("websocketEnabled", Toggles.isToggleOn(Toggles.BROWSER_CONSOLE_LOG_WS));
        data.put("useIframeSandbox", systemEnvironment.useIframeSandbox());
        data.put("isEditableViaUI", goConfigService.isPipelineEditable(jobDetail.getPipelineName()));
        data.put("isAgentAlive", agentService.isRegistered(jobDetail.getAgentUuid()));
        data.put("disallowPropertiesAccess", disallowPropertiesAccess);
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

        ClusterProfile clusterProfile = jobAgentMetadata.clusterProfile();
        if (clusterProfile == null) {
            return;
        }

        final String pluginId = clusterProfile.getPluginId();
        final ElasticAgentPluginInfo pluginInfo = elasticAgentMetadataStore.getPluginInfo(pluginId);

        if (pluginInfo != null && pluginInfo.getCapabilities().supportsAgentStatusReport()) {
            String clusterProfileId = jobAgentMetadata.clusterProfile().getId();
            String elasticProfileId = jobAgentMetadata.elasticProfile().getId();
            data.put("clusterProfileId", clusterProfileId);
            data.put("elasticAgentProfileId", elasticProfileId);
            data.put("elasticAgentPluginId", pluginId);
            data.put("doesUserHaveViewAccessToStatusReportPage", securityService.doesUserHasPermissions(SessionUtils.currentUsername(), SupportedAction.VIEW, SupportedEntity.ELASTIC_AGENT_PROFILE, elasticProfileId, clusterProfileId));

            final Agent agent = agentService.getAgentByUUID(jobInstance.getAgentUuid());

            if (agent != null && agent.isElastic()) {
                data.put("elasticAgentPluginId", agent.getElasticPluginId());
                data.put("elasticAgentId", agent.getElasticAgentId());
                return;
            }
        }
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
