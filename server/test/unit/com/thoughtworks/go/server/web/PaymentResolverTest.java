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

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertThat;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.hamcrest.Matchers.is;

public class PaymentResolverTest {
    private MockHttpServletRequest mockHttpServletRequest;

    @Before
    public void setUp() {
        mockHttpServletRequest = new MockHttpServletRequest();
    }

    @Test
    public void shouldRequirePaymentForJsonRequest() {
        mockHttpServletRequest.setRequestURI("/bla/bla/pipelineStatus.json");
        PaymentResolver paymentResolver = new PaymentResolver();
        assertThat("should require payment for json request", paymentResolver.shouldPay(mockHttpServletRequest),
                is(true));
    }

    @Test
    public void shouldRequirePaymentForRestfulJsonRequest() {
        mockHttpServletRequest.setRequestURI("/go/repository/restful/artifact/GET/json/things");
        PaymentResolver paymentResolver = new PaymentResolver();
        assertThat("should require payment for restful json request", paymentResolver.shouldPay(mockHttpServletRequest),
                is(true));
    }

    @Test
    public void shouldRequirePaymentForRestfulCsvRequest() {
        mockHttpServletRequest.setRequestURI("/go/repository/restful/artifact/GET/json/things.csv");
        PaymentResolver paymentResolver = new PaymentResolver();
        assertThat("should require payment for restful csv request", paymentResolver.shouldPay(mockHttpServletRequest),
                is(true));
    }

    @Test
    public void shouldNotRequirePaymentForRestfulHtmlRequest() {
        mockHttpServletRequest.setRequestURI("/go/repository/restful/artifact/GET/html");
        PaymentResolver paymentResolver = new PaymentResolver();
        assertThat("should not require payment for restful html request",
                paymentResolver.shouldPay(mockHttpServletRequest),
                is(false));
    }
}
