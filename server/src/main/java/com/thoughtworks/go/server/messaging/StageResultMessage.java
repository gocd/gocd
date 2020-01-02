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
import com.thoughtworks.go.domain.StageEvent;
import com.thoughtworks.go.server.domain.Username;

public class StageResultMessage implements GoMessage {
    private final StageIdentifier stageIdentifier;
    private final StageEvent event;
    private Username cancelledBy;

    public StageResultMessage(StageIdentifier stageIdentifier, StageEvent event, Username cancelledBy) {
        this.stageIdentifier = stageIdentifier;
        this.event = event;
        this.cancelledBy = cancelledBy;
    }

    public StageIdentifier getStageIdentifier() {
        return stageIdentifier;
    }

    public StageEvent getEvent() {
        return event;
    }

    public Username getCancelledBy() {
        return cancelledBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        StageResultMessage that = (StageResultMessage) o;

        if (cancelledBy != null ? !cancelledBy.equals(that.cancelledBy) : that.cancelledBy != null) {
            return false;
        }
        if (event != that.event) {
            return false;
        }
        if (stageIdentifier != null ? !stageIdentifier.equals(that.stageIdentifier) : that.stageIdentifier != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = (stageIdentifier != null ? stageIdentifier.hashCode() : 0);
        result = 31 * result + (event != null ? event.hashCode() : 0);
        result = 31 * result + (cancelledBy != null ? cancelledBy.hashCode() : 0);
        return result;
    }
}
