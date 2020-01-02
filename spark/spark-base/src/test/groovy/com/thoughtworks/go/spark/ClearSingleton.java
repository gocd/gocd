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
package com.thoughtworks.go.spark;

import com.thoughtworks.go.plugin.access.analytics.AnalyticsMetadataStore;
import com.thoughtworks.go.plugin.access.artifact.ArtifactMetadataStore;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationMetadataStore;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsMetadataStore;
import com.thoughtworks.go.plugin.access.configrepo.ConfigRepoMetadataStore;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentMetadataStore;
import com.thoughtworks.go.plugin.access.notification.NotificationMetadataStore;
import com.thoughtworks.go.plugin.access.packagematerial.PackageMaterialMetadataStore;
import com.thoughtworks.go.plugin.access.packagematerial.PackageMetadataStore;
import com.thoughtworks.go.plugin.access.packagematerial.RepositoryMetadataStore;
import com.thoughtworks.go.plugin.access.pluggabletask.PluggableTaskConfigStore;
import com.thoughtworks.go.plugin.access.pluggabletask.PluggableTaskMetadataStore;
import com.thoughtworks.go.plugin.access.scm.NewSCMMetadataStore;
import com.thoughtworks.go.plugin.access.scm.SCMMetadataStore;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.newsecurity.x509.CachingSubjectDnX509PrincipalExtractor;

public class ClearSingleton {

    public static void clearSingletons() {
        AnalyticsMetadataStore.instance().clear();
        ArtifactMetadataStore.instance().clear();
        AuthorizationMetadataStore.instance().clear();
        ConfigRepoMetadataStore.instance().clear();
        ElasticAgentMetadataStore.instance().clear();
        NewSCMMetadataStore.instance().clear();
        NotificationMetadataStore.instance().clear();
        PackageMaterialMetadataStore.instance().clear();
        PluggableTaskMetadataStore.instance().clear();

        new CachingSubjectDnX509PrincipalExtractor().getCache().removeAll();

        //
        SessionUtils.unsetCurrentUser();

        //
        PackageMetadataStore.getInstance().clear();
        PluggableTaskConfigStore.store().clear();
        PluginSettingsMetadataStore.getInstance().clear();
        RepositoryMetadataStore.getInstance().clear();
        SCMMetadataStore.getInstance().clear();
    }
}
