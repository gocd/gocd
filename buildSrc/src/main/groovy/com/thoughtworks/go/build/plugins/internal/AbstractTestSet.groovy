package com.thoughtworks.go.build.plugins.internal

import com.thoughtworks.go.build.plugins.dsl.TestSet


abstract class AbstractTestSet implements TestSet {

  @Override
  String getTestTaskName() {
    name
  }


  @Override
  String getJarTaskName() {
    "${name}Jar"
  }


  @Override
  String getSourceSetName() {
    name
  }


  @Override
  String getCompileConfigurationName() {
    "${name}Compile"
  }


  String getImplementationConfigurationName() {
    "${name}Implementation"
  }


  @Override
  String getRuntimeConfigurationName() {
    "${name}Runtime"
  }


  @Override
  String getRuntimeOnlyConfigurationName() {
    "${name}RuntimeOnly"
  }


  @Override
  String getArtifactConfigurationName() {
    name
  }


  @Override
  String getClassifier() {
    name
  }
}
