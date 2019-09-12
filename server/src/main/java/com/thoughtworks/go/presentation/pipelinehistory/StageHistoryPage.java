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
package com.thoughtworks.go.presentation.pipelinehistory;

import com.thoughtworks.go.server.util.Pagination;

import java.util.List;

/**
 * @understands a single page of the stagehistory
 */
public class StageHistoryPage {
    private List<StageHistoryEntry> stages;
    private Pagination pagination;
    private final StageHistoryEntry immediateChronologicallyForwardStageHistoryEntry;

    public StageHistoryPage(List<StageHistoryEntry> stages, Pagination pagination, StageHistoryEntry immediateChronologicallyForwardStageHistoryEntry) {
        this.stages = stages;
        this.pagination = pagination;
        this.immediateChronologicallyForwardStageHistoryEntry = immediateChronologicallyForwardStageHistoryEntry;
    }

    public List<StageHistoryEntry> getStages() {
        return stages;
    }

    public Pagination getPagination() {
        return pagination;
    }

    public int currentPage(){
        return pagination.getCurrentPage();
    }

    public StageHistoryEntry getImmediateChronologicallyForwardStageHistoryEntry() {
        return immediateChronologicallyForwardStageHistoryEntry;
    }
}
