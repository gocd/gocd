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
package com.thoughtworks.go.util.validators;

import com.thoughtworks.go.util.SystemUtil;

import static java.text.MessageFormat.format;

public class ServerPortValidator implements Validator {
    private final int port;

    public ServerPortValidator(int port) {
        this.port = port;
    }

    @Override
    public Validation validate(Validation validation) {
        if (SystemUtil.isLocalhostReachable(port)) {
            String message = format("Port {0} could not be opened. Please Check if it is available",
                    String.valueOf(port));
            return validation.addError(new RuntimeException(message));
        } else {
            return Validation.SUCCESS;
        }
    }
}
