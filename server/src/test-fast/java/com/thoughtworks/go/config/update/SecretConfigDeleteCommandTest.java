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
package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.SecretConfig;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.domain.SecretConfigUsage;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class SecretConfigDeleteCommandTest {
    private BasicCruiseConfig cruiseConfig;

    @BeforeEach
    public void setUp() throws Exception {
        cruiseConfig = GoConfigMother.defaultCruiseConfig();
    }

    @Test
    public void shouldDeleteASecretConfig() {
        SecretConfig secretConfig = new SecretConfig("foo", "file-based");
        cruiseConfig.getSecretConfigs().add(secretConfig);

        SecretConfigDeleteCommand command = new SecretConfigDeleteCommand(null, secretConfig, Collections.emptySet(), null, null, null);
        command.update(cruiseConfig);

        assertThat(cruiseConfig.getSecretConfigs(), is(empty()));
    }

    @Test
    public void shouldRaiseExceptionInCaseSecretConfigDoesNotExist() {
        SecretConfig secretConfig = new SecretConfig("foo", "file-based");

        assertThat(cruiseConfig.getSecretConfigs(), is(empty()));
        SecretConfigDeleteCommand command = new SecretConfigDeleteCommand(null, secretConfig, Collections.emptySet(), null, null, new HttpLocalizedOperationResult());
        assertThrows(RecordNotFoundException.class, () -> command.update(cruiseConfig));

        assertThat(cruiseConfig.getSecretConfigs(), is(empty()));
    }

    @Test
    public void shouldNotValidateIfSecretConfigIsInUseByPipeline() {
        SecretConfig secretConfig = new SecretConfig("foo", "file-based");

        Set<SecretConfigUsage> usageInfo = new HashSet<>(Arrays.asList(
                new SecretConfigUsage("gocd", "P1", "S1", "J1", "template1")
        ));

        SecretConfigDeleteCommand command = new SecretConfigDeleteCommand(null, secretConfig, usageInfo, null, null, new HttpLocalizedOperationResult());

        GoConfigInvalidException goConfigInvalidException = assertThrows(GoConfigInvalidException.class, () -> {
            command.isValid(cruiseConfig);
        });

        assertThat(goConfigInvalidException.getMessage(), is("The secret config 'foo' is being referenced by pipeline(s): P1."));
    }

    @Test
    public void shouldValidateIfSecretConfigIsNotInUseByPipeline() {
        SecretConfig secretConfig = new SecretConfig("foo", "file-based");

        SecretConfigDeleteCommand command = new SecretConfigDeleteCommand(null, secretConfig, Collections.emptySet(), null, null, new HttpLocalizedOperationResult());
        assertThat(command.isValid(cruiseConfig), is(true));
    }
}
