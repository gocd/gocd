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

package com.thoughtworks.go.config.security.permissions;

import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.config.security.users.Users;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public record StageDerivedPipelinePermission(List<StagePermission> permissions) implements PipelinePermission {
    @Override
    public @NotNull Users pipelineOperators() {
        return permissions.getFirst().stageOperators();
    }

    @Override
    public @NotNull Optional<Users> stageOperators(String stageName) {
        return permissions.stream()
            .filter(s -> s.stageName().equals(stageName)).findFirst()
            .map(StagePermission::stageOperators);
    }

    public static @NotNull PipelinePermission from(@NotNull PipelineConfig pipeline, @NotNull StageOperators stageOperators) {
        return new StageDerivedPipelinePermission(pipeline.stream()
            .map(stage -> new StagePermission(stage.name().toString(), stageOperators.calculateFor(stage)))
            .toList());
    }

    @FunctionalInterface
    public interface StageOperators {
        Users calculateFor(StageConfig stage);
    }
}
