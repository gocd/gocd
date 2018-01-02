/*
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.config.preprocessor;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.GoConfigPreprocessor;
import com.thoughtworks.go.config.ParamsConfig;
import com.thoughtworks.go.config.PipelineConfig;

/**
 * @understands Interpolation of config parameters
 */
public class ConfigParamPreprocessor implements GoConfigPreprocessor {

    private final ParamResolver resolver;

    public ConfigParamPreprocessor() {
        this.resolver = new ParamResolver(new ParamSubstitutionHandlerFactory(new ParamsConfig()), new ClassAttributeCache.FieldCache());
    }

    public void process(CruiseConfig cruiseConfig) {
        resolver.resolve(cruiseConfig);
    }

    public void process(PipelineConfig pipelineConfig) {
        resolver.resolve(pipelineConfig);
    }
}
