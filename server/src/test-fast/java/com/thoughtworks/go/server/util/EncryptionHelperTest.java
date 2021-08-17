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
package com.thoughtworks.go.server.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;

class EncryptionHelperTest {

    @Test
    void shouldEncryptAndDecryptChunkUsingRSA() throws Exception {
        File privateKeyFile = new File(getClass().getClassLoader().getResource("rsa/subordinate-private.pem").getFile());
        File publicKeyFile = new File(getClass().getClassLoader().getResource("rsa/subordinate-public.pem").getFile());

        String chunk = "Encryption is Awesome!";

        String encryptedChunk = EncryptionHelper.encryptUsingRSA(chunk, FileUtils.readFileToString(publicKeyFile, "utf8"));
        String decryptedChunk = EncryptionHelper.decryptUsingRSA(encryptedChunk, FileUtils.readFileToString(privateKeyFile, "utf8"));

        assertThat(decryptedChunk, is(chunk));
    }

    @Test
    void shouldVerifySignatureAndSubordinatePublicKeyWithMasterPublicKey() throws Exception {
        String subordinatePublicKey = FileUtils.readFileToString(new File(getClass().getClassLoader().getResource("rsa/subordinate-public.pem").getFile()), "utf8");
        String signature = FileUtils.readFileToString(new File(getClass().getClassLoader().getResource("rsa/subordinate-public.pem.sha512").getFile()), "utf8");
        String masterPublicKey = FileUtils.readFileToString(new File(getClass().getClassLoader().getResource("rsa/master-public.pem").getFile()), "utf8");

        assertTrue(EncryptionHelper.verifyRSASignature(subordinatePublicKey, signature, masterPublicKey));
    }

    @Test
    void shouldNotVerifyInvalidSignatureOrInvalidSubordinatePublicKeyWithMasterPublicKey() throws Exception {
        String subordinatePublicKey = FileUtils.readFileToString(new File(getClass().getClassLoader().getResource("rsa/subordinate-public.pem").getFile()), "utf8");
        subordinatePublicKey = subordinatePublicKey + "\n";
        String signature = FileUtils.readFileToString(new File(getClass().getClassLoader().getResource("rsa/subordinate-public.pem.sha512").getFile()), "utf8");
        String masterPublicKey = FileUtils.readFileToString(new File(getClass().getClassLoader().getResource("rsa/master-public.pem").getFile()), "utf8");

        assertFalse(EncryptionHelper.verifyRSASignature(subordinatePublicKey, signature, masterPublicKey));
    }

    @Test
    void shouldEncryptAndDecryptChunkUsingAES() throws Exception {
        String chunk = StringUtils.repeat("Encryption is awesome!", 150);

        SecretKey secretKey = EncryptionHelper.generateAESKey();

        String encryptedData = EncryptionHelper.encryptUsingAES(secretKey, chunk);
        String decryptedData = EncryptionHelper.decryptUsingAES(secretKey, encryptedData);

        assertThat(chunk, is(decryptedData));
    }

    @Test
    void shouldEncryptAndDecryptChunkUsingAESandRSA() throws Exception {
        String chunk = StringUtils.repeat("Encryption is awesome!", 150);

        File privateKeyFile = new File(getClass().getClassLoader().getResource("rsa/subordinate-private.pem").getFile());
        File publicKeyFile = new File(getClass().getClassLoader().getResource("rsa/subordinate-public.pem").getFile());

        SecretKey secretKey = EncryptionHelper.generateAESKey();

        String aesEncryptedData = EncryptionHelper.encryptUsingAES(secretKey, chunk);
        String rsaEncryptedAESKey = EncryptionHelper.encryptUsingRSA(Base64.getEncoder().encodeToString(secretKey.getEncoded()), FileUtils.readFileToString(publicKeyFile, "utf8"));

        String secretKeyContent = EncryptionHelper.decryptUsingRSA(rsaEncryptedAESKey, FileUtils.readFileToString(privateKeyFile, "utf8"));
        byte[] decryptedKey = Base64.getDecoder().decode(secretKeyContent);
        secretKey = new SecretKeySpec(decryptedKey, 0, decryptedKey.length, "AES");

        String decryptedData = EncryptionHelper.decryptUsingAES(secretKey, aesEncryptedData);

        assertThat(chunk, is(decryptedData));
    }
}
