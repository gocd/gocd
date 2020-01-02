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
package com.thoughtworks.go.spark.mocks

import com.google.gson.Gson
import spark.ModelAndView
import spark.TemplateEngine

class StubTemplateEngine extends TemplateEngine {
  private static final Gson GSON = new Gson()

  private static String asJson(Object model) {
    // reconstruct HashMap because Gson does not serialize anonymous inner classes
    return GSON.toJson(new HashMap((Map) model))
  }

  @Override
  String render(ModelAndView modelAndView) {
    return "rendered template with locals ${asJson(modelAndView.model)}"
  }
}
