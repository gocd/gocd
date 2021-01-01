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
package com.thoughtworks.go.apiv4.configrepos.representers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.thoughtworks.go.api.base.OutputListWriter;
import com.thoughtworks.go.api.representers.ErrorGetter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.config.Validatable;
import com.thoughtworks.go.config.rules.*;

import java.util.HashMap;
import java.util.Optional;

import static com.thoughtworks.go.config.rules.DirectiveType.fromString;

public class RulesRepresenter {
    public static void toJSON(OutputListWriter listWriter, Rules rules) {
        rules.forEach(directive -> {
            listWriter.addChild(directiveWriter -> {
                if (directive.hasErrors()) {
                    directiveWriter.addChild("errors", errorWriter -> {
                        new ErrorGetter(new HashMap<>()).toJSON(directiveWriter, directive);
                    });
                }

                if (directive instanceof Unknown) {
                    directiveWriter.add("directive", ((Unknown) directive).getDirective());
                } else {
                    directiveWriter.add("directive", directive.getDirectiveType().type());
                }

                directiveWriter.add("action", directive.action());
                directiveWriter.add("type", directive.type());
                directiveWriter.add("resource", directive.resource());
            });
        });
    }


    public static Rules fromJSON(JsonArray jsonArray) {
        Rules rules = new Rules();

        jsonArray.forEach(directiveJSON -> {
            rules.add(getDirective(directiveJSON));
        });

        return rules;
    }

    private static Directive getDirective(JsonElement directiveJson) {
        JsonReader reader = GsonTransformer.getInstance().jsonReaderFrom(directiveJson.toString());
        String directive = reader.optString("directive").orElse(null);
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
            addError("directive", "Invalid directive, must be either 'allow' or 'deny'.");
        }

        public String getDirective() {
            return directive;
        }

        @Override
        public Result apply(String refer, Class<? extends Validatable> aClass, String group) {
            return null;
        }
    }
}
