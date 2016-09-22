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

describe ApiV3::Config::ParamRepresenter do

  it "should represent a param" do
    presenter = ApiV3::Config::ParamRepresenter.new(ParamConfig.new("command","echo"))
    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)
    expect(actual_json).to eq(param_hash)
  end

  it "should deserialize" do
    presenter = ApiV3::Config::ParamRepresenter.new(ParamConfig.new)
    deserialized_object = presenter.from_hash(param_hash)
    expected = ParamConfig.new("command","echo")
    expect(deserialized_object).to eq(expected)
  end

  def param_hash
    {
      name:         "command",
      value:       "echo"
    }
  end
end
