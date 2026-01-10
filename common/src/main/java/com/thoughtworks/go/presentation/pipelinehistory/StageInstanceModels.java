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
package com.thoughtworks.go.presentation.pipelinehistory;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.StageContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.Comparator;
import java.util.Date;
import java.util.Optional;

import static com.thoughtworks.go.config.CaseInsensitiveString.str;

public class StageInstanceModels extends BaseCollection<StageInstanceModel> implements StageContainer {

    public @Nullable Date getScheduledDate() {
        return this.stream()
            .filter(StageInstanceModel::isScheduled)
            .min((o1, o2) -> Comparator.<Date>nullsFirst(Comparator.naturalOrder()).compare(o1.getScheduledDate(), o2.getScheduledDate()))
            .map(StageInstanceModel::getScheduledDate)
            .orElse(null);
    }

    @Override
    public boolean add(StageInstanceModel stageInstanceModel) {
        return super.add(stageInstanceModel);
    }

    @Override
    public boolean hasStage(String stageName) {
        return byName(stageName) != null;
    }

    @Override
    public String nextStageName(String stageName) {
        int index = indexOf(byName(stageName));
        if (index > -1 && index < size() - 1) {
            return get(index + 1).getName();
        }
        return null;
    }

    public StageInstanceModel byName(String name) {
        for (StageInstanceModel stage : this) {
            if (stage.getName().equals(name)) {
                return stage;
            }
        }
        return null;
    }

    public Boolean isLatestStageUnsuccessful() {
        return latestStage().hasUnsuccessfullyCompleted();
    }

    public @Nullable StageInstanceModel latestStage() {
        if (isEmpty()) {
            return null;
        }
        StageInstanceModel latest = getFirst();
        for (int i = 1; i < size(); i++) {
            StageInstanceModel current = get(i);
            if (!(current instanceof NullStageHistoryItem) && current.getScheduledDate().after(latest.getScheduledDate())) {
                latest = current;
            }
        }
        return latest;
    }

    public Boolean isLatestStageSuccessful() {
        return latestStage().hasPassed();
    }

    /**
     * Use {@link #add(StageInstanceModel)} instead
     */
    @TestOnly
    public void addStage(String name, JobHistory history) {
        add(new StageInstanceModel(name, "1", history));
    }

    public StageInstanceModels addFutureStage(String name, boolean isAutoApproved) {
        add(new NullStageHistoryItem(name, isAutoApproved));
        return this;
    }

    public boolean isLatestStage(StageInstanceModel stage) {
        return latestStage().equals(stage);
    }

    public void updateFutureStagesFrom(PipelineConfig pipelineConfig) {
        Optional<StageInstanceModel> lastStage = getLastStage();

        StageConfig nextStage;
        if (lastStage.isEmpty()) {
            nextStage = pipelineConfig.getFirstStageConfig();
        } else {
            nextStage = pipelineConfig.nextStageAfter(new CaseInsensitiveString(lastStage.get().getName()));
        }

        while (nextStage != null && !this.hasStage(str(nextStage.name()))) {
            this.addFutureStage(str(nextStage.name()), !nextStage.requiresApproval());
            nextStage = pipelineConfig.nextStageAfter(nextStage.name());
        }
    }

    private @NotNull Optional<StageInstanceModel> getLastStage() {
        return isEmpty() ? Optional.empty() : Optional.of(getLast());
    }
}
