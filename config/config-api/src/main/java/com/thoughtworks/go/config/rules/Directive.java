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
package com.thoughtworks.go.config.rules;

import com.thoughtworks.go.config.ConfigInterface;
import com.thoughtworks.go.config.Validatable;

import java.io.Serializable;

@ConfigInterface
public interface Directive extends Validatable, Serializable {
    boolean hasErrors();

    String action();

    String type();

    String resource();

    DirectiveType getDirectiveType();

    Result apply(String refer, Class<? extends Validatable> aClass, String group);
}
