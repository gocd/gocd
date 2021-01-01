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
package com.thoughtworks.go.security;

import com.thoughtworks.go.util.SystemEnvironment;
import org.springframework.util.Assert;

public class DesToAesMigrator {
    final Encrypter desEncrypter;

    public DesToAesMigrator() {
        this(new SystemEnvironment());
    }

    public DesToAesMigrator(SystemEnvironment systemEnvironment) {
        if (desCipherFileExists(systemEnvironment)) {
            this.desEncrypter = new DESEncrypter(new DESCipherProvider(systemEnvironment));
        } else {
            this.desEncrypter = null;
        }
    }

    private boolean desCipherFileExists(SystemEnvironment systemEnvironment) {
        return systemEnvironment.getDESCipherFile().exists();
    }

    // used for XSLT, needs to be static
    public static String desToAES(String cipherText) throws CryptoException {
        return new DesToAesMigrator()._desToAES(cipherText);
    }

    private String _desToAES(String cipherText) throws CryptoException {
        Assert.notNull(desEncrypter, "DES encrypter not set");

        String plainText = desEncrypter.decrypt(cipherText);
        return new GoCipher().encrypt(plainText);
    }
}
