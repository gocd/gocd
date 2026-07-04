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
package com.thoughtworks.go.server.messaging;

import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.domain.StageResult;
import com.thoughtworks.go.domain.StageState;
import com.thoughtworks.go.server.domain.Username;

import java.util.Objects;

public class StageStatusMessage implements GoMessage {
    private final StageIdentifier stageIdentifier;
    private final StageState stageState;
    private final StageResult result;
    private final Username userName;

    public StageStatusMessage(StageIdentifier stageIdentifier, StageState stageState, StageResult result) {
        this(stageIdentifier, stageState, result, Username.BLANK);
    }

    public StageStatusMessage(StageIdentifier stageIdentifier, StageState stageState, StageResult result,
                              Username userName) {
        this.stageIdentifier = stageIdentifier;
        this.stageState = stageState;
        this.result = result;
        this.userName = userName;
    }

    public StageIdentifier getStageIdentifier() {
        return stageIdentifier;
    }

    public StageResult getStageResult() {
        return result;
    }

    public Username username() {
        return userName;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StageStatusMessage that = (StageStatusMessage) o;
        return Objects.equals(stageIdentifier, that.stageIdentifier) &&
            stageState == that.stageState &&
            result == that.result &&
            Objects.equals(userName, that.userName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stageIdentifier, stageState, result, userName);
    }

    @Override
    public String toString() {
        return "StageStatusMessage{stageIdentifier=" + stageIdentifier + ", stageState=" + stageState + ", result=" + result + ", userName=" + userName + '}';
    }
}
