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

import com.thoughtworks.go.domain.GoVersion;
import com.thoughtworks.go.domain.VersionInfo;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.dao.VersionInfoDao;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.SystemTimeClock;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ServerVersionInfoManagerTest {

    VersionInfoDao versionInfoDao;
    ServerVersionInfoBuilder builder;
    ServerVersionInfoManager manager;
    GoCache goCache;
    SystemEnvironment systemEnvironment;

    @Before
    public void setUp() {
        builder = mock(ServerVersionInfoBuilder.class);
        versionInfoDao = mock(VersionInfoDao.class);
        goCache = mock(GoCache.class);
        systemEnvironment = mock(SystemEnvironment.class);
        manager = new ServerVersionInfoManager(builder, versionInfoDao, new SystemTimeClock(), goCache, systemEnvironment);

        when(systemEnvironment.isGOUpdateCheckEnabled()).thenReturn(true);
    }

    @Test
    public void shouldUseServerVersionInfoBuilderToGetServerVersionInfo(){
        when(builder.getServerVersionInfo()).thenReturn(new VersionInfo());

        manager.initialize();

        verify(builder).getServerVersionInfo();
    }

    @Test
    public void shouldAddNewVersionInfoToCacheIfLatestVersionIsGreaterThanInstalledVersion(){
        GoVersion currentVersion = new GoVersion("1.2.3-1");
        GoVersion latestVersion = new GoVersion("2.3.4-2");
        VersionInfo versionInfo = new VersionInfo("go_server", currentVersion, latestVersion, null);

        when(builder.getServerVersionInfo()).thenReturn(versionInfo);

        manager.initialize();

        verify(builder).getServerVersionInfo();
        verify(goCache).put("GOUpdate", latestVersion.toString());
    }

    @Test
    public void shouldNotUpdateCacheIfLatestVersionIsLesserThanInstalledVersion(){
        GoVersion currentVersion = new GoVersion("4.7.3-1");
        GoVersion latestVersion = new GoVersion("2.3.4-2");
        VersionInfo versionInfo = new VersionInfo("go_server", currentVersion, latestVersion, null);

        when(builder.getServerVersionInfo()).thenReturn(versionInfo);

        manager.initialize();

        verify(builder).getServerVersionInfo();
        verify(goCache, never()).put(anyString(), anyString());
    }

    @Test
    public void shouldNotUpdateCacheIfServerVersionInfoIsUnAvailable(){
        when(builder.getServerVersionInfo()).thenReturn(null);

        manager.initialize();

        verify(builder).getServerVersionInfo();
        verify(goCache, never()).put(anyString(), anyString());
    }
    @Test
    public void shouldReturnVersionInfoIfServerLatestVersionNotUpdated(){
        Date yesterday = new Date(System.currentTimeMillis() - 24*60*60*1000);
        VersionInfo versionInfo = new VersionInfo("go_server", new GoVersion("1.2.3-1"), new GoVersion("2.3.4-2"), yesterday);

        when(builder.getServerVersionInfo()).thenReturn(versionInfo);

        manager.initialize();
        VersionInfo goVersionInfo = manager.versionInfoForUpdate();

        assertThat(goVersionInfo, is(versionInfo));
    }

    @Test
    public void shouldNotReturnVersionInfoIfLatestVersionUpdatedToday(){
        Date today = new Date();
        VersionInfo versionInfo = new VersionInfo("go_server", new GoVersion("1.2.3-1"), new GoVersion("2.3.4-2"), today);

        when(builder.getServerVersionInfo()).thenReturn(versionInfo);

        manager.initialize();
        VersionInfo serverVersionInfo = manager.versionInfoForUpdate();

        assertNull(serverVersionInfo);
    }

    @Test
    public void shouldNotReturnVersionInfoForDevelopementServer(){
        when(builder.getServerVersionInfo()).thenReturn(null);

        manager.initialize();

        assertNull(manager.versionInfoForUpdate());
    }


    @Test
    public void shouldNotGetVersionInfoIfLatestVersionIsBeingUpdated(){
        Date yesterday = new Date(System.currentTimeMillis() - 24*60*60*1000);
        VersionInfo versionInfo = new VersionInfo("go_server", new GoVersion("1.2.3-1"), new GoVersion("2.3.4-2"), yesterday);

        when(builder.getServerVersionInfo()).thenReturn(versionInfo);

        manager.initialize();
        VersionInfo serverVersionInfo1 = manager.versionInfoForUpdate();
        VersionInfo serverVersionInfo2 = manager.versionInfoForUpdate();

        assertNotNull(serverVersionInfo1);
        assertNull(serverVersionInfo2);
    }

    @Test
    public void shouldGetVersionInfoIflatestVersionIsBeingUpdatedForMoreThanHalfAnHour(){
        SystemTimeClock systemTimeClock = mock(SystemTimeClock.class);
        Date yesterday = new Date(System.currentTimeMillis() - 24*60*60*1000);
        DateTime halfAnHourFromNow = new DateTime(System.currentTimeMillis() - 35 * 60 * 1000);
        VersionInfo versionInfo = new VersionInfo("go_server", new GoVersion("1.2.3-1"), new GoVersion("2.3.4-2"), yesterday);

        when(builder.getServerVersionInfo()).thenReturn(versionInfo);
        when(systemTimeClock.currentDateTime()).thenReturn(halfAnHourFromNow);

        manager = new ServerVersionInfoManager(builder, versionInfoDao, systemTimeClock, goCache, systemEnvironment);
        manager.initialize();

        VersionInfo serverVersionInfo1 = manager.versionInfoForUpdate();
        VersionInfo serverVersionInfo2 = manager.versionInfoForUpdate();

        assertNotNull(serverVersionInfo1);
        assertNotNull(serverVersionInfo2);
    }

    @Test
    public void shouldBeAbleToUpdateTheLatestGOVersion(){
        SystemTimeClock systemTimeClock = mock(SystemTimeClock.class);
        VersionInfo versionInfo = new VersionInfo("go_server", new GoVersion("1.2.3-1"), new GoVersion("2.3.4-1"), null);
        Date now = new Date();

        when(builder.getServerVersionInfo()).thenReturn(versionInfo);
        when(systemTimeClock.currentTime()).thenReturn(now);

        manager = new ServerVersionInfoManager(builder, versionInfoDao, systemTimeClock, goCache, systemEnvironment);

        manager.initialize();
        VersionInfo info = manager.updateLatestVersion("15.0.1-123");

        assertThat(info.getLatestVersion().toString(), is("15.0.1-123"));
        assertThat(info.getLatestVersionUpdatedAt(), is(now));
        verify(versionInfoDao).saveOrUpdate(versionInfo);
    }

    @Test
    public void shouldUpdateCacheWithNewVersionIfLatestVersionIsGreaterThanInstalledVersion(){
        SystemTimeClock systemTimeClock = mock(SystemTimeClock.class);
        VersionInfo versionInfo = new VersionInfo("go_server", new GoVersion("1.2.3-1"), new GoVersion("1.2.3-1"), null);
        Date now = new Date();

        when(builder.getServerVersionInfo()).thenReturn(versionInfo);
        when(systemTimeClock.currentTime()).thenReturn(now);

        manager = new ServerVersionInfoManager(builder, versionInfoDao, systemTimeClock, goCache, systemEnvironment);
        manager.initialize();
        manager.updateLatestVersion("15.0.1-123");

        verify(goCache).put("GOUpdate", "15.0.1-123");
    }

    @Test
    public void shouldGetGoUpdateFromCache(){
        String newRelease = "15.0.1-123";
        GoCache goCache = mock(GoCache.class);

        when(goCache.get("GOUpdate")).thenReturn(newRelease);

        manager = new ServerVersionInfoManager(builder, versionInfoDao, new SystemTimeClock(), goCache, null);

        assertThat(manager.getGoUpdate(), is(newRelease));
    }

    @Test
    public void shouldBeTrueIfVersionCheckEnabledOnProductionServer(){
        SystemEnvironment systemEnvironment = mock(SystemEnvironment.class);

        when(builder.getServerVersionInfo()).thenReturn(new VersionInfo());
        when(systemEnvironment.isGOUpdateCheckEnabled()).thenReturn(true);

        manager = new ServerVersionInfoManager(builder, versionInfoDao, new SystemTimeClock(), goCache, systemEnvironment);
        manager.initialize();

        assertTrue(manager.isUpdateCheckEnabled());
    }

    @Test
    public void shouldBeFalseForADevelopmentServer(){
        SystemEnvironment systemEnvironment = mock(SystemEnvironment.class);

        when(builder.getServerVersionInfo()).thenReturn(null);
        when(systemEnvironment.isGOUpdateCheckEnabled()).thenReturn(true);

        manager = new ServerVersionInfoManager(builder, versionInfoDao, new SystemTimeClock(), goCache, systemEnvironment);
        manager.initialize();

        assertFalse(manager.isUpdateCheckEnabled());
    }

    @Test
    public void shouldBeFalseIfVersionCheckIsDisabled(){
        SystemEnvironment systemEnvironment = mock(SystemEnvironment.class);

        when(builder.getServerVersionInfo()).thenReturn(null);
        when(systemEnvironment.isGOUpdateCheckEnabled()).thenReturn(true);

        manager = new ServerVersionInfoManager(builder, versionInfoDao, new SystemTimeClock(), goCache, systemEnvironment);
        manager.initialize();

        assertFalse(manager.isUpdateCheckEnabled());
    }
}