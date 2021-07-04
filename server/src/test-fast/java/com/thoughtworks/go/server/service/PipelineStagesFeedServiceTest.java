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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.domain.feed.FeedEntries;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class PipelineStagesFeedServiceTest {
    private Username user;
    private FeedEntries expected;
    private StageService stageService;
    private FeedResolver pipelineStagesFeedResolver;
    private HttpLocalizedOperationResult operationResult;
    private SecurityService securityService;

    @BeforeEach
    public void setUp() throws Exception {
        user = new Username(new CaseInsensitiveString("barrow"));
        expected = new FeedEntries();
        stageService = mock(StageService.class);
        securityService = mock(SecurityService.class);
        operationResult = new HttpLocalizedOperationResult();
        pipelineStagesFeedResolver = new PipelineStagesFeedService(stageService, securityService).feedResolverFor("cruise");
    }

    @Test
    public void shouldGetFeedsForTheGivenPipeline() {
        when(securityService.hasViewPermissionForPipeline(user, "cruise")).thenReturn(true);
        when(stageService.feed("cruise", user)).thenReturn(expected);
        assertSame(expected, pipelineStagesFeedResolver.feed(user, operationResult));
    }

    @Test
    public void shouldGetFeedPageBeforeAGivenEntryId() {
        when(securityService.hasViewPermissionForPipeline(user, "cruise")).thenReturn(true);
        when(stageService.feedBefore(991, "cruise", user)).thenReturn(expected);
        assertSame(expected, pipelineStagesFeedResolver.feedBefore(user, 991, operationResult));
    }

    @Test
    public void shouldReturnUnauthorizedIfUserDoesNotHaveViewPermissionsForFeedBefore(){
        when(securityService.hasViewPermissionForPipeline(user, "cruise")).thenReturn(false);

        FeedEntries feedEntries = pipelineStagesFeedResolver.feedBefore(user, 100L, operationResult);

        assertThat(feedEntries, is(nullValue()));
        verifyThatUserIsUnauthorized();
        verifyNoMoreInteractions(stageService);
    }

    @Test
    public void shouldReturnUnauthorizedIfUserDoesNotHaveViewPermissionOnThePipeline() {
        when(securityService.hasViewPermissionForPipeline(user, "cruise")).thenReturn(false);

        FeedEntries feedEntries = pipelineStagesFeedResolver.feed(user, operationResult);

        assertThat(feedEntries, is(nullValue()));
        verifyThatUserIsUnauthorized();
        verifyNoMoreInteractions(stageService);
    }

    private void verifyThatUserIsUnauthorized() {
        assertThat(operationResult.isSuccessful(), is(false));
        assertThat(operationResult.httpCode(), is(403));//verify we are unauthorized

        operationResult.message();
    }
}
