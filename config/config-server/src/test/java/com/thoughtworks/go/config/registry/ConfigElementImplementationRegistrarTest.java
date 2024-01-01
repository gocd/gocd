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
package com.thoughtworks.go.config.registry;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.PackageMaterialConfig;
import com.thoughtworks.go.config.materials.PluggableSCMMaterialConfig;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.config.materials.perforce.P4MaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig;
import com.thoughtworks.go.config.pluggabletask.PluggableTask;
import com.thoughtworks.go.domain.Task;
import com.thoughtworks.go.domain.config.Admin;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@ExtendWith(MockitoExtension.class)
public class ConfigElementImplementationRegistrarTest {

    private ConfigElementImplementationRegistry registry;

    @BeforeEach
    public void setUp() {
        registry = new ConfigElementImplementationRegistry();
        new ConfigElementImplementationRegistrar(registry).initialize();
    }

    @Test
    public void testShouldProvideTheDefaultTaskConfigMappingsOnlyForBuiltInTasks() {
        List<Class<? extends Task>> tasks = new ArrayList<>();
        tasks.add(AntTask.class);
        tasks.add(NantTask.class);
        tasks.add(ExecTask.class);
        tasks.add(RakeTask.class);
        tasks.add(FetchTask.class);
        tasks.add(PluggableTask.class);
        tasks.add(FetchPluggableArtifactTask.class);

        assertThat(registry.implementersOf(Task.class), is(tasks));
    }

    @Test
    public void testShouldProvideTheDefaultMaterialConfigMappings() {
        List<Class<? extends MaterialConfig>> materials = new ArrayList<>();
        materials.add(SvnMaterialConfig.class);
        materials.add(HgMaterialConfig.class);
        materials.add(GitMaterialConfig.class);
        materials.add(DependencyMaterialConfig.class);
        materials.add(P4MaterialConfig.class);
        materials.add(TfsMaterialConfig.class);
        materials.add(PackageMaterialConfig.class);
        materials.add(PluggableSCMMaterialConfig.class);

        assertThat(registry.implementersOf(MaterialConfig.class), is(materials));
    }

    @Test
    public void testShouldProvideTheDefaultArtifactsConfigMappings() {
        List<Class<? extends ArtifactTypeConfig>> artifacts = new ArrayList<>();
        artifacts.add(TestArtifactConfig.class);
        artifacts.add(BuildArtifactConfig.class);
        artifacts.add(PluggableArtifactConfig.class);

        assertThat(registry.implementersOf(ArtifactTypeConfig.class), is(artifacts));
    }

    @Test
    public void testShouldProvideTheDefaultAdminConfigMappings() {
        List<Class<? extends Admin>> admin = new ArrayList<>();
        admin.add(AdminUser.class);
        admin.add(AdminRole.class);

        assertThat(registry.implementersOf(Admin.class), is(admin));
    }
}
