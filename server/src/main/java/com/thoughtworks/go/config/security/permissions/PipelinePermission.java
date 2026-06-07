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

import com.thoughtworks.go.config.security.users.Users;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface PipelinePermission {
    PipelinePermission NOONE = new NoOne();
    PipelinePermission EVERYONE = new Everyone();

    @NotNull Users pipelineOperators();

    @NotNull Optional<Users> stageOperators(String stageName);

    @EqualsAndHashCode
    @ToString
    class NoOne implements PipelinePermission {
        @Override
        public @NotNull Users pipelineOperators() {
            return Users.NOONE;
        }

        @Override
        public @NotNull Optional<Users> stageOperators(String stageName) {
            return Optional.of(Users.NOONE);
        }
    }

    @EqualsAndHashCode
    @ToString
    class Everyone implements PipelinePermission {
        @Override
        public @NotNull Users pipelineOperators() {
            return Users.EVERYONE;
        }

        @Override
        public @NotNull Optional<Users> stageOperators(String stageName) {
            return Optional.of(Users.EVERYONE);
        }
    }
}
