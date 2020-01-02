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
package com.thoughtworks.go.helpers;

import com.thoughtworks.go.plugin.domain.analytics.AnalyticsPluginInfo;
import com.thoughtworks.go.plugin.domain.analytics.SupportedAnalytics;
import com.thoughtworks.go.plugin.domain.artifact.ArtifactPluginInfo;
import com.thoughtworks.go.plugin.domain.authorization.AuthorizationPluginInfo;
import com.thoughtworks.go.plugin.domain.authorization.Capabilities;
import com.thoughtworks.go.plugin.domain.authorization.SupportedAuthType;
import com.thoughtworks.go.plugin.domain.common.*;
import com.thoughtworks.go.plugin.domain.configrepo.ConfigRepoPluginInfo;
import com.thoughtworks.go.plugin.domain.elastic.ElasticAgentPluginInfo;
import com.thoughtworks.go.plugin.domain.notification.NotificationPluginInfo;
import com.thoughtworks.go.plugin.domain.packagematerial.PackageMaterialPluginInfo;
import com.thoughtworks.go.plugin.domain.pluggabletask.PluggableTaskPluginInfo;
import com.thoughtworks.go.plugin.domain.scm.SCMPluginInfo;
import com.thoughtworks.go.plugin.domain.secrets.SecretsPluginInfo;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginBundleDescriptor;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;

import java.util.ArrayList;
import java.util.List;

public class PluginInfoMother {
    public static SecretsPluginInfo createSecretConfigPluginInfo() {
        GoPluginDescriptor descriptor = getDescriptor("plugin_id", "1", "/home/pluginjar/", true);
        return new SecretsPluginInfo(descriptor, getPluggableSettings(), new Image("content_type", "data", "hash"));
    }

    public static AuthorizationPluginInfo createAuthorizationPluginInfo() {
        Capabilities capabilities = new Capabilities(SupportedAuthType.Password, true, true, true);
        return new AuthorizationPluginInfo(getGoPluginDescriptor(), getPluggableSettings(), getPluggableSettings(), new Image("content_type", "data", "hash"), capabilities);
    }

    public static AuthorizationPluginInfo createAuthorizationPluginInfoWithoutAbout() {
        Capabilities capabilities = new Capabilities(SupportedAuthType.Password, true, true, true);
        GoPluginDescriptor descriptor = GoPluginDescriptor.builder().id("plugin_id").version("1").pluginJarFileLocation("/home/pluginjar/").isBundledPlugin(true).build();
        return new AuthorizationPluginInfo(descriptor, getPluggableSettings(), null, new Image("content_type", "data", "hash"), capabilities);
    }

    public static AuthorizationPluginInfo createAuthorizationPluginInfoWithoutImage() {
        Capabilities capabilities = new Capabilities(SupportedAuthType.Password, true, true, true);
        return new AuthorizationPluginInfo(getGoPluginDescriptor(), getPluggableSettings(), null, null, capabilities);
    }

    public static AuthorizationPluginInfo createAuthorizationPluginInfoWithoutRoleSettings() {
        Capabilities capabilities = new Capabilities(SupportedAuthType.Password, true, true, true);
        return new AuthorizationPluginInfo(getGoPluginDescriptor(), getPluggableSettings(), null, new Image("content_type", "data", "hash"), capabilities);
    }

    public static SCMPluginInfo createSCMPluginInfo() {

        ArrayList<PluginConfiguration> configurations = new ArrayList<>();
        PluginConfiguration pluginConfiguration1 = new PluginConfiguration("key1", new MetadataWithPartOfIdentity(true, false, true));
        configurations.add(pluginConfiguration1);
        return new SCMPluginInfo(getGoPluginDescriptor(), "SCM", new PluggableInstanceSettings(configurations, new PluginView("Template")), null);
    }

    public static ConfigRepoPluginInfo createConfigRepoPluginInfo() {
        return new ConfigRepoPluginInfo(getGoPluginDescriptor(), null, getPluggableSettings(), new com.thoughtworks.go.plugin.domain.configrepo.Capabilities(true, true));
    }

    public static ConfigRepoPluginInfo createConfigRepoPluginInfoWithoutPluginSettings() {
        return new ConfigRepoPluginInfo(getGoPluginDescriptor(), null, null, new com.thoughtworks.go.plugin.domain.configrepo.Capabilities(true, true));
    }

    public static ElasticAgentPluginInfo createElasticAgentPluginInfoForV4() {
        return new ElasticAgentPluginInfo(getGoPluginDescriptor(), getPluggableSettings(), null, null, getPluggableSettings(), new com.thoughtworks.go.plugin.domain.elastic.Capabilities(true, false));
    }

    public static ElasticAgentPluginInfo createElasticAgentPluginInfoForV5() {
        GoPluginDescriptor descriptor = getDescriptor("plugin_id", "GoPlugin", "/home/pluginjar/", true, "os");
        return new ElasticAgentPluginInfo(descriptor, getPluggableSettings(), getPluggableSettings(), null, null, new com.thoughtworks.go.plugin.domain.elastic.Capabilities(true, true, true));
    }

    public static GoPluginDescriptor getDescriptor(String pluginId,
                                                   String name,
                                                   String pluginJarLocation,
                                                   boolean isBundledPlugin,
                                                   String... targetOperatingSystems) {
        GoPluginDescriptor.About about = GoPluginDescriptor.About.builder()
                .name(name)
                .version("v1")
                .targetGoVersion("goVersion1")
                .description("go plugin")
                .vendor(new GoPluginDescriptor.Vendor("go", "goUrl"))
                .targetOperatingSystems(List.of(targetOperatingSystems))
                .build();

        GoPluginDescriptor descriptor = GoPluginDescriptor.builder()
                .id(pluginId)
                .version("1")
                .about(about)
                .pluginJarFileLocation(pluginJarLocation)
                .isBundledPlugin(isBundledPlugin)
                .build();

        descriptor.setBundleDescriptor(new GoPluginBundleDescriptor(descriptor));

        return descriptor;
    }

    public static CombinedPluginInfo createBadPluginInfo() {
        List<String> messages = new ArrayList<>();
        messages.add("This is bad plugin");
        GoPluginDescriptor badPluginDescriptor = getDescriptor("bad_plugin", "BadPlugin", "/home/bad_plugin/plugin_jar/", true);
        badPluginDescriptor.markAsInvalid(messages, null);
        return new CombinedPluginInfo(new BadPluginInfo(badPluginDescriptor));
    }

    public static PluggableInstanceSettings getPluggableSettings() {
        PluginConfiguration pluginConfiguration1 = new PluginConfiguration("key1", new Metadata(true, false));
        PluginConfiguration pluginConfiguration2 = new PluginConfiguration("key2", new Metadata(true, false));
        ArrayList<PluginConfiguration> configurations = new ArrayList<>();

        configurations.add(pluginConfiguration1);
        configurations.add(pluginConfiguration2);

        return new PluggableInstanceSettings(configurations, new PluginView("Template"));
    }

    public static PluggableTaskPluginInfo createTaskPluginInfo() {

        return new PluggableTaskPluginInfo(getGoPluginDescriptor(), "Task", getPluggableSettings());
    }

    public static PackageMaterialPluginInfo createPackageMaterialPluginInfo() {
        PluginConfiguration settings = new PluginConfiguration("key1", new PackageMaterialMetadata(true, true, true, "Test", 0));
        ArrayList<PluginConfiguration> repositorySettings = new ArrayList<>();
        repositorySettings.add(settings);
        PluggableInstanceSettings pluggableRepositorySettings = new PluggableInstanceSettings(repositorySettings, new PluginView("Template"));

        ArrayList<PluginConfiguration> packageSettings = new ArrayList<>();
        packageSettings.add(settings);
        PluggableInstanceSettings pluggablePackageSettings = new PluggableInstanceSettings(packageSettings, new PluginView("Template"));

        return new PackageMaterialPluginInfo(getGoPluginDescriptor(), pluggableRepositorySettings, pluggablePackageSettings, null);
    }

    private static GoPluginDescriptor getGoPluginDescriptor() {
        return getDescriptor("plugin_id", "GoPlugin", "/home/pluginjar/", true, "os");
    }

    public static NotificationPluginInfo createNotificationPluginInfo() {
        return new NotificationPluginInfo(getGoPluginDescriptor(), getPluggableSettings());
    }

    public static NotificationPluginInfo createNotificationPluginInfoWithoutPluginSettings() {
        return new NotificationPluginInfo(getGoPluginDescriptor(), null);
    }

    public static AnalyticsPluginInfo createAnalyticsPluginInfo() {
        List<SupportedAnalytics> supportedAnalytics = new ArrayList<>();
        supportedAnalytics.add(new SupportedAnalytics("Type 1", "Id 1", "Title 1"));
        return new AnalyticsPluginInfo(getGoPluginDescriptor(), null, new com.thoughtworks.go.plugin.domain.analytics.Capabilities(supportedAnalytics), getPluggableSettings());
    }

    public static AnalyticsPluginInfo createAnalyticsPluginInfoWithoutPluginSettings() {
        List<SupportedAnalytics> supportedAnalytics = new ArrayList<>();
        supportedAnalytics.add(new SupportedAnalytics("Type 1", "Id 1", "Title 1"));
        return new AnalyticsPluginInfo(getGoPluginDescriptor(), null, new com.thoughtworks.go.plugin.domain.analytics.Capabilities(supportedAnalytics), null);
    }

    public static AnalyticsPluginInfo createAnalyticsPluginWithoutSupportedAnalytics() {
        List<SupportedAnalytics> supportedAnalytics = new ArrayList<>();
        return new AnalyticsPluginInfo(getGoPluginDescriptor(), null, new com.thoughtworks.go.plugin.domain.analytics.Capabilities(supportedAnalytics), getPluggableSettings());
    }

    public static ArtifactPluginInfo createArtifactExtension() {
        return new ArtifactPluginInfo(getGoPluginDescriptor(), getPluggableSettings(), getPluggableSettings(), getPluggableSettings(), null, null);
    }
}
