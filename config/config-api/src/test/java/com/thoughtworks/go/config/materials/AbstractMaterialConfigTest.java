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
package com.thoughtworks.go.config.materials;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineConfigSaveValidationContext;
import com.thoughtworks.go.config.ValidationContext;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AbstractMaterialConfigTest {
    @Test
    void shouldRecomputePipelineUniqueFingerprint_whenAttributesChanged() {
        TestMaterialConfig testMaterialConfig = new TestMaterialConfig("foo");

        String pipelineUniqueFingerprint = testMaterialConfig.getPipelineUniqueFingerprint();
        testMaterialConfig.setConfigAttributes(m("bar", "baz"));
        assertThat(testMaterialConfig.getPipelineUniqueFingerprint()).isNotEqualTo(pipelineUniqueFingerprint);
    }

    @Test
    void shouldNotSetMaterialNameIfItIsSetToEmptyAsItsAnOptionalField() {
        AbstractMaterialConfig materialConfig = new TestMaterialConfig("");
        Map<String, String> map = new HashMap<>();
        map.put(AbstractMaterialConfig.MATERIAL_NAME, "");

        materialConfig.setConfigAttributes(map);

        assertThat(materialConfig.getName()).isNull();
    }

    @Test
    void shouldRecomputeSqlCriteriaAndXmlAttributeMap_whenAttributesChanged() {
        AbstractMaterialConfig testMaterialConfig = new TestMaterialConfig("foo");

        Map<String, Object> sqlCriteria = testMaterialConfig.getSqlCriteria();
        testMaterialConfig.setConfigAttributes(m("bar", "baz"));
        assertThat(testMaterialConfig.getSqlCriteria()).isNotEqualTo(sqlCriteria);
        assertThat(testMaterialConfig.getSqlCriteria().get("foo")).isEqualTo("baz");
    }

    @Test
    void shouldReturnTrueIfMaterialNameIsUsedInPipelineTemplate() {
        AbstractMaterialConfig material = new TestMaterialConfig("");
        material.setName(new CaseInsensitiveString("funky_name"));
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("blah"), "${COUNT}-${funky_name}", "", false, null, new BaseCollection<>());
        assertThat(material.isUsedInLabelTemplate(pipelineConfig)).isTrue();
    }

    @Test
    void shouldReturnTrueIfMaterialNameIsUsedInPipelineTemplate_caseInsensitive() {
        AbstractMaterialConfig material = new TestMaterialConfig("");
        material.setName(new CaseInsensitiveString("funky_name"));
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("blah"), "${COUNT}-${funky_Name}", "", false, null, new BaseCollection<>());
        assertThat(material.isUsedInLabelTemplate(pipelineConfig)).isTrue();
    }

    @Test
    void shouldReturnFalseIfMaterialNameIsNotUsedInPipelineTemplate() {
        AbstractMaterialConfig material = new TestMaterialConfig("");
        material.setName(new CaseInsensitiveString("funky_name"));
        assertThat(material.isUsedInLabelTemplate(new PipelineConfig(new CaseInsensitiveString("blah"), "${COUNT}-${test1}-test", "", false, null, new BaseCollection<>()))).isFalse();
    }

    @Test
    void shouldReturnFalseIfMaterialNameIsNotDefined() {
        AbstractMaterialConfig material = new TestMaterialConfig("test");
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("blah"), "${COUNT}-${test}-test", "", false, null, new BaseCollection<>());
        assertThat(material.isUsedInLabelTemplate(pipelineConfig)).isFalse();
    }

    @Test
    void shouldNotUseNameFieldButInsteadUseTheNameMethodToCheckIfTheMaterialNameIsUsedInThePipelineLabel() throws Exception {
        PipelineConfig pipelineConfig = mock(PipelineConfig.class);
        when(pipelineConfig.getLabelTemplate()).thenReturn("${COUNT}-${hg}-${dep}-${pkg}-${scm}");
        MaterialConfig hg = mock(HgMaterialConfig.class);
        when(hg.getName()).thenReturn(new CaseInsensitiveString("hg"));
        when(hg.isUsedInLabelTemplate(pipelineConfig)).thenCallRealMethod();
        MaterialConfig dependency = mock(DependencyMaterialConfig.class);
        when(dependency.getName()).thenReturn(new CaseInsensitiveString("dep"));
        when(dependency.isUsedInLabelTemplate(pipelineConfig)).thenCallRealMethod();
        MaterialConfig aPackage = mock(PackageMaterialConfig.class);
        when(aPackage.getName()).thenReturn(new CaseInsensitiveString("pkg"));
        when(aPackage.isUsedInLabelTemplate(pipelineConfig)).thenCallRealMethod();
        MaterialConfig aPluggableSCM = mock(PluggableSCMMaterialConfig.class);
        when(aPluggableSCM.getName()).thenReturn(new CaseInsensitiveString("scm"));
        when(aPluggableSCM.isUsedInLabelTemplate(pipelineConfig)).thenCallRealMethod();

        assertThat(hg.isUsedInLabelTemplate(pipelineConfig)).isTrue();
        assertThat(dependency.isUsedInLabelTemplate(pipelineConfig)).isTrue();
        assertThat(aPackage.isUsedInLabelTemplate(pipelineConfig)).isTrue();
        assertThat(aPluggableSCM.isUsedInLabelTemplate(pipelineConfig)).isTrue();

        verify(hg).getName();
        verify(dependency).getName();
        verify(aPackage).getName();
        verify(aPluggableSCM).getName();
    }

    @Test
    void shouldHandleBlankMaterialName() {
        TestMaterialConfig materialConfig = new TestMaterialConfig("");
        materialConfig.setName((CaseInsensitiveString) null);
        materialConfig.validate(PipelineConfigSaveValidationContext.forChain(true, "group", new PipelineConfig()));
        assertThat(materialConfig.errors().getAllOn(AbstractMaterialConfig.MATERIAL_NAME)).isEmpty();
        materialConfig.setName(new CaseInsensitiveString(null));
        materialConfig.validate(PipelineConfigSaveValidationContext.forChain(true, "group", new PipelineConfig()));
        assertThat(materialConfig.errors().getAllOn(AbstractMaterialConfig.MATERIAL_NAME)).isEmpty();
        materialConfig.setName(new CaseInsensitiveString(""));
        materialConfig.validate(PipelineConfigSaveValidationContext.forChain(true, "group", new PipelineConfig()));
        assertThat(materialConfig.errors().getAllOn(AbstractMaterialConfig.MATERIAL_NAME)).isEmpty();
    }

    private Map<String, String> m(String key, String value) {
        HashMap<String, String> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    public static class TestMaterialConfig extends AbstractMaterialConfig {
        private final String displayName;
        private String bar = "bar";
        private String quux = "quux";
        static int PIPELINE_UNIQUE_ATTRIBUTE_ADDED = 0;

        TestMaterialConfig(String displayName) {
            super(displayName);

            this.displayName = displayName;
        }

        @Override
        protected void appendPipelineUniqueCriteria(Map<String, Object> basicCriteria) {
            basicCriteria.put("pipeline-unique", "unique-" + PIPELINE_UNIQUE_ATTRIBUTE_ADDED++);
        }

        @Override
        protected void validateConcreteMaterial(ValidationContext validationContext) {
        }

        @Override
        protected void appendCriteria(Map<String, Object> parameters) {
            parameters.put("foo", bar);
        }

        @Override
        protected void appendAttributes(Map<String, Object> parameters) {
            parameters.put("baz", quux);
        }

        @Override
        public String getFolder() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Filter filter() {
            return null;
        }

        @Override
        public boolean isInvertFilter() {
            return false;
        }

        @Override
        public void setConfigAttributes(Object attributes) {
            super.setConfigAttributes(attributes);
            Map map = (Map) attributes;
            if (map.containsKey("bar")) {
                bar = (String) map.get("bar");
            }
            if (map.containsKey("quux")) {
                quux = (String) map.get("quux");
            }
        }

        @Override
        public boolean matches(String name, String regex) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getDescription() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getTypeForDisplay() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getDisplayName() {
            return displayName;
        }

        @Override
        public boolean isAutoUpdate() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setAutoUpdate(boolean autoUpdate) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getUriForDisplay() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Boolean isUsedInFetchArtifact(PipelineConfig pipelineConfig) {
            return false;
        }

        @Override
        public String getLongDescription() {
            throw new UnsupportedOperationException();
        }
    }
}
