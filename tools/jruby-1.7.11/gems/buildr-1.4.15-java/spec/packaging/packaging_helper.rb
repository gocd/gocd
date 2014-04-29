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


shared_examples_for 'packaging' do
  it 'should create artifact of proper type' do
    packaging = @packaging
    package_type = @package_type || @packaging
    define 'foo', :version=>'1.0' do
      package(packaging).type.should eql(package_type) rescue exit!
    end
  end

  it 'should create file with proper extension' do
    packaging = @packaging
    package_type = @package_type || @packaging
    define 'foo', :version=>'1.0' do
      package(packaging).to_s.should match(/.#{package_type}$/)
    end
  end

  it 'should always return same task for the same package' do
    packaging = @packaging
    define 'foo', :version=>'1.0' do
      package(packaging)
      package(packaging, :id=>'other')
    end
    project('foo').packages.uniq.size.should eql(2)
  end

  it 'should complain if option not known' do
    packaging = @packaging
    define 'foo', :version=>'1.0' do
      lambda { package(packaging, :unknown_option=>true) }.should raise_error(ArgumentError, /no such option/)
    end
  end

  it 'should respond to with() and return self' do
    packaging = @packaging
    define 'foo', :version=>'1.0' do
      package(packaging).with({}).should be(package(packaging))
    end
  end

  it 'should respond to with() and complain if unknown option' do
    packaging = @packaging
    define 'foo', :version=>'1.0' do
      lambda {  package(packaging).with(:unknown_option=>true) }.should raise_error(ArgumentError, /does not support the option/)
    end
  end
end
