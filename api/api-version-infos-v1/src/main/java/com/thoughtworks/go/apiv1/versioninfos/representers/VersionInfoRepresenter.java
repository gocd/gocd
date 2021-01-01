/*
 * Copyright 2021 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv1.versioninfos.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.domain.VersionInfo;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl;

public class VersionInfoRepresenter {
    private static final Logger LOG = LoggerFactory.getLogger(VersionInfoRepresenter.class);

    public static void toJSON(OutputWriter outputWriter, VersionInfo versionInfo, SystemEnvironment systemEnvironment) {
        if (versionInfo == null) {
            return;
        }
        outputWriter.addLinks(linkWriter -> {
            linkWriter.addAbsoluteLink("doc", apiDocsUrl("#version-info"))
                    .addLink("self", Routes.VersionInfos.BASE + Routes.VersionInfos.STALE);
        }).add("component_name", versionInfo.getComponentName());

        renderUpdatedServerUrl(outputWriter, versionInfo, systemEnvironment);

        outputWriter.add("installed_version", versionInfo.getInstalledVersion().toString());

        if (versionInfo.getLatestVersion() == null) {
            outputWriter.renderNull("latest_version");
        } else {
            outputWriter.add("latest_version", versionInfo.getLatestVersion().toString());
        }
    }

    private static void renderUpdatedServerUrl(OutputWriter outputWriter, VersionInfo versionInfo, SystemEnvironment systemEnvironment) {
        try {
            URI updateServerUri = URI.create(systemEnvironment.getUpdateServerUrl());
            URIBuilder uriBuilder = new URIBuilder(updateServerUri);

            String currentVersion = versionInfo.getInstalledVersion() != null ? versionInfo.getInstalledVersion().toString() : "unknown";
            uriBuilder.addParameter("current_version", currentVersion);

            String updatedServeUrl = uriBuilder.build().toString();
            outputWriter.add("update_server_url", updatedServeUrl);
        } catch (Exception e) {
            LOG.error("Exception occurred while building updated server url", e);
            outputWriter.renderNull("update_server_url");
        }
    }
}
