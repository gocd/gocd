package com.thoughtworks.go.build.plugins.internal

import com.thoughtworks.go.build.plugins.dsl.ConfigurableTestSet
import com.thoughtworks.go.build.plugins.dsl.TestSet
import com.thoughtworks.go.build.plugins.dsl.TestSetContainer
import org.gradle.api.internal.AbstractNamedDomainObjectContainer
import org.gradle.internal.reflect.Instantiator

class DefaultTestSetContainer extends AbstractNamedDomainObjectContainer<TestSet> implements TestSetContainer {

  private final TestSet predefinedUnitTestSet

  DefaultTestSetContainer(Instantiator instantiator) {
    super(TestSet.class, instantiator)
    this.predefinedUnitTestSet = new PredefinedUnitTestSet()
    super.add(predefinedUnitTestSet)
  }


  @Override
  protected TestSet doCreate(String name) {
    new DefaultTestSet(name)
  }


  boolean add(TestSet testSet) {

    boolean added = super.add(testSet)

    if (added) {
      if (testSet instanceof ConfigurableTestSet) {
        ((ConfigurableTestSet) testSet).extendsFrom(predefinedUnitTestSet)
      }
    }

    return added
  }
}
