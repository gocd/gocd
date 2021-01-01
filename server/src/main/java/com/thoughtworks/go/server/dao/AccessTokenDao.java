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
package com.thoughtworks.go.server.dao;

import com.thoughtworks.go.domain.AccessToken;
import com.thoughtworks.go.server.service.AccessTokenFilter;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface AccessTokenDao {
    void saveOrUpdate(AccessToken accessToken);

    AccessToken loadForAdminUser(long id);

    AccessToken loadNotDeletedTokenForUser(long id, String username);

    List<AccessToken> findAllTokens(AccessTokenFilter filter);

    List<AccessToken> findAllTokensForUser(String username, AccessTokenFilter filter);

    AccessToken findAccessTokenBySaltId(String saltId);

    void revokeTokensBecauseOfUserDelete(Collection<String> usernames, String byWhom);

    void updateLastUsedTime(Map<Long, Timestamp> accessTokenIdToLastUsedTimestamp);
}
