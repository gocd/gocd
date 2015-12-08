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

package com.thoughtworks.go.server.dao;

import com.thoughtworks.go.domain.GoVersion;
import com.thoughtworks.go.domain.VersionInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Date;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class VersionInfoSqlMapDaoIntegrationTest {
    @Autowired
    private VersionInfoSqlMapDao versionInfoSqlMapDao;

    @Autowired
    private DatabaseAccessHelper dbHelper;

    @Before
    public void setup() throws Exception {
        dbHelper.onSetUp();
        versionInfoSqlMapDao.deleteAll();
    }

    @After
    public void teardown() throws Exception {
        dbHelper.onTearDown();
        versionInfoSqlMapDao.deleteAll();
    }

    @Test
    public void shouldSaveVersionInfo(){
        GoVersion installedVersion = new GoVersion("14.1.0-123");
        GoVersion latestVersion = new GoVersion("15.1.0-876");
        Date now = new Date();
        VersionInfo versionInfo = new VersionInfo("GOServer", installedVersion, latestVersion, now);

        versionInfoSqlMapDao.saveOrUpdate(versionInfo);

        VersionInfo info = versionInfoSqlMapDao.findByComponentName(versionInfo.getComponentName());
        assertThat(info.getInstalledVersion().toString(), is(installedVersion.toString()));
        assertThat(info.getLatestVersion().toString(), is(latestVersion.toString()));
        assertThat(info.getLatestVersionUpdatedAt().compareTo(now), is(0));
    }

    @Test
    public void shouldUpdateVersionInfo(){
        GoVersion installedVersion = new GoVersion("14.1.0-123");
        GoVersion latestVersion = new GoVersion("15.1.0-876");
        Date now = new Date();
        VersionInfo versionInfo = new VersionInfo("GOServer", installedVersion, latestVersion, now);

        versionInfoSqlMapDao.saveOrUpdate(versionInfo);

        versionInfo.setLatestVersion(new GoVersion("15.2.1-111"));
        versionInfoSqlMapDao.saveOrUpdate(versionInfo);

        VersionInfo info = versionInfoSqlMapDao.findByComponentName(versionInfo.getComponentName());

        assertThat(info.getLatestVersion().toString(), is("15.2.1-111"));
    }
}