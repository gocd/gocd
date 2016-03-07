##########################GO-LICENSE-START################################
# Copyright 2016 ThoughtWorks, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
##########################GO-LICENSE-END##################################

require 'spec_helper'

describe 'HeaderConstraint' do

  it 'should return true if request header matches the required header' do
    request = double('foo', :headers => {'HTTP_ACCEPT' => 'application/vnd.go.cd.v1+text'})
    
    expect(HeaderConstraint.new.matches?(request)).to be(true)
  end

  it 'should return false if request header does not exist' do
    request = double('foo', :headers => {})

    expect(HeaderConstraint.new.matches?(request)).to be(false)
  end

  it 'should return false if request header does match required header' do
    request = double('foo', :headers => {'HTTP_ACCEPT' => 'application/vnd.go.cd.v1+json'})

    expect(HeaderConstraint.new.matches?(request)).to be(false)
  end
end
