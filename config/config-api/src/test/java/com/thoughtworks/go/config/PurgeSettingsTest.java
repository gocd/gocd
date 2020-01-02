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
package com.thoughtworks.go.config;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class PurgeSettingsTest {

    @Test
    public void validate_shouldAddErrorsIfThePurgeStartIsBiggerThanPurgeUpto() {
        PurgeSettings purgeSettings = createPurgeSettings(20.1, 20.05);

        assertThat(purgeSettings.errors().isEmpty(), is(true));

        purgeSettings.validate(null);

        assertThat(purgeSettings.errors().isEmpty(), is(false));
        assertThat(purgeSettings.errors().on(ServerConfig.PURGE_START), is("Error in artifact cleanup values. The trigger value (20.1GB) should be less than the goal (20.05GB)"));
    }

    @Test
    public void validate_shouldAddErrorsIfThePurgeStartIsNotSpecifiedButPurgeUptoIs() {
        PurgeSettings purgeSettings = createPurgeSettings(null, 20.05);

        assertThat(purgeSettings.errors().isEmpty(), is(true));

        purgeSettings.validate(null);

        assertThat(purgeSettings.errors().isEmpty(), is(false));
        assertThat(purgeSettings.errors().on(ServerConfig.PURGE_START), is("Error in artifact cleanup values. The trigger value has to be specified when a goal is set"));
    }

    @Test
    public void validate_shouldAddErrorsIfThePurgeStartIs0SpecifiedButPurgeUptoIs() {
        PurgeSettings purgeSettings = createPurgeSettings(0.0, 20.05);

        assertThat(purgeSettings.errors().isEmpty(), is(true));

        purgeSettings.validate(null);

        assertThat(purgeSettings.errors().isEmpty(), is(false));
        assertThat(purgeSettings.errors().on(ServerConfig.PURGE_START), is("Error in artifact cleanup values. The trigger value has to be specified when a goal is set"));
    }

    @Test
    public void validate_shouldNotAddErrorsIfThePurgeStartIsSmallerThanPurgeUpto() {
        PurgeSettings purgeSettings = createPurgeSettings(20.0, 20.05);

        assertThat(purgeSettings.errors().isEmpty(), is(true));

        purgeSettings.validate(null);

        assertThat(purgeSettings.errors().isEmpty(), is(true));
    }

    @Test
    public void validate_shouldNotAddErrorsIfThePurgeStartAndPurgeUptoAreBothNotSet() {
        PurgeSettings purgeSettings = createPurgeSettings(null, null);

        assertThat(purgeSettings.errors().isEmpty(), is(true));

        purgeSettings.validate(null);

        assertThat(purgeSettings.errors().isEmpty(), is(true));
    }

    @Test
    public void validate_shouldAddErrorIfThePurgeUptoIsNull() {
        PurgeSettings purgeSettings = createPurgeSettings(10.0, null);

        assertThat(purgeSettings.errors().isEmpty(), is(true));

        purgeSettings.validate(null);

        assertThat(purgeSettings.errors().isEmpty(), is(false));
        assertThat(purgeSettings.errors().on(ServerConfig.PURGE_UPTO), is("Error in artifact cleanup values. Please specify goal value"));
    }

    @Test
    public void validate_shouldNotAddErrorIfThePurgeUptoAndPurgeStartIsZero() {
        PurgeSettings purgeSettings = createPurgeSettings(0.0, 0.0);

        assertThat(purgeSettings.errors().isEmpty(), is(true));

        purgeSettings.validate(null);

        assertThat(purgeSettings.errors().isEmpty(), is(false));
        assertThat(purgeSettings.errors().on(ServerConfig.PURGE_START), is("Error in artifact cleanup values. The trigger value has to be specified when a goal is set"));
    }

    private PurgeSettings createPurgeSettings(Double purgeStart, Double purgeUpto) {
        PurgeSettings purgeSettings = new PurgeSettings();
        purgeSettings.setPurgeStart(new PurgeStart(purgeStart));
        purgeSettings.setPurgeUpto(new PurgeUpto(purgeUpto));
        return purgeSettings;
    }
}
