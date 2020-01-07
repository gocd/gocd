/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.apiv1.feedsapi;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.config.exceptions.BadRequestException;
import com.thoughtworks.go.server.service.FeedService;
import com.thoughtworks.go.spark.RequestContext;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.io.StringWriter;

import static java.lang.String.format;
import static org.apache.commons.io.FilenameUtils.removeExtension;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static spark.Spark.*;

@Component
public class FeedsApiControllerV1 extends ApiController implements SparkSpringController {
    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final FeedService feedService;

    @Autowired
    public FeedsApiControllerV1(ApiAuthenticationHelper apiAuthenticationHelper,
                                FeedService feedService) {
        super(ApiVersion.v1);
        this.mimeType = APPLICATION_XML_VALUE;
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.feedService = feedService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.FeedsAPI.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("/*", mimeType, this::setContentType);
            before("/*", mimeType, this.apiAuthenticationHelper::checkUserAnd403);

            get(Routes.FeedsAPI.PIPELINES_XML, this.mimeType, this::pipelinesXML);
            get(Routes.FeedsAPI.STAGES_XML, this.mimeType, this::stagesXML);
            get(Routes.FeedsAPI.PIPELINE_XML, this.mimeType, this::pipelineXML);
            get(Routes.FeedsAPI.STAGE_XML, this.mimeType, this::stageXML);
            get(Routes.FeedsAPI.JOB_XML, this.mimeType, this::jobXML);
            get(Routes.FeedsAPI.SCHEDULED_JOB_XML, this.mimeType, this::scheduledJobs);
            get(Routes.FeedsAPI.MATERIAL_URL, this.mimeType, this::materialXML);
        });
    }

    public String pipelinesXML(Request request, Response response) throws IOException {
        return prettyPrint(feedService.pipelinesXml(currentUsername(), baseUrl(request)));
    }

    public String pipelineXML(Request request, Response response) throws IOException {
        String pipelineName = request.params("pipeline_name");
        Integer pipelineCounter = parseInt(removeExtension(request.params("pipeline_counter")), "pipeline counter");
        return prettyPrint(feedService.pipelineXml(currentUsername(), pipelineName, pipelineCounter, baseUrl(request)));
    }

    public String stagesXML(Request request, Response response) throws IOException {
        String pipelineName = request.params("pipeline_name");
        String beforeFromRequest = request.queryParams("before");
        Integer before = isBlank(beforeFromRequest) ? null : parseInt(beforeFromRequest, "before");
        return prettyPrint(feedService.stagesXml(currentUsername(), pipelineName, before, baseUrl(request)));
    }

    public String stageXML(Request request, Response response) throws IOException {
        String pipelineName = request.params("pipeline_name");
        Integer pipelineCounter = parseInt(request.params("pipeline_counter"), "pipeline counter");
        String stageName = request.params("stage_name");
        Integer stageCounter = parseInt(removeExtension(request.params("stage_counter")), "stage counter");
        return prettyPrint(feedService.stageXml(currentUsername(), pipelineName, pipelineCounter, stageName, stageCounter, baseUrl(request)));
    }

    public String jobXML(Request request, Response response) throws IOException {
        String pipelineName = request.params("pipeline_name");
        Integer pipelineCounter = parseInt(request.params("pipeline_counter"), "pipeline counter");
        String stageName = request.params("stage_name");
        Integer stageCounter = parseInt(request.params("stage_counter"), "stage counter");
        String jobName = removeExtension(request.params("job_name"));

        return prettyPrint(feedService.jobXml(currentUsername(), pipelineName, pipelineCounter, stageName, stageCounter, jobName, baseUrl(request)));
    }

    public String scheduledJobs(Request request, Response response) throws IOException {
        return prettyPrint(feedService.waitingJobPlansXml(baseUrl(request)));
    }

    public String materialXML(Request request, Response response) throws IOException {
        String pipelineName = request.params("pipeline_name");
        Integer pipelineCounter = parseInt(request.params("pipeline_counter"), "pipeline counter");
        String fingerprint = removeExtension(request.params("fingerprint"));

        return prettyPrint(feedService.materialXml(currentUsername(), pipelineName, pipelineCounter, fingerprint, baseUrl(request)));
    }

    public static String prettyPrint(Document document) throws IOException {
        StringWriter writer = new StringWriter();
        OutputFormat format = OutputFormat.createPrettyPrint();
        format.setNewLineAfterDeclaration(false);
        format.setIndentSize(2);
        new XMLWriter(writer, format).write(document);

        return writer.toString();
    }

    private Integer parseInt(String value, String entity) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException nfe) {
            throw new BadRequestException(format("The '%s' must be an integer.", entity));
        }
    }

    private String baseUrl(Request request) {
        return RequestContext.requestContext(request).urlFor("");
    }
}
