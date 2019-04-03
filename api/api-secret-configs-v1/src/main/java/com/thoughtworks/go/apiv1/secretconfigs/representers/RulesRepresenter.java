/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv1.secretconfigs.representers;

import com.google.gson.JsonArray;
import com.thoughtworks.go.Deny;
import com.thoughtworks.go.api.base.OutputListWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.api.util.HaltApiResponses;
import com.thoughtworks.go.config.Allow;
import com.thoughtworks.go.config.Directive;
import com.thoughtworks.go.config.Directive.DirectiveType;
import com.thoughtworks.go.config.Rules;

import java.util.Optional;

import static com.thoughtworks.go.config.Directive.DirectiveType.*;

public class RulesRepresenter {
    public static void toJSON(OutputListWriter listWriter, Rules rules) {
        rules.forEach(directive -> {
            listWriter.addChild(directiveWriter -> {
                directiveWriter.add("directive", directive.getDirectiveType().type());
                directiveWriter.add("action", directive.action());
                directiveWriter.add("type", directive.type());
                directiveWriter.add("resource", directive.resource());
            });
        });
    }


    public static Rules fromJSON(JsonArray jsonArray) {
        Rules rules = new Rules();

        jsonArray.forEach(directiveJSON -> {
            GsonTransformer gsonTransformer = GsonTransformer.getInstance();
            rules.add(getDirective(gsonTransformer.jsonReaderFrom(directiveJSON.toString())));
        });

        return rules;
    }

    private static Directive getDirective(JsonReader reader) {
        String directive = reader.getString("directive");
        String action = reader.getString("action");
        String type = reader.getString("type");
        String resource = reader.getString("resource");

        Optional<DirectiveType> directiveType = fromString(directive);

        if (!directiveType.isPresent()) {
            HaltApiResponses.haltBecauseOfReason("Directive '%s' is not recognized as a valid directive in json '%s'", directive, reader.toString());
        }

        switch (directiveType.get()) {
            case ALLOW:
                return new Allow(action, type, resource);
            case DENY:
                return new Deny(action, type, resource);
            default:
                // it will never reach here
                break;
        }
        return null;
    }
}
