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

package com.thoughtworks.go.config.policy.elasticagents;

import com.thoughtworks.go.config.policy.AbstractDirective;
import com.thoughtworks.go.config.policy.DirectiveType;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;

import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;

public abstract class ElasticAgentProfilesAbstractDirective extends AbstractDirective {
    protected static final String SEPARATOR = ":";
    private final String resourceToOperateWithin;

    public ElasticAgentProfilesAbstractDirective(DirectiveType allow, String action, String type, String resource, String resourceToOperateWithin) {
        super(allow, action, type, resource);
        this.resourceToOperateWithin = resourceToOperateWithin;
    }

    protected boolean matchesResourceToOperateWithin(String resource) {
        if (equalsIgnoreCase("*", this.resourceToOperateWithin)) {
            return true;
        }

        return FilenameUtils.wildcardMatch(resource, this.resourceToOperateWithin, IOCase.INSENSITIVE);
    }
}
