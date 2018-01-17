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

import cd.go.jrepresenter.Link;
import cd.go.jrepresenter.LinksProvider;
import cd.go.jrepresenter.RequestContext;
import cd.go.jrepresenter.annotations.Property;
import cd.go.jrepresenter.annotations.Represents;
import cd.go.jrepresenter.annotations.RepresentsSubClasses;
import cd.go.jrepresenter.util.TrueBiFunction;
import com.thoughtworks.go.api.ErrorGetter;
import com.thoughtworks.go.api.IfNoErrors;
import com.thoughtworks.go.api.serializers.CaseInsensitiveStringDeserializer;
import com.thoughtworks.go.api.serializers.CaseInsensitiveStringSerializer;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.Role;
import com.thoughtworks.go.config.RoleConfig;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

@Represents(value = Role.class)
@RepresentsSubClasses(
        property = "type",
        nestedUnder = "attributes",
        subClasses = {
                @RepresentsSubClasses.SubClassInfo(
                        value = "gocd",
                        representer = GoCDRoleConfigRepresenter.class,
                        linksProvider = RoleRepresenter.RoleConfigLinksProvider.class),
                @RepresentsSubClasses.SubClassInfo(
                        value = "plugin",
                        representer = PluginRoleConfigRepresenter.class,
                        linksProvider = RoleRepresenter.RoleConfigLinksProvider.class)
        })
public interface RoleRepresenter {

    @Property(deserializer = CaseInsensitiveStringDeserializer.class,
            serializer = CaseInsensitiveStringSerializer.class,
            modelAttributeType = CaseInsensitiveString.class)
    String name();

    @Property(getter = RoleTypeGetter.class, skipParse = TrueBiFunction.class)
    String type();

    @Property(skipParse = TrueBiFunction.class,
            skipRender = IfNoErrors.class,
            getter = RoleErrorGetter.class,
            modelAttributeType = Map.class,
            modelAttributeName = "errors")
    Map errors();


    public class RoleTypeGetter implements BiFunction<Role, RequestContext, String> {
        @Override
        public String apply(Role role, RequestContext requestContext) {
            if (role instanceof RoleConfig) {
                return "gocd";
            } else {
                return "plugin";
            }
        }
    }

    class RoleErrorGetter extends ErrorGetter {
        public RoleErrorGetter() {
            super(Collections.singletonMap("authConfigId", "auth_config_id"));
        }
    }

    class RoleConfigLinksProvider implements LinksProvider<Role> {
        private static final Link DOC = new Link("doc", "https://api.gocd.org/#roles");

        @Override
        public List<Link> getLinks(Role model, RequestContext requestContext) {
            return Arrays.asList(
                    DOC,
                    requestContext.build("self", "/go/api/admin/security/roles/%s", model.getName()),
                    requestContext.build("find", "/go/api/admin/security/roles/:role_name")
            );
        }
    }

}
