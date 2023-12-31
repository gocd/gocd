/*
 * Copyright 2024 Thoughtworks, Inc.
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
package com.thoughtworks.go.domain.materials;

import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.AbstractMaterial;
import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.util.command.ConsoleOutputStreamConsumer;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

public class AbstractMaterialTest {

    public static class TestMaterial extends AbstractMaterial {
        private static final AtomicInteger PIPELINE_UNIQUE_ATTRIBUTE_ADDED = new AtomicInteger(0);
        private final String displayName;

        public TestMaterial(String displayName) {
            super(displayName);

            this.displayName = displayName;
        }

        @Override
        protected void appendPipelineUniqueCriteria(Map<String, Object> basicCriteria) {
            basicCriteria.put("pipeline-unique", "unique-" + PIPELINE_UNIQUE_ATTRIBUTE_ADDED.getAndIncrement());
        }

        @Override
        protected void appendCriteria(Map<String, Object> parameters) {
            parameters.put("foo", "bar");
        }

        @Override
        protected void appendAttributes(Map<String, Object> parameters) {
            parameters.put("baz", "quux");
        }

        @Override
        public String getFolder() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateTo(ConsoleOutputStreamConsumer outputStreamConsumer, File baseDir, RevisionContext revisionContext, final SubprocessExecutionContext execCtx) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void toJson(Map<String, Object> jsonMap, Revision revision) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean matches(String name, String regex) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void emailContent(StringBuilder content, Modification modification) {
            throw new UnsupportedOperationException();
        }

        @Override
        public MaterialInstance createMaterialInstance() {
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
        public void populateEnvironmentContext(EnvironmentVariableContext context, MaterialRevision materialRevision, File workingDir) {
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
        public MatchedRevision createMatchedRevision(Modification modifications, String searchString) {
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
        public Class<MaterialInstance> getInstanceType() {
            throw new UnsupportedOperationException("instance not available for test material");
        }

        @Override
        public Revision oldestRevision(Modifications modifications) {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        @Override
        public String getLongDescription() {
            throw new UnsupportedOperationException();
        }
    }

    @Test
    public void shouldCacheCriteriaAndAttributeMap() {
        TestMaterial testMaterial = new TestMaterial("foo");

        Map<String, Object> sqlCriteria = testMaterial.getSqlCriteria();
        assertThat(testMaterial.getSqlCriteria()).isSameAs(sqlCriteria);
        assertThat(testMaterial.getSqlCriteria().get("foo")).isEqualTo("bar");
        assertThat(testMaterial.getSqlCriteria().getClass().getCanonicalName()).isEqualTo("java.util.Collections.UnmodifiableMap");

        Map<String, Object> attributesForXml = testMaterial.getAttributesForXml();
        assertThat(testMaterial.getAttributesForXml()).isSameAs(attributesForXml);
        assertThat(testMaterial.getAttributesForXml().get("baz")).isEqualTo("quux");

        assertThat(testMaterial.getAttributesForXml().getClass().getCanonicalName()).isEqualTo("java.util.Collections.UnmodifiableMap");
    }

    @Test
    public void shouldCachePipelineUniqueFingerprint() {
        TestMaterial testMaterial = new TestMaterial("foo");

        String pipelineUniqueFingerprint = testMaterial.getPipelineUniqueFingerprint();
        int appendPipelineUniqueAttrsCallCount = TestMaterial.PIPELINE_UNIQUE_ATTRIBUTE_ADDED.get();
        assertThat(testMaterial.getPipelineUniqueFingerprint()).isSameAs(pipelineUniqueFingerprint);
        assertThat(appendPipelineUniqueAttrsCallCount).isEqualTo(TestMaterial.PIPELINE_UNIQUE_ATTRIBUTE_ADDED.get());
    }

    @Test
    public void shouldReturnFullNameIfTheLengthIsLessThanGivenThreshold() throws Exception {
        AbstractMaterial material = new TestMaterial("foo_bar_baz_quuz_ban");
        assertThat(material.getTruncatedDisplayName()).isEqualTo("foo_bar_baz_quuz_ban");
    }

    @Test
    public void shouldReturnTruncatedNameIfTheLengthIsGreaterThanGivenThreshold() throws Exception {
        AbstractMaterial material = new TestMaterial("foo_bar_baz_quuz_ban_pavan");
        assertThat(material.getTruncatedDisplayName()).isEqualTo("foo_bar_ba..._ban_pavan");
    }

    @Test
    public void shouldNotIgnoreForSchedulingByDefault_sinceItOnlyAppliesToDependencyMaterial() {
        AbstractMaterial material = new TestMaterial("salty_sumatran_rhinoceros");
        assertThat(material.ignoreForScheduling()).isFalse();
    }

    @Test
    public void shouldThrowExceptionOnCheckConnection() {
        TestMaterial material = new TestMaterial("name");
        SubprocessExecutionContext executionContext = mock(SubprocessExecutionContext.class);
        assertThatCode(() -> material.checkConnection(executionContext))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("'checkConnection' cannot be performed on material of type name");
    }

    @Test
    public void updateConfigShouldIgnoreNonPasswordAwareConfig() {
        MaterialConfig config = mock(MaterialConfig.class);
        SvnMaterial passwordAwareMaterial = new SvnMaterial("url", "username", "password", false);
        passwordAwareMaterial.updateFromConfig(config);
        verifyNoInteractions(config);
    }

    @Test
    public void updateConfigShouldTakeUserPassFromPasswordAwareConfig() {
        SvnMaterialConfig config = mock(SvnMaterialConfig.class);
        when(config.getUserName()).thenReturn("newUsername");
        when(config.getPassword()).thenReturn("newPassword");
        SvnMaterial passwordAwareMaterial = new SvnMaterial("url", "username", "password", false);
        passwordAwareMaterial.updateFromConfig(config);

        assertThat(passwordAwareMaterial.getUserName()).isEqualTo("newUsername");
        assertThat(passwordAwareMaterial.getPassword()).isEqualTo("newPassword");

        verify(config).getUserName();
        verify(config).getPassword();
    }
}
