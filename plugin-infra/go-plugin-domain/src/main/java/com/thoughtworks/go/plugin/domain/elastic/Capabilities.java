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

package com.thoughtworks.go.plugin.domain.elastic;

public class Capabilities {
    private boolean supportsStatusReport;

    public Capabilities(boolean supportsStatusReport) {
        this.supportsStatusReport = supportsStatusReport;
    }

    public boolean supportsStatusReport() {
        return supportsStatusReport;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Capabilities that = (Capabilities) o;

        return supportsStatusReport == that.supportsStatusReport;

    }

    @Override
    public int hashCode() {
        return (supportsStatusReport ? 1 : 0);
    }
}
