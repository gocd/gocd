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

package com.thoughtworks.go.server.dao;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.domain.User;
import com.thoughtworks.go.server.domain.user.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.sql.Timestamp;
import java.util.*;

import static com.thoughtworks.go.server.domain.user.DashboardFilter.DEFAULT_NAME;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class PipelineSelectionDaoIntegrationTest {
    @Autowired
    private UserSqlMapDao userSqlMapDao;

    @Autowired
    private PipelineSelectionDao pipelineSelectionDao;

    @Autowired
    private DatabaseAccessHelper dbHelper;

    @Before
    public void setup() throws Exception {
        dbHelper.onSetUp();
    }

    @After
    public void teardown() throws Exception {
        dbHelper.onTearDown();
    }

    @Test
    public void shouldSaveSelectedPipelinesWithoutUserId() {
        List<String> unSelected = Arrays.asList("pipeline1", "pipeline2");

        long id = pipelineSelectionDao.saveOrUpdate(blacklist(unSelected, null));
        PipelineSelections found = pipelineSelectionDao.findPipelineSelectionsById(id);

        final DashboardFilter filter = found.getViewFilters().filters().get(0);
        assertAllowsPipelines(filter, "pipeline3", "pipeline4");
        assertDeniesPipelines(filter, "pipeline1", "pipeline2");
        assertNull(found.getUserId());
    }

    @Test
    public void shouldSaveSelectedPipelinesWithUserId() {
        User user = createUser();

        List<String> unSelected = Arrays.asList("pipeline1", "pipeline2");
        long id = pipelineSelectionDao.saveOrUpdate(blacklist(unSelected, user.getId()));
        assertThat(pipelineSelectionDao.findPipelineSelectionsById(id).getUserId(), is(user.getId()));
    }

    @Test
    public void shouldSaveSelectedPipelinesWithBlacklistPreferenceFalse() {
        User user = createUser();

        List<String> selected = Arrays.asList("pipeline1", "pipeline2");
        final PipelineSelections whitelist = whitelist(selected, user.getId());
        long id = pipelineSelectionDao.saveOrUpdate(whitelist);
        assertEquals(whitelist, pipelineSelectionDao.findPipelineSelectionsById(id));
    }

    @Test
    public void shouldSaveSelectedPipelinesWithBlacklistPreferenceTrue() {
        User user = createUser();

        List<String> unSelected = Arrays.asList("pipeline1", "pipeline2");
        final PipelineSelections blacklist = blacklist(unSelected, user.getId());
        long id = pipelineSelectionDao.saveOrUpdate(blacklist);
        assertEquals(blacklist, pipelineSelectionDao.findPipelineSelectionsById(id));
    }

    @Test
    public void shouldFindSelectedPipelinesByUserId() {
        User user = createUser();

        List<String> unSelected = Arrays.asList("pipeline1", "pipeline2");
        long id = pipelineSelectionDao.saveOrUpdate(blacklist(unSelected, user.getId()));
        assertThat(pipelineSelectionDao.findPipelineSelectionsByUserId(user.getId()).getId(), is(id));
    }

    @Test
    public void shouldReturnNullAsPipelineSelectionsIfUserIdIsNull() {
        assertThat(pipelineSelectionDao.findPipelineSelectionsByUserId(null), is(nullValue()));
    }

    @Test
    public void shouldReturnNullAsPipelineSelectionsIfSelectionsExistForUser() {
        assertThat(pipelineSelectionDao.findPipelineSelectionsByUserId(10L), is(nullValue()));
    }

    @Test
    public void shouldReturnNullForInvalidIds() {
        assertThat(pipelineSelectionDao.findPipelineSelectionsById(null), is(nullValue()));
        assertThat(pipelineSelectionDao.findPipelineSelectionsById(""), is(nullValue()));
        assertThat(pipelineSelectionDao.findPipelineSelectionsById("123"), is(nullValue()));
        try {
            pipelineSelectionDao.findPipelineSelectionsById("foo");
            fail("should throw error");
        } catch (NumberFormatException e) {

        }
    }

    private User createUser() {
        userSqlMapDao.saveOrUpdate(new User("loser"));
        return userSqlMapDao.findUser("loser");
    }

    private void assertAllowsPipelines(DashboardFilter filter, String... pipelines) {
        for (String pipeline : pipelines) {
            assertTrue(filter.isPipelineVisible(new CaseInsensitiveString(pipeline)));
        }
    }

    private void assertDeniesPipelines(DashboardFilter filter, String... pipelines) {
        for (String pipeline : pipelines) {
            assertFalse(filter.isPipelineVisible(new CaseInsensitiveString(pipeline)));
        }
    }

    private PipelineSelections blacklist(List<String> pipelines, Long userId) {
        final List<CaseInsensitiveString> pipelineNames = CaseInsensitiveString.list(pipelines);
        Filters filters = new Filters(Collections.singletonList(new BlacklistFilter(DEFAULT_NAME, pipelineNames, new HashSet<>())));
        return new PipelineSelections(filters, new Timestamp(0), userId);
    }

    private PipelineSelections whitelist(List<String> pipelines, Long userId) {
        final List<CaseInsensitiveString> pipelineNames = CaseInsensitiveString.list(pipelines);
        Filters filters = new Filters(Collections.singletonList(new WhitelistFilter(DEFAULT_NAME, pipelineNames, new HashSet<>())));
        return new PipelineSelections(filters, new Timestamp(0), userId);
    }
}

