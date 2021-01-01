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
package com.thoughtworks.go.domain.materials.git;

import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import de.skuzzle.semantic.Version;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.File;
import java.util.Optional;
import java.util.regex.Pattern;

import static com.thoughtworks.go.domain.materials.git.EnabledOnGitVersions.WILDCARD;
import static java.lang.String.format;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

public class EnabledOnGitVersionCondition implements ExecutionCondition {
    private static final Pattern IS_VERSION = Pattern.compile("^\\d+\\.\\d+\\.\\d+$");

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        Optional<EnabledOnGitVersions> maybe = findAnnotation(context.getElement(), EnabledOnGitVersions.class);
        if (maybe.isPresent()) {
            final EnabledOnGitVersions annotation = maybe.get();
            final Version gitVersion = fetchGitVersion();

            if (noneSpecified(annotation)) {
                return ConditionEvaluationResult.enabled("Version requirements not provided. Running by default.");
            }

            if (atLeast(annotation.from(), gitVersion) && atMost(annotation.through(), gitVersion)) {
                return ConditionEvaluationResult.enabled(
                        format("Git version %s satisfies %s", gitVersion, criteria(annotation))
                );
            } else {
                return ConditionEvaluationResult.disabled(
                        format("Git version %s does not satisfy %s", gitVersion, criteria(annotation))
                );
            }
        }

        return ConditionEvaluationResult.enabled("Version requirements not provided. Running by default.");
    }

    private boolean atLeast(final String required, Version actual) {
        if (WILDCARD.equals(required)) {
            return true;
        }

        if (!IS_VERSION.matcher(required).matches()) {
            throw new IllegalArgumentException(format("Bad version [%s]; `from` must be in major.minor.patch format!", required));
        }
        return actual.compareTo(Version.parseVersion(required)) >= 0;
    }

    private boolean atMost(final String required, Version actual) {
        if (WILDCARD.equals(required)) {
            return true;
        }

        if (!IS_VERSION.matcher(required).matches()) {
            throw new IllegalArgumentException(format("Bad version [%s]; `through` must be in major.minor.patch format!", required));
        }
        return actual.compareTo(Version.parseVersion(required)) <= 0;
    }

    private boolean bothSpecified(EnabledOnGitVersions annotation) {
        return !WILDCARD.equals(annotation.from()) && !WILDCARD.equals(annotation.through());
    }

    private boolean noneSpecified(EnabledOnGitVersions annotation) {
        return WILDCARD.equals(annotation.from()) && WILDCARD.equals(annotation.through());
    }

    private String criteria(EnabledOnGitVersions annotation) {
        if (noneSpecified(annotation)) {
            return "<any version>";
        }

        if (bothSpecified(annotation)) {
            return format("[from: %s, through: %s]", annotation.from(), annotation.through());
        }

        if (!WILDCARD.equals(annotation.from())) {
            return format("[from: %s]", annotation.from());
        }

        return format("[through: %s]", annotation.through());
    }

    private Version fetchGitVersion() {
        return new GitCommand(null, new File(""), GitMaterialConfig.DEFAULT_BRANCH, false, null).
                version().getVersion();
    }
}
