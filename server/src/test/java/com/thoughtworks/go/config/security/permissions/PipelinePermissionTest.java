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

import com.thoughtworks.go.config.security.users.AllowedUsers;
import com.thoughtworks.go.config.security.users.Users;
import com.thoughtworks.go.helper.PipelineConfigMother;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.thoughtworks.go.config.CaseInsensitiveString.cis;
import static org.assertj.core.api.Assertions.assertThat;

class PipelinePermissionTest {

    PipelinePermission pipelinePermission;

    @BeforeEach
    void setUp() {

        StagePermission stage1 = new StagePermission("stage1", new AllowedUsers(Set.of("admin", "operator1", "operator2"), Collections.emptySet()));
        StagePermission stage2 = new StagePermission("stage2", new AllowedUsers(Set.of("admin", "operator1"), Collections.emptySet()));

        pipelinePermission = new StageDerivedPipelinePermission(List.of(stage1, stage2));
    }

    @Test
    void shouldReturnStage1PermissionsAsPipelinePermissions() {
        assertThat(pipelinePermission.pipelineOperators())
            .isEqualTo(new AllowedUsers(Set.of("admin", "operator1", "operator2"), Collections.emptySet()));
    }

    @Test
    void shouldReturnStagePermissionsProvidedStageName() {
        assertThat(pipelinePermission.stageOperators("stage1"))
            .contains(new AllowedUsers(Set.of("admin", "operator1", "operator2"), Collections.emptySet()));
        assertThat(pipelinePermission.stageOperators("stage2"))
            .contains(new AllowedUsers(Set.of("admin", "operator1"), Collections.emptySet()));
    }

    @Test
    void shouldNotFailWhenInvalidStageNameIsSpecified() {
        assertThat(pipelinePermission.stageOperators("stageX")).isEmpty();
    }

    @Test
    void shouldConstructFromStages() {
        assertThat(StageDerivedPipelinePermission.from(PipelineConfigMother.createPipelineConfigWithStages("p", "s1", "s2"), s ->
            cis("s1").equals(s.name()) ? Users.EVERYONE : Users.NOONE ))
            .isEqualTo(new StageDerivedPipelinePermission(List.of(
                new StagePermission("s1", Users.EVERYONE),
                new StagePermission("s2", Users.NOONE)
            )));
    }
}
