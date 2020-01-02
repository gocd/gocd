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

import com.opensymphony.oscache.base.Cache;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.persistence.ArtifactPlanRepository;
import com.thoughtworks.go.server.persistence.ResourceRepository;
import com.thoughtworks.go.server.service.ClusterProfilesService;
import com.thoughtworks.go.server.service.StubGoCache;
import com.thoughtworks.go.server.transaction.SqlMapClientTemplate;
import com.thoughtworks.go.server.transaction.TestTransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Map;

import static com.thoughtworks.go.util.DataStructureUtils.m;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

class JobInstanceSqlMapDaoTest {
    private JobInstanceSqlMapDao jobInstanceSqlMapDao;
    @Mock
    private Cache cache;
    @Mock
    private TransactionSynchronizationManager transactionSynchronizationManager;
    private GoCache goCache;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private EnvironmentVariableDao environmentVariableDao;
    @Mock
    private JobAgentMetadataDao jobAgentMetadataDao;
    @Mock
    private ResourceRepository resourceRepository;
    @Mock
    private ArtifactPlanRepository artifactPlanRepository;
    @Mock
    private SystemEnvironment systemEnvironment;
    @Mock
    private SqlMapClientTemplate template;
    @Mock
    private ClusterProfilesService clusterProfileService;

    @BeforeEach
    void setUp() {
        initMocks(this);
        goCache = new StubGoCache(new TestTransactionSynchronizationManager());
        jobInstanceSqlMapDao = new JobInstanceSqlMapDao(environmentVariableDao, goCache, transactionTemplate, null,
                cache, transactionSynchronizationManager, systemEnvironment, null, resourceRepository,
                artifactPlanRepository, clusterProfileService, jobAgentMetadataDao);
        jobInstanceSqlMapDao.setSqlMapClientTemplate(template);
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

        @Test
        public void shouldCacheJobIdentifierForGivenAttributes() {
            String pipelineName = "pipeline-name";
            String jobName = "job_name";
            String jobNameInDifferentCase = jobName.toUpperCase();
            String stageName = "stage-name";
            int pipelineCounter = 1;
            String stageCounter = "1";
            Map attrs = m(
                    "pipelineName", pipelineName,
                    "pipelineCounter", 1,
                    "stageName", stageName,
                    "stageCounter", 1,
                    "jobName", jobNameInDifferentCase);

            JobIdentifier jobIdentifier = new JobIdentifier(pipelineName, pipelineCounter, null, stageName, stageCounter, jobName);

            when(template.queryForObject("findJobId", attrs)).thenReturn(jobIdentifier);

            Assert.assertThat(jobInstanceSqlMapDao.findOriginalJobIdentifier(
                    new StageIdentifier(pipelineName, pipelineCounter, null, stageName, stageCounter),
                    jobNameInDifferentCase),
                    is(jobIdentifier));

            verify(template).queryForObject("findJobId", attrs);

            Assert.assertThat(jobInstanceSqlMapDao.findOriginalJobIdentifier(
                    new StageIdentifier(pipelineName, pipelineCounter, null, stageName, stageCounter),
                    jobNameInDifferentCase), not(sameInstance(jobIdentifier)));

            Assert.assertThat(jobInstanceSqlMapDao.findOriginalJobIdentifier(
                    new StageIdentifier(pipelineName, pipelineCounter, null, stageName, stageCounter),
                    jobName), is(jobIdentifier));

            verifyNoMoreInteractions(template);
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
    class CacheKeyForFindJobInstance {
        @Test
        void shouldGenerateCacheKey() {
            assertThat(jobInstanceSqlMapDao.cacheKeyForFindJobInstance("Foo", "Bar", "Baz", 1, 1))
                    .isEqualTo("com.thoughtworks.go.server.dao.JobInstanceSqlMapDao.$findJobInstance.$foo.$bar.$baz.$1.$1");
        }

        @Test
        void shouldGenerateADifferentMutexWhenPartOfPipelineIsInterchangedWithStageName() {
            assertThat(jobInstanceSqlMapDao.cacheKeyForFindJobInstance("Foo", "Bar_Jaz", "Baz", 1, 1))
                    .isNotEqualTo(jobInstanceSqlMapDao.cacheKeyForFindJobInstance("Foo_Bar", "Jaz", "Baz", 1, 1));
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
