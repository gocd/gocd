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

import com.thoughtworks.go.config.validation.NameTypeValidator;
import com.thoughtworks.go.domain.AuthToken;
import com.thoughtworks.go.server.dao.AuthTokenDao;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Date;

@Service
public class AuthTokenService {
    private AuthTokenDao authTokenDao;

    @Autowired
    public AuthTokenService(AuthTokenDao authTokenDao) {
        this.authTokenDao = authTokenDao;
    }

    public AuthToken create(String tokenName, String description, HttpLocalizedOperationResult result) throws Exception {
        if (!new NameTypeValidator().isNameValid(tokenName)) {
            result.unprocessableEntity(NameTypeValidator.errorMessage("auth token", tokenName));
            return null;
        }

        if (description != null && description.length() > 1024) {
            result.unprocessableEntity("Validation Failed. Auth token description can not be longer than 1024 characters.");
            return null;
        }

        if (authTokenDao.findAuthToken(tokenName) != null) {
            result.conflict("Validation Failed. Another auth token with name '" + tokenName + "' already exists.");
            return null;
        }

        AuthToken tokenToCreate = getAuthTokenFor(tokenName, description);
        authTokenDao.saveOrUpdate(tokenToCreate);
        return tokenToCreate;
    }

    private AuthToken getAuthTokenFor(String tokenName, String description) throws Exception {
        AuthToken authToken = new AuthToken();

        authToken.setName(tokenName);
        authToken.setDescription(description);
        authToken.setCreatedAt(new Date());

        String originalToken = generateNewToken();
        //original value needs to be there for rendering back to the user.
        authToken.setOriginalValue(originalToken);
        //hashed value needs to be there to persist it in the DB.
        authToken.setValue(hashToken(originalToken));

        //redundant, if not specified as the object will set it to false.But for good practice (and clarity), lets set it explicitly.
        authToken.setLastUsed(null);
        authToken.setRevoked(false);

        return authToken;
    }

    public String hashToken(String originalToken) throws Exception {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] digestedToken = md5.digest(originalToken.getBytes(StandardCharsets.UTF_8));
        return new String(Hex.encodeHex(digestedToken));
    }

    private String generateNewToken() {
        byte randomBytes[] = new byte[24];
        new SecureRandom().nextBytes(randomBytes);
        return Hex.encodeHexString(randomBytes);
    }

    public AuthToken find(String tokenName) {
        return authTokenDao.findAuthToken(tokenName);
    }
}
