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
package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class CreateOrUpdateDefaultJobTimeoutCommandTest {

    private BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();

    @Test
    void shouldUpdateTheDefaultJobTimeout() throws Exception {
        String defaultJobTimeout = "10";
        CreateOrUpdateDefaultJobTimeoutCommand command = new CreateOrUpdateDefaultJobTimeoutCommand(defaultJobTimeout);

        assertThat(cruiseConfig.server().getJobTimeout()).isSameAs("0");

        command.update(cruiseConfig);

        assertThat(cruiseConfig.server().getJobTimeout()).isSameAs(defaultJobTimeout);
    }

    @Test
    void shouldReturnTrueWhenTheDefaultJobTimeoutIsValid() throws Exception {
        String defaultJobTimeout = "10";
        CreateOrUpdateDefaultJobTimeoutCommand command = new CreateOrUpdateDefaultJobTimeoutCommand(defaultJobTimeout);

        command.update(cruiseConfig);

        assertThat(command.isValid(cruiseConfig)).isTrue();
    }

    @Test
    void shouldThrowExceptionWhenTheDefaultJobTimeoutIsNotValid() throws Exception {
        String defaultJobTimeout = "foo";
        CreateOrUpdateDefaultJobTimeoutCommand command = new CreateOrUpdateDefaultJobTimeoutCommand(defaultJobTimeout);

        command.update(cruiseConfig);

        assertThatCode(() -> command.isValid(cruiseConfig))
                .isInstanceOf(GoConfigInvalidException.class)
                .hasMessage("Timeout should be a valid number as it represents number of minutes");
    }
}
