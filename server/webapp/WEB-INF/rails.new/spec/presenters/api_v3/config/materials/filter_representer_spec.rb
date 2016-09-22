##########################################################################
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
##########################################################################

require 'spec_helper'

describe ApiV3::Config::Materials::FilterRepresenter do
  it 'should serialize' do
    filter      = com.thoughtworks.go.config.materials.Filter.new(IgnoredFiles.new('**/*.html'), IgnoredFiles.new('**/foobar/'))
    presenter   = ApiV3::Config::Materials::FilterRepresenter.prepare(filter)
    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)
    expect(actual_json).to eq(filter_hash)
  end

  it 'should deserialize' do
    expected_filter = com.thoughtworks.go.config.materials.Filter.new(IgnoredFiles.new('**/*.html'), IgnoredFiles.new('**/foobar/'))
    actual_filter   = com.thoughtworks.go.config.materials.Filter.new
    presenter       = ApiV3::Config::Materials::FilterRepresenter.prepare(actual_filter)
    presenter.from_hash(filter_hash)
    expect(actual_filter).to eq(expected_filter)
  end
  it 'should serialize to nil when ignored is empty' do
    filter      = com.thoughtworks.go.config.materials.Filter.new()
    presenter   = ApiV3::Config::Materials::FilterRepresenter.prepare(filter)
    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)
    expect(actual_json).to eq(nil)
  end

  it 'should deserialize to nil when no ignored files' do
    actual_filter   = com.thoughtworks.go.config.materials.Filter.new
    presenter       = ApiV3::Config::Materials::FilterRepresenter.prepare(actual_filter)
    presenter.from_hash(empty_filter_hash)
    expect(actual_filter).to eq(com.thoughtworks.go.config.materials.Filter.new)
  end

  def filter_hash
    {
      ignore: %w(**/*.html **/foobar/)
    }
  end

  def empty_filter_hash
    {
      ignore: []
    }
  end

end
