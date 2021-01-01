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
package com.thoughtworks.go.apiv3.dashboard.representers;

import com.thoughtworks.go.api.base.OutputLinkWriter;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.server.dashboard.DashboardGroup;
import com.thoughtworks.go.server.dashboard.GoDashboardEnvironment;
import com.thoughtworks.go.server.dashboard.GoDashboardPipelineGroup;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.spark.Routes;

import java.util.function.Consumer;


public class DashboardGroupRepresenter {

    public static void toJSON(OutputWriter jsonOutputWriter, DashboardGroup model, Username username) {
        jsonOutputWriter
            .addLinks(linkWriterFor(model))
            .add("name", model.name())
            .addChildList("pipelines", model.pipelines())
            .add("can_administer", model.canAdminister(username));
    }

    private static Consumer<OutputLinkWriter> linkWriterFor(DashboardGroup group) {
        final String doc;
        final String self;

        if (group instanceof GoDashboardPipelineGroup) {
            doc = Routes.PipelineGroup.DOC;
            self = Routes.PipelineGroup.SELF;
        } else if (group instanceof GoDashboardEnvironment) {
            doc = Routes.EnvironmentConfig.DOC;
            self = Routes.EnvironmentConfig.name(group.name());
        } else {
            throw new IllegalArgumentException("Unknown DashboardGroup type: " + group.getClass().getCanonicalName());
        }

        return linksWriter -> linksWriter.
                addAbsoluteLink("doc", doc).
                addLink("self", self);
    }
}
