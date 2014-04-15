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

package com.thoughtworks.go.plugin.access.packagematerial;

import java.util.List;

import com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageMaterialConfiguration;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageMaterialProvider;
import com.thoughtworks.go.plugin.api.material.packagerepository.RepositoryConfiguration;
import com.thoughtworks.go.plugin.infra.Action;
import com.thoughtworks.go.plugin.infra.ActionWithReturn;
import com.thoughtworks.go.plugin.infra.ExceptionHandler;
import com.thoughtworks.go.plugin.infra.PluginChangeListener;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PackageMaterialMetadataLoaderTest {

    private PackageMaterialMetadataLoader metadataLoader;
    private PackageMaterialConfiguration packageMaterialConfiguration;
    private GoPluginDescriptor pluginDescriptor;

    @Before
    public void setUp() throws Exception {
        PackageMaterialProvider packageMaterialProvider = mock(PackageMaterialProvider.class);
        packageMaterialConfiguration = mock(PackageMaterialConfiguration.class);
        when(packageMaterialProvider.getConfig()).thenReturn(packageMaterialConfiguration);
        pluginDescriptor = new GoPluginDescriptor("plugin-id", "1.0", null, null, null, true);
        RepositoryMetadataStore.getInstance().removeMetadata(pluginDescriptor.id());
        PackageMetadataStore.getInstance().removeMetadata(pluginDescriptor.id());
        metadataLoader = new PackageMaterialMetadataLoader(dummyPluginManager(packageMaterialProvider, pluginDescriptor));
    }

    @Test
        public void shouldFetchPackageMetadataForPluginsWhichImplementPackageRepositoryMaterialExtensionPoint() {
        RepositoryConfiguration expectedRepoConfigurations = new RepositoryConfiguration();
        com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration expectedPackageConfigurations = new PackageConfiguration();
        when(packageMaterialConfiguration.getRepositoryConfiguration()).thenReturn(expectedRepoConfigurations);
        when(packageMaterialConfiguration.getPackageConfiguration()).thenReturn(expectedPackageConfigurations);


        metadataLoader.fetchRepositoryAndPackageMetaData(pluginDescriptor);

        assertThat(RepositoryMetadataStore.getInstance().getMetadata(pluginDescriptor.id()).getRepositoryConfiguration(), is(expectedRepoConfigurations));
        assertThat(PackageMetadataStore.getInstance().getMetadata(pluginDescriptor.id()).getPackageConfiguration(), is(expectedPackageConfigurations));
    }

    @Test
    public void shouldRegisterAsPluginFrameworkStartListener() throws Exception {
        PluginManager pluginManager = mock(PluginManager.class);
        metadataLoader = new PackageMaterialMetadataLoader(pluginManager);
        verify(pluginManager).addPluginChangeListener(metadataLoader, PackageMaterialProvider.class);
    }

    @Test
    public void shouldFetchMetadataOnPluginLoadedCallback() throws Exception {
        PackageMaterialMetadataLoader spy = spy(metadataLoader);
        doNothing().when(spy).fetchRepositoryAndPackageMetaData(pluginDescriptor);
        spy.pluginLoaded(pluginDescriptor);
        verify(spy).fetchRepositoryAndPackageMetaData(pluginDescriptor);
    }

    @Test
    public void shouldRemoveMetadataOnPluginUnLoadedCallback() throws Exception {
        RepositoryMetadataStore.getInstance().addMetadataFor(pluginDescriptor.id(), new PackageConfigurations());
        PackageMetadataStore.getInstance().addMetadataFor(pluginDescriptor.id(), new PackageConfigurations());
        metadataLoader.pluginUnLoaded(pluginDescriptor);
        assertThat(RepositoryMetadataStore.getInstance().getMetadata(pluginDescriptor.id()), is(nullValue()));
        assertThat(PackageMetadataStore.getInstance().getMetadata(pluginDescriptor.id()), is(nullValue()));
    }

    private PluginManager dummyPluginManager(final PackageMaterialProvider mock, final GoPluginDescriptor pluginDescriptor) {
        return new PluginManager() {
            @Override
            public List<GoPluginDescriptor> plugins() {
                return null;
            }

            @Override
            public GoPluginDescriptor getPluginDescriptorFor(String pluginId) {
                return null;
            }

            @Override
            public <T> void doOnAll(Class<T> serviceReferenceClass, Action<T> actionToDoOnEachRegisteredServiceWhichMatches) {
            }

            @Override
            public <T> void doOnAll(Class<T> serviceReferenceClass, Action<T> actionToDoOnEachRegisteredServiceWhichMatches, ExceptionHandler<T> exceptionHandler) {
            }

            @Override
            public <T, R> R doOn(Class<T> serviceReferenceClass, String pluginId, ActionWithReturn<T, R> actionToDoOnTheRegisteredServiceWhichMatches) {
                return null;
            }

            @Override
            public <T> void doOn(Class<T> serviceReferenceClass, String pluginId, Action<T> action) {

            }

            @Override
            public <T> void doOnIfHasReference(Class<T> serviceReferenceClass, String pluginId, Action<T> action) {
                action.execute((T)mock,pluginDescriptor);
            }

            @Override
            public void startPluginInfrastructure() {
            }

            @Override
            public void stopPluginInfrastructure() {
            }

            @Override
            public void addPluginChangeListener(PluginChangeListener pluginChangeListener, Class<?>... serviceReferenceClass) {

            }
        };
    }
}
