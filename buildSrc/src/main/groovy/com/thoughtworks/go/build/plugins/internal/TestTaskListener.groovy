package com.thoughtworks.go.build.plugins.internal

import com.thoughtworks.go.build.plugins.dsl.TestSet
import com.thoughtworks.go.build.plugins.dsl.TestSetContainer
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.testing.Test

class TestTaskListener {

  final Project project


  TestTaskListener(Project project) {
    this.project = project

    def testSets = project.testSets as TestSetContainer
    testSets.whenObjectAdded { testSetAdded(it) }
  }


  void testSetAdded(TestSet testSet) {
    SourceSet sourceSet = (SourceSet) project.sourceSets[testSet.sourceSetName]

    project.tasks.create(testSet.testTaskName, Test) { thisTask ->
      thisTask.group = JavaBasePlugin.VERIFICATION_GROUP
      thisTask.description = "Runs the ${testSet.name} tests"

      thisTask.setTestClassesDirs(sourceSet.output.classesDirs)
      thisTask.classpath += sourceSet.runtimeClasspath
    }
  }
}
