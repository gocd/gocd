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
package com.thoughtworks.go.validation;

import com.thoughtworks.go.domain.materials.ValidationBean;

public abstract class Validator<T> {
    protected final String errorMessage;

    public Validator(String errorMessageKey) {
        this.errorMessage = errorMessageKey;
    }

    public abstract ValidationBean validate(T value);

    public void assertValid(T value) {
        ValidationBean validationBean = this.validate(value);
        if (!validationBean.isValid()) {
            throw new RuntimeException(errorMessage);
        }
    }

    public static EmailValidator emailValidator() {
        return new EmailValidator();
    }

    public static LengthValidator lengthValidator(int length) {
        return new LengthValidator(length);
    }

    public static Validator<String> presenceValidator(String errorMessage) {
        return new PresenceValidator(errorMessage);
    }

}
