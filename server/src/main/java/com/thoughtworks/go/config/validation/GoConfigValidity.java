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
package com.thoughtworks.go.config.validation;


import com.thoughtworks.go.config.ConfigSaveState;

public abstract class GoConfigValidity {
    public static final ValidityType VT_CONFLICT = new ValidityType("VT_CONFLICT");
    public static final ValidityType VT_MERGE_OPERATION_ERROR = new ValidityType("VT_MERGE_OPERATION_ERROR");
    public static final ValidityType VT_MERGE_PRE_VALIDATION_ERROR = new ValidityType("VT_MERGE_PRE_VALIDATION_ERROR");
    public static final ValidityType VT_MERGE_POST_VALIDATION_ERROR = new ValidityType("VT_MERGE_POST_VALIDATION_ERROR");

    public static ValidGoConfig valid(ConfigSaveState configSaveState) {
        return new ValidGoConfig(configSaveState);
    }

    public static ValidGoConfig valid() {
        return new ValidGoConfig();
    }

    public static InvalidGoConfig invalid(String errorMessage) {
        return new InvalidGoConfig(errorMessage, null);
    }

    public static InvalidGoConfig fromConflict(String errorMessage) {
        return new InvalidGoConfig(errorMessage, VT_CONFLICT);
    }

    public static InvalidGoConfig mergeConflict(String errorMessage) {
        return new InvalidGoConfig(errorMessage, VT_MERGE_OPERATION_ERROR);
    }

    public static InvalidGoConfig mergePreValidationError(String errorMessage) {
        return new InvalidGoConfig(errorMessage, VT_MERGE_PRE_VALIDATION_ERROR);
    }

    public static InvalidGoConfig mergePostValidationError(String errorMessage) {
        return new InvalidGoConfig(errorMessage, VT_MERGE_POST_VALIDATION_ERROR);
    }

    public boolean isValid() {
        return this instanceof ValidGoConfig;
    }

    public static class ValidityType {
        private final String typeName;

        private ValidityType(String typeName) {
            this.typeName = typeName;
        }

        @Override
        public String toString() {
            return String.format("ValidityType{typeName='%s\'}", typeName);
        }
    }

    public static class ValidGoConfig extends GoConfigValidity {
        private ConfigSaveState configSaveState;

        private ValidGoConfig() {
        }

        private ValidGoConfig(ConfigSaveState configSaveState) {
            this.configSaveState = configSaveState;
        }

        public boolean wasMerged() {
            return ConfigSaveState.MERGED.equals(configSaveState);
        }

        @Override
        public String toString() {
            return String.format("GoConfigValidity{configSaveState=%s}", configSaveState);
        }
    }

    public static class InvalidGoConfig extends GoConfigValidity {
        private final String errorMessage;
        private final ValidityType validityType;

        private InvalidGoConfig(String errorMessage, ValidityType validityType) {
            this.errorMessage = errorMessage;
            this.validityType = validityType;
        }

        public boolean isType(ValidityType type) {
            return type.equals(this.validityType);
        }

        public String errorMessage() {
            return errorMessage;
        }

        public boolean isMergeConflict() {
            return VT_MERGE_OPERATION_ERROR.equals(validityType);
        }

        public boolean isPostValidationError() {
            return VT_MERGE_POST_VALIDATION_ERROR.equals(validityType);
        }
    }
}


