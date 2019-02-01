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
import com.thoughtworks.go.domain.AccessToken;
import com.thoughtworks.go.server.dao.AccessTokenDao;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.exceptions.InvalidAccessTokenException;
import com.thoughtworks.go.server.exceptions.RevokedAccessTokenException;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

@Service
public class AccessTokenService {
    private static final int DEFAULT_ITERATIONS = 4096;
    private static final int DESIRED_KEY_LENGTH = 256;

    private static final int SALT_LENGTH = 32;

    private AccessTokenDao accessTokenDao;

    @Autowired
    public AccessTokenService(AccessTokenDao accessTokenDao) {
        this.accessTokenDao = accessTokenDao;
    }

    public AccessToken create(String tokenName, String description, Username username, String authConfigId, HttpLocalizedOperationResult result) throws Exception {
        if (!new NameTypeValidator().isNameValid(tokenName)) {
            result.unprocessableEntity(NameTypeValidator.errorMessage("access token", tokenName));
            return null;
        }

        if (description != null && description.length() > 1024) {
            result.unprocessableEntity("Validation Failed. Access token description can not be longer than 1024 characters.");
            return null;
        }

        if (hasTokenWithNameForTheUser(tokenName, username)) {
            result.conflict(String.format("Validation Failed. Another access token with name '%s' already exists.", tokenName));
            return null;
        }

        AccessToken tokenToCreate = generateAccessTokenFor(tokenName, description, username, authConfigId);
        accessTokenDao.saveOrUpdate(tokenToCreate);
        return tokenToCreate;
    }

    private boolean hasTokenWithNameForTheUser(String tokenName, Username username) {
        return accessTokenDao.findAccessToken(tokenName, username.getUsername().toString()) != null;
    }

    private AccessToken generateAccessTokenFor(String tokenName, String description, Username username, String authConfigId) throws Exception {
        AccessToken accessToken = new AccessToken();

        accessToken.setName(tokenName);
        accessToken.setDescription(description);
        accessToken.setAuthConfigId(authConfigId);
        accessToken.setCreatedAt(new Date());

        String originalToken = generateSecureRandomString(16);
        String saltId = generateSecureRandomString(4);
        String saltValue = generateSalt();
        String hashedToken = digestToken(originalToken, saltValue);
        String finalTokenValue = String.format("%s%s", saltId, originalToken);

        accessToken.setOriginalValue(finalTokenValue);
        accessToken.setSaltId(saltId);
        accessToken.setSaltValue(saltValue);
        accessToken.setValue(hashedToken);

        accessToken.setUsername(username.getUsername().toString());

        return accessToken;
    }

    String digestToken(String originalToken, String salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        SecretKey key = factory.generateSecret(new PBEKeySpec(originalToken.toCharArray(), salt.getBytes(), DEFAULT_ITERATIONS, DESIRED_KEY_LENGTH));
        return Hex.encodeHexString(key.getEncoded());
    }

    private String generateSecureRandomString(int byteLength) {
        byte[] randomBytes = new byte[byteLength];
        new SecureRandom().nextBytes(randomBytes);
        return Hex.encodeHexString(randomBytes);
    }

    private static String generateSalt() throws Exception {
        return Base64.encodeBase64String(SecureRandom.getInstance("SHA1PRNG").generateSeed(SALT_LENGTH));
    }

    public AccessToken find(String tokenName, String username) {
        return accessTokenDao.findAccessToken(tokenName, username);
    }

    public AccessToken findByAccessToken(String actualToken) throws Exception {
        if (actualToken.length() != 40) {
            throw new InvalidAccessTokenException();
        }

        String saltId = StringUtils.substring(actualToken, 0, 8);
        String originalToken = StringUtils.substring(actualToken, 8);

        AccessToken token = accessTokenDao.findAccessTokenBySaltId(saltId);
        if (token == null) {
            throw new InvalidAccessTokenException();
        }

        String saltValue = token.getSaltValue();
        String digestOfUserProvidedToken = digestToken(originalToken, saltValue);

        if (!token.getValue().equals(digestOfUserProvidedToken)) {
            throw new InvalidAccessTokenException();
        }

        if (token.isRevoked()) {
            throw new RevokedAccessTokenException(token.getRevokedAt());
        }

        return token;
    }

    public void revokeAccessToken(String name, String username, HttpLocalizedOperationResult result) {
        AccessToken fetchedAccessToken = accessTokenDao.findAccessToken(name, username);

        if (fetchedAccessToken == null) {
            result.unprocessableEntity(String.format("Validation Failed. Access Token with name '%s' for user '%s' does not exists.", name, username));
            return;
        }

        if (fetchedAccessToken.isRevoked()) {
            result.unprocessableEntity(String.format("Validation Failed. Access Token with name '%s' for user '%s' has already been revoked.", name, username));
            return;
        }

        fetchedAccessToken.setRevoked(true);
        fetchedAccessToken.setRevokedAt(new Timestamp(System.currentTimeMillis()));

        accessTokenDao.saveOrUpdate(fetchedAccessToken);
    }

    public List<AccessToken> findAllTokensForUser(Username username) {
        return accessTokenDao.findAllTokensForUser(username.getUsername().toString());
    }
}
