/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv3.configrepos;

import com.thoughtworks.go.config.PartialConfigParseResult;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;

import java.util.Objects;

public class ConfigRepoWithResult {
    private final ConfigRepoConfig repo;
    private final PartialConfigParseResult result;
    private final boolean isMaterialUpdateInProgress;

    public ConfigRepoWithResult(ConfigRepoConfig repo, PartialConfigParseResult result, boolean isMaterialUpdateInProgress) {
        this.repo = repo;
        this.result = result;
        this.isMaterialUpdateInProgress = isMaterialUpdateInProgress;
    }

    public ConfigRepoConfig repo() {
        return repo;
    }

    public PartialConfigParseResult result() {
        return result;
    }

    public boolean isMaterialUpdateInProgress() {
        return isMaterialUpdateInProgress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConfigRepoWithResult that = (ConfigRepoWithResult) o;
        return Objects.equals(repo, that.repo) &&
                Objects.equals(result, that.result);
    }

    @Override
    public int hashCode() {
        return Objects.hash(repo, result);
    }
}
