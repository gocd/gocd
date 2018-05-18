/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import com.thoughtworks.go.domain.StageIdentifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JobInstanceSqlMapDaoTest {
    private JobInstanceSqlMapDao jobInstanceSqlMapDao;

    @BeforeEach
    void setUp() {
        jobInstanceSqlMapDao = new JobInstanceSqlMapDao(null, null, null, null,
                null, null, null, null, null,
                null, null, null);
    }

    @Nested
    class CacheKeyForJobInstanceWithTransitions {
        @Test
        void shouldGenerateCacheKey() {
            assertThat(jobInstanceSqlMapDao.cacheKeyForJobInstanceWithTransitions(1L))
                    .isEqualTo("com.thoughtworks.go.server.dao.JobInstanceSqlMapDao.$jobInstanceWithTransitionIds.$1");
        }
    }

    @Nested
    class CacheKeyForOriginalJobIdentifier {
        @Test
        void shouldGenerateCacheKey() {
            final StageIdentifier stageIdentifier = new StageIdentifier("Foo", 1, "Bar", "Baz", "1");
            assertThat(jobInstanceSqlMapDao.cacheKeyForOriginalJobIdentifier(stageIdentifier, "job"))
                    .isEqualTo("com.thoughtworks.go.server.dao.JobInstanceSqlMapDao.$originalJobIdentifier.$Foo.$bar.$1.$baz.$1.$job");
        }

        @Test
        void shouldGenerateADifferentMutexWhenPartOfPipelineIsInterchangedWithStageName() {
            final StageIdentifier stageIdentifierOne = new StageIdentifier("Foo_", 1, "Bar", "stage", "1");
            final StageIdentifier stageIdentifierTwo = new StageIdentifier("Foo", 1, "_Bar", "stage", "1");

            System.out.println(jobInstanceSqlMapDao.cacheKeyForOriginalJobIdentifier(stageIdentifierOne, "job"));
            System.out.println(jobInstanceSqlMapDao.cacheKeyForOriginalJobIdentifier(stageIdentifierTwo, "job"));

            assertThat(jobInstanceSqlMapDao.cacheKeyForOriginalJobIdentifier(stageIdentifierOne, "job"))
                    .isNotEqualTo(jobInstanceSqlMapDao.cacheKeyForOriginalJobIdentifier(stageIdentifierTwo, "job"));
        }
    }

    @Nested
    class CacheKeyForLatestCompletedJobs {
        @Test
        void shouldGenerateCacheKey() {
            assertThat(jobInstanceSqlMapDao.cacheKeyForLatestCompletedJobs("Foo", "Bar", "Baz", 1))
                    .isEqualTo("com.thoughtworks.go.server.dao.JobInstanceSqlMapDao.$latestCompletedJobs.$foo.$bar.$baz.$1");
        }

        @Test
        void shouldGenerateADifferentMutexWhenPartOfPipelineIsInterchangedWithStageName() {
            assertThat(jobInstanceSqlMapDao.cacheKeyForLatestCompletedJobs("Foo", "Bar_Jaz", "Baz", 1))
                    .isNotEqualTo(jobInstanceSqlMapDao.cacheKeyForLatestCompletedJobs("Foo_Bar", "Jaz", "Baz", 1));
        }
    }

    @Nested
    class CacheKeyForGetJobHistoryCount {
        @Test
        void shouldGenerateCacheKey() {
            assertThat(jobInstanceSqlMapDao.cacheKeyForGetJobHistoryCount("Foo", "Bar", "Baz"))
                    .isEqualTo("com.thoughtworks.go.server.dao.JobInstanceSqlMapDao.$getJobHistoryCount.$foo.$bar.$baz");
        }

        @Test
        void shouldGenerateADifferentMutexWhenPartOfPipelineIsInterchangedWithStageName() {
            assertThat(jobInstanceSqlMapDao.cacheKeyForGetJobHistoryCount("Foo", "Bar_Jaz", "Baz"))
                    .isNotEqualTo(jobInstanceSqlMapDao.cacheKeyForGetJobHistoryCount("Foo_Bar", "Jaz", "Baz"));
        }
    }

    @Nested
    class CacheKeyForFindJobHistoryPage {
        @Test
        void shouldGenerateCacheKey() {
            assertThat(jobInstanceSqlMapDao.cacheKeyForFindJobHistoryPage("Foo", "Bar", "Baz", 1, 1))
                    .isEqualTo("com.thoughtworks.go.server.dao.JobInstanceSqlMapDao.$findJobHistoryPage.$foo.$bar.$baz.$1.$1");
        }

        @Test
        void shouldGenerateADifferentMutexWhenPartOfPipelineIsInterchangedWithStageName() {
            assertThat(jobInstanceSqlMapDao.cacheKeyForFindJobHistoryPage("Foo", "Bar_Jaz", "Baz", 1, 1))
                    .isNotEqualTo(jobInstanceSqlMapDao.cacheKeyForFindJobHistoryPage("Foo_Bar", "Jaz", "Baz", 1, 1));
        }
    }

    @Nested
    class CacheKeyForJobPlan {
        @Test
        void shouldGenerateCacheKey() {
            assertThat(jobInstanceSqlMapDao.cacheKeyForJobPlan(1L))
                    .isEqualTo("com.thoughtworks.go.server.dao.JobInstanceSqlMapDao.$jobPlan.$1");
        }
    }

    @Nested
    class CacheKeyForActiveJob {
        @Test
        void shouldGenerateCacheKey() {
            assertThat(jobInstanceSqlMapDao.cacheKeyForActiveJob(1L))
                    .isEqualTo("com.thoughtworks.go.server.dao.JobInstanceSqlMapDao.$activeJob.$1");
        }
    }

    @Nested
    class CacheKeyForActiveJobIds {
        @Test
        void shouldGenerateCacheKey() {
            assertThat(jobInstanceSqlMapDao.cacheKeyForActiveJobIds())
                    .isEqualTo("com.thoughtworks.go.server.dao.JobInstanceSqlMapDao.$activeJobIds");
        }
    }

}
