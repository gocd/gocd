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

class ConfigRepoDocumentMother {
    String versionOneWithLockingSetTo(boolean enablePipelineLockingValue) {
        Map<String, Object> map = getJSONFor("/v1_simple.json");
        store(map, "1", "target_version");
        store(map, enablePipelineLockingValue, "pipelines", 0, "enable_pipeline_locking");
        return JsonUtils.toJsonString(map);
    }

    String versionOneComprehensiveWithNoLocking() {
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

    String versionTwoComprehensive() {
        return JsonUtils.toJsonString(getJSONFor("/v2_comprehensive.json"));
    }

    String v2WithFetchTask() {
        return JsonUtils.toJsonString(getJSONFor("/v2_with_fetch_tasks.json"));
    }

    String v2WithFetchExternalArtifactTask() {
        return JsonUtils.toJsonString(getJSONFor("/v2_with_fetch_external_artifact_task.json"));
    }

    String v3Comprehensive() {
        return JsonUtils.toJsonString(getJSONFor("/v3_comprehensive.json"));
    }

    String v3WithFetchTask() {
        return JsonUtils.toJsonString(getJSONFor("/v3_with_fetch_tasks.json"));
    }

    String v3WithFetchExternalArtifactTask() {
        return JsonUtils.toJsonString(getJSONFor("/v3_with_fetch_external_artifact_task.json"));
    }

    String v3ComprehensiveWithDisplayOrderWeightsOf10AndNull() {
        return JsonUtils.toJsonString(getJSONFor("/v3_comprehensive_with_display_order_weight_which_was_introduced_in_v4_for_one_pipeline.json"));
    }

    String v4ComprehensiveWithDisplayOrderWeightOfMinusOneForBothPipelines() {
        return JsonUtils.toJsonString(getJSONFor("/v4_comprehensive_with_display_order_weight_of_minus_one_for_both_pipelines.json"));
    }

    String v4ComprehensiveWithDisplayOrderWeightsOf10AndMinusOne() {
        return JsonUtils.toJsonString(getJSONFor("/v4_comprehensive_with_display_order_weights_of_10_and_minus_one.json"));
    }

    String v4GitMaterialWithCredentialInUrl() {
        return JsonUtils.toJsonString(getJSONFor("/v4_git_and_hg_material_with_credential_inside_url.json"));
    }

    String v5GitMaterialWithCredentialNotInUrl() {
        return JsonUtils.toJsonString(getJSONFor("/v5_git_and_hg_materials_with_denormalized_url.json"));
    }
}
