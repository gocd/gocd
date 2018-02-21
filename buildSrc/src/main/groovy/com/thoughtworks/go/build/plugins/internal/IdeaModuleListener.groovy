/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.build.plugins.internal

import com.thoughtworks.go.build.plugins.dsl.TestSet
import com.thoughtworks.go.build.plugins.dsl.TestSetContainer
import org.gradle.api.Project
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.tasks.GroovySourceSet
import org.gradle.api.tasks.SourceSet
import org.gradle.plugins.ide.idea.model.*

import java.nio.file.Paths

class IdeaModuleListener {

  final Project project


  IdeaModuleListener(Project project) {
    this.project = project

    def testSets = project.testSets as TestSetContainer
    testSets.whenObjectAdded { testSetAdded(it) }
  }

  void testSetAdded(TestSet testSet) {

    def ideaModel = project.extensions.findByType IdeaModel
    if (ideaModel) {

      def ideaModule = ideaModel.module

      def sourceSet = project.sourceSets[testSet.sourceSetName] as SourceSet

//      def srcDirs = sourceSet.allJava.srcDirs
//      ideaModule.testSourceDirs += srcDirs
      testSet.whenDirNameChanged { oldDirName, newDirName ->

        if (oldDirName == null) {
          oldDirName = sourceSet.name
        }

        ideaModule.testSourceDirs += [project.file("src/$newDirName/java")]
        ideaModule.testSourceDirs += [project.file("src/$oldDirName/java")]
      }

      def groovySourceSet = new DslObject(sourceSet).convention.findPlugin(GroovySourceSet)
      if (groovySourceSet) {
        def groovySrcDirs = groovySourceSet.allGroovy.srcDirs
        ideaModule.testSourceDirs += groovySrcDirs
        testSet.whenDirNameChanged { oldDirName, newDirName ->

          if (oldDirName == null) {
            oldDirName = sourceSet.name
          }

          ideaModule.testSourceDirs += [project.file("src/$newDirName/groovy")]
          ideaModule.testSourceDirs += [project.file("src/$oldDirName/groovy")]
        }
      }

      ideaModule.iml.withXml {
        def moduleRootManager = it.asNode().component.find { it.@name == 'NewModuleRootManager' }
        def sourceFolderNode = moduleRootManager?.content?.sourceFolder?.last()
        sourceSet.resources.srcDirs.each { resourcesDir ->
          def relPath = project.relativePath(resourcesDir.canonicalPath).replace('\\', '/')
          sourceFolderNode += {
            sourceFolder(url: 'file://$MODULE_DIR$/' + relPath,
              type: 'java-test-resource')
          }
        }
      }

      addConfigurationToClasspath testSet.compileConfigurationName, ideaModule
      addConfigurationToClasspath testSet.runtimeConfigurationName, ideaModule

      applyLibraryFix ideaModule
    }
  }


  private void addConfigurationToClasspath(String configurationName, IdeaModule ideaModule) {
    def testSetConfiguration = project.configurations.findByName configurationName
    if (testSetConfiguration) {
      ideaModule.scopes.TEST.plus += [testSetConfiguration]
    }
  }

  /**
   * Removes module-library entries for the outputs of other source sets that the test set depends on.
   * The idea plugin adds them because of the dependency, but there is really no point in keeping them
   * because the sources are already in the classpath.
   *
   * @param ideaModule the {@link IdeaModule} instance being configured
   */
  private void applyLibraryFix(IdeaModule ideaModule) {

    ideaModule.iml.whenMerged { Module module ->

      // Get all output directories for other source sets' classes and resources.
      def outputPaths = project.sourceSets*.output
        .collect { it.classesDirs + [it.resourcesDir] }
        .flatten { file -> Paths.get(file as String) } as Set

      module.dependencies.removeAll { dep ->
        dep instanceof ModuleLibrary && ((ModuleLibrary) dep).classes.any { Path path ->

          def canonicalUrl = URI.create(path.canonicalUrl)
          if (canonicalUrl.scheme == 'file') {
            // Add another slash to the scheme specific part if necessary... IDEA uses 2 forward slashes
            // for file URLs, whereas java.nio uses 3
            if (!canonicalUrl.schemeSpecificPart.startsWith('///')) {
              canonicalUrl = new URI('file', '/' + canonicalUrl.schemeSpecificPart, null)
            }
            return Paths.get(canonicalUrl) in outputPaths
          } else {
            return false
          }
        }
      }
    }
  }
}
