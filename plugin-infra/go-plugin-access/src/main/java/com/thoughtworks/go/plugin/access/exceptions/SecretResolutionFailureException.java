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
package com.thoughtworks.go.plugin.access.exceptions;

import java.util.Set;

import static java.lang.String.format;

public class SecretResolutionFailureException extends RuntimeException {
    public SecretResolutionFailureException(String message) {
        super(message);
    }

    public static SecretResolutionFailureException withMissingSecretParams(String secretConfigId, Set<String> secretsToResolve, Set<String> missingSecrets) {
        return new SecretResolutionFailureException(format("Expected plugin to resolve secret param(s) `%s` using secret config `%s` but plugin failed to resolve secret param(s) `%s`. Please make sure that secret(s) with the same name exists in your secret management tool.", csv(secretsToResolve), secretConfigId, csv(missingSecrets)));
    }

    public static SecretResolutionFailureException withUnwantedSecretParams(String secretConfigId, Set<String> secretsToResolve, Set<String> unwantedSecrets) {
        return new SecretResolutionFailureException(format("Expected plugin to resolve secret param(s) `%s` using secret config `%s` but plugin sent additional secret param(s) `%s`.", csv(secretsToResolve), secretConfigId, csv(unwantedSecrets)));
    }

    private static String csv(Set<String> secretsToResolve) {
        return String.join(", ", secretsToResolve);
    }
}
