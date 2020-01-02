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
package com.thoughtworks.go.domain;

import org.junit.Test;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class IpAddressTest  {

    @Test
    public void shouldParseValidIpAddressString() {
        assertThat(IpAddress.create("10.12.16.18").toString(), is("10.12.16.18"));
    }

    @Test public void invalidIpAddress(){
        assertThat(IpAddress.create("").toString(), is(""));
    }

    @Test public void shouldAcceptLegalValuesForIpAddresses() {
        assertThat(IpAddress.create("255.255.255.255").toString(), is("255.255.255.255"));
    }


    @Test public void ipAddressComparator(){
        assertThat(IpAddress.create("10.12.34.20").compareTo(IpAddress.create("10.12.34.3")),is(greaterThan(0)));
        assertThat(IpAddress.create("10.12.34.20").compareTo(IpAddress.create("10.12.34.20")),is(0));
        assertThat(IpAddress.create("112.12.34.20").compareTo(IpAddress.create("10.12.34.20")), is(greaterThan(0)));
        assertThat(IpAddress.create("10.12.34.20").compareTo(IpAddress.create("")), is(greaterThan(0)));
        assertThat(IpAddress.create("").compareTo(IpAddress.create("10.12.34.3")), is(org.hamcrest.Matchers.lessThan(0)));
        assertThat(IpAddress.create("").compareTo(IpAddress.create("")), is(org.hamcrest.Matchers.lessThan(0)));
        assertThat(IpAddress.create("8:8:8:8:8:8:8:8").compareTo(IpAddress.create("10.12.34.20")), is(greaterThan(0)));
    }

}
