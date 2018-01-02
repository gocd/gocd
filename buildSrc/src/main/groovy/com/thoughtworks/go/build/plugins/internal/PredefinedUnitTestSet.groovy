package com.thoughtworks.go.build.plugins.internal

import com.thoughtworks.go.build.plugins.dsl.TestSet
import org.gradle.api.Action
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet

class PredefinedUnitTestSet extends AbstractTestSet {

  static final String NAME = "unitTest"

  @Override
  String getName() {
    NAME
  }


  @Override
  boolean isCreateArtifact() {
    false
  }


  @Override
  String getDirName() {
    SourceSet.TEST_SOURCE_SET_NAME
  }


  @Override
  Set<TestSet> getExtendsFrom() {
    Collections.emptySet()
  }


  @Override
  String getTestTaskName() {
    JavaPlugin.TEST_TASK_NAME
  }


  @Override
  String getSourceSetName() {
    SourceSet.TEST_SOURCE_SET_NAME
  }


  @Override
  String getCompileConfigurationName() {
    JavaPlugin.TEST_COMPILE_CONFIGURATION_NAME
  }


  @Override
  String getImplementationConfigurationName() {
    return JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME
  }


  @Override
  String getRuntimeConfigurationName() {
    //noinspection GrDeprecatedAPIUsage
    JavaPlugin.TEST_RUNTIME_CONFIGURATION_NAME
  }


  @Override
  String getRuntimeOnlyConfigurationName() {
    return JavaPlugin.TEST_RUNTIME_ONLY_CONFIGURATION_NAME
  }


  @Override
  void whenExtendsFromAdded(Action<TestSet> action) {
  }


  @Override
  void whenDirNameChanged(BiAction<String, String> action) {
  }
}
