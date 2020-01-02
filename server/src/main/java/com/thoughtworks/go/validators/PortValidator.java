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
package com.thoughtworks.go.validators;

import com.thoughtworks.go.server.service.result.LocalizedOperationResult;

public class PortValidator implements Validator<Integer> {

    @Override
    public void validate(Integer port, LocalizedOperationResult result) {
        if (outOfRange(port)) {
            result.notAcceptable("Invalid port.");
        }
    }

    private boolean outOfRange(Integer port) {
        return port == null || port <= 0 || port > 65535;
    }

}
