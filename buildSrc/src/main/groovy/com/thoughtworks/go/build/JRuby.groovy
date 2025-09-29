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

import org.gradle.api.Project
import org.gradle.process.ExecOperations
import org.gradle.process.JavaExecSpec

class JRuby {
  static jrubyJvmArgs = [
    // Enable native sub-process control by default, required on JDK 17+ and often needed by bundler and such to fork processes
    '--add-opens=java.base/sun.nio.ch=ALL-UNNAMED',
    '--add-opens=java.base/java.io=ALL-UNNAMED',
  ]

  static jrubySystemProperties = [
    'jruby.home': 'uri:classloader://META-INF/jruby.home',
  ]

  static def exec = { @Deprecated Project project, ExecOperations execOperations, Closure<JavaExecSpec> cl ->
    try {
      execOperations.javaexec { JavaExecSpec javaExecSpec ->
        cl.delegate = javaExecSpec

        LinkedHashMap<String, Object> originalEnv = new LinkedHashMap<String, Object>(javaExecSpec.environment)

        setup(javaExecSpec, project)

        cl.call()

        ExecuteUnderRailsTask.debugEnvironment(javaExecSpec, originalEnv)
        ExecuteUnderRailsTask.dumpTaskCommand(javaExecSpec)
      }
    } finally {
      System.out.flush()
      System.err.flush()
    }
  }

  static void setup(JavaExecSpec execSpec, @Deprecated Project project) {
    execSpec.with {
      OperatingSystemHelper.normalizeEnvironmentPath(environment)
      environment['PATH'] = (project.additionalJRubyPaths + [environment['PATH']]).join(File.pathSeparator)

      classpath(project.configurations.jruby)
      standardOutput = new PrintStream(System.out, true)
      errorOutput = new PrintStream(System.err, true)

      environment += project.jrubyEnvironment
      jvmArgs += jrubyJvmArgs
      systemProperties += jrubySystemProperties

      mainClass.set('org.jruby.Main')
    }
  }
}
