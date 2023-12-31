/*
 * Copyright 2024 Thoughtworks, Inc.
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
  EDL_1_0("EDL-1.0", "Eclipse Distribution License - v1.0", "Eclipse Distribution License - v 1.0", "Eclipse Distribution License v1.0", "Eclipse Distribution License (New BSD License)"),
  GPL_2_0_CLASSPATH_EXCEPTION('GPL-2.0+Classpath-exception-2.0', 'GPLv2 with the Classpath Exception', 'GNU GENERAL PUBLIC LICENSE, Version 2 + Classpath Exception'),
  GPL_2_0_UNIVERSAL_FOSS_EXCEPTION('GPL-2.0+Universal-FOSS-exception-1.0', 'GPLv2 with Oracle Universal FOSS exception', 'The GNU General Public License, v2 with FOSS exception', 'The GNU General Public License, v2 with Universal FOSS Exception, v1.0', 'https://oss.oracle.com/licenses/universal-foss-exception/'),
  MPL_2_0_EPL_1_0('MPL-2.0+EPL-1.0', 'MPL 2.0 or EPL 1.0', 'Dual license consisting of the MPL 2.0 or EPL 1.0', 'Mozilla Public License 2.0 or Eclipse Public License 1.0'),
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
