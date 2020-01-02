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

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.server.dao.JobInstanceDao;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JobResolverServiceTest {
    private JobInstanceDao jobDao;
    private JobResolverService jobResolverService;

    @Before
    public void setUp() {
         jobDao = mock(JobInstanceDao.class);
         jobResolverService = new JobResolverService(jobDao);
    }

    @Test
    public void shouldLoadActualIdentifierForGivenJobIdentifier() {
        JobIdentifier oldId = new JobIdentifier("pipeline-name", 10, "label-10", "stage-name", "2", "build-name", 17l);
        JobIdentifier idFoundByDao = new JobIdentifier("pipeline-name", 5, "label-5", "stage-name", "1", "build-name", 12l);
        when(jobDao.findOriginalJobIdentifier(oldId.getStageIdentifier(), "build-name")).thenReturn(idFoundByDao);
        JobIdentifier actualId = jobResolverService.actualJobIdentifier(oldId);
        assertThat(actualId, is(idFoundByDao));
    }
}
