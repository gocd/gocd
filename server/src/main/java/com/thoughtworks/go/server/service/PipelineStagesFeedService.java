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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.domain.feed.FeedEntries;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.thoughtworks.go.serverhealth.HealthStateType.forbiddenForPipeline;

@Service
public class PipelineStagesFeedService {
    private final StageService stageService;
    private final SecurityService securityService;

    public static class PipelineStageFeedResolver implements FeedResolver {
        private final String pipelineName;
        private final StageService stageService;
        private final SecurityService securityService;

        private PipelineStageFeedResolver(String pipelineName, StageService stageService, SecurityService securityService) {
            this.pipelineName = pipelineName;
            this.stageService = stageService;
            this.securityService = securityService;
        }

        @Override
        public FeedEntries feed(Username user, LocalizedOperationResult operationResult) {
            if (userDoesNotHaveViewPermission(user, operationResult)) {
                return null;
            }
            return stageService.feed(pipelineName, user);
        }

        @Override
        public FeedEntries feedBefore(Username user, long entryId, LocalizedOperationResult operationResult) {
            if (userDoesNotHaveViewPermission(user, operationResult)) {
                return null;
            }
            return stageService.feedBefore(entryId, pipelineName, user);
        }

        private boolean userDoesNotHaveViewPermission(Username user, LocalizedOperationResult operationResult) {
            if (!securityService.hasViewPermissionForPipeline(user, pipelineName)) {
                operationResult.forbidden("User '" + CaseInsensitiveString.str(user.getUsername()) + "' does not have view permission on pipeline '" + pipelineName + "'", forbiddenForPipeline(pipelineName));
                return true;
            }
            return false;
        }
    }

    @Autowired
    public PipelineStagesFeedService(StageService stageService, SecurityService securityService) {
        this.stageService = stageService;
        this.securityService = securityService;
    }

    public FeedResolver feedResolverFor(String pipelineName) {
        return new PipelineStageFeedResolver(pipelineName, stageService, securityService);
    }
}
