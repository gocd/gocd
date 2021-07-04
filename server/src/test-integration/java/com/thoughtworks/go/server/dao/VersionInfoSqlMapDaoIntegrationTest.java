/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Date;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class VersionInfoSqlMapDaoIntegrationTest {
    @Autowired
    private VersionInfoSqlMapDao versionInfoSqlMapDao;

    @Autowired
    private DatabaseAccessHelper dbHelper;

    @BeforeEach
    public void setup() throws Exception {
        dbHelper.onSetUp();
        versionInfoSqlMapDao.deleteAll();
    }

    @AfterEach
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
