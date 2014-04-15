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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.util.StringUtil;

@ConfigTag("params")
@ConfigCollection(ParamConfig.class)
public class ParamsConfig extends BaseCollection<ParamConfig> implements Validatable {
    private final ConfigErrors configErrors = new ConfigErrors();

    public ParamsConfig() {}

    public ParamsConfig(ParamsConfig paramsConfig) {
        this();
        addAll(paramsConfig);
    }

    public ParamsConfig addOrReplace(ParamsConfig newParams) {
        ParamsConfig myCopy = new ParamsConfig(this);
        for (ParamConfig newParam : newParams) {
            if (myCopy.hasParamNamed(newParam.getName())) {
                myCopy.removeParamNamed(newParam.getName());
            }
            myCopy.add(newParam);
        }
        return myCopy;
    }

    public void validate(ValidationContext validationContext) {
        HashMap<String, ParamConfig> paramConfigMap = new HashMap<String, ParamConfig>();
        for (ParamConfig paramConfig : this) {
            paramConfig.validateName(paramConfigMap, validationContext);
        }
    }

    public ConfigErrors errors() {
        return configErrors;
    }

    public int getIndex(String name) {
        for (int i = 0; i < this.size(); i++) {
            if (get(i).getName().equals(name)) {
                return i;
            }
        }
        throw new IllegalArgumentException("param '" + name + "' not found");
    }

    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

    private void removeParamNamed(String name) {
        this.remove(getParamNamed(name));
    }

    public boolean hasParamNamed(String name) {
        return getParamNamed(name) != null;
    }

    public ParamConfig getParamNamed(String name) {
        for (ParamConfig paramConfig : this) {
            if (paramConfig.getName().equals(name)) {
                return paramConfig;
            }
        }
        return null;
    }

    public void setConfigAttributes(Object attributes) {
        if (attributes != null) {
            this.clear();
            for (Map attributeMap : (List<Map>) attributes) {
                String name = (String) attributeMap.get(ParamConfig.NAME);
                String value = (String) attributeMap.get(ParamConfig.VALUE);
                if (StringUtil.isBlank(name) && StringUtil.isBlank(value)) {
                    continue;
                }
                this.add(new ParamConfig(name, value));
            }
        }
    }

}
