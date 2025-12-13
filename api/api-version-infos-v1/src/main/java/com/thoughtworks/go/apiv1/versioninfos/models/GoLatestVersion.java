/*
 * Copyright Thoughtworks, Inc.
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

import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.json.JsonHelper;
import lombok.Data;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

@Data
@Accessors(chain = true)
public class GoLatestVersion {
    private static final Logger LOG = LoggerFactory.getLogger(GoLatestVersion.class);
    private SystemEnvironment systemEnvironment;
    private String message;
    private String messageSignature;
    private String signingPublicKey;
    private String signingPublicKeySignature;

    public String latestVersion() {
        return (String) JsonHelper.fromJson(message, Map.class).get("latest-version");
    }

    public boolean isValid() {
        try {
            return !signingPublicKeyTampered() && !messageTampered();
        } catch (Exception e) {
            LOG.error("Exception occurred while verifying message and public key.", e);
            return false;
        }
    }

    private boolean signingPublicKeyTampered() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException {
        String publicKey = Files.readString(Path.of(systemEnvironment.getUpdateServerPublicKeyPath()), UTF_8);
        return !EncryptionHelper.verifyRSASignature(signingPublicKey, signingPublicKeySignature, publicKey);
    }

    private boolean messageTampered() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, InvalidKeySpecException {
        return !EncryptionHelper.verifyRSASignature(message, messageSignature, signingPublicKey);
    }
}
