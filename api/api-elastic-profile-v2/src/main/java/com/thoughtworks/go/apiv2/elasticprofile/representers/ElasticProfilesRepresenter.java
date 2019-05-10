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

package com.thoughtworks.go.apiv2.elasticprofile.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.config.elastic.ElasticProfiles;
import com.thoughtworks.go.spark.Routes;

public class ElasticProfilesRepresenter {
    public static void toJSON(OutputWriter writer, ElasticProfiles elasticProfiles) {
            writer.addLinks(
                    outputLinkWriter -> outputLinkWriter
                            .addLink("self", Routes.ElasticProfileAPI.BASE)
                            .addAbsoluteLink("doc", Routes.ElasticProfileAPI.DOC)
                            .addLink("find", Routes.ElasticProfileAPI.find()))
                    .addChild("_embedded",
                            embeddedWriter -> embeddedWriter.addChildList("profiles",
                                    elasticProfilesWriter -> elasticProfiles.forEach(
                                            store -> elasticProfilesWriter.addChild(
                                                    elasticProfileWriter -> ElasticProfileRepresenter.toJSON(elasticProfileWriter, store)))));
    }
}
