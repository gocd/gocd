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

package com.thoughtworks.go.util.validators;

import java.util.ArrayList;
import org.apache.commons.logging.Log;

public class Validation {
    public static final Validation SUCCESS = new Validation();

    private ArrayList<Exception> errors = new ArrayList<>();

    public boolean isSuccessful() {
        return errors.size() == 0;
    }

    public void logErrors() {
        for (Exception error : errors) {
            System.err.println(error.getMessage());
        }
    }

    public void logErrors(Log logger) {
        for (Exception error : errors) {
            logger.error(error.getMessage());
        }
    }

    public Validation addError(Exception error) {
        errors.add(error);
        return this;
    }
}
