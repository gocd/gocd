#
# Copyright 2020 ThoughtWorks, Inc.
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
#

require 'rails_helper'

describe Admin::AdminHelper do
  include Admin::AdminHelper

  def system_environment
    @system_environment ||= instance_double("com.thoughtworks.go.util.SystemEnvironment")
  end

  it "should return template's first stage" do
    @cruise_config = BasicCruiseConfig.new
    @pipeline = GoConfigMother.new.addPipelineWithTemplate(@cruise_config, "pipeline", "template", "stage", ["job"].to_java(java.lang.String))
    stage = first_stage_of_template(@cruise_config, @pipeline.getTemplateName())
    expect(stage.name().to_s).to eq('stage')
  end

  it "should return true when postgresql is used" do
    expect(system_environment).to receive(:isDefaultDbProvider).and_return(false)
    expect(external_db?).to eq(true)
  end

  it "should return false when postgresql is not used" do
    expect(system_environment).to receive(:isDefaultDbProvider).and_return(true)
    expect(external_db?).to eq(false)
  end

end
