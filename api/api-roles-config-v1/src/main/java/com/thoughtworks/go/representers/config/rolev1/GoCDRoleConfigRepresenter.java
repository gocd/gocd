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

package com.thoughtworks.go.representers.config.rolev1;

import cd.go.jrepresenter.Link;
import cd.go.jrepresenter.LinksProvider;
import cd.go.jrepresenter.RequestContext;
import cd.go.jrepresenter.annotations.Collection;
import cd.go.jrepresenter.annotations.Represents;
import com.thoughtworks.go.config.RoleConfig;
import com.thoughtworks.go.config.RoleUser;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

@Represents(value = RoleConfig.class)
public interface GoCDRoleConfigRepresenter {

    @Collection(modelAttributeType = RoleUser.class, serializer = RoleUserSerializer.class, deserializer = RoleUserDeserializer.class)
    List<String> users();

    class RoleConfigLinksProvider implements LinksProvider<RoleConfig> {
        private static final Link DOC = new Link("doc", "https://api.gocd.org/#roles");

        @Override
        public List<Link> getLinks(RoleConfig model, RequestContext requestContext) {
            return Arrays.asList(
                    DOC,
                    requestContext.build("self", "/go/api/admin/security/roles/%s", model.getName()),
                    requestContext.build("find", "/go/api/admin/security/roles/:role_name")
            );
        }
    }

    class RoleUserSerializer implements Function<RoleUser, String> {
        @Override
        public String apply(RoleUser roleUser) {
            if (roleUser == null || roleUser.getName() == null) {
                return null;
            }
            return roleUser.getName().toString();
        }
    }

    class RoleUserDeserializer implements Function<String, RoleUser> {
        @Override
        public RoleUser apply(String s) {
            return new RoleUser(s);
        }
    }
}
