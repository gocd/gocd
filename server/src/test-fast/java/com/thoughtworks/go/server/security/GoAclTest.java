/*
 * Copyright 2024 Thoughtworks, Inc.
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

import com.thoughtworks.go.config.CaseInsensitiveString;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class GoAclTest {

    @Test
    public void shouldBeGrantedIfUserInApprovalList() {
        GoAcl acl = new GoAcl(List.of(new CaseInsensitiveString("admin")));
        assertThat(acl.isGranted(new CaseInsensitiveString("admin"))).isTrue();
    }

    @Test
    public void shouldBeGrantedIfAnyUserInApprovalList() {
        GoAcl acl = new GoAcl(List.of(new CaseInsensitiveString("admin")));
        assertThat(acl.isGranted(new CaseInsensitiveString("admin"))).isTrue();
    }

    @Test
    public void shouldNotBeGrantedIfUserNotInApprovalList() {
        GoAcl acl = new GoAcl(List.of(new CaseInsensitiveString("admin")));
        assertThat(acl.isGranted(new CaseInsensitiveString("noexist"))).isFalse();
    }

    @Test
    public void userNameShouldNotBeCaseSensitive() {
        GoAcl acl = new GoAcl(List.of(new CaseInsensitiveString("admin")));
        boolean granted = acl.isGranted(new CaseInsensitiveString("ADMIN"));
        assertThat(granted).isTrue();
    }

    @Test
    public void shouldNotGrantIfNoUsersDefined() {
        GoAcl acl = new GoAcl(List.of());
        assertThat(acl.isGranted(new CaseInsensitiveString("ADMIN"))).isFalse();
    }
}
