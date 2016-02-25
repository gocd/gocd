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

package com.thoughtworks.go.plugin.api.response.validation;


/**
 * Represents error reported when validation fails
 */
@Deprecated
//Will be moved to internal scope
public class ValidationError {

    private static final String EMPTY_KEY = "";

    private String key;

    private String message;

    /**
     * creates instance of ValidationError
     * @param key the error key
     * @param message the error message
     */
    public ValidationError(String key, String message) {
        this.key = key;
        this.message = message;
    }

    /**
     * creates instance of ValidationError
     * @param message the error message
     */
    public ValidationError(String message) {
        this.key = EMPTY_KEY;
        this.message = message;
    }

    /**
     * Gets error key
     * @return error key
     */
    public String getKey() {
        return key;
    }

    /**
     * Gets error message
     * @return error message
     */
    public String getMessage() {
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ValidationError validationError = (ValidationError) o;

        if (key != null ? !key.equals(validationError.key) : validationError.key != null) {
            return false;
        }
        if (message != null ? !message.equals(validationError.message) : validationError.message != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (message != null ? message.hashCode() : 0);
        return result;
    }
}
