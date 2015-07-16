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

import static com.thoughtworks.go.validators.HostNameValidator.INVALID_HOSTNAME_KEY;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class HostNameValidatorTest {
    @Test
    public void shouldMarkANullHostnameAsInvalid() throws Exception {
        LocalizedResult result = mock(LocalizedResult.class);

        new HostNameValidator().validate(null, result);

        verify(result).invalid(INVALID_HOSTNAME_KEY, "null");
    }

    @Test
    public void shouldMarkAValidIPv4IPAsInvalid() throws Exception {
        assertValid("123.107.28.255");
        assertValid("somehostname.local");
        assertValid("somehostname.com");
    }

    @Test
    public void shouldMarkAValidIPv6IPAsInvalid() throws Exception {
        assertValid("2001:0db8:85a3:0000:0000:8a2e:0370:7334");
    }

    @Test
    public void shouldMarkAnIPv6IPWhichIsNotInHexAsInvalid() throws Exception {
        assertInvalid("2001:0Gb8:85a3:0000:0000:8a2e:0370:7334");
        assertInvalid("2001:0xb8:85a3:0000:0000:8a2e:0370:7334");
    }

    @Test
    public void marksInvalidIPv4AddressesAsValidThoughItProbablyShouldNot() throws Exception {
        assertValid("345.980.123.98");
        assertValid("999.980.123.98");
        assertValid("123.107");
        assertValid("123.107.123.98.23");
        assertValid("123.107a.123.98.23");
    }

    private void assertValid(String hostname) {
        LocalizedResult result = mock(LocalizedResult.class);
        new HostNameValidator().validate(hostname, result);
        verifyZeroInteractions(result);
    }

    private void assertInvalid(String hostname) {
        LocalizedResult result = mock(LocalizedResult.class);
        new HostNameValidator().validate(hostname, result);
        verify(result).invalid(INVALID_HOSTNAME_KEY, hostname);
    }
}