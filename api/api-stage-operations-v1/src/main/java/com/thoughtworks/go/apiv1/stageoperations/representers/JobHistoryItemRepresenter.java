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
package com.thoughtworks.go.apiv1.stageoperations.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.presentation.pipelinehistory.JobHistoryItem;

public class JobHistoryItemRepresenter {
    public static void toJSON(OutputWriter jsonWriter, JobHistoryItem jobHistoryItem) {
        jsonWriter.add("id", jobHistoryItem.getId());
        jsonWriter.add("name", jobHistoryItem.getName());
        if (jobHistoryItem.getState() != null) {
            jsonWriter.add("state", jobHistoryItem.getState().toString());
        }
        if (jobHistoryItem.getResult() != null) {
            jsonWriter.add("result", jobHistoryItem.getResult().toString());
        }
        if (jobHistoryItem.getScheduledDate() != null) {
            jsonWriter.add("scheduled_date", jobHistoryItem.getScheduledDate().getTime());
        }
    }
}
