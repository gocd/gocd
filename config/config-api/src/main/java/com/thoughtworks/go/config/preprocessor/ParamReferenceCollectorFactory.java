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

import java.util.HashSet;
import java.util.Set;

import com.thoughtworks.go.config.ParamsConfig;

/**
 * @understands creating reference collecting handler
 */
public class ParamReferenceCollectorFactory implements ParamHandlerFactory {

    private HashSet<String> params = new HashSet<>();

    @Override
    public ParamHandler createHandler(Object resolvable, String fieldName, String stringToResolve) {
        return new ParamReferenceCollectorHandler(params);
    }

    @Override
    public ParamHandlerFactory override(ParamsConfig paramsConfig) {
        return null;
    }

    public Set<String> referredParams() {
        return params;
    }

    private static class ParamReferenceCollectorHandler implements ParamHandler {

        private final Set<String> params;

        public ParamReferenceCollectorHandler(Set<String> params) {
            this.params = params;
        }

        @Override
        public String getResult() {
            return null;
        }

        @Override
        public void handlePatternFound(StringBuilder pattern) {
            params.add(pattern.toString());
        }

        @Override
        public void handleNotInPattern(char ch) {
        }

        @Override
        public void handlePatternStarted(char ch) {
        }

        @Override
        public void handleAfterResolution(ParamStateMachine.ReaderState state) throws IllegalStateException {
        }
    }
}
