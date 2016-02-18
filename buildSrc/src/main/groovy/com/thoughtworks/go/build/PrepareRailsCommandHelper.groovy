/*
 * Copyright 2016 ThoughtWorks, Inc.
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

class PrepareRailsCommandHelper {

  Project project
  List jrubyOpts = []

  PrepareRailsCommandHelper(Project project) {
    this.project = project;
    jrubyOpts += '-J-XX:MaxPermSize=400m'
    systemProperties().each { k, v ->
      jrubyOpts += ["-J-D${k}=${v}"]
    }
  }

  public classpath() {
    (project.sourceSets.test.output + project.sourceSets.test.runtimeClasspath)
  }

  public void prepare() {
    project.delete(testDataDir)
    testDbDir.mkdirs()
    testConfigDir.mkdirs()
    testH2DbDir.mkdirs()
    testDbDeltasDir.mkdirs()

    project.copy {
      from('config')
      into testConfigDir
    }

    project.copy {
      from('db/dbtemplate/h2db')
      into testH2DbDir
    }

    project.copy {
      from('db/migrate/h2deltas')
      into testDbDeltasDir
    }
  }

  LinkedHashMap<String, Object> systemProperties() {
    [
        'log4j.configuration'             : project.file('properties/test/log4j.properties'),
        'always.reload.config.file'       : true,
        'cruise.i18n.cache.life'          : 0,
        'cruise.config.dir'               : testConfigDir,
        'cruise.database.dir'             : testH2DbDir,
        'plugins.go.provided.path'        : testBundledPluginsDir,
        'plugins.external.provided.path'  : testExternalPluginsDir,
        'plugins.work.path'               : testPluginsWorkDir,
        'rails.use.compressed.js'         : false,
        'go.enforce.serverId.immutability': 'N',
    ]
  }

  File getBuildDir() {
    return project.buildDir
  }

  File getTestDataDir() {
    return project.file("${buildDir}/railsTests")
  }

  File getTestConfigDir() {
    return project.file("${testDataDir}/config")
  }

  File getTestDbDir() {
    return project.file("${testDataDir}/db")
  }

  File getTestH2DbDir() {
    return project.file("${testDbDir}/h2db")
  }

  File getTestDbDeltasDir() {
    return project.file("${testDbDir}/h2deltas")
  }

  File getTestPluginsDir() {
    return project.file("${testDataDir}/plugins")
  }

  File getTestBundledPluginsDir() {
    return project.file("${testPluginsDir}/bundled")
  }

  File getTestExternalPluginsDir() {
    return project.file("${testPluginsDir}/external")
  }

  File getTestPluginsWorkDir() {
    return project.file("${testPluginsDir}/work")
  }
}
