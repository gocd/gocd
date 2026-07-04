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
package com.thoughtworks.go.config.security;

import com.thoughtworks.go.config.security.permissions.PipelinePermission;
import com.thoughtworks.go.config.security.users.Users;
import org.jetbrains.annotations.NotNull;

public record Permissions(Users viewers, Users operators, Users admins, PipelinePermission pipelinePermission) {
    public static final Permissions NOONE = new Permissions(Users.NOONE, Users.NOONE, Users.NOONE, PipelinePermission.NOONE);
    public static final Permissions EVERYONE = new Permissions(Users.EVERYONE, Users.EVERYONE, Users.EVERYONE, PipelinePermission.EVERYONE);

    public @NotNull Users pipelineOperators() {
        return pipelinePermission.pipelineOperators();
    }

    public @NotNull Users stageOperators(String stageName) {
        return this.pipelinePermission.stageOperators(stageName).orElse(operators);
    }
}
