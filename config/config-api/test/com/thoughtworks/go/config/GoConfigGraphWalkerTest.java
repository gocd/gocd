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

package com.thoughtworks.go.config;

import com.thoughtworks.go.config.materials.PackageMaterialConfig;
import com.thoughtworks.go.config.materials.PluggableSCMMaterialConfig;
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.domain.scm.SCM;
import org.junit.Test;

import static com.thoughtworks.go.util.ReflectionUtil.setField;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class GoConfigGraphWalkerTest {

    @Test
    public void walkedObject_shouldOnlyAcceptObjectsInThoughtworksPackage() {
        assertThat(new GoConfigGraphWalker.WalkedObject("non-tw object").shouldWalk(), is(false));
        assertThat(new GoConfigGraphWalker.WalkedObject(new PipelineConfig()).shouldWalk(), is(true));
    }

    @Test
    public void walkedObject_shouldNotWalkNull() {
        assertThat(new GoConfigGraphWalker.WalkedObject(null).shouldWalk(), is(false));
    }

    @Test
    public void shouldNotWalkFieldsWhichAreTaggedWithIgnoreTraversal() {
        PackageRepository repository = mock(PackageRepository.class);
        PackageDefinition packageDefinition = new PackageDefinition();
        packageDefinition.setRepository(repository);
        new GoConfigGraphWalker(packageDefinition).walk(new GoConfigGraphWalker.Handler() {
            @Override
            public void handle(Validatable validatable, ValidationContext ctx) {
                validatable.validate(ctx);
            }
        });
        verify(repository, never()).validate(any(ValidationContext.class));
    }

    @Test
    public void shouldNotWalkPackageDefinitionWhileTraversingPackageMaterial() {
        PackageDefinition packageDefinition = mock(PackageDefinition.class);
        PackageMaterialConfig packageMaterialConfig = new PackageMaterialConfig("package-id");
        setField(packageMaterialConfig, "packageDefinition", packageDefinition);
        new GoConfigGraphWalker(packageMaterialConfig).walk(new GoConfigGraphWalker.Handler() {
            @Override
            public void handle(Validatable validatable, ValidationContext ctx) {
                validatable.validate(ctx);
            }
        });
        verify(packageDefinition, never()).validate(any(ValidationContext.class));
    }

    @Test
    public void shouldNotWalkSCMMaterialWhileTraversingPluggableSCMMaterial() {
        SCM scmConfig = mock(SCM.class);
        PluggableSCMMaterialConfig pluggableSCMMaterialConfig = new PluggableSCMMaterialConfig("scm-id");
        setField(pluggableSCMMaterialConfig, "scmConfig", scmConfig);
        new GoConfigGraphWalker(pluggableSCMMaterialConfig).walk(new GoConfigGraphWalker.Handler() {
            @Override
            public void handle(Validatable validatable, ValidationContext ctx) {
                validatable.validate(ctx);
            }
        });
        verify(scmConfig, never()).validate(any(ValidationContext.class));
    }
}
