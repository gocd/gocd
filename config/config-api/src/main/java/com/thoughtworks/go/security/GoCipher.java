/*
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.thoughtworks.go.security;

import com.thoughtworks.go.config.EncryptedVariableValueConfig;
import com.thoughtworks.go.domain.config.EncryptedConfigurationValue;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.Objects;

public class GoCipher implements Serializable {

    final Encrypter aesEncrypter;
    final Encrypter desEncrypter;

    public GoCipher() {
        this(new SystemEnvironment());
    }

    public GoCipher(SystemEnvironment systemEnvironment) {
        this.aesEncrypter = new AESEncrypter(new AESCipherProvider(systemEnvironment));

        if (desCipherFileExists(systemEnvironment)) {
            this.desEncrypter = new DESEncrypter(new DESCipherProvider(systemEnvironment));
        } else {
            this.desEncrypter = null;
        }
    }

    private boolean desCipherFileExists(SystemEnvironment systemEnvironment) {
        return systemEnvironment.getDESCipherFile().exists();
    }

    public String encrypt(String plainText) throws CryptoException {
        return aesEncrypter.encrypt(plainText);
    }

    public String decrypt(String cipherText) throws CryptoException {
        if (isAES(cipherText)) {
            return aesEncrypter.decrypt(cipherText);
        } else {
            throw new CryptoException("Unable to decrypt cipherText");
        }
    }

    public boolean isAES(String cipherText) {
        return aesEncrypter.canDecrypt(cipherText);
    }

    public boolean passwordEquals(EncryptedConfigurationValue p1, EncryptedConfigurationValue p2) {
        if (p1 == null && p2 == null) {
            return true;
        }

        if (p1 == null || p2 == null) {
            return false;
        }

        String password1 = p1.getValue();
        String password2 = p2.getValue();

        return passwordEquals(password1, password2);
    }


    public boolean passwordEquals(EncryptedVariableValueConfig p1, EncryptedVariableValueConfig p2) {
        if (p1 == null && p2 == null) {
            return true;
        }

        if (p1 == null || p2 == null) {
            return false;
        }

        String password1 = p1.getValue();
        String password2 = p2.getValue();

        return passwordEquals(password1, password2);
    }

    public int passwordHashcode(EncryptedVariableValueConfig value) {
        if (value == null) {
            return 0;
        }

        return passwordHashcode(value.getValue());
    }

    public int passwordHashcode(EncryptedConfigurationValue value) {
        if (value == null) {
            return 0;
        }

        return passwordHashcode(value.getValue());
    }

    public int passwordHashcode(String cipherText) {
        try {
            String decrypt = decrypt(cipherText);
            return decrypt.hashCode();
        } catch (CryptoException e) {
            return ("bad-password-" + cipherText).hashCode();
        }
    }

    public boolean passwordEquals(String p1, String p2) {
        if (Objects.equals(p1, p2)) {
            return true;
        }

        try {
            if (StringUtils.startsWith(p1, "AES:") && StringUtils.startsWith(p2, "AES:")) {
                return decrypt(p1).equals(decrypt(p2));
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    // used for XSLT, needs to be static
    public static String desToAES(String cipherText) throws CryptoException {
        return new GoCipher()._desToAES(cipherText);
    }

    private String _desToAES(String cipherText) throws CryptoException {
        Assert.notNull(desEncrypter, "DES encrypter not set");

        String plainText = desEncrypter.decrypt(cipherText);
        return encrypt(plainText);
    }

    public String maybeReEncryptForPostConstructWithoutExceptions(String encryptedValue) {
        try {
            if (isAES(encryptedValue)) {
                return encryptedValue;
            } else {
                return _desToAES(encryptedValue);
            }
        } catch (Exception ignore) {
            // this method is called at the time of deserializing from xml or APIs.
            // The validation done after this will handle exceptions due to bad cipher texts
            return encryptedValue;
        }
    }
}
