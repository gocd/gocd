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

package com.thoughtworks.go.apiv2.notificationfilter.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.domain.NotificationFilter;
import com.thoughtworks.go.spark.Routes;

import java.util.List;

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl;

public class NotificationFiltersRepresenter {
    public static void toJSON(OutputWriter writer, List<NotificationFilter> notificationFilters) {
        writer.addLinks(outputLinkWriter -> {
            outputLinkWriter.addLink("self", Routes.NotificationFilterAPI.API_BASE);
            outputLinkWriter.addAbsoluteLink("doc", apiDocsUrl("#notification-filters"));
        });

        writer.addEmbedded(embeddedWriter -> {
            embeddedWriter.addChildList("filters", listWriter -> notificationFilters
                    .forEach(notificationFilter -> listWriter.addChild(childWriter -> NotificationFilterRepresenter.toJSON(childWriter, notificationFilter))));
        });
    }
}
