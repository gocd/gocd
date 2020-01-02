/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.domain.materials.scm;

import com.thoughtworks.go.config.materials.PluggableSCMMaterial;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.json.JsonHelper;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.*;

public class PluggableSCMMaterialInstanceTest {
    @Test
    public void shouldConvertMaterialInstanceToMaterial() {
        PluggableSCMMaterial material = MaterialsMother.pluggableSCMMaterial();
        SCM scmConfig = material.getScmConfig();
        PluggableSCMMaterialInstance materialInstance = new PluggableSCMMaterialInstance(JsonHelper.toJsonString(material), "flyweight");
        materialInstance.setId(1L);

        PluggableSCMMaterial constructedMaterial = (PluggableSCMMaterial) materialInstance.toOldMaterial(null, null, null);

        assertThat(constructedMaterial.getId(), is(1L));
        assertThat(constructedMaterial.getScmConfig().getPluginConfiguration().getId(), is(scmConfig.getPluginConfiguration().getId()));
        assertThat(constructedMaterial.getScmConfig().getConfiguration(), is(scmConfig.getConfiguration()));
    }

    @Test
    public void shouldTestEqualsBasedOnConfiguration() {
        PluggableSCMMaterial material = MaterialsMother.pluggableSCMMaterial("scm-id", "scm-name", ConfigurationPropertyMother.create("key1", false, "value1"));
        MaterialInstance materialInstance = material.createMaterialInstance();
        MaterialInstance materialInstanceCopy = material.createMaterialInstance();

        material.getScmConfig().getConfiguration().add(ConfigurationPropertyMother.create("key2", false, "value2"));
        MaterialInstance newMaterialInstance = material.createMaterialInstance();

        assertThat(materialInstance, is(materialInstanceCopy));
        assertThat(materialInstance, is(not(newMaterialInstance)));
    }

    @Test
    public void shouldCorrectlyCheckIfUpgradeIsNecessary() {
        PluggableSCMMaterial material = MaterialsMother.pluggableSCMMaterial("scm-id", "scm-name", ConfigurationPropertyMother.create("key1", false, "value1"));
        PluggableSCMMaterialInstance materialInstance = (PluggableSCMMaterialInstance) material.createMaterialInstance();
        materialInstance.setId(10L);
        PluggableSCMMaterialInstance materialInstanceCopy = (PluggableSCMMaterialInstance) material.createMaterialInstance();

        material.getScmConfig().getConfiguration().add(ConfigurationPropertyMother.create("key2", false, "value2"));
        PluggableSCMMaterialInstance newMaterialInstance = (PluggableSCMMaterialInstance) material.createMaterialInstance();

        assertThat(materialInstance.shouldUpgradeTo(materialInstanceCopy), is(false));
        assertThat(materialInstance.shouldUpgradeTo(newMaterialInstance), is(true));
    }

    @Test
    public void shouldCorrectlyCopyConfigurationValue() {
        PluggableSCMMaterialInstance materialInstance = (PluggableSCMMaterialInstance) MaterialsMother.pluggableSCMMaterial().createMaterialInstance();
        materialInstance.setId(10L);
        PluggableSCMMaterial latestMaterial = MaterialsMother.pluggableSCMMaterial("scm-id", "scm-name", ConfigurationPropertyMother.create("key1", false, "value1"));
        PluggableSCMMaterialInstance newPluggableSCMMaterialInstance = (PluggableSCMMaterialInstance) latestMaterial.createMaterialInstance();

        materialInstance.upgradeTo(newPluggableSCMMaterialInstance);

        assertThat(materialInstance.getId(), is(10L));
        assertThat(materialInstance.getConfiguration(), is(newPluggableSCMMaterialInstance.getConfiguration()));
    }

    @Test
    public void shouldSetFingerprintWhenConvertingMaterialInstanceToMaterial() {
        String fingerprint = "fingerprint";
        PluggableSCMMaterial material = MaterialsMother.pluggableSCMMaterial();
        PluggableSCMMaterialInstance materialInstance = new PluggableSCMMaterialInstance(JsonHelper.toJsonString(material), "flyweight");
        ReflectionUtil.setField(materialInstance, "fingerprint", fingerprint);
        materialInstance.setId(1L);

        PluggableSCMMaterial constructedMaterial = (PluggableSCMMaterial) materialInstance.toOldMaterial(null, null, null);

        assertThat(constructedMaterial.getFingerprint(), is(fingerprint));
    }
}