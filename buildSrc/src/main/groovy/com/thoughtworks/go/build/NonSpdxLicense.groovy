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

package com.thoughtworks.go.build

enum NonSpdxLicense {

  EDL_1_0("EDL-1.0", "Eclipse Distribution License - v1.0", "Eclipse Distribution License v1.0", "Eclipse Distribution License (New BSD License)"),
  PUBLIC_DOMAIN("Public Domain"),

  public final Set<String> names
  public final String id

  NonSpdxLicense(String id, String... names) {
    this.names = names.collect { it -> it.toLowerCase() }
    this.id = id
  }

  static String normalizedLicense(String license) {
    if (license == null || license.allWhitespace) {
      return null
    }

    for (NonSpdxLicense eachLicense : values()) {
      if (license == eachLicense.id || eachLicense.names.contains(license.toLowerCase())) {
        return eachLicense.id
      }
    }

    return null
  }

}
