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
package com.thoughtworks.go.apiv1.elasticprofileoperation.representers;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.go.domain.ElasticProfileUsage;

import java.util.Collection;

public class ElasticProfileUsageRepresenter {
    private static Gson GSON = new GsonBuilder().
            setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    public static String toJSON(Collection<ElasticProfileUsage> jobsUsingElasticProfile) {
        if (jobsUsingElasticProfile == null) {
            return "[]";
        }

        return GSON.toJson(jobsUsingElasticProfile);
    }
}
