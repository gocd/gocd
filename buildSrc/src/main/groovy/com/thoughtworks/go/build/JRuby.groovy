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

import org.gradle.process.ExecOperations
import org.gradle.process.JavaExecSpec
import org.jruby.runtime.Constants

class JRuby {
  static jar = (JRuby.class.classLoader as URLClassLoader).URLs.find { it.toString().contains('jruby-complete')}.file

  static bundledGemRubyVersion = "${Constants.RUBY_MAJOR_VERSION}.0"

  static jrubyJvmArgs = [
    // Enable native sub-process control by default, required on JDK 17+ and often needed by bundler and such to fork processes
    '--add-opens=java.base/sun.nio.ch=ALL-UNNAMED',
    '--add-opens=java.base/java.io=ALL-UNNAMED',
  ]

  static jrubySystemProperties = [
    'jruby.home': 'uri:classloader://META-INF/jruby.home',
  ]

  static def exec = { ExecOperations execOperations, List<File> additionalPaths, Map<String, ?> additionalEnvironment, Closure<JavaExecSpec> cl ->
    try {
      execOperations.javaexec { JavaExecSpec javaExecSpec ->
        cl.delegate = javaExecSpec

        Map<String, Object> originalEnv = new LinkedHashMap<String, Object>(javaExecSpec.environment)

        setup(javaExecSpec, additionalPaths, additionalEnvironment)

        cl.call()

        ExecuteUnderRailsTask.debugEnvironment(javaExecSpec, originalEnv)
        ExecuteUnderRailsTask.dumpTaskCommand(javaExecSpec)
      }
    } finally {
      System.out.flush()
      System.err.flush()
    }
  }

  static void setup(JavaExecSpec execSpec, List<File> additionalPaths, Map<String, ?> additionalEnvironment) {
    execSpec.with {
      OperatingSystemHelper.normalizeEnvironmentPath(environment)
      environment['PATH'] = (additionalPaths + [environment['PATH']]).join(File.pathSeparator)

      classpath(jar)
      standardOutput = new PrintStream(System.out, true)
      errorOutput = new PrintStream(System.err, true)

      environment += additionalEnvironment
      jvmArgs += jrubyJvmArgs
      systemProperties += jrubySystemProperties

      mainClass.set('org.jruby.Main')
    }
  }
}
