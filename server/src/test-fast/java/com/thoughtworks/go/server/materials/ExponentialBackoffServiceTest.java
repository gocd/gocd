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

package com.thoughtworks.go.server.materials;

import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;


public class ExponentialBackoffServiceTest {

    private MaterialUpdateCompletedTopic materialUpdateCompletedTopic;
    private ExponentialBackoffService exponentialBackoffService;

    @BeforeEach
    void setUp() {
        materialUpdateCompletedTopic = mock(MaterialUpdateCompletedTopic.class);
        exponentialBackoffService = new ExponentialBackoffService(materialUpdateCompletedTopic, new SystemEnvironment());
    }

    @Test
    void shouldBeMaterialUpdateCompleteMessageListener() {
        MaterialUpdateCompletedTopic completedTopic = mock(MaterialUpdateCompletedTopic.class);

        new ExponentialBackoffService(completedTopic, new SystemEnvironment());

        verify(completedTopic).addListener(any(ExponentialBackoffService.class));
    }

    @Nested
    class shouldBackoff {
        @Test
        void shouldBackoffUpdatesForMaterialWithMDUFailures() {
            GitMaterial material = new GitMaterial("http://github.com/example.git", "master");
            MaterialUpdateFailedMessage failedMessage = new MaterialUpdateFailedMessage(material, 1L, new RuntimeException());

            exponentialBackoffService.onMessage(failedMessage);

            assertThat(exponentialBackoffService.shouldBackOff(material).shouldBackOff()).isTrue();
        }

        @Test
        void shouldNotBackOffUpdatesForMaterialWithoutMDUFailures() {
            assertThat(exponentialBackoffService.shouldBackOff(new GitMaterial("http://github.com/ex.git")).shouldBackOff()).isFalse();
        }

        @Test
        void shouldEnsurePreviouslyFailedMaterialsAreNotBackedOffAfterSuccessfulMDU() {
            GitMaterial material = new GitMaterial("http://github.com/example.git", "master");
            MaterialUpdateFailedMessage failedMessage = new MaterialUpdateFailedMessage(material, 1L, new RuntimeException());

            exponentialBackoffService.onMessage(failedMessage);

            assertThat(exponentialBackoffService.shouldBackOff(material).shouldBackOff()).isTrue();

            exponentialBackoffService.onMessage(new MaterialUpdateSuccessfulMessage(material, 2L));

            assertThat(exponentialBackoffService.shouldBackOff(material).shouldBackOff()).isFalse();
        }
    }
}
