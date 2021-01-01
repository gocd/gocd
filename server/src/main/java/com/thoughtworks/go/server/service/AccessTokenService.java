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

import com.thoughtworks.go.config.exceptions.ConflictException;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.domain.AccessToken;
import com.thoughtworks.go.server.dao.AccessTokenDao;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.exceptions.InvalidAccessTokenException;
import com.thoughtworks.go.server.exceptions.RevokedAccessTokenException;
import com.thoughtworks.go.util.Clock;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class AccessTokenService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccessTokenService.class);
    private static final Logger ACCESS_TOKEN_LOGGER = LoggerFactory.getLogger(AccessToken.class);
    private final Clock timeProvider;

    private final AccessTokenDao accessTokenDao;
    private final SecurityService securityService;
    private final ConcurrentMap<Long, Timestamp> accessTokenIdToLastUsedTimestampCache = new ConcurrentHashMap<>();

    @Autowired
    public AccessTokenService(AccessTokenDao accessTokenDao, Clock clock, SecurityService securityService) {
        this.accessTokenDao = accessTokenDao;
        this.timeProvider = clock;
        this.securityService = securityService;
    }

    public AccessToken.AccessTokenWithDisplayValue create(String description, String username, String authConfigId) {
        AccessToken.AccessTokenWithDisplayValue tokenToCreate = AccessToken.create(description, username, authConfigId, timeProvider);
        tokenToCreate.validate(null);
        if (tokenToCreate.errors().isEmpty()) {
            accessTokenDao.saveOrUpdate(tokenToCreate);
        }
        return tokenToCreate;
    }

    public AccessToken find(long id, String username) {
        AccessToken token;
        if (securityService.isUserAdmin(new Username(username))) {
            token = accessTokenDao.loadForAdminUser(id);
        } else {
            token = accessTokenDao.loadNotDeletedTokenForUser(id, username);
        }

        if (token == null) {
            throw new RecordNotFoundException(EntityType.AccessToken, id);
        }

        return token;
    }

    // load access token within GoCD
    private AccessToken findAccessTokenForGoCD(long id) {
        AccessToken token;
        token = accessTokenDao.loadForAdminUser(id);

        if (token == null) {
            throw new RecordNotFoundException(EntityType.AccessToken, id);
        }

        return token;
    }

    public AccessToken findByAccessToken(String actualToken) {
        if (actualToken.length() != 40) {
            throw new InvalidAccessTokenException();
        }

        String saltId = StringUtils.substring(actualToken, 0, 8);

        AccessToken token = accessTokenDao.findAccessTokenBySaltId(saltId);
        if (token == null) {
            throw new InvalidAccessTokenException();
        }

        boolean isValid = token.isValidToken(actualToken);

        if (!isValid) {
            throw new InvalidAccessTokenException();
        }

        if (token.isRevoked()) {
            throw new RevokedAccessTokenException(token.getRevokedAt());
        }

        return token;
    }

    // for APIs
    public AccessToken revokeAccessToken(long id, String username, String revokeCause) {
        AccessToken fetchedAccessToken = find(Long.parseLong(String.valueOf(id)), username);
        return revoke(fetchedAccessToken, username, revokeCause);
    }

    // for GoCD
    public AccessToken revokeAccessTokenByGoCD(long id, String revokeCause) {
        AccessToken fetchedAccessToken = findAccessTokenForGoCD(Long.parseLong(String.valueOf(id)));
        return revoke(fetchedAccessToken, "GoCD", revokeCause);
    }

    private AccessToken revoke(AccessToken fetchedAccessToken, String username, String revokeCause) {
        if (fetchedAccessToken.isRevoked()) {
            throw new ConflictException("Access token has already been revoked!");
        }

        ACCESS_TOKEN_LOGGER.debug("[Access Token] Revoking access token with id: '{}' for user '{}' with revoked cause '{}'.", fetchedAccessToken.getId(), username, revokeCause);
        fetchedAccessToken.revoke(username, revokeCause, timeProvider.currentTimestamp());
        accessTokenDao.saveOrUpdate(fetchedAccessToken);

        ACCESS_TOKEN_LOGGER.debug("[Access Token] Done revoking access token with id: '{}' for user '{}' with revoked cause '{}'.", fetchedAccessToken.getId(), username, revokeCause);

        return fetchedAccessToken;
    }

    public List<AccessToken> findAllTokensForUser(String username, AccessTokenFilter filter) {
        return accessTokenDao.findAllTokensForUser(username, filter);
    }

    public List<AccessToken> findAllTokensForAllUsers(AccessTokenFilter filter) {
        return accessTokenDao.findAllTokens(filter);
    }

    public void updateLastUsedCacheWith(AccessToken accessToken) {
        if (!securityService.isSecurityEnabled()) {
            throw new UnsupportedOperationException("Security is disable. Updating cache is not allowed.");
        }

        synchronized (accessTokenIdToLastUsedTimestampCache) {
            accessTokenIdToLastUsedTimestampCache.put(accessToken.getId(), timeProvider.currentTimestamp());
        }
    }

    public void onTimer() {
        if (!securityService.isSecurityEnabled()) {
            LOGGER.debug("Security is disable. Not updating `LastUsedTime` in DB.");
            return;
        }

        Map<Long, Timestamp> dataInCache = cloneAndClearCache();

        if (dataInCache.isEmpty()) {
            LOGGER.debug("Access token cache for `LastUsedTime` is empty.");
            return;
        }

        accessTokenDao.updateLastUsedTime(dataInCache);
    }

    private Map<Long, Timestamp> cloneAndClearCache() {
        synchronized (accessTokenIdToLastUsedTimestampCache) {
            Map<Long, Timestamp> dataInCache = new HashMap<>(accessTokenIdToLastUsedTimestampCache);
            accessTokenIdToLastUsedTimestampCache.clear();
            return dataInCache;
        }
    }
}
