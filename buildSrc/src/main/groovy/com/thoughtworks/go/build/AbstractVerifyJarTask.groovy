/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.build

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * Verifies that a set of jars (e.g. those packaged inside a jar/war, or a classpath destined to be packaged) exactly
 * matches a set of expected jar names/regexes, grouped by location (e.g. a directory inside the jar).
 */
abstract class AbstractVerifyJarTask extends DefaultTask {

  @Input Map<String, List<String>> expectedJars

  /**
   * @return the names of the actual jars for each location key in {@link #expectedJars}
   */
  protected abstract Collection<String> actualJarNames(String location)

  protected abstract String describe(String location)

  @TaskAction
  void perform() {
    def expectedMessages = []

    expectedJars.each { location, expectedJarsInLocation ->
      def allJars = actualJarNames(location).unique().sort()

      def extra = allJars.findAll { actual -> !expectedJarsInLocation.any { expected -> actual =~ expected } }
      if (!extra.empty) {
        expectedMessages += ["Got some extra jars in ${describe(location)} that were not expected"]
        extra.each { jar ->
          expectedMessages += ["  - ${jar}"]
        }
      }

      def missing = expectedJarsInLocation.findAll { expected -> !allJars.any { actual -> actual =~ expected } }
      if (!missing.empty) {
        expectedMessages += ["Some jars that were expected in ${describe(location)} were not present"]
        missing.each { jar ->
          expectedMessages += ["  - ${jar}"]
        }
      }
    }

    if (!expectedMessages.empty) {
      throw new IllegalStateException(expectedMessages.join(System.lineSeparator()))
    }
  }
}
