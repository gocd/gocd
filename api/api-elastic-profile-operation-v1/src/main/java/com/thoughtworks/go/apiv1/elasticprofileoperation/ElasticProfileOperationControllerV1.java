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
package com.thoughtworks.go.apiv1.elasticprofileoperation;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.apiv1.elasticprofileoperation.representers.ElasticProfileUsageRepresenter;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.config.policy.SupportedEntity;
import com.thoughtworks.go.domain.ElasticProfileUsage;
import com.thoughtworks.go.server.service.ElasticProfileService;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.util.Collection;

import static spark.Spark.*;

@Component
public class ElasticProfileOperationControllerV1 extends ApiController implements SparkSpringController {
    private static final String PROFILE_ID_PARAM = "profile_id";
    private ElasticProfileService elasticProfileService;
    private final ApiAuthenticationHelper apiAuthenticationHelper;

    @Autowired
    public ElasticProfileOperationControllerV1(ElasticProfileService elasticProfileService, ApiAuthenticationHelper apiAuthenticationHelper) {
        super(ApiVersion.v1);
        this.elasticProfileService = elasticProfileService;
        this.apiAuthenticationHelper = apiAuthenticationHelper;
    }

    @Override
    public String controllerBasePath() {
        return Routes.ElasticProfileAPI.INTERNAL_BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);

            before(Routes.ElasticProfileAPI.ID + Routes.ElasticProfileAPI.USAGES, mimeType, (request, response) -> {
                ElasticProfile profile = elasticProfileService.findProfile(request.params(PROFILE_ID_PARAM));
                apiAuthenticationHelper.checkUserHasPermissions(currentUsername(), getAction(request), SupportedEntity.ELASTIC_AGENT_PROFILE, profile.getId(), profile.getClusterProfileId());
            });

            get(Routes.ElasticProfileAPI.ID + Routes.ElasticProfileAPI.USAGES, mimeType, this::usages);
        });
    }

    public String usages(Request request, Response response) {
        final String elasticProfileId = StringUtils.stripToEmpty(request.params(PROFILE_ID_PARAM));
        final Collection<ElasticProfileUsage> jobsUsingElasticProfile = elasticProfileService.getUsageInformation(elasticProfileId);
        return ElasticProfileUsageRepresenter.toJSON(jobsUsingElasticProfile);
    }
}
