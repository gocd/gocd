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
package com.thoughtworks.go.presentation;

import java.io.Serializable;

public class FlashMessageModel implements Serializable {

    private final String message;
    private final String flashClassName;

    public FlashMessageModel(String message, String flashClassName) {
        this.message = message;
        this.flashClassName = flashClassName;
    }

    @Override public String toString() {
        return this.message;
    }

    public String getFlashClass() {
        return this.flashClassName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FlashMessageModel that = (FlashMessageModel) o;

        if (flashClassName != null ? !flashClassName.equals(that.flashClassName) : that.flashClassName != null) {
            return false;
        }
        if (message != null ? !message.equals(that.message) : that.message != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = message != null ? message.hashCode() : 0;
        result = 31 * result + (flashClassName != null ? flashClassName.hashCode() : 0);
        return result;
    }
}
