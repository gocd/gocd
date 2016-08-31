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

describe ApiV2::Config::PropertyConfigRepresenter do
  before :each do
    @property = com.thoughtworks.go.config.ArtifactPropertiesGenerator.new('foo', 'target/emma/coverage.xml', 'substring-before(//report/data/all/coverage[starts-with(@type,class)]/@value, %)')
  end

  it 'should serialize property' do
    presenter = ApiV2::Config::PropertyConfigRepresenter.new(@property)
    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)

    expect(actual_json).to eq(property_hash)
  end

  it 'should deserialize test artifact' do
    actual    = com.thoughtworks.go.config.ArtifactPropertiesGenerator.new
    presenter = ApiV2::Config::PropertyConfigRepresenter.new(actual)
    presenter.from_hash(property_hash)
    expect(actual.getSrc).to eq(@property.getSrc)
    expect(actual.getName).to eq(@property.getName)
    expect(actual.getXpath).to eq(@property.getXpath)
    expect(actual).to eq(@property)
  end

  it 'should map errors' do
    @property.setName('')
    @property.validateTree(PipelineConfigSaveValidationContext.forChain(true, "g", PipelineConfig.new, StageConfig.new, JobConfig.new))
    presenter   = ApiV2::Config::PropertyConfigRepresenter.new(@property)
    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)

    expect(actual_json).to eq(property_with_errors)
  end

  def property_hash
    {
        name: 'foo',
        source: 'target/emma/coverage.xml',
        xpath: 'substring-before(//report/data/all/coverage[starts-with(@type,class)]/@value, %)'
    }
  end

  def property_with_errors
    {
        name: '',
        source: 'target/emma/coverage.xml',
        xpath: 'substring-before(//report/data/all/coverage[starts-with(@type,class)]/@value, %)',
        errors:      {
            name:      ["Invalid property name ''. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters."]
        }
    }
  end
end
