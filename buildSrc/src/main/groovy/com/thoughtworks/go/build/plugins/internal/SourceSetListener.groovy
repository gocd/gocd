package com.thoughtworks.go.build.plugins.internal

import com.thoughtworks.go.build.plugins.dsl.TestSet
import com.thoughtworks.go.build.plugins.dsl.TestSetContainer
import org.gradle.api.Project
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.tasks.GroovySourceSet
import org.gradle.api.tasks.SourceSet

class SourceSetListener {

  private final Project project


  SourceSetListener(Project project) {
    this.project = project

    def testSets = project.testSets as TestSetContainer
    testSets.whenObjectAdded { testSetAdded(it) }
  }

  void testSetAdded(TestSet testSet) {
    createSourceSet(testSet)
    createDependency(testSet)

    testSet.whenDirNameChanged { oldDirName, newDirName ->
      dirNameChanged(testSet, oldDirName, newDirName)
    }
  }


  private void createSourceSet(TestSet testSet) {
    // Remember: creating a SourceSet will also implicitly create a compile and runtime configuration
    project.sourceSets.create testSet.sourceSetName
  }


  private void createDependency(TestSet testSet) {
    project.dependencies.add testSet.compileConfigurationName, getMainSourceSet().output
  }


  private SourceSet getMainSourceSet() {
    project.sourceSets[SourceSet.MAIN_SOURCE_SET_NAME] as SourceSet
  }


  void dirNameChanged(TestSet testSet, String oldDirName, String newDirName) {
    def sourceSet = project.sourceSets[testSet.sourceSetName] as SourceSet

    applyJavaSrcDir(sourceSet, oldDirName, newDirName)
    applyResourcesSrcDir(sourceSet, oldDirName, newDirName)
    applyGroovySrcDir(sourceSet, oldDirName, newDirName)
  }


  private void applyResourcesSrcDir(SourceSet sourceSet, String oldDirName, String newDirName) {
    if (oldDirName == null) {
      oldDirName = sourceSet.name
    }
    sourceSet.resources.srcDirs += [project.file("src/$newDirName/resources")]
    sourceSet.resources.srcDirs -= [project.file("src/$oldDirName/resources")]
  }


  private void applyJavaSrcDir(SourceSet sourceSet, String oldDirName, String newDirName) {
    if (oldDirName == null) {
      oldDirName = sourceSet.name
    }
    sourceSet.java.srcDirs += [project.file("src/$newDirName/java")]
    sourceSet.java.srcDirs -= [project.file("src/$oldDirName/java")]
  }


  private void applyGroovySrcDir(SourceSet sourceSet, String oldDirName, String newDirName) {
    def groovySourceSet = new DslObject(sourceSet).convention.findPlugin GroovySourceSet
    if (groovySourceSet != null) {
      groovySourceSet.groovy.srcDirs += ["src/$newDirName/groovy"]
      groovySourceSet.groovy.srcDirs -= ["src/$oldDirName/groovy"]
    }
  }
}
