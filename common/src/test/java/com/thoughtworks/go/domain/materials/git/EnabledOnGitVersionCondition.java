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
package com.thoughtworks.go.domain.materials.git;

import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import de.skuzzle.semantic.Version;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.File;
import java.util.Optional;

import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

public class EnabledOnGitVersionCondition implements ExecutionCondition {


    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        Optional<EnabledOnGitVersionsAbove> annotation = findAnnotation(context.getElement(), EnabledOnGitVersionsAbove.class);
        if (annotation.isPresent()) {
            String version = annotation.get().value();
            Version requiredVersion = Version.parseVersion(version);
            GitCommand git = new GitCommand(null, new File(""), GitMaterialConfig.DEFAULT_BRANCH, false, null);
            Version gitVersion = git.version().getVersion();
            if (gitVersion.compareTo(requiredVersion) >= 0) {
                return ConditionEvaluationResult.enabled(String.format(
                        "Git version required for this test is %s or later. Detected Version is %s", requiredVersion, gitVersion));
            } else {
                return ConditionEvaluationResult.disabled(String.format(
                        "Git version required for this test is %s or later. Detected Version is %s", requiredVersion, gitVersion));
            }
        }
        return ConditionEvaluationResult.enabled("Version not provided. Running by default");
    }
}
