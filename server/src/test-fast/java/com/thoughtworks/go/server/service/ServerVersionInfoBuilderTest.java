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

import com.thoughtworks.go.CurrentGoCDVersion;
import com.thoughtworks.go.domain.GoVersion;
import com.thoughtworks.go.domain.VersionInfo;
import com.thoughtworks.go.server.dao.VersionInfoDao;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class ServerVersionInfoBuilderTest {
    private VersionInfoDao versionInfoDao;
    private ServerVersionInfoBuilder builder;

    @Before
    public void setUp() {
        versionInfoDao = mock(VersionInfoDao.class);
        builder = spy(new ServerVersionInfoBuilder(versionInfoDao));
    }

    @Test
    public void shouldGetVersionInfoForGOServerIfExists(){
        VersionInfo goVersionInfo = new VersionInfo("go_server", new GoVersion("1.2.3-1"));
        when(versionInfoDao.findByComponentName("go_server")).thenReturn(goVersionInfo);

        VersionInfo versionInfo = builder.getServerVersionInfo();

        assertThat(versionInfo.getComponentName(), is(goVersionInfo.getComponentName()));
        assertThat(versionInfo.getInstalledVersion(), is(goVersionInfo.getInstalledVersion()));
    }

    @Test
    public void shouldCreateVersionInfoForGOServerIfDoesNotExist(){
        when(versionInfoDao.findByComponentName("go_server")).thenReturn(null);

        VersionInfo versionInfo = builder.getServerVersionInfo();

        verify(versionInfoDao).saveOrUpdate(isA(VersionInfo.class));
        assertThat(versionInfo.getComponentName(), is("go_server"));
        assertThat(versionInfo.getInstalledVersion().toString(), is(new GoVersion(CurrentGoCDVersion.getInstance().formatted()).toString()));
    }

    @Test
    public void shouldUpdateTheVersionInfoIfInstalledVersionHasChanged(){
        VersionInfo goVersionInfo = new VersionInfo("go_server", new GoVersion("1.2.3-1"));
        when(versionInfoDao.findByComponentName("go_server")).thenReturn(goVersionInfo);

        VersionInfo versionInfo = builder.getServerVersionInfo();

        verify(versionInfoDao).saveOrUpdate(isA(VersionInfo.class));
        assertThat(versionInfo.getComponentName(), is(goVersionInfo.getComponentName()));
        assertThat(versionInfo.getInstalledVersion().toString(), is(new GoVersion(CurrentGoCDVersion.getInstance().formatted()).toString()));
    }

    @Test
    public void shouldNotCreateAVersionInfoOnDevelopmentServer(){
        when(versionInfoDao.findByComponentName("go_server")).thenReturn(null);
        when(builder.getInstalledVersion()).thenReturn("N/A");
        VersionInfo versionInfo = builder.getServerVersionInfo();

        verify(versionInfoDao, never()).saveOrUpdate(isA(VersionInfo.class));
        assertNull(versionInfo);
    }

    @Test
    public void shouldNotUpdateTheVersionInfoIfUnableToParseTheInstalledVersion(){
        VersionInfo goVersionInfo = new VersionInfo("go_server", new GoVersion("1.2.3-1"));
        when(versionInfoDao.findByComponentName("go_server")).thenReturn(goVersionInfo);
        when(builder.getInstalledVersion()).thenReturn("N/A");

        VersionInfo versionInfo = builder.getServerVersionInfo();

        verify(versionInfoDao, never()).saveOrUpdate(isA(VersionInfo.class));
        assertThat(versionInfo.getComponentName(), is(goVersionInfo.getComponentName()));
        assertThat(versionInfo.getInstalledVersion(), is(goVersionInfo.getInstalledVersion()));
    }
}
