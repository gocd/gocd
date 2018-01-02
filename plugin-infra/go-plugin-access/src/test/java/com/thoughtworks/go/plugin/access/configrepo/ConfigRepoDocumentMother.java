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

package com.thoughtworks.go.plugin.access.configrepo;

import com.bazaarvoice.jolt.JsonUtils;
import org.apache.commons.io.IOUtils;

import java.util.Map;

import static com.bazaarvoice.jolt.utils.JoltUtils.remove;
import static com.bazaarvoice.jolt.utils.JoltUtils.store;

public class ConfigRepoDocumentMother {
    public String versionOneWithLockingSetTo(boolean enablePipelineLockingValue) {
        Map<String, Object> map = getJSONFor("/v1_simple.json");
        store(map, "1", "target_version");
        store(map, enablePipelineLockingValue, "pipelines", 0, "enable_pipeline_locking");
        return JsonUtils.toJsonString(map);
    }

    public String versionOneComprehensiveWithNoLocking() {
        Map<String, Object> map = getJSONFor("/v1_comprehensive.json");
        store(map, "1", "target_version");
        remove(map, "pipelines", 0, "enable_pipeline_locking");
        remove(map, "pipelines", 1, "enable_pipeline_locking");
        return JsonUtils.toJsonString(map);
    }

    private Map<String, Object> getJSONFor(String fileName) {
        try {
            String transformJSON = IOUtils.toString(this.getClass().getResourceAsStream(fileName), "UTF-8");
            return JsonUtils.jsonToMap(transformJSON);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String versionTwoComprehensive() {
        return JsonUtils.toJsonString(getJSONFor("/v2_comprehensive.json"));
    }
}
