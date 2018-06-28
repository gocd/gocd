/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.util;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class RSAEncryptionHelperTest {

    @Test
    void shouldEncryptAndDecryptChunk() throws Exception {
        File privateKeyFile = new File(getClass().getClassLoader().getResource("rsa/private_key.pem").getFile());
        File publicKeyFile = new File(getClass().getClassLoader().getResource("rsa/public_key.pem").getFile());

        String chunk = "Encryption is Awesome!";

        String encryptedChunk = RSAEncryptionHelper.encrypt(chunk, publicKeyFile.getAbsolutePath());
        String decryptedChunk = RSAEncryptionHelper.decrypt(encryptedChunk, privateKeyFile.getAbsolutePath());

        assertThat(decryptedChunk, is(chunk));
    }
}
