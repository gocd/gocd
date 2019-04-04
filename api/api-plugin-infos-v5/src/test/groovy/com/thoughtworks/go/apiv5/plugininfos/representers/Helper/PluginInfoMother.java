/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv5.plugininfos.representers.Helper;

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
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PluginInfoMother {
    public static AuthorizationPluginInfo createAuthorizationPluginInfo() {
        ArrayList<String> targetOperatingSystems = new ArrayList<>();
        targetOperatingSystems.add("os");
        GoPluginDescriptor descriptor = new GoPluginDescriptor("plugin_id", "1", new GoPluginDescriptor.About("GoPlugin", "v1", "goVersion1", "go plugin", new GoPluginDescriptor.Vendor("go", "goUrl"), targetOperatingSystems), "/home/pluginjar/", null, true);
        Capabilities capabilities = new Capabilities(SupportedAuthType.Password, true, true, true);

        return new AuthorizationPluginInfo(descriptor, getPluggableSettings(), getPluggableSettings(), new Image("content_type", "data", "hash"), capabilities);
    }

    public static SCMPluginInfo createSCMPluginInfo() {
        ArrayList<String> targetOperatingSystems = new ArrayList<>();
        targetOperatingSystems.add("os");
        GoPluginDescriptor descriptor = new GoPluginDescriptor("plugin_id", "1", new GoPluginDescriptor.About("GoPlugin", "v1", "goVersion1", "go plugin", new GoPluginDescriptor.Vendor("go", "goUrl"), targetOperatingSystems), "/home/pluginjar/", null, true);

        ArrayList<PluginConfiguration> configurations = new ArrayList<>();
        PluginConfiguration pluginConfiguration1 = new PluginConfiguration("key1", new MetadataWithPartOfIdentity(true, false, true));
        configurations.add(pluginConfiguration1);

        return new SCMPluginInfo(descriptor, "SCM", new PluggableInstanceSettings(configurations, new PluginView("Template")), null);
    }

    public static ConfigRepoPluginInfo createConfigRepoPluginInfo() {
        ArrayList<String> targetOperatingSystems = new ArrayList<>();
        targetOperatingSystems.add("os");
        GoPluginDescriptor descriptor = new GoPluginDescriptor("plugin_id", "1", new GoPluginDescriptor.About("GoPlugin", "v1", "goVersion1", "go plugin", new GoPluginDescriptor.Vendor("go", "goUrl"), targetOperatingSystems), "/home/pluginjar/", null, true);

        return new ConfigRepoPluginInfo(descriptor, null, getPluggableSettings());
    }

    public static ElasticAgentPluginInfo createElasticAgentPluginInfoForV4() {
        ArrayList<String> targetOperatingSystems = new ArrayList<>();
        targetOperatingSystems.add("os");
        GoPluginDescriptor descriptor = new GoPluginDescriptor("plugin_id", "1", new GoPluginDescriptor.About("GoPlugin", "v1", "goVersion1", "go plugin", new GoPluginDescriptor.Vendor("go", "goUrl"), targetOperatingSystems), "/home/pluginjar/", null, true);

        return new ElasticAgentPluginInfo(descriptor, getPluggableSettings(), null, null, getPluggableSettings(), new com.thoughtworks.go.plugin.domain.elastic.Capabilities(true, false));
    }

    public static ElasticAgentPluginInfo createElasticAgentPluginInfoForV5() {
        ArrayList<String> targetOperatingSystems = new ArrayList<>();
        targetOperatingSystems.add("os");
        GoPluginDescriptor descriptor = new GoPluginDescriptor("plugin_id", "1", new GoPluginDescriptor.About("GoPlugin", "v1", "goVersion1", "go plugin", new GoPluginDescriptor.Vendor("go", "goUrl"), targetOperatingSystems), "/home/pluginjar/", null, true);

        return new ElasticAgentPluginInfo(descriptor, getPluggableSettings(), getPluggableSettings(), null, null, new com.thoughtworks.go.plugin.domain.elastic.Capabilities(true, true, true));
    }

    public static CombinedPluginInfo createBadPluginInfo() {
        List<String> messages = new ArrayList<>();
        messages.add("This is bad plugin");
        GoPluginDescriptor badPluginDescriptor = new GoPluginDescriptor("bad_plugin", "1", new GoPluginDescriptor.About("BadPlugin", "v1", "goVersion1", "go plugin", new GoPluginDescriptor.Vendor("go", "goUrl"), null), "/home/bad_plugin/plugin_jar/", null, true);
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
        ArrayList<String> targetOperatingSystems = new ArrayList<>();
        targetOperatingSystems.add("os");
        GoPluginDescriptor descriptor = new GoPluginDescriptor("plugin_id", "1", new GoPluginDescriptor.About("GoPlugin", "v1", "goVersion1", "go plugin", new GoPluginDescriptor.Vendor("go", "goUrl"), targetOperatingSystems), "/home/pluginjar/", null, true);

        return new PluggableTaskPluginInfo(descriptor, "Task", getPluggableSettings());
    }

    public static PackageMaterialPluginInfo createPackageMaterialPluginInfo() {
        PluginConfiguration settings = new PluginConfiguration("key1", new PackageMaterialMetadata(true, true, true, "Test", 0));
        ArrayList<PluginConfiguration> repositorySettings = new ArrayList<>();
        repositorySettings.add(settings);
        PluggableInstanceSettings pluggableRepositorySettings = new PluggableInstanceSettings(repositorySettings, new PluginView("Template"));

        ArrayList<PluginConfiguration> packageSettings = new ArrayList<>();
        packageSettings.add(settings);
        PluggableInstanceSettings pluggablePackageSettings = new PluggableInstanceSettings(packageSettings, new PluginView("Template"));

        ArrayList<String> targetOperatingSystems = new ArrayList<>();
        targetOperatingSystems.add("os");
        GoPluginDescriptor descriptor = new GoPluginDescriptor("plugin_id", "1", new GoPluginDescriptor.About("GoPlugin", "v1", "goVersion1", "go plugin", new GoPluginDescriptor.Vendor("go", "goUrl"), targetOperatingSystems), "/home/pluginjar/", null, true);


        return new PackageMaterialPluginInfo(descriptor, pluggableRepositorySettings, pluggablePackageSettings, null);
    }


    public static SecretsPluginInfo createSecretConfigPluginInfo() {
        GoPluginDescriptor descriptor = new GoPluginDescriptor("plugin_id", "1", new GoPluginDescriptor.About("GoPlugin", "v1", "goVersion1", "go plugin", new GoPluginDescriptor.Vendor("go", "goUrl"), Collections.emptyList()), "/home/pluginjar/", null, true);

        SecretsPluginInfo secretsPluginInfo = new SecretsPluginInfo(descriptor, getPluggableSettings(), new Image("content_type", "data", "hash"));
        return secretsPluginInfo;
    }

    public static NotificationPluginInfo createNotificationPluginInfo() {
        ArrayList<String> targetOperatingSystems = new ArrayList<>();
        targetOperatingSystems.add("os");
        GoPluginDescriptor descriptor = new GoPluginDescriptor("plugin_id", "1", new GoPluginDescriptor.About("GoPlugin", "v1", "goVersion1", "go plugin", new GoPluginDescriptor.Vendor("go", "goUrl"), targetOperatingSystems), "/home/pluginjar/", null, true);

        return new NotificationPluginInfo(descriptor, getPluggableSettings());
    }

    public static AnalyticsPluginInfo createAnalyticsPluginInfo() {
        ArrayList<String> targetOperatingSystems = new ArrayList<>();
        List<SupportedAnalytics> supportedAnalytics = new ArrayList<>();
        targetOperatingSystems.add("os");
        supportedAnalytics.add(new SupportedAnalytics("Type 1", "Id 1", "Title 1"));
        GoPluginDescriptor descriptor = new GoPluginDescriptor("plugin_id", "1", new GoPluginDescriptor.About("GoPlugin", "v1", "goVersion1", "go plugin", new GoPluginDescriptor.Vendor("go", "goUrl"), targetOperatingSystems), "/home/pluginjar/", null, true);

        return new AnalyticsPluginInfo(descriptor, null, new com.thoughtworks.go.plugin.domain.analytics.Capabilities(supportedAnalytics), getPluggableSettings());
    }

    public static ArtifactPluginInfo createArtifactExtension() {
        ArrayList<String> targetOperatingSystems = new ArrayList<>();
        targetOperatingSystems.add("os");
        GoPluginDescriptor descriptor = new GoPluginDescriptor("plugin_id", "1", new GoPluginDescriptor.About("GoPlugin", "v1", "goVersion1", "go plugin", new GoPluginDescriptor.Vendor("go", "goUrl"), targetOperatingSystems), "/home/pluginjar/", null, true);

        return new ArtifactPluginInfo(descriptor, getPluggableSettings(), getPluggableSettings(), getPluggableSettings(), null, null);
    }
}
