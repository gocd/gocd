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
import com.thoughtworks.go.config.Role;
import com.thoughtworks.go.config.RolesConfig;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Represents(value = RolesConfig.class, linksProvider = RolesRepresenter.RolesLinksProvider.class)
public interface RolesRepresenter {

    @Collection(modelAttributeType = Role.class, embedded = true, representer = RoleRepresenter.class)
    List<Map> roles();

    class RolesLinksProvider implements LinksProvider<RolesConfig> {
        @Override
        public List<Link> getLinks(RolesConfig model, RequestContext requestContext) {
            return Arrays.asList(
                    requestContext.build("self", "/go/api/admin/security/roles"),
                    new Link("doc", "https://api.gocd.org/#roles"),
                    requestContext.build("find", "/go/api/admin/security/roles/:role_name"));
        }

    }
}
