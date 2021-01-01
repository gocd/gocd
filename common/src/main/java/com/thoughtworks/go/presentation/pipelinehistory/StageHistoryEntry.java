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
package com.thoughtworks.go.presentation.pipelinehistory;

import com.thoughtworks.go.domain.PersistentObject;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.domain.StageState;

/**
 * @understands stage details to allow rendering of an entry in stage-history page
 */
public class StageHistoryEntry extends PersistentObject {
    private StageIdentifier identifier;
    private StageState state;
    private double naturalOrder;
    private Integer rerunOfCounter;
    private String configVersion;

    public StageHistoryEntry(Stage firstStage, double naturalOrder, final Integer rerunOfCounter) {
        this();
        this.id = firstStage.getId();
        this.identifier = firstStage.getIdentifier();
        this.state = firstStage.getState();
        this.naturalOrder = naturalOrder;
        this.rerunOfCounter = rerunOfCounter;
        this.configVersion = firstStage.getConfigVersion();
    }

    //make ibatis happy - start
    public StageHistoryEntry() {
    }

    public void setIdentifier(StageIdentifier identifier) {
        this.identifier = identifier;
    }

    public void setState(StageState state) {
        this.state = state;
    }

    public void setNaturalOrder(double naturalOrder) {
        this.naturalOrder = naturalOrder;
    }

    public void setConfigVersion(String configVersion) {
        this.configVersion = configVersion;
    }

    //make ibatis happy - end

    public StageIdentifier getIdentifier() {
        return identifier;
    }

    public StageState getState() {
        return state;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        StageHistoryEntry that = (StageHistoryEntry) o;

        if (Double.compare(that.naturalOrder, naturalOrder) != 0) {
            return false;
        }
        if (identifier != null ? !identifier.equals(that.identifier) : that.identifier != null) {
            return false;
        }
        if (state != that.state) {
            return false;
        }
        if (rerunOfCounter != null ? !rerunOfCounter.equals(that.rerunOfCounter) : that.rerunOfCounter != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = identifier != null ? identifier.hashCode() : 0;
        result = 31 * result + (state != null ? state.hashCode() : 0);
        temp = naturalOrder != +0.0d ? Double.doubleToLongBits(naturalOrder) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override public String toString() {
        return "StageHistoryEntry{" +
                "identifier=" + identifier +
                ", state=" + state +
                ", naturalOrder=" + naturalOrder +
                ", rerunOfCounter=" + rerunOfCounter +
                '}';
    }

    public boolean isBisect() {
        return PipelineInstanceModel.isBisect(naturalOrder);
    }

    public boolean hasRerunJobs() {
        return rerunOfCounter != null;
    }

    @Deprecated
    public void setRerunOfCounter(Integer rerunOfCounter) {
        this.rerunOfCounter = rerunOfCounter;
    }

    public String getConfigVersion() {
        return configVersion;
    }
}
