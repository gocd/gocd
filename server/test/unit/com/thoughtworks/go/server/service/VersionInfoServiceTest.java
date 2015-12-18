/*
 * Copyright 2015 ThoughtWorks, Inc.
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

import com.thoughtworks.go.domain.VersionInfo;
import com.thoughtworks.go.domain.exception.VersionFormatException;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class VersionInfoServiceTest {

    @Test
    public void shouldFetchVersionInfoWhichNeedsUpdate() {
        ServerVersionInfoManager versionInfoManager = mock(ServerVersionInfoManager.class);
        VersionInfo versionInfo = mock(VersionInfo.class);

        when(versionInfoManager.versionInfoForUpdate()).thenReturn(versionInfo);

        VersionInfoService versionInfoService = new VersionInfoService(versionInfoManager);
        VersionInfo info = versionInfoService.getStaleVersionInfo();

        assertThat(info, is(versionInfo));
    }

    @Test
    public void shouldUpdateLatestVersion(){
        ServerVersionInfoManager versionInfoManager = mock(ServerVersionInfoManager.class);
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        new VersionInfoService(versionInfoManager).updateServerLatestVersion("16.1.0-123", result);

        verify(versionInfoManager).updateLatestVersion("16.1.0-123");
        assertTrue(result.isSuccessful());
    }

    @Test
    public void shouldAddErrorToResultIfVersionFormatIsInValid(){
        ServerVersionInfoManager versionInfoManager = mock(ServerVersionInfoManager.class);
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        when(versionInfoManager.updateLatestVersion("16.1.0-123")).thenThrow(new VersionFormatException("fail"));

        new VersionInfoService(versionInfoManager).updateServerLatestVersion("16.1.0-123", result);

        assertFalse(result.isSuccessful());
    }

    @Test
    public void shouldGetGoUpdate(){
        String newRelease = "15.1.0-123";
        ServerVersionInfoManager manager = mock(ServerVersionInfoManager.class);

        when(manager.getGoUpdate()).thenReturn(newRelease);

        String goUpdate = new VersionInfoService(manager).getGoUpdate();

        assertThat(goUpdate, is(goUpdate));
    }

    @Test
    public void shouldReturnVersionCheckEnabled(){
        ServerVersionInfoManager manager = mock(ServerVersionInfoManager.class);

        when(manager.isUpdateCheckEnabled()).thenReturn(true);

        boolean isVersionCheckEnabled = new VersionInfoService(manager).isGOUpdateCheckEnabled();

        assertTrue(isVersionCheckEnabled);
    }
}
