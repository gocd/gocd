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
package com.thoughtworks.go.apiv1.jobinstance.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.PaginationRepresenter;
import com.thoughtworks.go.domain.JobInstances;
import com.thoughtworks.go.server.util.Pagination;

public class JobInstancesRepresenter {
    public static void toJSON(OutputWriter outputWriter, JobInstances jobInstances, Pagination pagination) {
        outputWriter.addChild("pagination", writer -> PaginationRepresenter.toJSON(writer, pagination))
                .addChildList("jobs", jobsWriter -> jobInstances.forEach(jobInstance -> jobsWriter.addChild(jobWriter -> JobInstanceRepresenter.toJSON(jobWriter, jobInstance))));
    }
}
