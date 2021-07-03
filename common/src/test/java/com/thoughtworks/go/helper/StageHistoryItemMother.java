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
package com.thoughtworks.go.helper;

import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModel;
import com.thoughtworks.go.util.GoConstants;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class StageHistoryItemMother {

    public static StageInstanceModel custom(String stageName, boolean isAutoApproved) {
        StageInstanceModel stageHistoryItem = new StageInstanceModel();
        stageHistoryItem.setName(stageName);
        if (isAutoApproved) {
            stageHistoryItem.setApprovalType(GoConstants.APPROVAL_SUCCESS);
        } else {
            stageHistoryItem.setApprovalType(GoConstants.APPROVAL_MANUAL);
        }
        assertThat("Should create the correct stub", stageHistoryItem.isAutoApproved(), is(isAutoApproved));
        return stageHistoryItem;
    }
}