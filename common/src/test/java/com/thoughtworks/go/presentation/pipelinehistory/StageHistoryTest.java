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
package com.thoughtworks.go.presentation.pipelinehistory;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class StageHistoryTest {
    private StageInstanceModels stageHistory;
    private static final String STAGE_FT = "ft";
    private static final String STAGE_UT = "ut";
    private static final String STAGE_RELEASE = "release";
    private static final Date EARLIEAR_DATE = new Date(1000000000);
    private static final Date DATE = new Date(2000000000);

    @Before
    public void setUp() throws Exception {
        stageHistory = new StageInstanceModels();
        stageHistory.add(new StageInstanceModel(STAGE_UT, "1", new JobHistory()));
        stageHistory.add(new StageInstanceModel(STAGE_FT, "1", new JobHistory()));
        stageHistory.add(new StageInstanceModel(STAGE_RELEASE, "1", new JobHistory()));
    }

    @Test
    public void hasStageTest() throws Exception {
        assertThat(stageHistory.hasStage(STAGE_FT), is(true));
        assertThat(stageHistory.hasStage("notExisting"), is(false));
        assertThat(stageHistory.hasStage(null), is(false));
        assertThat(stageHistory.hasStage(""), is(false));
    }

    @Test
    public void nextStageTest() throws Exception {
        assertThat(stageHistory.nextStageName(STAGE_UT), is(STAGE_FT));
        assertThat(stageHistory.nextStageName(STAGE_FT), is(STAGE_RELEASE));
        assertThat(stageHistory.nextStageName(STAGE_RELEASE), is(nullValue()));
        assertThat(stageHistory.nextStageName("notExisting"), is(nullValue()));
        assertThat(stageHistory.nextStageName(""), is(nullValue()));
        assertThat(stageHistory.nextStageName(null), is(nullValue()));
    }

    @Test
    public void shouldNotHaveDateForEmptyHistory() {
        assertThat(new StageInstanceModels().getScheduledDate(), is(nullValue()));
    }

    @Test
    public void shouldReturnEarliestDate() {
        StageInstanceModels history = new StageInstanceModels();
        history.add(new StageHistoryItemStub(EARLIEAR_DATE));
        history.add(new StageHistoryItemStub(DATE));
        assertThat(history.getScheduledDate(), Matchers.is(EARLIEAR_DATE));
    }

    @Test
    public void shouldNotCountNullStageIn() {
        StageInstanceModels history = new StageInstanceModels();
        history.add(new NullStageHistoryItemStub(EARLIEAR_DATE));
        history.add(new StageHistoryItemStub(DATE));
        assertThat(history.getScheduledDate(), Matchers.is(DATE));
    }

    private class StageHistoryItemStub extends StageInstanceModel {
        private final Date date;

        public StageHistoryItemStub(Date date) {
            this.date = date;
        }

        @Override
        public Date getScheduledDate() {
            return date;
        }
    }

    private class NullStageHistoryItemStub extends NullStageHistoryItem {
        private final Date date;

        public NullStageHistoryItemStub(Date date) {
            super("");
            this.date = date;
        }

        @Override
        public Date getScheduledDate() {
            return date;
        }
    }
}
