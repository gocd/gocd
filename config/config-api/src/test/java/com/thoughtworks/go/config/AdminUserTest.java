/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.config;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class AdminUserTest {

    @Test
    public void shouldNotValidateBlankUsers() {
        AdminUser adminUser = new AdminUser(null);
        adminUser.validate(null);
        assertThat(adminUser.errors().on(AdminUser.NAME), is("User cannot be blank."));
    }

    @Test
    public void shouldValidateNonBlankUsers() {
        AdminUser adminUser = new AdminUser(new CaseInsensitiveString("foo"));
        adminUser.validate(null);
        assertNull(adminUser.errors().on(AdminUser.NAME));
    }
}