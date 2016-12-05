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

import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.security.GoCipher;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class PasswordDeserializerTest {
    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void shouldErrorOutWhenBothPasswordAndEncryptedPasswordAreGivenForDeserialization() {
        SvnMaterialConfig svnMaterialConfig = new SvnMaterialConfig();
        PasswordDeserializer passwordDeserializer = new PasswordDeserializer();
        passwordDeserializer.deserialize("password", "encryptedPassword", svnMaterialConfig);
        assertThat(svnMaterialConfig.errors().getAllOn("password"), is(Arrays.asList("You may only specify `password` or `encrypted_password`, not both!")));
        assertThat(svnMaterialConfig.errors().getAllOn("encryptedPassword"), is(Arrays.asList("You may only specify `password` or `encrypted_password`, not both!")));
    }

    @Test
    public void shouldErrorOutWhenEncryptedPasswordIsInvalid() {
        SvnMaterialConfig svnMaterialConfig = new SvnMaterialConfig();
        PasswordDeserializer passwordDeserializer = new PasswordDeserializer();
        passwordDeserializer.deserialize(null, "invalidEncryptedPassword", svnMaterialConfig);
        assertThat(svnMaterialConfig.errors().getAllOn("encryptedPassword"), is(Arrays.asList("Encrypted value for password is invalid. This usually happens when the cipher text is invalid.")));
    }

    @Test
    public void shouldEncryptClearTextPasswordSentByUser() throws InvalidCipherTextException {
        SvnMaterialConfig svnMaterialConfig = new SvnMaterialConfig();
        PasswordDeserializer passwordDeserializer = new PasswordDeserializer();
        String encrypted = passwordDeserializer.deserialize("password", null, svnMaterialConfig);
        assertThat(encrypted, is(new GoCipher().encrypt("password")));
    }

    @Test
    public void shouldReturnTheEncryptedPasswordSentByUserIfValid() throws InvalidCipherTextException {
        String encryptedPassword = new GoCipher().encrypt("password");
        SvnMaterialConfig svnMaterialConfig = new SvnMaterialConfig();
        PasswordDeserializer passwordDeserializer = new PasswordDeserializer();
        String encrypted = passwordDeserializer.deserialize(null, encryptedPassword, svnMaterialConfig);
        assertThat(encrypted, is(encryptedPassword));
    }

    @Test
    public void shouldReturnNullIfBothPasswordAndEncryptedPasswordAreNull() {
        SvnMaterialConfig svnMaterialConfig = new SvnMaterialConfig();
        PasswordDeserializer passwordDeserializer = new PasswordDeserializer();
        String encrypted = passwordDeserializer.deserialize(null, null, svnMaterialConfig);
        assertNull(encrypted);
    }

    @Test
    public void shouldReturnNullIfBothPasswordAndEncryptedPasswordAreBlank() {
        SvnMaterialConfig svnMaterialConfig = new SvnMaterialConfig();
        PasswordDeserializer passwordDeserializer = new PasswordDeserializer();
        String encrypted = passwordDeserializer.deserialize("", "", svnMaterialConfig);
        assertNull(encrypted);
    }

    @Test
    public void shouldNotValidateEncryptedPasswordIfBlank() {
        SvnMaterialConfig svnMaterialConfig = new SvnMaterialConfig();
        PasswordDeserializer passwordDeserializer = new PasswordDeserializer();
        String encrypted = passwordDeserializer.deserialize(null, "", svnMaterialConfig);
        assertNull(encrypted);
    }

}