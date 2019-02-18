/*
 * Copyright 2019 ThoughtWorks, Inc.
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
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.domain.AccessToken;
import com.thoughtworks.go.server.dao.AccessTokenDao;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.exceptions.InvalidAccessTokenException;
import com.thoughtworks.go.server.exceptions.RevokedAccessTokenException;
import com.thoughtworks.go.util.Clock;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccessTokenService {
    private final Clock timeProvider;

    private final AccessTokenDao accessTokenDao;
    private final SecurityService securityService;

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
            throw new RecordNotFoundException("Cannot locate access token with id " + id + ".");
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

    public AccessToken revokeAccessToken(long id, String username, String revokeCause) {
        AccessToken fetchedAccessToken = find(Long.parseLong(String.valueOf(id)), username);

        if (fetchedAccessToken.isRevoked()) {
            throw new ConflictException("Access token has already been revoked!");
        }

        fetchedAccessToken.revoke(username, revokeCause, timeProvider.currentTimestamp());

        accessTokenDao.saveOrUpdate(fetchedAccessToken);

        return fetchedAccessToken;
    }

    public List<AccessToken> findAllTokensForUser(String username) {
        return accessTokenDao.findAllTokensForUser(username);
    }

    public List<AccessToken> findAllTokensForAllUsers() {
        return accessTokenDao.findAllTokens();
    }
}
