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

import com.thoughtworks.go.util.TestingClock;
import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class AccessTokenTest {

    @Test
    void shouldValidate() {
        AccessToken.AccessTokenWithDisplayValue token = AccessToken.create(null, null, null, new TestingClock());
        token.validate(null);

        assertThat(token.errors())
                .hasSize(3)
                .containsEntry("authConfigId", Collections.singletonList("must not be blank"))
                .containsEntry("description", Collections.singletonList("must not be blank"))
                .containsEntry("username", Collections.singletonList("must not be blank"));
    }

    @Test
    void shouldInitializeSaltAndToken() {
        AccessToken.AccessTokenWithDisplayValue token = AccessToken.create(null, null, null, new TestingClock());

        assertThat(token.getDisplayValue())
                .hasSize(40);
        assertThat(token.getValue())
                .hasSize(64);
        assertThat(token.getSaltId())
                .hasSize(8);
        assertThat(token.getSaltValue())
                .hasSize(64);
    }


    @Test
    void hashToken_shouldHashTheProvidedString() throws Exception {
        AccessToken.AccessTokenWithDisplayValue token = AccessToken.create(null, null, null, new TestingClock());

        SecretKey key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(new PBEKeySpec(token.getDisplayValue().substring(8).toCharArray(), token.getSaltValue().getBytes(StandardCharsets.UTF_8), 4096, 256));

        assertThat(token.getValue()).isEqualTo(Hex.encodeHexString(key.getEncoded()));
    }

    @Test
    void hashToken_shouldGenerateTheSameHashValueForTheSameInputString() {
        String tokenValue = "new-token";
        String saltValue = "new-salt";
        String hashed1 = AccessToken.digestToken(tokenValue, saltValue);
        String hashed2 = AccessToken.digestToken(tokenValue, saltValue);

        assertThat(hashed1).isEqualTo(hashed2);
    }

    @Test
    void shouldRevoke() {
        TestingClock clock = new TestingClock();
        AccessToken token = AccessToken.create(null, null, null, clock);
        token.revoke("admin", "because I can", clock.currentTimestamp());

        assertThat(token.isRevoked()).isTrue();
        assertThat(token.isDeletedBecauseUserDeleted()).isEqualTo(false);
        assertThat(token.getRevokedBy()).isEqualTo("admin");
        assertThat(token.getRevokeCause()).isEqualTo("because I can");
        assertThat(token.getRevokedAt()).isEqualTo(clock.currentTimestamp());
    }

    @Test
    void shouldPerforSoftDelete() {
        TestingClock clock = new TestingClock();
        AccessToken token = AccessToken.create(null, null, null, clock);
        token.revokeBecauseOfUserDelete("admin", clock.currentTimestamp());

        assertThat(token.isRevoked()).isTrue();
        assertThat(token.isDeletedBecauseUserDeleted()).isEqualTo(true);
        assertThat(token.getRevokedBy()).isEqualTo("admin");
        assertThat(token.getRevokeCause()).isEqualTo("Revoked because user was deleted by admin");
        assertThat(token.getRevokedAt()).isEqualTo(clock.currentTimestamp());
    }
}
