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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.StageFinder;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.domain.testinfo.FailureDetails;
import com.thoughtworks.go.server.dao.sparql.ShineDao;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.thoughtworks.go.domain.testinfo.FailureDetails.*;
import static com.thoughtworks.go.util.ReflectionUtil.getField;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class FailureServiceTest {
    private FailureService failureService;
    private ShineDao shineDao;
    private SecurityService securityService;
    private Username username;
    private JobIdentifier jobIdentifier;
    private HttpLocalizedOperationResult result;
    private StageFinder stageFinder;

    @Before
    public void setUp() {
        shineDao = mock(ShineDao.class);
        securityService = mock(SecurityService.class);
        stageFinder = mock(StageFinder.class);
        failureService = new FailureService(securityService, shineDao);
        username = new Username(new CaseInsensitiveString("foo"));
        jobIdentifier = new JobIdentifier(new StageIdentifier("pipeline", 10, "stage", "5"), "job");
        result = new HttpLocalizedOperationResult();
    }

    @After
    public void tearDown() {
        verifyNoMoreInteractions(shineDao);
    }

    @Test
    public void shouldFetchFailureDetailsForGivenTestCase() {
        when(securityService.hasViewPermissionForPipeline(username, "pipeline")).thenReturn(true);
        when(shineDao.failureDetailsForTest(jobIdentifier, "suite_name", "test_name", result)).thenReturn(new FailureDetails("hi", "there"));
        assertThat(failureService.failureDetailsFor(jobIdentifier, "suite_name", "test_name", username, result), is(new FailureDetails("hi", "there")));
        verify(shineDao).failureDetailsForTest(jobIdentifier, "suite_name", "test_name", result);
    }

    @Test
    public void shouldFailWhenUserIsNotPermittedToFetchFailureDetails() {
        when(securityService.hasViewPermissionForPipeline(username, "pipeline")).thenReturn(false);
        assertThat(failureService.failureDetailsFor(jobIdentifier, "suite_name", "test_name", username, result), is(nullFailureDetails()));
        assertThat(result.httpCode(), is(403));
        assertThat(result.hasMessage(), is(true));
        assertThat(getField(result, "message"), is("User '" + "foo" + "' does not have view permission on pipeline '" + "pipeline" + "'"));
    }
}
