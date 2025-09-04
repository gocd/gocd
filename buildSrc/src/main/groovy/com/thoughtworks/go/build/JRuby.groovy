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
  static def exec = { @Deprecated Project project, ExecOperations execOperations, Closure<JavaExecSpec> cl ->
    try {
      execOperations.javaexec { JavaExecSpec javaExecSpec ->
        cl.delegate = javaExecSpec

        LinkedHashMap<String, Object> originalEnv = new LinkedHashMap<String, Object>(javaExecSpec.environment)

        setup(javaExecSpec, project, false)

        cl.call()

        ExecuteUnderRailsTask.debugEnvironment(javaExecSpec, originalEnv)
        ExecuteUnderRailsTask.dumpTaskCommand(javaExecSpec)
      }
    } finally {
      System.out.flush()
      System.err.flush()
    }
  }

  static void setup(JavaExecSpec execSpec, @Deprecated Project project, boolean disableJRubyOptimization) {
    execSpec.with {
      OperatingSystemHelper.normalizeEnvironmentPath(environment)
      environment['PATH'] = (project.additionalJRubyPaths + [environment['PATH']]).join(File.pathSeparator)

      classpath( { project.jrubyJar.get() })
      standardOutput = new PrintStream(System.out, true)
      errorOutput = new PrintStream(System.err, true)

      environment += project.defaultJRubyEnvironment

      // flags to optimize jruby startup performance
      if (!disableJRubyOptimization) {
        jvmArgs += project.jrubyOptimizationJvmArgs
      }

      systemProperties += project.jrubyDefaultSystemProperties

      mainClass.set('org.jruby.Main')
    }
  }
}
