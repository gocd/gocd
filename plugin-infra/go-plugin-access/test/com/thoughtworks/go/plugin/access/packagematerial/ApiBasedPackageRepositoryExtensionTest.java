/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.plugin.access.packagematerial;

import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration;
import com.thoughtworks.go.plugin.api.material.packagerepository.*;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.infra.*;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ApiBasedPackageRepositoryExtensionTest {

    public static final String PLUGIN_ID = "plugin-id";
    private PluginManager pluginManager;
    private ApiBasedPackageRepositoryExtension extension;
    private PackageMaterialConfiguration packageMaterialConfiguration;
    private PackageMaterialPoller packageMaterialPoller;

    @Before
    public void setUp() throws Exception {
        PackageMaterialProvider packageMaterialProvider = mock(PackageMaterialProvider.class);
        packageMaterialConfiguration = mock(PackageMaterialConfiguration.class);
        when(packageMaterialProvider.getConfig()).thenReturn(packageMaterialConfiguration);
        packageMaterialPoller = mock(PackageMaterialPoller.class);
        when(packageMaterialProvider.getPoller()).thenReturn(packageMaterialPoller);
        extension = new ApiBasedPackageRepositoryExtension(dummyPluginManager(packageMaterialProvider));
    }

    @Test
    public void shouldGetRepositoryConfiguration() throws Exception {
        RepositoryConfiguration repositoryConfiguration = new RepositoryConfiguration();
        when(packageMaterialConfiguration.getRepositoryConfiguration()).thenReturn(repositoryConfiguration);
        assertThat(extension.getRepositoryConfiguration(PLUGIN_ID), is(repositoryConfiguration));
    }

    @Test
    public void shouldThrowExceptionForPluginSettingsConfiguration() {
        try {
            extension.getPluginSettingsConfiguration(PLUGIN_ID);
            fail("should have thrown up");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("not implemented"));
        }
    }

    @Test
    public void shouldThrowExceptionForPluginSettingsView() {
        try {
            extension.getPluginSettingsView(PLUGIN_ID);
            fail("should have thrown up");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("not implemented"));
        }
    }

    @Test
    public void shouldThrowExceptionForValidatePluginSettings() {
        try {
            extension.validatePluginSettings(PLUGIN_ID, null);
            fail("should have thrown up");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("not implemented"));
        }
    }

    @Test
    public void shouldGetPackageConfiguration() throws Exception {
        PackageConfiguration packageConfiguration = new PackageConfiguration();
        when(packageMaterialConfiguration.getPackageConfiguration()).thenReturn(packageConfiguration);
        assertThat(extension.getPackageConfiguration(PLUGIN_ID), is(packageConfiguration));
    }

    @Test
    public void shouldCheckIfRepositoryConfigurationValid() throws Exception {
        RepositoryConfiguration repositoryConfiguration = new RepositoryConfiguration();
        ValidationResult validationResult = new ValidationResult();
        when(packageMaterialConfiguration.isRepositoryConfigurationValid(repositoryConfiguration)).thenReturn(validationResult);
        assertThat(extension.isRepositoryConfigurationValid(PLUGIN_ID, repositoryConfiguration), is(validationResult));
    }

    @Test
    public void shouldCheckIfPackageConfigurationValid() throws Exception {
        ValidationResult validationResult = new ValidationResult();
        PackageConfiguration packageConfiguration = new PackageConfiguration();
        RepositoryConfiguration repositoryConfiguration = new RepositoryConfiguration();
        when(packageMaterialConfiguration.isPackageConfigurationValid(packageConfiguration, repositoryConfiguration)).thenReturn(validationResult);
        assertThat(extension.isPackageConfigurationValid(PLUGIN_ID, packageConfiguration, repositoryConfiguration), is(validationResult));
    }

    @Test
    public void shouldCheckRepositoryConnection() throws Exception {
        RepositoryConfiguration repositoryConfiguration = new RepositoryConfiguration();
        Result result = new Result();
        when(packageMaterialPoller.checkConnectionToRepository(repositoryConfiguration)).thenReturn(result);
        assertThat(extension.checkConnectionToRepository(PLUGIN_ID, repositoryConfiguration), is(result));
    }

    @Test
    public void shouldCheckPackageConnection() throws Exception {
        PackageConfiguration packageConfiguration = new PackageConfiguration();
        RepositoryConfiguration repositoryConfiguration = new RepositoryConfiguration();
        Result result = new Result();
        when(packageMaterialPoller.checkConnectionToPackage(packageConfiguration, repositoryConfiguration)).thenReturn(result);
        assertThat(extension.checkConnectionToPackage(PLUGIN_ID, packageConfiguration, repositoryConfiguration), is(result));
    }

    @Test
    public void shouldGetLatestPackageRevision() throws Exception {
        PackageConfiguration packageConfiguration = new PackageConfiguration();
        RepositoryConfiguration repositoryConfiguration = new RepositoryConfiguration();
        PackageRevision packageRevision = new PackageRevision("r1", null, "user");
        when(packageMaterialPoller.getLatestRevision(packageConfiguration, repositoryConfiguration)).thenReturn(packageRevision);
        assertThat(extension.getLatestRevision(PLUGIN_ID, packageConfiguration, repositoryConfiguration), is(packageRevision));
    }

    @Test
    public void shouldGetLatestPackageRevisionSince() throws Exception {
        PackageConfiguration packageConfiguration = new PackageConfiguration();
        RepositoryConfiguration repositoryConfiguration = new RepositoryConfiguration();
        PackageRevision previousPackageRevision = new PackageRevision("r1", null, "user");
        PackageRevision packageRevision = new PackageRevision("r1", null, "user");
        when(packageMaterialPoller.latestModificationSince(packageConfiguration, repositoryConfiguration, previousPackageRevision)).thenReturn(packageRevision);
        assertThat(extension.latestModificationSince(PLUGIN_ID, packageConfiguration, repositoryConfiguration, previousPackageRevision), is(packageRevision));
    }

    private PluginManager dummyPluginManager(final PackageMaterialProvider mock) {
        return new PluginManager() {
            @Override
            public List<GoPluginDescriptor> plugins() {
                return Arrays.asList(new GoPluginDescriptor("yum", "1.0", null, null, null, true));
            }

            @Override
            public GoPluginDescriptor getPluginDescriptorFor(String pluginId) {
                return new GoPluginDescriptor("yum", "1.0", null, null, null, true);
            }

            @Override
            public <T> void doOnAll(Class<T> serviceReferenceClass, Action<T> actionToDoOnEachRegisteredServiceWhichMatches) {
            }

            @Override
            public <T> void doOnAll(Class<T> serviceReferenceClass, Action<T> actionToDoOnEachRegisteredServiceWhichMatches, ExceptionHandler<T> exceptionHandler) {
            }

            @Override
            public <T, R> R doOn(Class<T> serviceReferenceClass, String pluginId, ActionWithReturn<T, R> actionToDoOnTheRegisteredServiceWhichMatches) {
                return actionToDoOnTheRegisteredServiceWhichMatches.execute((T) mock, null);
            }

            @Override
            public <T> void doOn(Class<T> serviceReferenceClass, String pluginId, Action<T> action) {

            }

            @Override
            public <T> void doOnIfHasReference(Class<T> serviceReferenceClass, String pluginId, Action<T> action) {
                action.execute((T) mock, new GoPluginDescriptor("yum", "1.0", null, null, null, true));
            }

            @Override
            public void startInfrastructure(boolean shouldPoll) {
            }

            @Override
            public void registerPluginsFolderChangeListener() {
            }

            @Override
            public void stopInfrastructure() {

            }

            @Override
            public void addPluginChangeListener(PluginChangeListener pluginChangeListener, Class<?>... serviceReferenceClass) {

            }

            @Override
            public GoPluginApiResponse submitTo(String pluginId, GoPluginApiRequest apiRequest) {
                return null;
            }

            @Override
            public List<GoPluginIdentifier> allPluginsOfType(String extension) {
                return null;
            }

            @Override
            public boolean hasReferenceFor(Class serviceReferenceClass, String pluginId) {
                return false;
            }

            @Override
            public boolean isPluginOfType(String extension, String pluginId) {
                return false;
            }

            @Override
            public String resolveExtensionVersion(String pluginId, List<String> goSupportedExtensionVersions) {
                return null;
            }
        };
    }
}