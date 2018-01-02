/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.plugin.domain.analytics;


public class Capabilities {
    private final boolean supportsPipelineAnalytics;

    public Capabilities(boolean supportsPipelineAnalytics) {
        this.supportsPipelineAnalytics = supportsPipelineAnalytics;
    }

    public boolean supportsPipelineAnalytics() {
        return supportsPipelineAnalytics;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Capabilities that = (Capabilities) o;

        return supportsPipelineAnalytics == that.supportsPipelineAnalytics;
    }

    @Override
    public int hashCode() {
        return (supportsPipelineAnalytics ? 1 : 0);
    }
}
