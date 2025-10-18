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

import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskAction

abstract class JRuby extends JavaExec {
  static jrubyJvmArgs = [
    '--add-opens=java.base/java.io=ALL-UNNAMED',     // JDK 17+: Enable native sub-process control by default
    '--add-opens=java.base/sun.nio.ch=ALL-UNNAMED',  //          Often needed by bundler and such to fork processes
    '--enable-native-access=ALL-UNNAMED',            // JDK 25+: Needed by com.kenai.jffi.internal.StubLoader at least
    '--sun-misc-unsafe-memory-access=allow',         // JDK 25+: sun.misc.Unsafe needed by org.jruby.util.StringSupport at least
    '-XX:+IgnoreUnrecognizedVMOptions',              // JDK <25: Allow use of --sun-misc-unsafe-memory-access on older JVMs without errors
  ]

  static jrubySystemProperties = [
    'jruby.home': 'uri:classloader://META-INF/jruby.home',
  ]

  @InputFiles List<File> additionalPaths = new ArrayList<>()

  private Map<String, ?> originalEnv = new LinkedHashMap<String, ?>(environment)

  JRuby() {
    additionalPaths = [project.jrubyScriptsDir]

    standardOutput = new PrintStream(System.out, true)
    errorOutput = new PrintStream(System.err, true)

    jvmArgs += jrubyJvmArgs
    systemProperties += jrubySystemProperties

    classpath(project.configurations.jruby)
    mainClass.set('org.jruby.Main')
  }

  @Override
  @TaskAction
  void exec() {
    try {
        OperatingSystemHelper.normalizeEnvironmentPath(environment)
        environment['PATH'] = (additionalPaths + [environment['PATH']]).join(File.pathSeparator)

        debugEnvironment()
        dumpTaskCommand()

        super.exec()
    } finally {
      standardOutput.flush()
      errorOutput.flush()
    }
  }

  void dumpTaskCommand() {
    println "[${workingDir}]\$ java ${allJvmArgs.join(' ')} ${mainClass.get()} ${args.join(' ')}"
  }

  void debugEnvironment() {
    println "Using environment variables"
    def toDump = environment - originalEnv

    int longestEnv = toDump.keySet().sort { a, b -> a.length() - b.length() }.last().length()

    toDump.keySet().sort().each { k ->
      println """${k.padLeft(longestEnv)}='${toDump.get(k)}' \\"""
    }
  }
}
