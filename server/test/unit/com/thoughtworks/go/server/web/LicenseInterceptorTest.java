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

import javax.servlet.http.HttpServletResponse;

import com.thoughtworks.go.licensing.Edition;
import com.thoughtworks.go.server.service.GoLicenseService;
import com.thoughtworks.go.util.ClassMockery;
import com.thoughtworks.go.util.GoConstants;
import org.hamcrest.core.Is;
import static org.hamcrest.core.Is.is;
import org.jmock.Expectations;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class LicenseInterceptorTest {
    private LicenseInterceptor licenseInterceptor;
    private ClassMockery context = new ClassMockery();
    private GoLicenseService goLicenseService;
    private MockHttpServletRequest mockHttpServletRequest;
    private MockHttpServletResponse mockHttpServletResponse;
    private IgnoreResolver ignoreResolver;
    private PaymentResolver paymentResolver;

    @Before
    public void setup() throws Exception {
        goLicenseService = context.mock(GoLicenseService.class);
        ignoreResolver = context.mock(IgnoreResolver.class);
        paymentResolver = context.mock(PaymentResolver.class);
        licenseInterceptor = new LicenseInterceptor(goLicenseService, ignoreResolver, paymentResolver);
        mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.setContextPath("/go");
        mockHttpServletResponse = new MockHttpServletResponse();
    }

    @Test
    public void shouldPassLicenseCheckAndPutCruiseEditionIntoRequestIfLicenseIsValid() throws Exception {
        assumeLicenseIsValid();

        context.checking(new Expectations() {
            {
                one(goLicenseService).getCruiseEdition();
                will(returnValue(Edition.Enterprise));
            }
        });

        assertThat("Should return true if license is valid",
                licenseInterceptor.preHandle(mockHttpServletRequest, mockHttpServletResponse, null), is(true));
        assertThat((Edition) mockHttpServletRequest.getAttribute(GoConstants.EDITION), Is.is(Edition.Enterprise));

    }

    @Test
    public void shouldPassLicenseCheckIfRequestShouldBeIgnored() throws Exception {
        assumeLicenseIsInValid();
        context.checking(new Expectations() {
            {
                one(ignoreResolver).shouldIgnore(mockHttpServletRequest);
                will(returnValue(true));
            }
        });
        assertThat("Should return true if request should be ignored",
                licenseInterceptor.preHandle(mockHttpServletRequest, mockHttpServletResponse, null), is(true));

    }

    @Test
    public void shouldRedirectUserToAboutPageWhenLicenseIsNotValidAndPaymentIsRequired() throws Exception {
        assumeShouldNotIgnore();
        assumeLicenseIsInvalid();

        context.checking(new Expectations() {
            {
                one(paymentResolver).shouldPay(mockHttpServletRequest);
                will(returnValue(true));
            }
        });

        boolean returnCode = licenseInterceptor.preHandle(mockHttpServletRequest, mockHttpServletResponse, null);
        assertThat(returnCode, is(false));
        assertThat(mockHttpServletResponse.getStatus(), is(HttpServletResponse.SC_PAYMENT_REQUIRED));
    }

    @Test
    public void shouldRedirectUserToAboutPageWhenLicenseIsNotValidAndPaymentIsNotRequired() throws Exception {
        assumeShouldNotIgnore();
        assumeLicenseIsInvalid();

        context.checking(new Expectations() {
            {
                one(paymentResolver).shouldPay(mockHttpServletRequest);
                will(returnValue(false));
            }
        });
        boolean returnCode = licenseInterceptor.preHandle(mockHttpServletRequest, mockHttpServletResponse, null);
        assertThat(returnCode, is(false));
        assertThat(mockHttpServletResponse.getRedirectedUrl(), is("/go/about"));
    }

    @Test
    public void editionShouldBeEmptyIfLicenseIsInvalid() throws Exception {
        assumeLicenseIsInValid();
        assumeShouldIgnore();
        licenseInterceptor.preHandle(mockHttpServletRequest, mockHttpServletResponse, null);
        assertThat((String) mockHttpServletRequest.getAttribute(GoConstants.EDITION), Is.is(""));
    }

    private void assumeShouldNotIgnore() {
        context.checking(new Expectations() {
            {
                one(ignoreResolver).shouldIgnore(mockHttpServletRequest);
                will(returnValue(false));
            }
        });
    }

    private void assumeShouldIgnore() {
        context.checking(new Expectations() {
            {
                one(ignoreResolver).shouldIgnore(mockHttpServletRequest);
                will(returnValue(true));
            }
        });
    }

    private void assumeLicenseIsValid() {
        context.checking(new Expectations() {
            {
                one(goLicenseService).isLicenseValid();
                will(returnValue(true));
            }
        });
    }

    private void assumeLicenseIsInValid() {
        context.checking(new Expectations() {
            {
                one(goLicenseService).isLicenseValid();
                will(returnValue(false));
            }
        });
    }

    private void assumeLicenseIsInvalid() {
        context.checking(new Expectations() {
            {
                one(goLicenseService).isLicenseValid();
                will(returnValue(false));
            }
        });
    }

}
