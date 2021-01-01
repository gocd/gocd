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
package com.thoughtworks.go.config.preprocessor;

import com.thoughtworks.go.config.ParamConfig;
import com.thoughtworks.go.config.ParamsConfig;
import com.thoughtworks.go.config.Validatable;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

public class ParamSubstitutionHandler implements ParamHandler {
    public static final String NO_PARAM_FOUND_MSG = "Parameter '%s' is not defined. All pipelines using this parameter directly or via a template must define it.";

    private StringBuilder result = new StringBuilder();

    private ParamsConfig paramsConfig;
    private Object resolvable;
    private String fieldName;
    private String stringToResolve;

    @Override
    public String getResult() {
        return result.toString();
    }

    public ParamSubstitutionHandler(ParamsConfig paramsConfig, Object resolvable, String fieldName, String stringToResolve) {
        this.paramsConfig = paramsConfig;
        this.resolvable = resolvable;
        this.fieldName = fieldName;
        this.stringToResolve = stringToResolve;
    }

    @Override
    public void handlePatternFound(StringBuilder pattern) {
        result.append(substitute(pattern.toString(), paramsConfig));
    }

    @Override
    public void handleNotInPattern(char ch) {
        result.append(ch);
    }

    @Override
    public void handlePatternStarted(char ch) {
        result.append(ch);
    }

    @Override
    public void handleAfterResolution(ParamStateMachine.ReaderState state) throws IllegalStateException {
        try {
            if (state != ParamStateMachine.ReaderState.NOT_IN_PATTERN) {
                if (state == ParamStateMachine.ReaderState.INVALID_PATTERN) {
                    throw bombWithBadParamUsage(stringToResolve, fieldName);
                } else if (state == ParamStateMachine.ReaderState.HASH_SEEN) {
                    throw bombWithBadParamUsage(stringToResolve, fieldName);
                } else if (state == ParamStateMachine.ReaderState.IN_PATTERN) {
                    throw bombWithIncompleteParamUsage(stringToResolve);
                } else {
                    throw bomb("Unrecognized state: " + state);
                }
            }
        } catch (IllegalStateException e) {
            handleIllegalStateException(e);
        }
    }

    private String substitute(String name, ParamsConfig paramsConfig) {
        ParamConfig param = paramsConfig.getParamNamed(name);
        if (param != null) {
            return param.getValue();
        }
        handleIllegalStateException(new IllegalStateException(String.format(NO_PARAM_FOUND_MSG, name)));
        return result.toString(); //Doesn't matter
    }

    private void handleIllegalStateException(IllegalStateException e) throws IllegalStateException {
        if (Validatable.class.isAssignableFrom(resolvable.getClass())) {
            Validatable validatable = (Validatable) resolvable;
            validatable.addError(fieldName, e.getMessage());
        } else {
            throw e;
        }
        result = new StringBuilder(stringToResolve);
    }


    private static IllegalStateException bombWithBadParamUsage(String stringToResolve, String fieldName) {
        return new IllegalStateException(
                String.format("Error when processing params for '%s' used in field '%s', # must be followed by a parameter pattern or escaped by another #", stringToResolve, fieldName));
    }

    private static IllegalStateException bombWithIncompleteParamUsage(String stringToResolve) {
        return new IllegalStateException(String.format("Incomplete param usage in '%s'", stringToResolve));
    }
}

