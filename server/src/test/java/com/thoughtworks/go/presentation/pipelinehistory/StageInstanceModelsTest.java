/*
 * Copyright Thoughtworks, Inc.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class StageInstanceModelsTest {
    private static final String STAGE_FT = "ft";
    private static final String STAGE_UT = "ut";
    private static final String STAGE_RELEASE = "release";
    private static final Date EARLIER_DATE = new Date(1000000000);
    private static final Date DATE = new Date(2000000000);

    private StageInstanceModels models;

    @Nested
    class NextStage {
        @BeforeEach
        public void setUp() {
            models = new StageInstanceModels();
            models.add(new StageInstanceModel(STAGE_UT, "1", new JobHistory()));
            models.add(new StageInstanceModel(STAGE_FT, "1", new JobHistory()));
            models.add(new StageInstanceModel(STAGE_RELEASE, "1", new JobHistory()));
        }

        @Test
        public void hasStageTest() {
            assertThat(models.hasStage(STAGE_FT)).isTrue();
            assertThat(models.hasStage("notExisting")).isFalse();
            assertThat(models.hasStage(null)).isFalse();
            assertThat(models.hasStage("")).isFalse();
        }

        @Test
        public void nextStageTest() {
            assertThat(models.nextStageName(STAGE_UT)).isEqualTo(STAGE_FT);
            assertThat(models.nextStageName(STAGE_FT)).isEqualTo(STAGE_RELEASE);
            assertThat(models.nextStageName(STAGE_RELEASE)).isNull();
            assertThat(models.nextStageName("notExisting")).isNull();
            assertThat(models.nextStageName("")).isNull();
            assertThat(models.nextStageName(null)).isNull();
        }
    }

    @Nested
    class ScheduledDate {
        @Test
        public void shouldNotHaveDateForEmptyHistory() {
            assertThat(new StageInstanceModels().getScheduledDate()).isNull();
        }

        @Test
        public void shouldNotHaveDateIfNoDatesDetermined() {
            StageInstanceModels history = new StageInstanceModels();
            history.add(new StageHistoryItemStub(null));
            history.add(new StageHistoryItemStub(null));
            assertThat(history.getScheduledDate()).isNull();
        }

        @Test
        public void shouldReturnEarliestDate() {
            StageInstanceModels history = new StageInstanceModels();
            history.add(new StageHistoryItemStub(EARLIER_DATE));
            history.add(new StageHistoryItemStub(DATE));
            assertThat(history.getScheduledDate()).isEqualTo(EARLIER_DATE);
        }

        @Test
        public void shouldNotCountNonScheduledStage() {
            StageInstanceModels history = new StageInstanceModels();
            history.add(new NullStageHistoryItemStub(EARLIER_DATE));
            history.add(new StageHistoryItemStub(DATE));
            assertThat(history.getScheduledDate()).isEqualTo(DATE);
        }

        @Test
        public void shouldConsiderScheduledWithNullDateEarlier() {
            StageInstanceModels history = new StageInstanceModels();
            history.add(new StageHistoryItemStub(null));
            history.add(new StageHistoryItemStub(DATE));
            assertThat(history.getScheduledDate()).isNull();
        }

        private static class StageHistoryItemStub extends StageInstanceModel {
            private final Date date;

            public StageHistoryItemStub(Date date) {
                this.date = date;
            }

            @Override
            public Date getScheduledDate() {
                return date;
            }
        }

        private static class NullStageHistoryItemStub extends NullStageHistoryItem {
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
}
