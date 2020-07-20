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

package com.thoughtworks.go.plugin.access.configrepo;

import com.bazaarvoice.jolt.ContextualTransform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class AddEmptyTaskJoltTransformer implements ContextualTransform {
    private static final Logger LOG = LoggerFactory.getLogger(AddEmptyTaskJoltTransformer.class);

    @Override
    public Object transform(Object input, Map<String, Object> context) {
        List<?> pipelines = getFieldFromMap(input, "pipelines");
        if (pipelines == null) {
            return input;
        }
        pipelines.forEach((Object pipeline) -> {
            List<?> stages = getFieldFromMap(pipeline, "stages");
            stages.forEach((Object stage) -> {
                List<?> jobs = getFieldFromMap(stage, "jobs");
                jobs.forEach((Object job) -> {
                    LinkedHashMap<String, Object> echoTask = new LinkedHashMap<>();
                    echoTask.put("command", "echo");
                    echoTask.put("run_if", "passed");
                    echoTask.put("type", "exec");
                    echoTask.put("arguments", Collections.emptyList());

                    List<LinkedHashMap> tasks = ((LinkedHashMap<String, List<LinkedHashMap>>) job).get("tasks");
                    if (tasks == null) {
                        ((LinkedHashMap<String, List<LinkedHashMap>>) job).put("tasks", Arrays.asList(echoTask));
                    }

                    if (tasks != null && tasks.isEmpty()) {
                        tasks.add(echoTask);
                    }
                });
            });
        });

        return input;
    }

    private List<?> getFieldFromMap(Object input, String field) {
        if (input == null) {
            return Collections.emptyList();
        }

        List<?> result = ((LinkedHashMap<String, List<?>>) input).get(field);
        return result == null ? Collections.emptyList() : result;
    }
}
