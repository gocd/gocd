/*
 * Copyright Thoughtworks, Inc.
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

import java.util.*;

@SuppressWarnings("unused") // Used by config repo migrations
public class AddEmptyTaskJoltTransformer implements ContextualTransform {

    @Override
    public Object transform(Object input, Map<String, Object> context) {
        List<?> pipelines = getFieldFromMap(input, "pipelines");
        pipelines.forEach(pipeline -> {
            List<?> stages = getFieldFromMap(pipeline, "stages");
            stages.forEach(stage -> {
                List<?> jobs = getFieldFromMap(stage, "jobs");
                jobs.forEach(job -> {

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> tasks = ((Map<String, List<Map<String, Object>>>) job).computeIfAbsent("tasks", k -> new ArrayList<>());

                    if (tasks.isEmpty()) {
                        Map<String, Object> echoTask = new LinkedHashMap<>();
                        echoTask.put("command", "echo");
                        echoTask.put("run_if", "passed");
                        echoTask.put("type", "exec");
                        echoTask.put("arguments", Collections.emptyList());
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

        @SuppressWarnings("unchecked")
        List<?> result = ((LinkedHashMap<String, List<?>>) input).get(field);

        return result == null ? Collections.emptyList() : result;
    }
}
