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
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Date;
import java.util.List;

@Service
public class AuthTokenService {
    private static final int DEFAULT_ITERATIONS = 4096;
    private static final int DESIRED_KEY_LENGTH = 256;

    private static final int SALT_LENGTH = 32;

    private AuthTokenDao authTokenDao;

    @Autowired
    public AuthTokenService(AuthTokenDao authTokenDao) {
        this.authTokenDao = authTokenDao;
    }

    public AuthToken create(String tokenName, String description, Username username, String authConfigId, HttpLocalizedOperationResult result) throws Exception {
        if (!new NameTypeValidator().isNameValid(tokenName)) {
            result.unprocessableEntity(NameTypeValidator.errorMessage("auth token", tokenName));
            return null;
        }

        if (description != null && description.length() > 1024) {
            result.unprocessableEntity("Validation Failed. Auth token description can not be longer than 1024 characters.");
            return null;
        }

        if (hasTokenWithNameForTheUser(tokenName, username)) {
            result.conflict(String.format("Validation Failed. Another auth token with name '%s' already exists.", tokenName));
            return null;
        }

        AuthToken tokenToCreate = getAuthTokenFor(tokenName, description, username, authConfigId);
        authTokenDao.save(tokenToCreate);
        return tokenToCreate;
    }

    private boolean hasTokenWithNameForTheUser(String tokenName, Username username) {
        return authTokenDao.findAuthToken(tokenName, username.getUsername().toString()) != null;
    }

    AuthToken getAuthTokenFor(String tokenName, String description, Username username, String authConfigId) throws Exception {
        AuthToken authToken = new AuthToken();

        authToken.setName(tokenName);
        authToken.setDescription(description);
        authToken.setAuthConfigId(authConfigId);
        authToken.setCreatedAt(new Date());

        String originalToken = generateSecureRandomString(16);
        String saltId = generateSecureRandomString(4);
        String saltValue = generateSalt();
        String hashedToken = digestToken(originalToken, saltValue);
        String finalTokenValue = String.format("%s%s", saltId, originalToken);

        authToken.setOriginalValue(finalTokenValue);
        authToken.setSaltId(saltId);
        authToken.setSaltValue(saltValue);
        authToken.setValue(hashedToken);

        authToken.setUsername(username.getUsername().toString());

        return authToken;
    }

    String digestToken(String originalToken, String salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        SecretKey key = factory.generateSecret(new PBEKeySpec(originalToken.toCharArray(), salt.getBytes(), DEFAULT_ITERATIONS, DESIRED_KEY_LENGTH));
        return Hex.encodeHexString(key.getEncoded());
    }

    private String generateSecureRandomString(int byteLength) {
        byte randomBytes[] = new byte[byteLength];
        new SecureRandom().nextBytes(randomBytes);
        return Hex.encodeHexString(randomBytes);
    }

    private static String generateSalt() throws Exception {
        return Base64.encodeBase64String(SecureRandom.getInstance("SHA1PRNG").generateSeed(SALT_LENGTH));
    }

    public AuthToken find(String tokenName, Username username) {
        return authTokenDao.findAuthToken(tokenName, username.getUsername().toString());
    }

    public List<AuthToken> findAllTokensForUser(Username username) {
        return authTokenDao.findAllTokensForUser(username.getUsername().toString());
    }
}
