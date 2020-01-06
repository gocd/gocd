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

package com.thoughtworks.go.apiv1.apiinfo.representers;

import com.thoughtworks.go.api.base.OutputListWriter;
import com.thoughtworks.go.spark.spring.RouteEntry;
import spark.utils.SparkUtils;

import java.util.List;
import java.util.stream.Collectors;

public class RouteEntryRepresenter {
    public static void toJSON(OutputListWriter writer, List<RouteEntry> routes) {
        routes.forEach(entry -> {
            writer.addChild(entryWriter -> {
                entryWriter
                    .add("method", entry.getHttpMethod().name())
                    .add("path", entry.getPath())
                    .add("version", entry.getAcceptedType())
                    .addChildList("path_params", getParams(entry));
            });
        });
    }

    public static List<String> getParams(RouteEntry entry) {
        return SparkUtils.convertRouteToList(entry.getPath())
            .stream()
            .filter(SparkUtils::isParam)
            .collect(Collectors.toList());
    }
}
