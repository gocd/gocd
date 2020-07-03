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

package com.thoughtworks.go.config.security.permissions;

import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.security.users.Users;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Getter
@Setter
@Accessors(chain = true)
@EqualsAndHashCode
@ToString
public class PipelinePermission extends ArrayList<StagePermission> {

    public PipelinePermission(List<StagePermission> permissions) {
        super(permissions);
    }

    public PipelinePermission() {
        super();
    }

    public Users getPipelineOperators() {
        return this.get(0).getStageOperators();
    }

    public Users getStageOperators(String stageName) {
        return this.stream().filter(s -> s.getStageName().equals(stageName)).findFirst().map(StagePermission::getStageOperators).orElse(null);
    }

    public static PipelinePermission from(PipelineConfig pipeline, Users operators) {
        List<StagePermission> permissions = pipeline.stream().map(stage -> new StagePermission(stage.name().toString(), operators)).collect(Collectors.toList());
        return new PipelinePermission(permissions);
    }
}
