/*************************GO-LICENSE-START*********************************
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.security.providers;

import com.thoughtworks.go.server.service.UserService;
import org.springframework.beans.factory.FactoryBean;

public class GoAuthenticationProviderFactory implements FactoryBean {
    private final UserService userService;

    public GoAuthenticationProviderFactory(UserService userService) {
        this.userService = userService;
    }

    public Object getObject() throws Exception {
        return new GoAuthenticationProvider(userService, null);
    }

    public Class getObjectType() {
        return GoAuthenticationProvider.class;
    }

    public boolean isSingleton() {
        return false;
    }
}
