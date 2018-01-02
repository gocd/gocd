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


package com.thoughtworks.go.server.web.i18n;

import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.ViewableStatus;

@Deprecated
public final class CurrentResult implements ViewableStatus {

    public static final CurrentResult PASSED = new CurrentResult("Passed");

    public static final CurrentResult FAILED = new CurrentResult("Failed");

    public static final CurrentResult UNKNOWN = new CurrentResult("Unknown");

    private final String result;

    public static CurrentResult getCurrentResult(JobResult result) {
        switch (result) {
            case Cancelled:
            case Failed:
                return FAILED;
            case Passed:
                return PASSED;
            default:
                return UNKNOWN;
        }
    }

    private CurrentResult(String result) {
        this.result = result;
    }

    public String getStatus() {
        return result;
    }

    public String getCruiseStatus() {
        return getStatus();
    }

    public JobResult getBuildInstanceResult() {
        if (CurrentResult.PASSED.equals(this)) {
            return JobResult.Passed;
        }
        if (CurrentResult.FAILED.equals(this)) {
            return JobResult.Failed;
        }
        return JobResult.Unknown;
    }
}
