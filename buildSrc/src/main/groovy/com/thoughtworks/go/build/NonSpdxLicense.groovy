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

package com.thoughtworks.go.build

enum NonSpdxLicense {
  EDL_1_0("EDL-1.0", "Eclipse Distribution License - v1.0", "Eclipse Distribution License v1.0", "Eclipse Distribution License (New BSD License)"),
  CDDL_1_1_GPL_2_0('CDDL-1.1+GPL-2.0',    'CDDL+GPL License', 'Dual license consisting of the CDDL v1.1 and GPL v2', 'https://glassfish.java.net/public/CDDL+GPL_1_1.html'),
  GPL_2_0_CLASSPATH_EXCEPTION('GPL-2.0+CE', 'GPLv2 with the Classpath Exception', 'GNU GENERAL PUBLIC LICENSE, Version 2 + Classpath Exception'),
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

    final String normalized = license.toLowerCase().trim().replaceAll(~/\s+/, ' ')
    for (NonSpdxLicense eachLicense : values()) {
      if (license == eachLicense.id || eachLicense.names.contains(normalized)) {
        return eachLicense.id
      }
    }

    return null
  }
}
