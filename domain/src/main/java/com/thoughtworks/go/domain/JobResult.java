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
package com.thoughtworks.go.domain;

import java.util.Comparator;

/**
 *
 */
public enum JobResult implements ViewableStatus {
    Passed,
    Failed,
    Cancelled,
    Unknown;

    public boolean isPassed() {
        return this.equals(Passed);
    }

    public boolean isFailed() {
        return this.equals(Failed);
    }

    public boolean isCancelled() {
        return this.equals(Cancelled);
    }

    public boolean isUnknown() {
        return this.equals(Unknown);
    }

    @Override
    public String getStatus() {
        return this.toString();
    }

    @Override
    public String getCruiseStatus() {
        return this.toString();
    }

    public String toLowerCase() {
        return toString().toLowerCase();
    }

    public String toCctrayStatus() {
        switch (this) {
            case Failed:
            case Cancelled:
                return "Failure";
            default:
                return "Success";
        }
    }

    public static final Comparator<JobResult> JOB_RESULT_COMPARATOR = (o1, o2) -> {

        if (o1._isFailed() && o2._isFailed()) {
            return 0;
        }

        if (o1._isFailed()) {
            return -1;
        }

        if (o2._isFailed()) {
            return 1;
        }

        if (o1.isUnknown() && !o2.isUnknown()) {
            return -1;
        }

        if (o2.isUnknown() && !o1.isUnknown()) {
            return 1;
        }

        return o1.compareTo(o2);
    };

    private boolean _isFailed() {
        return this == Failed || this == Cancelled;
    }
}
