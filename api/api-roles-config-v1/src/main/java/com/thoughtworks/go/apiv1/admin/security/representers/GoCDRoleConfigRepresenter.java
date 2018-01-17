/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv1.admin.security.representers;

import cd.go.jrepresenter.RequestContext;
import cd.go.jrepresenter.annotations.Collection;
import cd.go.jrepresenter.annotations.Represents;
import com.thoughtworks.go.config.RoleConfig;
import com.thoughtworks.go.config.RoleUser;

import java.util.List;
import java.util.function.BiFunction;

@Represents(value = RoleConfig.class)
public interface GoCDRoleConfigRepresenter {

    @Collection(modelAttributeType = RoleUser.class, serializer = RoleUserSerializer.class, deserializer = RoleUserDeserializer.class)
    List<String> users();


    class RoleUserSerializer implements BiFunction<RoleUser, RequestContext, String> {
        @Override
        public String apply(RoleUser roleUser, RequestContext requestContext) {
            if (roleUser == null || roleUser.getName() == null) {
                return null;
            }
            return roleUser.getName().toString();
        }
    }

    class RoleUserDeserializer implements BiFunction<String, RequestContext, RoleUser> {
        @Override
        public RoleUser apply(String s, RequestContext requestContext) {
            return new RoleUser(s);
        }
    }
}
