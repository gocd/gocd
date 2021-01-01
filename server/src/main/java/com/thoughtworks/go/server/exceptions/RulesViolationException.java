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
package com.thoughtworks.go.server.exceptions;

import static java.lang.String.format;

public class RulesViolationException extends RuntimeException {
    public RulesViolationException(String message) {
        super(message);
    }

    public static void throwSecretConfigNotFound(String entityType, String entityName, String secretConfigId) {
        throw new RulesViolationException(format("%s '%s' is referring to none-existent secret config '%s'.", entityType, entityName, secretConfigId));
    }

    public static void throwCannotRefer(String entityType, String entityName, String secretConfigId) {
        throw new RulesViolationException(format("%s '%s' does not have permission to refer to secrets using secret config '%s'", entityType, entityName, secretConfigId));
    }
}
