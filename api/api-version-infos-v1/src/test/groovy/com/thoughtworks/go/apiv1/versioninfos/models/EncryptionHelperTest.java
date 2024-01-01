/*
 * Copyright 2024 Thoughtworks, Inc.
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
package com.thoughtworks.go.apiv1.versioninfos.models;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EncryptionHelperTest {

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

}
