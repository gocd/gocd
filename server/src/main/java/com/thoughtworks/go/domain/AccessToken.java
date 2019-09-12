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
package com.thoughtworks.go.domain;

import com.thoughtworks.go.config.Validatable;
import com.thoughtworks.go.config.ValidationContext;
import com.thoughtworks.go.util.Clock;
import lombok.*;
import lombok.experimental.Accessors;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.persistence.Cacheable;
import javax.persistence.Entity;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.Timestamp;

import static org.apache.commons.lang3.StringUtils.isBlank;

@EqualsAndHashCode(doNotUseGetters = true, callSuper = true)
@ToString(callSuper = true)
@Getter
@Setter(AccessLevel.PROTECTED)
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.NONE)
@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class AccessToken extends HibernatePersistedObject implements Validatable {

    private static final int DEFAULT_ITERATIONS = 4096;
    private static final int DESIRED_KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 32;
    private static final String KEY_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static SecureRandom SECURE_RANDOM;
    private static final Logger ACCESS_TOKEN_LOGGER = LoggerFactory.getLogger(AccessToken.class);

    static {
        SECURE_RANDOM = new SecureRandom();
    }

    //this is the hashed token value
    private String value;
    private String description;
    private boolean revoked;
    private Timestamp revokedAt;
    private Timestamp createdAt;
    private Timestamp lastUsed;
    private String username;
    private String saltId;
    private String saltValue;
    private String authConfigId;
    private String revokeCause;
    private String revokedBy;
    private boolean deletedBecauseUserDeleted;

    @EqualsAndHashCode.Exclude
    private transient ConfigErrors errors = new ConfigErrors();

    public static AccessTokenWithDisplayValue create(String description,
                                                     String username,
                                                     String authConfigId,
                                                     Clock clock) {
        ACCESS_TOKEN_LOGGER.debug("[Access Token] Creating new access token for user '{}' description '{}' using auth config '{}'.", username, description, authConfigId);
        ACCESS_TOKEN_LOGGER.debug("[Access Token] Generating Secure Random String of length 16 bytes for original token.");
        String originalToken = generateSecureRandomString(16);
        ACCESS_TOKEN_LOGGER.debug("[Access Token] Generating Secure Random String of length 4 bytes for salt id.");
        String saltId = generateSecureRandomString(4);
        ACCESS_TOKEN_LOGGER.debug("[Access Token] Generating Secure Random String of length {} bytes for salt value.", SALT_LENGTH);
        String saltValue = generateSecureRandomString(SALT_LENGTH);
        ACCESS_TOKEN_LOGGER.debug("[Access Token] Generating hashed token from original token and salt value.");
        String hashedToken = digestToken(originalToken, saltValue);
        String finalTokenValue = String.format("%s%s", saltId, originalToken);
        ACCESS_TOKEN_LOGGER.debug("[Access Token] Done creating new access token for user '{}' description '{}' using auth config '{}'.", username, description, authConfigId);

        return (AccessTokenWithDisplayValue) new AccessTokenWithDisplayValue()
                .setDisplayValue(finalTokenValue)
                .setDescription(description)
                .setAuthConfigId(authConfigId)
                .setCreatedAt(clock.currentTimestamp())
                .setSaltId(saltId)
                .setSaltValue(saltValue)
                .setValue(hashedToken)
                .setUsername(username);
    }

    private static String generateSecureRandomString(int byteLength) {
        byte[] randomBytes = new byte[byteLength];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Hex.encodeHexString(randomBytes);
    }

    static String digestToken(String originalToken, String salt) {
        try {
            ACCESS_TOKEN_LOGGER.debug("Generating secret using algorithm: {} with spec: DEFAULT_ITERATIONS: {}, DESIRED_KEY_LENGTH: {}", KEY_ALGORITHM, DEFAULT_ITERATIONS, DESIRED_KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_ALGORITHM);
            SecretKey key = factory.generateSecret(new PBEKeySpec(originalToken.toCharArray(), salt.getBytes(), DEFAULT_ITERATIONS, DESIRED_KEY_LENGTH));
            return Hex.encodeHexString(key.getEncoded());
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    public AccessToken revoke(String revokedBy, String revokeCause, Timestamp revokedAt) {
        return setRevoked(true)
                .setRevokedBy(revokedBy)
                .setRevokeCause(revokeCause)
                .setRevokedAt(revokedAt);
    }

    public AccessToken revokeBecauseOfUserDelete(String deletedBy, Timestamp revokedAt) {
        return revoke(deletedBy, "Revoked because user was deleted by " + deletedBy, revokedAt)
                .setDeletedBecauseUserDeleted(true);
    }

    @Override
    public void validate(ValidationContext validationContext) {
        if (isBlank(description)) {
            errors.add("description", "must not be blank");
        }

        if (isBlank(username)) {
            errors.add("username", "must not be blank");
        }

        if (isBlank(authConfigId)) {
            errors.add("authConfigId", "must not be blank");
        }

        if (createdAt == null) {
            errors.add("createdAt", "must not be null");
        }
    }

    @Override
    public ConfigErrors errors() {
        return this.errors;
    }

    @Override
    public void addError(String fieldName, String message) {
        this.errors.add(fieldName, message);
    }

    public boolean isValidToken(String actualToken) {
        String originalToken = StringUtils.substring(actualToken, 8);
        String saltValue = getSaltValue();
        String digestOfUserProvidedToken = digestToken(originalToken, saltValue);

        // to avoid timing attacks. See https://security.stackexchange.com/a/83670
        return MessageDigest.isEqual(getValue().getBytes(StandardCharsets.UTF_8), digestOfUserProvidedToken.getBytes(StandardCharsets.UTF_8));
    }

    @Getter
    @Setter(AccessLevel.PROTECTED)
    @Accessors(chain = true)
    @NoArgsConstructor
    @AllArgsConstructor(access = AccessLevel.NONE)
    public static class AccessTokenWithDisplayValue extends AccessToken {
        private String displayValue;
    }
}
