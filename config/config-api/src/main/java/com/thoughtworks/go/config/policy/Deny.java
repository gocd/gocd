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
package com.thoughtworks.go.config.policy;

import com.thoughtworks.go.config.ConfigTag;
import com.thoughtworks.go.config.Validatable;

@ConfigTag("deny")
public class Deny extends AbstractDirective {
    public Deny() {
        super(DirectiveType.DENY);
    }

    public Deny(String action, String type, String resource) {
        super(DirectiveType.DENY, action, type, resource);
    }

    @Override
    public Result apply(String action, Class<? extends Validatable> entityClass, String resource) {
        if (matchesAction(action) && matchesType(entityClass) && matchesResource(resource)) {
            return Result.DENY;
        }

        return Result.SKIP;
    }

}
