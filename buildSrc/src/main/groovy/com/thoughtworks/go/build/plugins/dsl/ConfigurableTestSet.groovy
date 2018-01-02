package com.thoughtworks.go.build.plugins.dsl

/**
 * A {@link TestSet} whose properties can be modified.
 */
interface ConfigurableTestSet extends TestSet {

  ConfigurableTestSet extendsFrom(TestSet... superTestSets)


  void setCreateArtifact(boolean createArtifact)


  void setClassifier(String classifier)


  void setDirName(String dirName)
}
