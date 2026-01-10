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
package com.thoughtworks.go.domain;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

import static java.util.stream.Collectors.toCollection;


public class Stages extends BaseCollection<Stage> implements StageContainer {

    private static final Comparator<Stage> STAGE_COMPARATOR = Comparator.comparingInt(Stage::getOrderId);

    public Stages() {
        super();
    }

    public Stages(Collection<Stage> stages) {
        this.addAll(stages);
    }

    public Stages(Stage... stage) {
        super(Arrays.asList(stage));
    }

    @Override
    public boolean hasStage(String stageName) {
        return stream().anyMatch(s -> s.getName().equalsIgnoreCase(stageName));
    }

    @Override
    public @Nullable String nextStageName(String stageName) {
        this.sort(STAGE_COMPARATOR); // This mutates the collection; bit strange but kept for backward compatibility
        return stream()
            .dropWhile(s -> !stageName.equals(s.getName()))
            .skip(1)
            .findFirst()
            .map(Stage::getName)
            .orElse(null);
    }

    public @NotNull Stage byName(String name) {
        return stream()
            .filter(s -> name.equals(s.getName()))
            .findFirst()
            .orElseGet(() -> new NullStage(name, new JobInstances()));
    }

    public @NotNull Stage byId(long stageId) {
        return stream()
            .filter(stage -> stage.getId() == stageId)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Could not load stage with id " + stageId));
    }

    public boolean isAnyStageActive() {
        return stream().anyMatch(s -> s.stageState().isActive());
    }

    public @NotNull Stage byCounter(int counter) {
        return stream()
            .filter(stage -> stage.getCounter() == counter)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Cannot find a stage with counter '" + counter + "'. Actual stages are: " + this));
    }

    public Stages latestStagesInRunOrder() {
        return stream().filter(Stage::isLatestRun)
            .sorted(Comparator.comparingInt(Stage::getOrderId))
            .collect(toCollection(Stages::new));
    }
}
