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

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

import static org.apache.commons.lang3.StringUtils.substringAfter
import static org.apache.commons.lang3.StringUtils.uncapitalize

class CapabilitySupport {
  /**
   * Workaround to deprecated functionality to use the main sourceSet with feature variants from https://github.com/gradle/gradle/issues/29455
   * See https://docs.gradle.org/8.13/userguide/upgrading_version_8.html#deprecate_register_feature_main_source_set
   *
   * Configures each feature's configurations to include the same as for the `main` sourceSet, and allows the main sourceSet
   * to be compiled using the feature specific dependencies.
   */
  static def extendMain(Project project, String... features) {
    project.with {
      features.each { feature ->
        // Make feature configurations include the the same artifacts and variants as main
        configurations.matching { it.name == "${feature}ApiElements" || it.name == "${feature}RuntimeElements" }.configureEach { c ->
          def main = configurations.named(uncapitalize(substringAfter(name, feature)))
          c.outgoing.artifacts.clear()
          c.outgoing.artifacts(main.map { it.outgoing.artifacts })
          c.outgoing.variants.clear()
          c.outgoing.variants.addAllLater(main.map { it.outgoing.variants })
        }

        // Make the "main" classpath configurations able to consume the feature's dependencies
        ['compileClasspath', 'testCompileClasspath'].each { c ->
          configurations.named(c).configure { mc ->
            mc.extendsFrom(configurations."${feature}CompileClasspath" as Configuration)
          }
        }
        ['testRuntimeClasspath'].each { c ->
          configurations.named(c).configure { mc ->
            mc.extendsFrom(configurations."${feature}RuntimeClasspath" as Configuration)
          }
        }
      }
    }
  }
}