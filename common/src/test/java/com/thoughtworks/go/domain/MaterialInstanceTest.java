/*
 * Copyright Thoughtworks, Inc.
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

import com.thoughtworks.go.config.materials.PluggableSCMMaterial;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.util.SerializationTester;
import com.thoughtworks.go.util.json.JsonHelper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class MaterialInstanceTest {
    @Test
    public void shouldGenerateUniqueFingerprintOnCreation() {
        MaterialInstance one = new HgMaterial("url", null).createMaterialInstance();
        MaterialInstance two = new HgMaterial("otherurl", null).createMaterialInstance();
        assertThat(one.getFingerprint()).isNotNull();
        assertThat(one.getFingerprint()).isNotEqualTo(two.getFingerprint());
    }

    @Test
    public void shouldSerializeAndUnserializeAllAttributes() throws IOException, ClassNotFoundException {
        HgMaterial m = MaterialsMother.hgMaterial("url");
        MaterialInstance materialInstance = m.createMaterialInstance();
        materialInstance.setId(10);
        MaterialInstance unserializedMaterial = SerializationTester.objectSerializeAndDeserialize(materialInstance);
        assertThat(unserializedMaterial).isEqualTo((materialInstance));
        assertThat(unserializedMaterial.getId()).isEqualTo(10L);
        assertThat(unserializedMaterial).isEqualTo(materialInstance);
    }

    @Test
    public void shouldAnswerRequiresUpdate() {
        PluggableSCMMaterial material = MaterialsMother.pluggableSCMMaterial();
        MaterialInstance materialInstance = material.createMaterialInstance();
        // null
        materialInstance.setAdditionalData(null);
        assertThat(materialInstance.requiresUpdate(null)).isFalse();
        assertThat(materialInstance.requiresUpdate(new HashMap<>())).isFalse();

        // empty
        materialInstance.setAdditionalData(JsonHelper.toJsonExposeOnly(new HashMap<String, String>()));
        assertThat(materialInstance.requiresUpdate(null)).isFalse();
        assertThat(materialInstance.requiresUpdate(new HashMap<>())).isFalse();

        // with data
        Map<String, String> data = new HashMap<>();
        data.put("k1", "v1");
        data.put("k2", "v2");
        materialInstance.setAdditionalData(JsonHelper.toJsonExposeOnly(data));
        assertThat(materialInstance.requiresUpdate(null)).isTrue();
        assertThat(materialInstance.requiresUpdate(new HashMap<>())).isTrue();
        assertThat(materialInstance.requiresUpdate(data)).isFalse();

        // missing key-value
        Map<String, String> dataWithMissingKey = new HashMap<>(data);
        dataWithMissingKey.remove("k1");
        assertThat(materialInstance.requiresUpdate(dataWithMissingKey)).isTrue();

        // extra key-value
        Map<String, String> dataWithExtraKey = new HashMap<>(data);
        dataWithExtraKey.put("k3", "v3");
        assertThat(materialInstance.requiresUpdate(dataWithExtraKey)).isTrue();
    }
}
