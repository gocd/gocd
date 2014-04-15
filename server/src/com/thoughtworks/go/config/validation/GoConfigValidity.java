/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.config.validation;


import com.thoughtworks.go.config.ConfigSaveState;

public class GoConfigValidity {

    public static final ValidityType VT_CONFLICT = new ValidityType("VT_CONFLICT");
    public static final ValidityType VT_MERGE_OPERATION_ERROR = new ValidityType("VT_MERGE_OPERATION_ERROR");
    public static final ValidityType VT_MERGE_PRE_VALIDATION_ERROR = new ValidityType("VT_MERGE_PRE_VALIDATION_ERROR");
    public static final ValidityType VT_MERGE_POST_VALIDATION_ERROR = new ValidityType("VT_MERGE_POST_VALIDATION_ERROR");
    private final String exceptionMessage;
    private ConfigSaveState configSaveState;
    private ValidityType type;

    private GoConfigValidity(ConfigSaveState configSaveState, String exceptionMessage) {
        this.configSaveState = configSaveState;
        this.exceptionMessage = exceptionMessage;
    }

    private GoConfigValidity(String errorMessage) {
        this(null, errorMessage);
    }

    public static GoConfigValidity valid(ConfigSaveState configSaveState) {
        return new GoConfigValidity(configSaveState, null);
    }

    public static GoConfigValidity valid() {
        return new GoConfigValidity(null, null);
    }

    public static GoConfigValidity invalid(Exception e) {
        return invalid(e.getMessage());
    }

    public static GoConfigValidity invalid(String errorMessage) {
        return new GoConfigValidity(errorMessage);
    }


    public boolean isValid() {
        return exceptionMessage == null;
    }

    public boolean isType(ValidityType type) {
        return type.equals(this.type);
    }

    public String errorMessage() {
        return isValid() ? "" : exceptionMessage;
    }

    public GoConfigValidity fromConflict() {
        this.type = VT_CONFLICT;
        return this;
    }

    public boolean wasMerged() {
        return ConfigSaveState.MERGED.equals(configSaveState);
    }

    public GoConfigValidity mergeConflict() {
        this.type = VT_MERGE_OPERATION_ERROR;
        return this;
    }

    public GoConfigValidity mergePreValidationError() {
        this.type = VT_MERGE_PRE_VALIDATION_ERROR;
        return this;
    }

    public GoConfigValidity mergePostValidationError() {
        this.type = VT_MERGE_POST_VALIDATION_ERROR;
        return this;
    }

    public boolean isMergeConflict() {
        return !isValid() && VT_MERGE_OPERATION_ERROR.equals(type);
    }

    public boolean isPostValidationError() {
        return !isValid() && VT_MERGE_POST_VALIDATION_ERROR.equals(type);
    }

    @Override
    public String toString() {
        return String.format("GoConfigValidity{exceptionMessage='%s\', configSaveState=%s, type=%s}", exceptionMessage, configSaveState, type);
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
}


