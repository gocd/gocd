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

import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.domain.AccessToken;
import com.thoughtworks.go.server.dao.AccessTokenDao;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.util.Clock;
import com.thoughtworks.go.util.TestingClock;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.sql.Timestamp;
import java.util.Map;

import static com.thoughtworks.go.helper.AccessTokenMother.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

class AccessTokenServiceTest {
    @Mock
    private AccessTokenDao accessTokenDao;
    @Mock
    private SecurityService securityService;
    private AccessTokenService accessTokenService;
    private HttpLocalizedOperationResult result;
    private String username;
    private String authConfigId;
    private Clock clock = new TestingClock();

    @BeforeEach
    void setUp() {
        initMocks(this);
        accessTokenService = new AccessTokenService(accessTokenDao, clock, securityService);
        result = new HttpLocalizedOperationResult();

        username = "Bob";
        authConfigId = "auth-config-1";
    }

    @Test
    void shouldMakeACallToSQLDaoForAccessTokenCreation() throws Exception {
        String longerDescription = RandomStringUtils.randomAlphanumeric(1024).toUpperCase();
        accessTokenService.create(longerDescription, username, authConfigId);

        assertThat(result.isSuccessful()).isTrue();

        verify(accessTokenDao, times(1)).saveOrUpdate(any(AccessToken.class));
    }

    @Test
    void shouldMakeACallToSQLDaoForFetchingAccessToken() {
        long tokenId = 42;
        when(securityService.isUserAdmin(new Username(username))).thenReturn(true);
        when(accessTokenDao.loadForAdminUser(42)).thenReturn(randomAccessTokenForUser(username));

        accessTokenService.find(tokenId, username);
        verify(accessTokenDao, times(1)).loadForAdminUser(42);
        verifyNoMoreInteractions(accessTokenDao);
    }

    @Test
    void shouldVerifyAccessTokenBelongsToUser() {
        long tokenId = 42;
        when(securityService.isUserAdmin(new Username(username))).thenReturn(true);
        when(accessTokenDao.loadForAdminUser(42)).thenReturn(randomAccessTokenForUser(username));

        accessTokenService.find(tokenId, username);
        verify(accessTokenDao, times(1)).loadForAdminUser(42);
        verifyNoMoreInteractions(accessTokenDao);
    }

    @Test
    void shouldVerifyAccessTokenBelongsToAdminUser() {
        long tokenId = 42;
        when(accessTokenDao.loadForAdminUser(42)).thenReturn(randomAccessTokenForUser(username));
        when(securityService.isUserAdmin(new Username("root"))).thenReturn(true);

        accessTokenService.find(tokenId, "root");
        verify(accessTokenDao, times(1)).loadForAdminUser(42);
        verifyNoMoreInteractions(accessTokenDao);
    }

    @Test
    void shouldBailIfUserDoesNotOwnToken() {
        long tokenId = 42;
        when(accessTokenDao.loadForAdminUser(anyLong())).thenReturn(null);
        when(securityService.isUserAdmin(new Username("hacker"))).thenReturn(false);

        assertThatCode(() -> accessTokenService.find(tokenId, "hacker"))
                .isInstanceOf(RecordNotFoundException.class)
                .hasMessageContaining(EntityType.AccessToken.notFoundMessage(tokenId));
        verify(accessTokenDao, never()).loadForAdminUser(anyLong());
    }

    @Test
    void shouldMakeACallToSQLDaoForFetchingAllAccessTokensBelongingToAUser() {
        accessTokenService.findAllTokensForUser(username, AccessTokenFilter.all);

        verify(accessTokenDao, times(1)).findAllTokensForUser(username, AccessTokenFilter.all);
        verifyNoMoreInteractions(accessTokenDao);
    }

    @Nested
    class OnTimer {

        private AccessToken accessToken;

        @BeforeEach
        void setUp() {
            accessToken = mock(AccessToken.class);
            when(accessToken.getId()).thenReturn(100L);

        }

        @Test
        void doNothingWhenSecurityIsDisabled() {
            when(securityService.isSecurityEnabled()).thenReturn(false);

            accessTokenService.onTimer();

            verifyZeroInteractions(accessTokenDao);
        }

        @Test
        void doNothingIfAccessTokenIdToLastUsedTimestampCacheIsEmpty() {
            when(securityService.isSecurityEnabled()).thenReturn(true);

            accessTokenService.onTimer();

            verifyZeroInteractions(accessTokenDao);
        }

        @Test
        void shouldUpdateDBForAccessTokenIdToLastUsedTimestampCacheWhenThereIsData() {
            when(securityService.isSecurityEnabled()).thenReturn(true);
            accessTokenService.updateLastUsedCacheWith(accessToken);

            accessTokenService.onTimer();

            final ArgumentCaptor<Map<Long, Timestamp>> argumentCaptor = ArgumentCaptor.forClass(Map.class);
            verify(accessTokenDao).updateLastUsedTime(argumentCaptor.capture());

            final Map<Long, Timestamp> argument = argumentCaptor.getValue();
            assertThat(argument)
                    .hasSize(1)
                    .containsEntry(100L, clock.currentTimestamp());
        }
    }

    @Nested
    class UpdateLastUsedCacheWith {
        @Test
        void shouldErrorOutWhenSecurityIsDisabled() {
            when(securityService.isSecurityEnabled()).thenReturn(false);

            assertThatCode(() -> accessTokenService.updateLastUsedCacheWith(mock(AccessToken.class)))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessage("Security is disable. Updating cache is not allowed.");
        }

        @Test
        void shouldUpdateCacheForAAccessToken() {
            final AccessToken accessToken = mock(AccessToken.class);
            when(accessToken.getId()).thenReturn(100L);
            when(securityService.isSecurityEnabled()).thenReturn(true);

            accessTokenService.updateLastUsedCacheWith(accessToken);
            accessTokenService.onTimer();

            final ArgumentCaptor<Map<Long, Timestamp>> argumentCaptor = ArgumentCaptor.forClass(Map.class);
            verify(accessTokenDao).updateLastUsedTime(argumentCaptor.capture());

            final Map<Long, Timestamp> argument = argumentCaptor.getValue();
            assertThat(argument)
                    .hasSize(1)
                    .containsEntry(100L, clock.currentTimestamp());
        }
    }

}
