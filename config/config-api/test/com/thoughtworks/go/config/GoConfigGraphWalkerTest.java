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
import com.thoughtworks.go.config.merge.MergePipelineConfigs;
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.domain.packagerepository.Packages;
import com.thoughtworks.go.domain.scm.SCM;
import org.junit.Test;

import java.util.Iterator;

import static com.thoughtworks.go.util.ReflectionUtil.setField;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class GoConfigGraphWalkerTest {

    @Test
    public void walkedObject_shouldOnlyAcceptObjectsInThoughtworksPackage() {
        assertThat(new GoConfigGraphWalker.WalkedObject("non-tw object").shouldWalk(), is(false));
        assertThat(new GoConfigGraphWalker.WalkedObject(new PipelineConfig()).shouldWalk(), is(true));
    }

    @Test
    public void walkedObject_shouldWalkMergePipelineConfigs()
    {
        assertThat(new GoConfigGraphWalker.WalkedObject(new MergePipelineConfigs(new BasicPipelineConfigs())).shouldWalk(), is(true));
    }

    @Test
    public void walkedObject_shouldNotWalkNull() {
        assertThat(new GoConfigGraphWalker.WalkedObject(null).shouldWalk(), is(false));
    }

    private PipelineConfig mockPipelineConfig() {
        PipelineConfig pipe = mock(PipelineConfig.class);
        when(pipe.iterator()).thenReturn(new Iterator<StageConfig>() {
            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public StageConfig next() {
                return null;
            }

            @Override
            public void remove() {

            }
        });
        return pipe;
    }

    @Test
    public void shouldWalkPipelineConfigsInBasicPipelineConfigs()
    {
        PipelineConfig pipe = mockPipelineConfig();
        BasicPipelineConfigs basicPipelines = new BasicPipelineConfigs(pipe);
        new GoConfigGraphWalker(basicPipelines).walk(new GoConfigGraphWalker.Handler() {
            @Override
            public void handle(Validatable validatable, ValidationContext ctx) {
                validatable.validate(ctx);
            }
        });
        verify(pipe, atLeastOnce()).validate(any(ValidationContext.class));
    }

    @Test
    public void shouldWalkPipelineConfigsInMergePipelineConfigs()
    {
        PipelineConfig pipe = mockPipelineConfig();
        MergePipelineConfigs mergePipelines = new MergePipelineConfigs(new BasicPipelineConfigs(pipe));
        new GoConfigGraphWalker(mergePipelines).walk(new GoConfigGraphWalker.Handler() {
            @Override
            public void handle(Validatable validatable, ValidationContext ctx) {
                validatable.validate(ctx);
            }
        });
        verify(pipe, atLeastOnce()).validate(any(ValidationContext.class));
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

        BasicCruiseConfig config = new BasicCruiseConfig();
        PackageRepository packageRepository=mock(PackageRepository.class);
        when(packageRepository.getPackages()).thenReturn(new Packages(packageDefinition));
        when(packageDefinition.getRepository()).thenReturn(packageRepository);
        when(packageRepository.doesPluginExist()).thenReturn(true);
        when(packageDefinition.getId()).thenReturn("package-id");
        config.getPackageRepositories().add(packageRepository);

        final ConfigSaveValidationContext context = new ConfigSaveValidationContext(config);

        new GoConfigGraphWalker(packageMaterialConfig).walk(new GoConfigGraphWalker.Handler() {
            @Override
            public void handle(Validatable validatable, ValidationContext ctx) {
                validatable.validate(context);
            }
        });
        verify(packageDefinition, never()).validate(any(ValidationContext.class));
    }

    @Test
    public void shouldNotWalkSCMMaterialWhileTraversingPluggableSCMMaterial() {
        SCM scmConfig = mock(SCM.class);
        when(scmConfig.getName()).thenReturn("scm");
        when(scmConfig.getId()).thenReturn("scm-id");
        PluggableSCMMaterialConfig pluggableSCMMaterialConfig = new PluggableSCMMaterialConfig("scm-id");
        setField(pluggableSCMMaterialConfig, "scmConfig", scmConfig);
        BasicCruiseConfig config = new BasicCruiseConfig();
        config.getSCMs().add(scmConfig);
        final ConfigSaveValidationContext context = new ConfigSaveValidationContext(config);
        new GoConfigGraphWalker(pluggableSCMMaterialConfig).walk(new GoConfigGraphWalker.Handler() {
            @Override
            public void handle(Validatable validatable, ValidationContext ctx) {
                validatable.validate(context);
            }
        });
        verify(scmConfig, never()).validate(any(ValidationContext.class));
    }
}
