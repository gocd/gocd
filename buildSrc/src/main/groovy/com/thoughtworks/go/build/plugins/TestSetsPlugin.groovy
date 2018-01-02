package com.thoughtworks.go.build.plugins

import com.thoughtworks.go.build.plugins.internal.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.internal.reflect.Instantiator

import javax.inject.Inject

class TestSetsPlugin implements Plugin<Project> {

  static final TESTSETS_EXTENSION_NAME = 'testSets'

  private final Instantiator instantiator


  @Inject
  TestSetsPlugin(Instantiator instantiator) {
    this.instantiator = instantiator
  }

  @Override
  void apply(Project project) {
    project.apply plugin: JavaPlugin

    project.extensions.create(TESTSETS_EXTENSION_NAME, DefaultTestSetContainer, instantiator)

    instantiator.newInstance SourceSetListener, project
    instantiator.newInstance ConfigurationDependencyListener, project
    instantiator.newInstance TestTaskListener, project
    instantiator.newInstance JarTaskListener, project
    instantiator.newInstance ArtifactListener, project
  }
}
