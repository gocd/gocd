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

package com.thoughtworks.go.config.policy.elasticagents;

import com.thoughtworks.go.config.Validatable;
import com.thoughtworks.go.config.policy.DirectiveType;
import com.thoughtworks.go.config.policy.Result;

public class ElasticAgentProfilesAllowDirective extends ElasticAgentProfilesAbstractDirective {
    private ElasticAgentProfilesAllowDirective(String action, String type, String resource, String resourceToOperateWithin) {
        super(DirectiveType.ALLOW, action, type, resource, resourceToOperateWithin);
    }

    @Override
    public Result apply(String action, Class<? extends Validatable> aClass, String resource, String resourceToOperateWithin) {
        if (isRequestForClusterProfiles(aClass) && isViewAction(action) && matchesResourceToOperateWithin(resource)) {
            return Result.ALLOW;
        }

        if (isRequestForElasticAgentProfiles(aClass) && matchesAction(action) && matchesResource(resource) && matchesResourceToOperateWithin(resourceToOperateWithin)) {
            return Result.ALLOW;
        }

        return Result.SKIP;
    }

    public static ElasticAgentProfilesAllowDirective parseResource(String action, String type, String resource) {
        String[] parts = resource.split(SEPARATOR);
        if (parts.length == 2) {
            return new ElasticAgentProfilesAllowDirective(action, type, parts[1], parts[0]);
        }

        return new ElasticAgentProfilesAllowDirective(action, type, parts[0], "*");
    }
}
