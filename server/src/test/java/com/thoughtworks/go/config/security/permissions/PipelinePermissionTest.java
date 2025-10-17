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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PipelinePermissionTest {

    PipelinePermission pipelinePermission;

    @BeforeEach
    void setUp() {

        StagePermission stage1 = new StagePermission("stage1", new AllowedUsers(Set.of("admin", "operator1", "operator2"), Collections.emptySet()));
        StagePermission stage2 = new StagePermission("stage2", new AllowedUsers(Set.of("admin", "operator1"), Collections.emptySet()));

        pipelinePermission = new PipelinePermission(List.of(stage1, stage2));
    }

    @Test
    void shouldReturnStage1PermissionsAsPipelinePermissions() {
        assertEquals(new AllowedUsers(Set.of("admin", "operator1", "operator2"), Collections.emptySet()), pipelinePermission.getPipelineOperators());
    }

    @Test
    void shouldReturnStagePermissionsProvidedStageName() {
        assertEquals(new AllowedUsers(Set.of("admin", "operator1", "operator2"), Collections.emptySet()), pipelinePermission.getStageOperators("stage1"));
        assertEquals(new AllowedUsers(Set.of("admin", "operator1"), Collections.emptySet()), pipelinePermission.getStageOperators("stage2"));
    }

    @Test
    void shouldNotFailWhenInvalidStageNameIsSpecified() {
        assertNull(pipelinePermission.getStageOperators("stageX"));
    }

    @Test
    void shouldReturnEveryonePermissionWhenPipelineIsNull() {
        assertEquals(EveryonePermission.INSTANCE, PipelinePermission.from(null, new AllowedUsers(Collections.emptySet(), Collections.emptySet())));
    }
}
