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
package com.thoughtworks.go.apiv2.clusterprofiles.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.config.PluginProfiles;
import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.spark.Routes;

import java.util.function.Function;

public class ClusterProfilesRepresenter {
    public static void toJSON(OutputWriter writer, PluginProfiles<ClusterProfile> clusterProfiles, Function<String, Boolean> canAdministerClusterProfile) {
        writer.addLinks(
                outputLinkWriter -> outputLinkWriter
                        .addLink("self", Routes.ClusterProfilesAPI.BASE)
                        .addAbsoluteLink("doc", Routes.ClusterProfilesAPI.DOC))
                .addChild("_embedded", embeddedWriter ->
                        embeddedWriter.addChildList("cluster_profiles", clustersWriter ->
                                clusterProfiles.forEach(cluster -> {
                                    boolean canAdminister = canAdministerClusterProfile.apply(cluster.getId());
                                    clustersWriter.addChild(clusterWriter -> ClusterProfileRepresenter.toJSON(clusterWriter, cluster, canAdminister));
                                })));
    }
}
