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
package com.thoughtworks.go.validators;

import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

public class ValidatorTest {

    @Test
    public void shouldValidateHostname() {
        LocalizedOperationResult result = mock(LocalizedOperationResult.class);

        new HostNameValidator().validate("pavan%pavan", result);
        verify(result).notAcceptable("Invalid hostname. A valid hostname can only contain letters (A-z) digits (0-9) hyphens (-) dots (.) and underscores (_).");

        new HostNameValidator().validate("hostname", result);
        new HostNameValidator().validate("hostname_1", result);
        new HostNameValidator().validate("hostname2-1", result);
        verifyNoMoreInteractions(result);
    }

    @Test
    public void shouldValidatePort() {
        LocalizedOperationResult result = mock(LocalizedOperationResult.class);

        new PortValidator().validate(1, result);
        verifyNoInteractions(result);

        result = mock(LocalizedOperationResult.class);
        new PortValidator().validate(234444, result);
        verify(result).notAcceptable("Invalid port.");

        result = mock(LocalizedOperationResult.class);
        new PortValidator().validate(0, result);
        verify(result).notAcceptable("Invalid port.");

        result = mock(LocalizedOperationResult.class);
        new PortValidator().validate(null, result);
        verify(result).notAcceptable("Invalid port.");
    }
}
