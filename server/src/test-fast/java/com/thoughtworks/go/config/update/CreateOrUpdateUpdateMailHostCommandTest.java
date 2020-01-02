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
import com.thoughtworks.go.config.MailHost;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class CreateOrUpdateUpdateMailHostCommandTest {

    private BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();

    @Test
    void shouldAddMailHost() {
        assertThat(cruiseConfig.server().mailHost()).isNull();

        MailHost newMailHost = new MailHost();
        CreateOrUpdateUpdateMailHostCommand command = new CreateOrUpdateUpdateMailHostCommand(newMailHost);
        command.update(cruiseConfig);

        assertThat(cruiseConfig.server().mailHost()).isSameAs(newMailHost);
    }

    @Test
    void shouldValidateMailHost() {
        assertThat(cruiseConfig.server().mailHost()).isNull();

        MailHost newMailHost = new MailHost();
        CreateOrUpdateUpdateMailHostCommand command = new CreateOrUpdateUpdateMailHostCommand(newMailHost);
        command.update(cruiseConfig);

        assertThat(command.isValid(cruiseConfig)).isFalse();
        assertThat(newMailHost.errors())
                .hasSize(4)
                .containsEntry("hostname", Collections.singletonList("Hostname must not be blank."))
                .containsEntry("port", Collections.singletonList("Port must be a positive number."))
                .containsEntry("sender_email", Collections.singletonList("Sender email must not be blank."))
                .containsEntry("admin_email", Collections.singletonList("Admin email must not be blank."))
        ;

        assertThat(command.getPreprocessedEntityConfig()).isSameAs(newMailHost);
    }
}
