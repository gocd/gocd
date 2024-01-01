/*
 * Copyright 2024 Thoughtworks, Inc.
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

package com.thoughtworks.go.config.validation;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.jupiter.api.Test;
import uk.org.webcompere.systemstubs.properties.SystemProperties;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServerIdImmutabilityValidatorTest {

    private final ServerIdImmutabilityValidator validator = new ServerIdImmutabilityValidator();
    private final CruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();

    @Test
    public void shouldBeValidWhenServerIdIsNullAndChanged() {
        validator.validate(cruiseConfig);
        validator.validate(cruiseConfig);
    }

    @Test
    public void shouldBeValidWhenServerIdIsUpdatedFromNull() {
        validator.validate(cruiseConfig);
        cruiseConfig.server().ensureServerIdExists();
        validator.validate(cruiseConfig);
        validator.validate(cruiseConfig);
    }

    @Test
    public void shouldBeInvalidWhenServerIdIsUpdatedFromSomethingToSomethingElse() {
        cruiseConfig.server().ensureServerIdExists();
        validator.validate(cruiseConfig);

        CruiseConfig newCruiseConfig = GoConfigMother.defaultCruiseConfig();
        newCruiseConfig.server().ensureServerIdExists();
        assertThatThrownBy(() -> validator.validate(newCruiseConfig))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("The value of 'serverId' uniquely identifies a Go server instance. This field cannot be modified");
    }

    @Test
    public void shouldBeValidWhenServerIdIsUpdatedFromSomethingToSomethingElseAndImmutabilityDisabled() throws Exception {
        cruiseConfig.server().ensureServerIdExists();
        validator.validate(cruiseConfig);

        new SystemProperties().set(SystemEnvironment.ENFORCE_SERVER_IMMUTABILITY, SystemEnvironment.CONFIGURATION_NO).execute(() -> {
            CruiseConfig newCruiseConfig = GoConfigMother.defaultCruiseConfig();
            newCruiseConfig.server().ensureServerIdExists();
            validator.validate(newCruiseConfig);
        });
    }

}