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

package com.thoughtworks.go.config.helper;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.ParamConfig;
import com.thoughtworks.go.config.ParamsConfig;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParamFinder {
    static final String PARAM_PATTERN = "#\\{(.*?)\\}";

    public boolean isAttributeAParam(String attribute) {
        return attribute != null && attribute.startsWith("#{");
    }

    public boolean isAttributeAParam(CaseInsensitiveString attribute) {
        return attribute != null && isAttributeAParam(attribute.toString());
    }

    public String getParamValue(ParamsConfig paramConfigs, String attribute) {
        if (attribute == null || paramConfigs == null || paramConfigs.isEmpty()) {
            return null;
        }
        String paramName = null;
        Matcher matcher = Pattern.compile(String.format("^%s$", PARAM_PATTERN)).matcher(attribute);
        if (matcher.find()) {
            paramName = matcher.group(1);
        }
        ParamConfig param = paramConfigs.getParamNamed(paramName);
        return param == null ? null : param.getValue();
    }

    public String getParamValue(ParamsConfig paramConfigs, CaseInsensitiveString attribute) {
       return getParamValue(paramConfigs, attribute.toString());
    }
}
