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

package com.thoughtworks.go.helper;

import com.thoughtworks.go.security.Encrypter;

/**
 * A silly encrypter that just reverses the plaintext. Great for tests.
 */
public class ReversingEncrypter implements Encrypter {
    @Override
    public boolean canDecrypt(String cipherText) {
        return true;
    }

    @Override
    public String encrypt(String plainText) {
        return new StringBuilder(plainText).reverse().toString();
    }

    @Override
    public String decrypt(String cipherText) {
        return new StringBuilder(cipherText).reverse().toString();
    }
}
