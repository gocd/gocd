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

package com.thoughtworks.go.server.web;

import com.thoughtworks.go.util.GoConstants;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LicenseExpiryInterceptorTest {
    @Test
    public void shouldSetErrorMessageIfUsingExpiredLicense() throws Exception {
        LicenseExpiryValidator expiryValidator = mock(LicenseExpiryValidator.class);
        when(expiryValidator.isLicenseExpired()).thenReturn(true);
        when(expiryValidator.description()).thenReturn("description");

        LicenseExpiryInterceptor interceptor = new LicenseExpiryInterceptor(expiryValidator);
        ModelAndView modelAndView = new ModelAndView();
        interceptor.postHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), new Object(), modelAndView);

        assertThat(modelAndView.getModel().get(GoConstants.ERROR_FOR_GLOBAL_MESSAGE), is((Object)"description"));
    }

    @Test
    public void shouldNotRemoveOtherErrorMessages() throws Exception {
        LicenseExpiryValidator expiryValidator = mock(LicenseExpiryValidator.class);
        when(expiryValidator.isLicenseExpired()).thenReturn(true);
        when(expiryValidator.description()).thenReturn("description");

        LicenseExpiryInterceptor interceptor = new LicenseExpiryInterceptor(expiryValidator);
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.getModel().put(GoConstants.ERROR_FOR_GLOBAL_MESSAGE, "old message");
        interceptor.postHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), new Object(), modelAndView);

        assertThat(modelAndView.getModel().get(GoConstants.ERROR_FOR_GLOBAL_MESSAGE), is((Object)"old message"));
    }
}
