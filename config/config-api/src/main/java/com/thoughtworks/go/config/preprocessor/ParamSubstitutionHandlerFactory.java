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
package com.thoughtworks.go.config.preprocessor;

import com.thoughtworks.go.config.ParamsConfig;

/**
 * @understands creating com.thoughtworks.go.config.preprocessor.ParamSubstitutionHandler
 */
public class ParamSubstitutionHandlerFactory implements ParamHandlerFactory {

    private final ParamsConfig paramsConfig;

    public ParamSubstitutionHandlerFactory(ParamsConfig paramsConfig) {
        this.paramsConfig = paramsConfig;
    }

    @Override
    public ParamHandler createHandler(Object resolvable, String fieldName, String stringToResolve) {
        return new ParamSubstitutionHandler(paramsConfig,  resolvable,  fieldName, stringToResolve);
    }

    @Override
    public ParamHandlerFactory override(ParamsConfig paramsConfig) {
        return new ParamSubstitutionHandlerFactory(this.paramsConfig.addOrReplace(paramsConfig));
    }
}
