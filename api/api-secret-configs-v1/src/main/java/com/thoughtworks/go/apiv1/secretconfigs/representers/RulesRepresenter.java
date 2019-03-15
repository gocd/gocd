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

import com.thoughtworks.go.Deny;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.Allow;
import com.thoughtworks.go.config.Directive;
import com.thoughtworks.go.config.Rules;

public class RulesRepresenter {

    public static void toJSON(OutputWriter jsonWriter, Rules rules) {
        if (!rules.getAllowDirectives().isEmpty()) {
            jsonWriter.addChildList("allow", outputListWriter -> {
                rules.getAllowDirectives().forEach(directive -> {
                    outputListWriter.addChild(outputWriter -> {
                        DirectiveRepresenter.toJSON(outputWriter, directive);
                    });
                });
            });
        }

        if (!rules.getDenyDirectives().isEmpty()) {
            jsonWriter.addChildList("deny", outputListWriter -> {
                rules.getDenyDirectives().forEach(directive -> {
                    outputListWriter.addChild(outputWriter -> {
                        DirectiveRepresenter.toJSON(outputWriter, directive);
                    });
                });
            });
        }
    }


    public static Rules fromJSON(JsonReader rulesJsonReader) {
        Rules rules = new Rules();

        rulesJsonReader.optJsonArray("allow").ifPresent(array -> {
            array.forEach(directive -> rules.add(getAllowDirective(new JsonReader(directive.getAsJsonObject()))));
        });

        rulesJsonReader.optJsonArray("deny").ifPresent(array -> {
            array.forEach(directive -> rules.add(getDenyDirective(new JsonReader(directive.getAsJsonObject()))));
        });

        return rules;
    }

    private static Directive getAllowDirective(JsonReader jsonReader) {
        return new Allow(jsonReader.getString("action"), jsonReader.getString("type"), jsonReader.getString("resource"));
    }

    private static Directive getDenyDirective(JsonReader jsonReader) {
        return new Deny(jsonReader.getString("action"), jsonReader.getString("type"), jsonReader.getString("resource"));
    }
}
