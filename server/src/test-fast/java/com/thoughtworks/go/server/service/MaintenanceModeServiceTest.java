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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.server.domain.ServerMaintenanceMode;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TimeProvider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class MaintenanceModeServiceTest {
    private TimeProvider timeProvider;
    private MaintenanceModeService maintenanceModeService;
    private SystemEnvironment systemEnvironment;

    @BeforeEach
    void setUp() {
        System.setProperty(SystemEnvironment.START_IN_MAINTENANCE_MODE.propertyName(), "false");
        timeProvider = new TimeProvider();
        systemEnvironment = new SystemEnvironment();
        maintenanceModeService = new MaintenanceModeService(timeProvider, systemEnvironment);
    }

    @Test
    void shouldSetMaintenanceModeServiceToFalseOnStartup() {
        assertThat(maintenanceModeService.isMaintenanceMode()).isFalse();
    }

    @Test
    void shouldSetMaintenanceModeServiceToTrueWhenServerIsStartedInMaintenanceMode() {
        System.setProperty(SystemEnvironment.START_IN_MAINTENANCE_MODE.propertyName(), "true");
        maintenanceModeService = new MaintenanceModeService(timeProvider, systemEnvironment);

        assertThat(maintenanceModeService.isMaintenanceMode()).isTrue();
        assertThat(maintenanceModeService.updatedBy()).isEqualTo("GoCD");
    }

    @Test
    void shouldUpdateMaintenanceModeSettings() {
        assertThat(maintenanceModeService.isMaintenanceMode()).isFalse();
        maintenanceModeService.update(new ServerMaintenanceMode(true, "admin", new Date()));
        assertThat(maintenanceModeService.isMaintenanceMode()).isTrue();
    }

    @Test
    void shouldGetRunningMDUs() {
        assertThat(maintenanceModeService.getRunningMDUs()).hasSize(0);
        SvnMaterial svnMaterial = MaterialsMother.svnMaterial();

        maintenanceModeService.mduStartedForMaterial(svnMaterial);

        assertThat(maintenanceModeService.getRunningMDUs()).hasSize(1);
    }

    @Test
    void shouldRemoveMDUFromRunningMDUsListOnMDUFinish() {
        SvnMaterial svnMaterial = MaterialsMother.svnMaterial();

        maintenanceModeService.mduStartedForMaterial(svnMaterial);
        assertThat(maintenanceModeService.getRunningMDUs()).hasSize(1);

        maintenanceModeService.mduFinishedForMaterial(svnMaterial);
        assertThat(maintenanceModeService.getRunningMDUs()).hasSize(0);
    }

    @Test
    void shouldReturnUpdatedOnTimeWhenServerIsInMaintenanceMode() {
        DateTime dateTime = new DateTime(2019, 6, 18, 14, 30, 15, DateTimeZone.UTC);
        maintenanceModeService.update(new ServerMaintenanceMode(true, "admin", dateTime.toDate()));
        assertThat(maintenanceModeService.updatedOn()).isEqualTo("2019-06-18T14:30:15Z");
    }

    @Test
    void shouldThrowServerNotInMaintenanceModeExceptionForUpdatedOnWhenServerIsNotInMaintenanceMode() {
        assertThatCode(maintenanceModeService::updatedOn)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("GoCD server is not in maintenance mode!");
    }

    @Test
    void shouldReturnUsernameWhenServerIsInMaintenanceMode() {
        maintenanceModeService.update(new ServerMaintenanceMode(true, "admin", new Date()));
        assertThat(maintenanceModeService.updatedBy()).isEqualTo("admin");
    }

    @Test
    void shouldThrowServerNotInMaintenanceModeExceptionForUpdatedByWhenServerIsNotInMaintenanceMode() {
        assertThatCode(maintenanceModeService::updatedBy)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("GoCD server is not in maintenance mode!");
    }
}
