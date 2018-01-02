package com.thoughtworks.go.build.plugins.internal

import com.thoughtworks.go.build.plugins.dsl.ConfigurableTestSet
import com.thoughtworks.go.build.plugins.dsl.TestSet
import org.gradle.api.Action

import java.util.concurrent.CopyOnWriteArrayList

class DefaultTestSet extends AbstractTestSet implements ConfigurableTestSet {

  final String name
  private final Set<TestSet> extendsFrom = []
  private String dirName
  boolean createArtifact = true
  String classifier
  private final List<Action<TestSet>> extendsFromAddedListeners = new CopyOnWriteArrayList<>()
  private final List<BiAction<String, String>> dirNameChangeListeners = new CopyOnWriteArrayList<>()


  DefaultTestSet(String name) {
    this.name = name
  }

  @Override
  String getDirName() {
    dirName ?: name
  }


  @Override
  void setDirName(String newValue) {
    def oldValue = this.dirName
    this.dirName = newValue
    dirNameChangeListeners.each { it.execute oldValue, newValue }
  }


  @Override
  ConfigurableTestSet extendsFrom(TestSet... superTestSets) {
    extendsFromInternal Arrays.asList(superTestSets)
  }


  private ConfigurableTestSet extendsFromInternal(Collection<TestSet> superTestSets) {
    for (superTestSet in superTestSets) {
      extendsFrom << superTestSet
      extendsFromAddedListeners.each { it.execute superTestSet }
    }
    this
  }


  @Override
  Set<TestSet> getExtendsFrom() {
    return Collections.unmodifiableSet(this.extendsFrom)
  }

  @Override
  String getClassifier() {
    classifier ?: name
  }

  @Override
  void whenExtendsFromAdded(Action<TestSet> action) {
    extendsFromAddedListeners << action
  }


  @Override
  void whenDirNameChanged(BiAction<String, String> action) {
    dirNameChangeListeners << action
  }
}
