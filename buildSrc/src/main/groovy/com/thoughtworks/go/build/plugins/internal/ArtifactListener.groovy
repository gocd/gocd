package com.thoughtworks.go.build.plugins.internal

import com.thoughtworks.go.build.plugins.dsl.TestSet
import com.thoughtworks.go.build.plugins.dsl.TestSetContainer
import org.gradle.api.Project

class ArtifactListener {

  private final Project project


  ArtifactListener(Project project) {
    this.project = project

    def testSets = project.testSets as TestSetContainer
    testSets.whenObjectAdded { testSetAdded(it) }
  }


  void testSetAdded(TestSet testSet) {

    project.afterEvaluate { project ->

      if (testSet.createArtifact) {

        project.configurations.maybeCreate testSet.artifactConfigurationName

        def jarTask = project.tasks.findByName(testSet.jarTaskName)

        project.artifacts.add(testSet.artifactConfigurationName, jarTask) { artifact ->
          artifact.classifier = testSet.classifier
        }
      }
    }
  }
}
