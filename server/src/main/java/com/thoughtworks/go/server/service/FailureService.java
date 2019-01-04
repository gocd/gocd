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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.testinfo.FailureDetails;
import com.thoughtworks.go.server.dao.sparql.ShineDao;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @understands understands fetching test failure data
 */
@Service
public class FailureService {
    private SecurityService securityService;
    private ShineDao shineDao;

    @Autowired
    public FailureService(SecurityService securityService, ShineDao shineDao) {
        this.securityService = securityService;
        this.shineDao = shineDao;
    }

    public FailureDetails failureDetailsFor(JobIdentifier jobIdentifier, String suiteName, String testName, Username username, HttpLocalizedOperationResult result) {
        String pipelineName = jobIdentifier.getPipelineName();
        if (securityService.hasViewPermissionForPipeline(username, pipelineName)) {
            return shineDao.failureDetailsForTest(jobIdentifier, suiteName, testName, result);
        }
        result.forbidden("User '" + CaseInsensitiveString.str(username.getUsername()) + "' does not have view permission on pipeline '" + pipelineName + "'", HealthStateType.forbiddenForPipeline(pipelineName));
        return FailureDetails.nullFailureDetails();
    }
}
