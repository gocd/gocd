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

package com.thoughtworks.go.apiv1.securityauthconfig.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.ConfigurationPropertyRepresenter;
import com.thoughtworks.go.api.representers.ErrorGetter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.PluginProfiles;
import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.spark.Routes;

import java.util.Collections;
import java.util.Map;

public class SecurityAuthConfigsRepresenter {

    public static void toJSON(OutputWriter writer, PluginProfiles<SecurityAuthConfig> securityAuthConfigs) {
        writer.addLinks(
                outputLinkWriter -> outputLinkWriter
                        .addLink("self", Routes.SecurityAuthConfigAPI.BASE)
                        .addAbsoluteLink("doc", Routes.SecurityAuthConfigAPI.DOC))
                .addChild("_embedded", embeddedWriter ->
                        embeddedWriter.addChildList("auth_configs", clustersWriter ->
                                securityAuthConfigs.forEach(cluster -> clustersWriter.addChild(clusterWriter -> SecurityAuthConfigRepresenter.toJSON(clusterWriter, cluster)))));
    }
}
