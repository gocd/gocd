/*
 * Copyright 2019 ThoughtWorks, Inc.
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
import com.thoughtworks.go.server.exceptions.ServerNotInMaintenanceModeException;
import com.thoughtworks.go.util.TimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class MaintenanceModeServiceTest {
    private TimeProvider timeProvider;
    private MaintenanceModeService maintenanceModeService;
    private String LOCAL_TIME_FORMAT = "dd MMM, yyyy 'at' HH:mm:ss 'Local Time'";
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat(LOCAL_TIME_FORMAT);

    @BeforeEach
    void setUp() {
        timeProvider = new TimeProvider();
        maintenanceModeService = new MaintenanceModeService(timeProvider);
    }

    @Test
    void shouldSetMaintenanceModeServiceToFalseOnStartup() {
        assertThat(maintenanceModeService.isMaintenanceMode()).isFalse();
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
        Date updatedOn = new Date();
        maintenanceModeService.update(new ServerMaintenanceMode(true, "admin", updatedOn));
        assertThat(maintenanceModeService.updatedOn()).isEqualTo(simpleDateFormat.format(updatedOn));
    }

    @Test
    void shouldThrowServerNotInMaintenanceModeExceptionForUpdatedOnWhenServerIsNotInMaintenanceMode() {
        assertThatCode(maintenanceModeService::updatedOn)
                .isInstanceOf(ServerNotInMaintenanceModeException.class)
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
                .isInstanceOf(ServerNotInMaintenanceModeException.class)
                .hasMessage("GoCD server is not in maintenance mode!");
    }
}
