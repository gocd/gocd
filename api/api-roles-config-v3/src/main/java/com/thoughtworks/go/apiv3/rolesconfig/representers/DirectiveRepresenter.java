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
package com.thoughtworks.go.apiv3.rolesconfig.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.ErrorGetter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.Validatable;
import com.thoughtworks.go.config.policy.*;

import java.util.HashMap;
import java.util.Optional;

import static com.thoughtworks.go.config.policy.DirectiveType.fromString;

public class DirectiveRepresenter {

    public static void toJSON(OutputWriter jsonWriter, Directive directive) {
        if (directive.hasErrors()) {
            jsonWriter.addChild("errors", errorWriter -> {
                new ErrorGetter(new HashMap<>()).toJSON(jsonWriter, directive);
            });
        }

        if (directive instanceof Unknown) {
            jsonWriter.add("permission", ((Unknown) directive).getDirective());
        } else {
            jsonWriter.add("permission", directive.getDirectiveType().type());
        }

        jsonWriter.add("action", directive.action())
                .add("type", directive.type())
                .add("resource", directive.resource());
    }

    public static Directive fromJSON(JsonReader reader) {
        String directive = reader.optString("permission").orElse(null);
        String action = reader.optString("action").orElse(null);
        String type = reader.optString("type").orElse(null);
        String resource = reader.optString("resource").orElse(null);

        Optional<DirectiveType> directiveType = fromString(directive);

        if (!directiveType.isPresent()) {
            return new Unknown(directive, action, type, resource);
        }

        switch (directiveType.get()) {
            case ALLOW:
                return new Allow(action, type, resource);
            case DENY:
                return new Deny(action, type, resource);
            default:
                return new Unknown(directive, action, type, resource);
        }
    }

    static class Unknown extends AbstractDirective {
        private final String directive;

        public Unknown(String directive, String action, String type, String resource) {
            super(null, action, type, resource);
            this.directive = directive;
            addError("permission", "Invalid permission, must be either 'allow' or 'deny'.");
        }

        public String getDirective() {
            return directive;
        }

        @Override
        public Result apply(String action, Class<? extends Validatable> aClass, String resource, String resourceToOperateWithin) {
            return null;
        }
    }
}
