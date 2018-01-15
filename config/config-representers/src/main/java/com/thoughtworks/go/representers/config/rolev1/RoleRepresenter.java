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

import cd.go.jrepresenter.annotations.Property;
import cd.go.jrepresenter.annotations.Represents;
import cd.go.jrepresenter.annotations.RepresentsSubClasses;
import cd.go.jrepresenter.util.TrueFunction;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.Role;
import com.thoughtworks.go.config.RoleConfig;
import com.thoughtworks.go.representers.config.CaseInsensitiveStringDeserializer;
import com.thoughtworks.go.representers.config.CaseInsensitiveStringSerializer;

import java.util.Map;
import java.util.function.Function;

@Represents(value = Role.class)
@RepresentsSubClasses(
        property = "type",
        nestedUnder = "attributes",
        subClasses = {
                @RepresentsSubClasses.SubClassInfo(
                        value = "gocd",
                        representer = GoCDRoleConfigRepresenter.class,
                        linksProvider = GoCDRoleConfigRepresenter.RoleConfigLinksProvider.class),
                @RepresentsSubClasses.SubClassInfo(
                        value = "plugin",
                        representer = PluginRoleConfigRepresenter.class,
                        linksProvider = PluginRoleConfigRepresenter.RoleConfigLinksProvider.class)
        })
public interface RoleRepresenter {

    @Property(deserializer = CaseInsensitiveStringDeserializer.class,
            serializer = CaseInsensitiveStringSerializer.class,
            modelAttributeType = CaseInsensitiveString.class)
    String name();

    @Property(getter = RoleTypeGetter.class, skipParse = TrueFunction.class)
    String type();

    @Property(skipParse = TrueFunction.class,
            skipRender = IfNoErrors.class,
            getter = ErrorGetter.class,
            modelAttributeType = Map.class,
            modelAttributeName = "errors")
    Map errors();


    public class RoleTypeGetter implements Function<Role, String> {
        @Override
        public String apply(Role role) {
            if (role instanceof RoleConfig) {
                return "gocd";
            } else {
                return "plugin";
            }
        }
    }

    class IfNoErrors implements Function<Role, Boolean> {
        @Override
        public Boolean apply(Role role) {
            return !role.hasErrors();
        }
    }

    class ErrorGetter implements Function<Role, Map> {
        @Override
        public Map apply(Role roleConfig) {
            return roleConfig.errors();
        }
    }

}
