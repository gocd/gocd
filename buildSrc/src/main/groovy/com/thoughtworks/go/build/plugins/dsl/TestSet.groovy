package com.thoughtworks.go.build.plugins.dsl

import com.thoughtworks.go.build.plugins.internal.BiAction
import org.gradle.api.Action
import org.gradle.api.Named


interface TestSet extends Named {

  Set<TestSet> getExtendsFrom()

  
  boolean isCreateArtifact()


  String getClassifier()


  String getDirName()


  String getTestTaskName()


  String getJarTaskName()


  String getSourceSetName()


  String getCompileConfigurationName()


  String getImplementationConfigurationName()


  String getRuntimeConfigurationName()


  String getRuntimeOnlyConfigurationName()


  String getArtifactConfigurationName()


  void whenExtendsFromAdded(Action<TestSet> action)


  void whenDirNameChanged(BiAction<String, String> action)
}
