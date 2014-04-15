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

package com.thoughtworks.go.domain.testinfo;

/**
 * @understands information about a failing test case
 */

public class FailureDetails {

    private final String message;
    private final String stackTrace;

    public FailureDetails(String message, String stackTrace) {
        this.message = message;
        this.stackTrace = stackTrace;
    }

    public String getMessage() {
        return message;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public static FailureDetails nullFailureDetails() {
        return new NullFailureDetails();
    }

    private static class NullFailureDetails extends FailureDetails {
        private NullFailureDetails() {
            super("NOT_YET_AVAILABLE", "NOT_YET_AVAILABLE");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FailureDetails details = (FailureDetails) o;

        if (!message.equals(details.message)) {
            return false;
        }
        if (!stackTrace.equals(details.stackTrace)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = message.hashCode();
        result = 31 * result + stackTrace.hashCode();
        return result;
    }
}
