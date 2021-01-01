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

public class ElasticAgentProfilesDenyDirective extends ElasticAgentProfilesAbstractDirective {
    private ElasticAgentProfilesDenyDirective(String action, String type, String resource, String resourceToOperateWithin) {
        super(DirectiveType.DENY, action, type, resource, resourceToOperateWithin);
    }

    @Override
    public Result apply(String action, Class<? extends Validatable> aClass, String resource, String resourceToOperateWithin) {
        if (isRequestForClusterProfiles(aClass) && isViewAction(action) && matchesResourceToOperateWithin(resource)) {
            return Result.DENY;
        }

        if (isRequestForElasticAgentProfiles(aClass) && matchesAction(action) && matchesResource(resource) && matchesResourceToOperateWithin(resourceToOperateWithin)) {
            return Result.DENY;
        }

        return Result.SKIP;
    }

    public static ElasticAgentProfilesDenyDirective parseResource(String action, String type, String resource) {
        //todo: add some validations at the time of save that the elastic agent profile resource should not contain more than 1 :
        String[] parts = resource.split(SEPARATOR);
        if (parts.length == 2) {
            return new ElasticAgentProfilesDenyDirective(action, type, parts[1], parts[0]);
        }

        return new ElasticAgentProfilesDenyDirective(action, type, parts[0], "*");
    }
}
