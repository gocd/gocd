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

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.Properties;
import com.thoughtworks.go.server.controller.actions.BasicRestfulAction;
import com.thoughtworks.go.server.security.HeaderConstraint;
import com.thoughtworks.go.server.service.PipelineService;
import com.thoughtworks.go.server.service.PropertiesService;
import com.thoughtworks.go.server.service.RestfulService;
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
import java.util.List;

import static com.thoughtworks.go.server.controller.actions.BasicRestfulAction.notFound;

@Controller
public class PropertiesController {
    private final PropertiesService propertyService;
    private final RestfulService restfulService;
    private final PipelineService pipelineService;
    private HeaderConstraint headerConstraint;
    private static final Logger LOGGER = Logger.getLogger(PropertiesController.class);

    public static final String INVALID_VALUE =
            "Unable to set property with invalid characters (must be numbers, letters, or _ - . /) or a valid URI";
    public static final String VALUE_TOO_LONG = "Unable to set property with value larger than 255 characters.";
    public static final String NAME_TOO_LONG = "Unable to set property with key larger than 255 characters.";

    @Autowired
    public PropertiesController(PropertiesService propertyService, RestfulService restfulService,
                                PipelineService pipelineService, SystemEnvironment systemEnvironment) {
        this.propertyService = propertyService;
        this.restfulService = restfulService;
        this.pipelineService = pipelineService;
        this.headerConstraint = new HeaderConstraint(systemEnvironment);
    }

    @RequestMapping(value = "/repository/restful/properties/post", method = RequestMethod.POST)
    public void setProperty(@RequestParam("pipelineName")String pipelineName,
                            @RequestParam("pipelineLabel")String pipelineLabel,
                            @RequestParam("stageName")String stageName,
                            @RequestParam("stageCounter")String stageCounter,
                            @RequestParam("jobName")String buildName,
                            @RequestParam("property")String property,
                            @RequestParam("value")String value,
                            HttpServletResponse response, HttpServletRequest request) throws Exception {

        if(!headerConstraint.isSatisfied(request)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing required header 'Confirm'");
            return;
        }

        JobIdentifier jobIdentifier;
        try {
            jobIdentifier = restfulService.findJob(pipelineName, pipelineLabel, stageName, stageCounter, buildName);
        } catch (Exception e) {
            BasicRestfulAction.jobNotFound(new JobIdentifier(pipelineName, -1, pipelineLabel, stageName, stageCounter,
                    buildName)).respond(response);
            return;
        }
        Long id = jobIdentifier.getBuildId();
        propertyService.addProperty(id, property, value).respond(response);
    }

    @RequestMapping("/repository/restful/properties/jobs/search")
    public ModelAndView jobsSearch(@RequestParam("pipelineName")String pipelineName,
                                   @RequestParam("stageName")String stageName,
                                   @RequestParam("jobName")String jobName,
                                   @RequestParam(value = "limitPipeline", required = false)String limitPipeline,
                                   @RequestParam(value = "limitCount", required = false)Integer limitCount,
                                   HttpServletResponse response) throws Exception {

        Long limitPipelineId = null;
        if (limitPipeline != null) {
            Pipeline pipeline = pipelineService.findPipelineByCounterOrLabel(pipelineName, limitPipeline);
            if (pipeline != null) {
                limitPipelineId = pipeline.getId();
            } else {
                return notFound(String.format(
                        "The value [%s] of query parameter 'limitPipeline' is neither a pipeline counter nor label",
                        limitPipeline)).respond(
                        response);
            }
        }

        limitCount = limitCount == null ? 100 : limitCount;
        try {
            List<Properties> result = propertyService.loadHistory(pipelineName, stageName, jobName, limitPipelineId,
                    limitCount);
            PropertiesService.PropertyLister propertyLister = PropertiesService.asCsv(jobName);
            return propertyLister.listPropertiesHistory(result).respond(response);
        } catch (Exception e) {
            String message = String.format(
                    "Error on listing properties history for job %s/%s/%s with limitPipeline=%s and limitCount=%s",
                    pipelineName,
                    stageName, jobName, limitPipeline, limitCount);
            LOGGER.error(message, e);
            return notFound(message + "\n" + e.getMessage()).respond(response);
        }
    }

    @RequestMapping("/repository/restful/properties/job/search")
    public ModelAndView jobSearch(
            @RequestParam("pipelineName")String pipelineName,
            @RequestParam("pipelineLabel")String pipelineLabel,
            @RequestParam("stageName")String stageName,
            @RequestParam("stageCounter")String stageCounter,
            @RequestParam("jobName")String buildName,
            @RequestParam(value = "type", required = false)String type,
            @RequestParam(value = "property", required = false)String propertyKey,
            HttpServletResponse response) throws Exception {
        JobIdentifier jobIdentifier;
        try {
            jobIdentifier = restfulService.findJob(pipelineName, pipelineLabel, stageName,
                    stageCounter, buildName);
            return propertyService.listPropertiesForJob(jobIdentifier, type, propertyKey).respond(response);
        } catch (Exception e) {
            return BasicRestfulAction.jobNotFound(new JobIdentifier(pipelineName, -1, pipelineLabel,
                    stageName, stageCounter,
                    buildName)).respond(response);
        }
    }

}
