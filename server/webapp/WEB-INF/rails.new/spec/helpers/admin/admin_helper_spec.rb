##########################GO-LICENSE-START################################
# Copyright 2014 ThoughtWorks, Inc.
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

describe Admin::AdminHelper do
  include Admin::AdminHelper

  it "should return template's first stage" do
    @cruise_config = BasicCruiseConfig.new
    @pipeline = GoConfigMother.new.addPipelineWithTemplate(@cruise_config, "pipeline", "template", "stage", ["job"].to_java(java.lang.String))
    stage = first_stage_of_template(@cruise_config, @pipeline.getTemplateName())
    stage.name().to_s.should == 'stage'
  end

  it "should return true when postgresql is used" do
    system_environment = double("system environment")
    allow(self).to receive(:system_environment).and_return(system_environment)
    system_environment.should_receive(:isDefaultDbProvider).and_return(false)
    external_db?.should == true
  end

  it "should return false when postgresql is not used" do
    system_environment = double("system environment")
    allow(self).to receive(:system_environment).and_return(system_environment)
    system_environment.should_receive(:isDefaultDbProvider).and_return(true)
    external_db?.should == false
  end

end
