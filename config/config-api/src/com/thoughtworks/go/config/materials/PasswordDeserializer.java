/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.config.materials;

import com.thoughtworks.go.security.GoCipher;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.springframework.stereotype.Component;

import static com.thoughtworks.go.config.materials.ScmMaterialConfig.ENCRYPTED_PASSWORD;
import static com.thoughtworks.go.config.materials.ScmMaterialConfig.PASSWORD;
import static org.apache.commons.lang.StringUtils.isNotBlank;

@Component
public class PasswordDeserializer {
    public GoCipher goCipher;

    public PasswordDeserializer() {
        this.goCipher = new GoCipher();
    }

    public String deserialize(String password, String encryptedPassword, AbstractMaterialConfig materialConfig) {
        String pass = null;
        if (isNotBlank(password) && isNotBlank(encryptedPassword)) {
            materialConfig.addError(PASSWORD, "You may only specify `password` or `encrypted_password`, not both!");
            materialConfig.addError(ScmMaterialConfig.ENCRYPTED_PASSWORD, "You may only specify `password` or `encrypted_password`, not both!");
        }

        if (isNotBlank(password)) {
            try {
                pass = goCipher.encrypt(password);
            } catch (InvalidCipherTextException e) {
                materialConfig.addError(PASSWORD, "Could not encrypt the password. This usually happens when the cipher text is invalid");
            }
        } else if (isNotBlank(encryptedPassword)) {
            try {
                goCipher.decrypt(encryptedPassword);
            } catch (Exception e) {
                materialConfig.addError(ENCRYPTED_PASSWORD, "Encrypted value for password is invalid. This usually happens when the cipher text is invalid.");
            }
            pass = encryptedPassword;
        }

        return pass;
    }
}
