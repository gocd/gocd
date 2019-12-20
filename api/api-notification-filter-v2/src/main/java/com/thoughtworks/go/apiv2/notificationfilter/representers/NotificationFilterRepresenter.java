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
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.domain.NotificationFilter;
import com.thoughtworks.go.domain.StageEvent;
import com.thoughtworks.go.spark.Routes;

import static com.thoughtworks.go.domain.PersistentObject.NOT_PERSISTED;

public class NotificationFilterRepresenter {
    public static void toJSON(OutputWriter writer, NotificationFilter filter) {
        writer.addLinks(linkWriter -> {
            linkWriter.addLink("self", Routes.NotificationFilterAPI.self(filter.getId()));
            linkWriter.addAbsoluteLink("doc", Routes.NotificationFilterAPI.DOC);
            linkWriter.addLink("find", Routes.NotificationFilterAPI.FIND);
        });
        writer.addIfNotNull("id", filter.getId() == NOT_PERSISTED ? null : filter.getId())
                .add("pipeline", filter.getPipelineName())
                .add("stage", filter.getStageName())
                .add("event", filter.getEvent().toString())
                .add("match_commits", filter.isMyCheckin());
    }

    public static NotificationFilter fromJSON(JsonReader reader) {
        NotificationFilter filter = new NotificationFilter(reader.getString("pipeline"),
                reader.getString("stage"),
                StageEvent.from(reader.getString("event")),
                reader.getBoolean("match_commits")
        );
        filter.setId(reader.optLong("id").orElse(NOT_PERSISTED));
        return filter;
    }
}
