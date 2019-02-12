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

package com.thoughtworks.go.domain;

import com.thoughtworks.go.util.Clock;
import lombok.*;
import lombok.experimental.Accessors;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.Timestamp;

@EqualsAndHashCode(callSuper = true, doNotUseGetters = true)
@ToString(callSuper = true)
@Getter
@Setter(AccessLevel.PROTECTED)
@Accessors(chain = true)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AccessToken extends PersistentObject {
    private static final int DEFAULT_ITERATIONS = 4096;
    private static final int DESIRED_KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 32;

    //this is the hashed token value
    private String value;
    private String description;
    private boolean isRevoked;
    private Timestamp revokedAt;
    private Timestamp createdAt;
    private Timestamp lastUsed;
    private String username;
    private String saltId;
    private String saltValue;
    private String authConfigId;
    private String revokeCause;
    private String revokedBy;

    public static AccessTokenWithDisplayValue create(String description, String username, String authConfigId, Clock clock) {
        String originalToken = generateSecureRandomString(16);
        String saltId = generateSecureRandomString(4);
        String saltValue = generateSalt();
        String hashedToken = digestToken(originalToken, saltValue);
        String finalTokenValue = String.format("%s%s", saltId, originalToken);

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
        new SecureRandom().nextBytes(randomBytes);
        return Hex.encodeHexString(randomBytes);
    }

    private static String generateSalt() {
        try {
            return Base64.encodeBase64String(SecureRandom.getInstance("SHA1PRNG").generateSeed(SALT_LENGTH));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static String digestToken(String originalToken, String salt) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            SecretKey key = factory.generateSecret(new PBEKeySpec(originalToken.toCharArray(), salt.getBytes(), DEFAULT_ITERATIONS, DESIRED_KEY_LENGTH));
            return Hex.encodeHexString(key.getEncoded());
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    public void revoke(String username, String revokeCause, Timestamp revokedAt) {
        setRevoked(true)
                .setRevokedBy(username)
                .setRevokeCause(revokeCause)
                .setRevokedAt(revokedAt);
    }

    @Getter
    @Setter(AccessLevel.PROTECTED)
    @Accessors(chain = true)
    @NoArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class AccessTokenWithDisplayValue extends AccessToken {
        private String displayValue;
    }
}
