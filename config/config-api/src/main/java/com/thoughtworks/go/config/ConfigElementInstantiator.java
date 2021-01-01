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
package com.thoughtworks.go.config;

import java.lang.reflect.Constructor;

import com.thoughtworks.go.security.GoCipher;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

/**
 * @understands creating instance of a config element class
 */
public class ConfigElementInstantiator {
    public static <T> T instantiateConfigElement(GoCipher goCipher, Class<T> toGenerate) {
        try {
            boolean isPasswordEncrypter = PasswordEncrypter.class.isAssignableFrom(toGenerate);
            Constructor<T> tConstructor = isPasswordEncrypter ? toGenerate.getDeclaredConstructor(GoCipher.class) : toGenerate.getDeclaredConstructor();
            tConstructor.setAccessible(true);
            return isPasswordEncrypter ? tConstructor.newInstance(goCipher) : tConstructor.newInstance();
        } catch (Exception e1) {
            throw bomb("Error creating new instance of class " + toGenerate.getName(), e1);
        }
    }
}
