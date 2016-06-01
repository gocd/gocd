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

package com.thoughtworks.go.config;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.ConfigErrors;

/**
* @understands references to existing agents that are associated to an Environment
 */
@ConfigTag("agents")
@ConfigCollection(EnvironmentAgentConfig.class)
public class EnvironmentAgentsConfig extends BaseCollection<EnvironmentAgentConfig> implements ParamsAttributeAware, Validatable {
    private final ConfigErrors configErrors = new ConfigErrors();

    public void validate(ValidationContext validationContext) {
    }

    public ConfigErrors errors() {
        return configErrors;
    }

    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

    public List<String> getUuids() {
        List<String> uuids = new ArrayList<>();
        for(EnvironmentAgentConfig config : this) {
            uuids.add(config.getUuid());
        }
        return uuids;
    }

    public void setConfigAttributes(Object attributes) {
        if (attributes != null) {
            List<Map> agentAttributes = (List) attributes;
            this.clear();
            for (Map attributeMap : agentAttributes) {
                this.add(new EnvironmentAgentConfig((String) attributeMap.get("uuid")));
            }
        }
    }
}
