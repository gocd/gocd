package com.thoughtworks.go.build.plugins.internal

import com.thoughtworks.go.build.plugins.dsl.TestSet
import com.thoughtworks.go.build.plugins.dsl.TestSetContainer
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.bundling.Jar

class JarTaskListener {

  private final Project project


  JarTaskListener(Project project) {
    this.project = project

    def testSets = project.testSets as TestSetContainer
    testSets.whenObjectAdded { testSetAdded(it) }
  }

  void testSetAdded(TestSet testSet) {
    project.tasks.create(testSet.jarTaskName, Jar) { thisTask ->
      thisTask.description = "Assembles a jar archive containing the ${testSet.name} classes."
      thisTask.group = BasePlugin.BUILD_GROUP
      thisTask.from {
        project.sourceSets[testSet.sourceSetName].output
      }
      thisTask.classifier = testSet.classifier
    }
  }
}
