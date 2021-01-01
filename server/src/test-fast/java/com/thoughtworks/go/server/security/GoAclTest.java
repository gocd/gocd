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
package com.thoughtworks.go.server.security;

import java.util.ArrayList;

import com.thoughtworks.go.config.CaseInsensitiveString;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class GoAclTest {

    @Test
    public void shouldBeGrantedIfUserInApprovalList() {
        GoAcl acl = new GoAcl(new ArrayList<CaseInsensitiveString>() {
            {
                add(new CaseInsensitiveString("admin"));
            }
        });
        assertThat("admin should be granted", acl.isGranted(new CaseInsensitiveString("admin")), is(true));
    }

    @Test
    public void shouldBeGrantedIfAnyUserInApprovalList() {
        GoAcl acl = new GoAcl(new ArrayList<CaseInsensitiveString>() {
            {
                add(new CaseInsensitiveString("admin"));
            }
        });
        assertThat("admin should be granted", acl.isGranted(new CaseInsensitiveString("admin")), is(true));
    }

    @Test public void shouldNotBeGrantedIfUserNotInApprovalList() throws Exception {
        GoAcl acl = new GoAcl(new ArrayList<CaseInsensitiveString>() {
            {
                add(new CaseInsensitiveString("admin"));
            }
        });
        assertThat("noexist should not be granted", acl.isGranted(new CaseInsensitiveString("noexist")), is(false));
    }

    @Test public void userNameShouldNotBeCaseSensitive() throws Exception {
        GoAcl acl = new GoAcl(new ArrayList<CaseInsensitiveString>() {
            {
                add(new CaseInsensitiveString("admin"));
            }
        });
        boolean granted = acl.isGranted(new CaseInsensitiveString("ADMIN"));
        assertThat("ADMIN should be granted", granted, is(true));
    }

    @Test public void shouldNotGrantIfNoUsersDefined() throws Exception {
        GoAcl acl = new GoAcl(new ArrayList<>());
        assertThat("ADMIN should not be granted", acl.isGranted(new CaseInsensitiveString("ADMIN")), is(false));
    }
}
