/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.service.materials;

import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.config.materials.perforce.P4Material;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.config.materials.tfs.TfsMaterial;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.materials.ValidationBean;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.server.service.MaterialConfigConverter;
import org.junit.Before;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@RunWith(Theories.class)
public class MaterialConnectivityServiceTest {

    private MaterialConnectivityService service;
    private static SubprocessExecutionContext executionContext;
    private static ValidationBean VALID = ValidationBean.valid();
    private static ValidationBean INVALID = ValidationBean.notValid("could not connect");
    private MaterialConfigConverter materialConfigConverter;

    @Before
    public void setUp() throws Exception {
        materialConfigConverter = mock(MaterialConfigConverter.class);
        executionContext = mock(SubprocessExecutionContext.class);
        service = new MaterialConnectivityService(materialConfigConverter);
    }

    @DataPoint public static RequestDataPoints GIT_VALID = new RequestDataPoints(new GitMaterial("url") {
        @Override
        public ValidationBean checkConnection(SubprocessExecutionContext execCtx) {
            return VALID;
        }
    }, GitMaterial.class, VALID);

    @DataPoint public static RequestDataPoints GIT_INVALID = new RequestDataPoints(new GitMaterial("url") {
        @Override
        public ValidationBean checkConnection(SubprocessExecutionContext execCtx) {
            return INVALID;
        }
    }, GitMaterial.class, INVALID);

    @DataPoint public static RequestDataPoints SVN_VALID = new RequestDataPoints(new SvnMaterial("url", "user", "password", false) {
        @Override
        public ValidationBean checkConnection(SubprocessExecutionContext execCtx) {
            return VALID;
        }
    }, SvnMaterial.class, VALID);

    @DataPoint public static RequestDataPoints SVN_INVALID = new RequestDataPoints(new SvnMaterial("url", "user", "password", false) {
        @Override
        public ValidationBean checkConnection(SubprocessExecutionContext execCtx) {
            return INVALID;
        }
    }, SvnMaterial.class, INVALID);


    @DataPoint public static RequestDataPoints TFS_VALID = new RequestDataPoints(new TfsMaterial(mock(GoCipher.class)) {
        @Override
        public ValidationBean checkConnection(SubprocessExecutionContext execCtx) {
            return VALID;
        }
    }, TfsMaterial.class, VALID);

    @DataPoint public static RequestDataPoints TFS_INVALID = new RequestDataPoints(new TfsMaterial(mock(GoCipher.class)) {
        @Override
        public ValidationBean checkConnection(SubprocessExecutionContext execCtx) {
            return INVALID;
        }
    }, TfsMaterial.class, INVALID);

    @DataPoint public static RequestDataPoints P4_VALID = new RequestDataPoints(new P4Material("url", "view", "user") {
        @Override
        public ValidationBean checkConnection(SubprocessExecutionContext execCtx) {
            return VALID;
        }
    }, P4Material.class, VALID);

    @DataPoint public static RequestDataPoints P4_INVALID = new RequestDataPoints(new P4Material("url", "view", "user") {
        @Override
        public ValidationBean checkConnection(SubprocessExecutionContext execCtx) {
            return INVALID;
        }
    }, P4Material.class, INVALID);

    @DataPoint public static RequestDataPoints HG_VALID = new RequestDataPoints(new HgMaterial("url", null) {
        @Override
        public ValidationBean checkConnection(SubprocessExecutionContext execCtx) {
            return VALID;
        }
    }, HgMaterial.class, VALID);

    @DataPoint public static RequestDataPoints HG_INVALID = new RequestDataPoints(new HgMaterial("url", null) {
        @Override
        public ValidationBean checkConnection(SubprocessExecutionContext execCtx) {
            return INVALID;
        }
    }, HgMaterial.class, INVALID);

    @DataPoint public static RequestDataPoints DEPENDENCY_MATERIAL_VALID = new RequestDataPoints(new DependencyMaterial() {
        @Override
        public ValidationBean checkConnection(SubprocessExecutionContext execCtx) {
            return VALID;
        }
    }, DependencyMaterial.class, VALID);

    @DataPoint public static RequestDataPoints DEPENDENCY_MATERAIL_INVALID = new RequestDataPoints(new DependencyMaterial() {
        @Override
        public ValidationBean checkConnection(SubprocessExecutionContext execCtx) {
            return INVALID;
        }
    }, DependencyMaterial.class, INVALID);

    @Theory
    public void shouldCheckConnections(RequestDataPoints dataPoints) throws Exception {
        MaterialConfig config = dataPoints.material.config();

        MaterialConnectivityService spy = spy(service);
        doReturn(dataPoints.klass).when(spy).getMaterialClass(dataPoints.material);
        doReturn(dataPoints.material).when(materialConfigConverter).toMaterial(config);

        ValidationBean actual = spy.checkConnection(config, executionContext);

        assertThat(actual, is(dataPoints.expectedResult));
    }

    private static class RequestDataPoints<T extends Material> {
        private final T material;
        private final Class klass;
        private final ValidationBean expectedResult;

        public RequestDataPoints(T material, Class klass, ValidationBean expectedResult) {
            this.material = material;
            this.klass = klass;
            this.expectedResult = expectedResult;
        }
    }

}
