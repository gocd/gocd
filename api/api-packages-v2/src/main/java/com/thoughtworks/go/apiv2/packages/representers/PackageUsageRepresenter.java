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

package com.thoughtworks.go.apiv2.packages.representers;

import com.thoughtworks.go.api.base.OutputListWriter;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.util.Pair;

import java.util.List;

import static java.lang.String.format;

public class PackageUsageRepresenter {
    public static void toJSON(OutputWriter outputWriter, String packageId, List<Pair<PipelineConfig, PipelineConfigs>> packageUsageInPipelines) {
        outputWriter
                .addLinks(linksWriter -> {
                    linksWriter.addLink("self", format("%s/%s/usages", Routes.Packages.BASE, packageId));
                    linksWriter.addAbsoluteLink("doc", Routes.Packages.DOC);
                    linksWriter.addLink("find", format("%s%s", Routes.Packages.BASE, Routes.Packages.USAGES));
                })
                .addChildList("usages", usagesWriter -> packageUsageInPipelines.forEach(pair -> toJSON(usagesWriter, pair)));
    }

    private static void toJSON(OutputListWriter outputListWriter, Pair<PipelineConfig, PipelineConfigs> pair) {
        outputListWriter.addChild(usageWriter -> usageWriter.add("group", pair.last().getGroup())
                .add("pipeline", pair.first().getName().toString()));
    }
}
