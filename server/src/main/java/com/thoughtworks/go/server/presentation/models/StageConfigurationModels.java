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
package com.thoughtworks.go.server.presentation.models;

import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.domain.BaseCollection;

import static com.thoughtworks.go.util.ExceptionUtils.methodNotImplemented;

public class StageConfigurationModels extends BaseCollection<StageConfigurationModel> {
    public StageConfigurationModels() {
    }

    public StageConfigurationModels(PipelineConfig pipelineConfig) {
        for (StageConfig config : pipelineConfig) {
            StageInfoAdapter adapter = new StageInfoAdapter(config);
            this.add(adapter);
        }
    }

    public boolean match(PipelineConfig pipelineConfig) {
        return this.equals(new StageConfigurationModels(pipelineConfig));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (getClass() != o.getClass()) {
            return false;
        }

        StageConfigurationModels that = (StageConfigurationModels) o;
        if (size() != that.size()) {
            return false;
        }
        for (int i = 0; i < that.size(); i++) {
            if (!StageInfoAdapter.equals(get(i), that.get(i))) {
                return false;
            }
        }
        return true;
    }

    // not intend to use this method, but to let checkstyle happy
    @Override
    public int hashCode() {
        methodNotImplemented();
        return 0;
    }
}
