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
package com.thoughtworks.go.server.messaging;

import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.domain.StageResult;
import com.thoughtworks.go.domain.StageState;
import com.thoughtworks.go.server.domain.Username;

public class StageStatusMessage implements GoMessage {
    private StageIdentifier stageIdentifier;
    private StageState stageState;
    private StageResult result;
    private final Username userName;

    public StageStatusMessage(StageIdentifier stageIdentifier, StageState stageState, StageResult result) {
        this(stageIdentifier, stageState, result, Username.BLANK);
    }

    //TODO: create a separate StageCancelledMessage
    public StageStatusMessage(StageIdentifier stageIdentifier, StageState stageState, StageResult result,
                              Username userName) {
        this.stageIdentifier = stageIdentifier;
        this.stageState = stageState;
        this.result = result;
        this.userName = userName;
    }

    @Override
    public String toString() {
        return "StageStatusMessage{" +
                "stageIdentifier=" + stageIdentifier +
                ", stageState=" + stageState +
                ", result=" + result +
                ", userName=" + userName +
                '}';
    }

    public boolean isStageCompleted() {
        return stageState.completed();
    }

    public StageIdentifier getStageIdentifier() {
        return stageIdentifier;
    }

    public StageResult getStageResult() {
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        StageStatusMessage that = (StageStatusMessage) o;

        if (result != that.result) {
            return false;
        }
        if (!stageIdentifier.equals(that.stageIdentifier)) {
            return false;
        }
        if (stageState != that.stageState) {
            return false;
        }
        if (userName != null ? !userName.equals(that.userName) : that.userName != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result1;
        result1 = stageIdentifier.hashCode();
        result1 = 31 * result1 + stageState.hashCode();
        result1 = 31 * result1 + result.hashCode();
        result1 = 31 * result1 + (userName != null ? userName.hashCode() : 0);
        return result1;
    }

    public Username username() {
        return userName;
    }
}
