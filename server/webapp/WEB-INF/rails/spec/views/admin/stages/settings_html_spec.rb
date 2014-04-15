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

require File.join(File.dirname(__FILE__), "/../../../spec_helper")

describe "admin/stages/settings.html.erb" do
  include GoUtil
  include FormUI

  before :each do
    assigns[:pipeline] = @pipeline = PipelineConfigMother.createPipelineConfigWithStages("pipeline-name", ["dev", "acceptance"].to_java(:string))
    assigns[:stage] = @stage = @pipeline.get(0)
    assigns[:pipeline_group] = @group = PipelineConfigMother::groupWithOperatePermission(@pipeline, ["admin", "badger"].to_java(java.lang.String))

    assigns[:cruise_config] = cruise_config = GoConfigMother.defaultCruiseConfig()
    set(cruise_config, "md5", "abc")
    cruise_config.addPipeline("group-1", @pipeline)

    in_params(:pipeline_name => "pipeline", :stage_name => "stage", :action => "settings", :controller => "admin/stages")

    template.stub(:admin_stage_update_path).and_return("admin_stage_update_url")
    template.stub(:admin_stage_edit_path).and_return("admin_stage_edit_url")
  end

  describe "stage approval" do

    it "should have manual, success and custom approval options" do
      render "admin/stages/settings.html.erb"
      response.body.should have_tag("form") do
        with_tag("label[for='auto']", "On Success")
        with_tag("input#auto[type='radio'][name='stage[approval][type]'][value='success']")
        with_tag("label[for='manual']", "Manual")
        with_tag("input#manual[type='radio'][name='stage[approval][type]'][value='manual']")
      end
    end
  end

end
