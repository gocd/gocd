/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.validators;

import com.thoughtworks.go.server.service.result.LocalizedResult;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class ValidatorTest {

    @Test
    public void shouldValidateHostname() {
        LocalizedResult result = mock(LocalizedResult.class);

        new HostNameValidator().validate("pavan%pavan", result);
        verify(result).invalid("INVALID_HOSTNAME", "pavan%pavan");

        new HostNameValidator().validate("hostname", result);
        new HostNameValidator().validate("hostname_1", result);
        new HostNameValidator().validate("hostname2-1", result);
        verifyNoMoreInteractions(result);
    }

    @Test
    public void shouldValidatePort() {
        LocalizedResult result = mock(LocalizedResult.class);

        new PortValidator().validate(1, result);

        result = mock(LocalizedResult.class);
        new PortValidator().validate(234444, result);
        verify(result).invalid("INVALID_PORT");
    }
}
