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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Represents result of validation
 */
@Deprecated
//Will be moved to internal scope
public class ValidationResult {

    private List<ValidationError> errors = new ArrayList<>();

    /**
     * Adds provided ValidationError to error container
     * @param validationError the validation error to be added
     */
    public void addError(ValidationError validationError) {
        errors.add(validationError);
    }

    /**
     * Adds collection of ValidationError to the error container
     * @param validationErrors collection of errors to be added to container
     */
    public void addErrors(Collection<ValidationError> validationErrors) {
        errors.addAll(validationErrors);
    }

    /**
     * Removes specified validation error from the error container
     * @param validationError the validation error to be removed from error container
     */
    public void removeError(ValidationError validationError) {
        errors.remove(validationError);
    }

    /**
     * Removes specified collection of validation errors from the container
     * @param validationErrors collection of validations errors to be removed
     */
    public void removeErrors(Collection<ValidationError> validationErrors) {
        errors.removeAll(validationErrors);
    }

    /**
     * Checks if result of the validation is successful based on the size of error container
     * @return if error container is empty returns true, else returns false
     */
    public boolean isSuccessful() {
        return errors.isEmpty();
    }

    /**
     * Gets list of errors for container
     * @return list of errors for container
     */
    public List<ValidationError> getErrors() {
        return new ArrayList<>(errors);
    }

    /**
     * Collects error message string as list from all the errors in the container
     * @return error message as list of string
     */
    public List<String> getMessages() {
        List<String> errorMessages = new ArrayList<>();
        for (ValidationError error : errors) {
            errorMessages.add(error.getMessage());
        }
        return errorMessages;
    }
}
