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
package com.thoughtworks.go.apiv3.dashboard.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.spark.Routes;

public class DashboardRepresenter {

    public static void toJSON(OutputWriter jsonOutputWriter, DashboardFor dashboardFor) {
        jsonOutputWriter
            .addLinks(linksWriter -> linksWriter.addLink("self", Routes.Dashboard.SELF)
                .addAbsoluteLink("doc", Routes.Dashboard.DOC))
            .add("_personalization", dashboardFor.getPersonalizationEtag())
            .addChild("_embedded", childWriter -> {
                childWriter

                    .addChildList("pipeline_groups", listWriter -> {
                        dashboardFor.getPipelineGroups().forEach(group -> {
                            listWriter.addChild(childItemWriter -> {
                                DashboardGroupRepresenter.toJSON(childItemWriter, group, dashboardFor.getUsername());
                            });
                        });
                    })

                    .addChildList("environments", listWriter -> {
                        dashboardFor.getEnvironments().forEach(group -> {
                            listWriter.addChild(childItemWriter -> {
                                DashboardGroupRepresenter.toJSON(childItemWriter, group, dashboardFor.getUsername());
                            });
                        });
                    })

                    .addChildList("pipelines", listWriter -> {
                        dashboardFor.getPipelines()
                            .forEach(pipeline -> {
                                listWriter.addChild(childItemWriter -> PipelineRepresenter.toJSON(childItemWriter, pipeline, dashboardFor.getUsername()));
                            });
                    });
            });
    }
}
