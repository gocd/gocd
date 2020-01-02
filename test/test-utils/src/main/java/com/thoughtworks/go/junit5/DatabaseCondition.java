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
package com.thoughtworks.go.junit5;

import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

public class DatabaseCondition implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        Optional<AnnotatedElement> element = context.getElement();
        Optional<EnableIfH2> enableIfH2 = findAnnotation(element, EnableIfH2.class);
        if (enableIfH2.isPresent()) {
            String databaseProvider = System.getProperty("go.database.provider", SystemEnvironment.H2_DATABASE);

            if (databaseProvider.endsWith(".H2Database")) {
                System.clearProperty("db.host");
                System.clearProperty("db.user");
                System.clearProperty("db.password");
                System.clearProperty("db.port");
                System.clearProperty("db.name");
                return ConditionEvaluationResult.enabled("Evaluating");
            }
        }
        return ConditionEvaluationResult.disabled("Disabling the test as DatabaseProvider is not H2.");
    }
}
