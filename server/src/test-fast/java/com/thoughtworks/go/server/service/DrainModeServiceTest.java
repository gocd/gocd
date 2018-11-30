/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.server.domain.ServerDrainMode;
import com.thoughtworks.go.util.TimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class DrainModeServiceTest {
    private TimeProvider timeProvider;
    private DrainModeService drainModeService;

    @BeforeEach
    void setUp() {
        timeProvider = new TimeProvider();
        drainModeService = new DrainModeService(timeProvider);
    }

    @Test
    void shouldSetDrainModeServiceToFalseOnStartup() {
        assertThat(drainModeService.isDrainMode()).isFalse();
    }

    @Test
    void shouldUpdateDrainModeSettings() {
        assertThat(drainModeService.isDrainMode()).isFalse();
        drainModeService.update(new ServerDrainMode(true, "admin", new Date()));
        assertThat(drainModeService.isDrainMode()).isTrue();
    }

    @Test
    void shouldGetRunningMDUs() {
        assertThat(drainModeService.getRunningMDUs()).hasSize(0);
        SvnMaterial svnMaterial = MaterialsMother.svnMaterial();

        drainModeService.mduStartedForMaterial(svnMaterial);

        assertThat(drainModeService.getRunningMDUs()).hasSize(1);
    }

    @Test
    void shouldRemoveMDUFromRunningMDUsListOnMDUFinish() {
        SvnMaterial svnMaterial = MaterialsMother.svnMaterial();

        drainModeService.mduStartedForMaterial(svnMaterial);
        assertThat(drainModeService.getRunningMDUs()).hasSize(1);

        drainModeService.mduFinishedForMaterial(svnMaterial);
        assertThat(drainModeService.getRunningMDUs()).hasSize(0);
    }
}
