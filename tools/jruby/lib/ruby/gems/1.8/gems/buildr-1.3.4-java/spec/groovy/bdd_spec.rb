# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with this
# work for additional information regarding copyright ownership.  The ASF
# licenses this file to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
# License for the specific language governing permissions and limitations under
# the License.

require File.join(File.dirname(__FILE__), '../spec_helpers')


describe Buildr::Groovy::EasyB do
  
  def foo(*args, &prc)
    define('foo', *args) do
      test.using :easyb
      if prc
        instance_eval(&prc)
      else
        self
      end
    end
  end

  it 'should apply to a project having EasyB sources' do
    define('one', :base_dir => 'one') do
      write _('src/spec/groovy/SomeBehaviour.groovy'), 'true;'
      Buildr::Groovy::EasyB.applies_to?(self).should be_true
    end
    define('two', :base_dir => 'two') do
      write _('src/test/groovy/SomeBehaviour.groovy'), 'true;'
      Buildr::Groovy::EasyB.applies_to?(self).should be_false
    end
    define('three', :base_dir => 'three') do
      write _('src/spec/groovy/SomeStory.groovy'), 'true;'
      Buildr::Groovy::EasyB.applies_to?(self).should be_true
    end
    define('four', :base_dir => 'four') do
      write _('src/test/groovy/SomeStory.groovy'), 'true;'
      Buildr::Groovy::EasyB.applies_to?(self).should be_false
    end
  end

  it 'should be selected by :easyb name' do
    foo { test.framework.should eql(:easyb) }
  end

  it 'should select a java compiler if java sources are found' do
    foo do
      write _('src/spec/java/SomeBehavior.java'), 'public class SomeBehavior {}'
      test.compile.language.should eql(:java)
    end
  end
  
  it 'should include src/spec/groovy/*Behavior.groovy' do
    foo do 
      spec = _('src/spec/groovy/SomeBehavior.groovy')
      write spec, 'true'
      test.invoke
      test.tests.should include(spec)
    end
  end

  it 'should include src/spec/groovy/*Story.groovy' do
    foo do 
      spec = _('src/spec/groovy/SomeStory.groovy')
      write spec, 'true'
      test.invoke
      test.tests.should include(spec)
    end
  end
  
end # EasyB