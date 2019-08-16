#
# Copyright 2019 ThoughtWorks, Inc.
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

describe "admin/stages/settings.html.erb" do
  include GoUtil
  include FormUI

  before :each do
    assign(:pipeline, @pipeline = PipelineConfigMother.createPipelineConfigWithStages("pipeline-name", ["dev", "acceptance"].to_java(:string)))
    assign(:stage, @stage = @pipeline.get(0))
    assign(:pipeline_group, @group = PipelineConfigMother::groupWithOperatePermission(@pipeline, ["admin", "badger"].to_java(java.lang.String)))

    assign(:cruise_config, cruise_config = GoConfigMother.defaultCruiseConfig())
    set(cruise_config, "md5", "abc")
    cruise_config.addPipeline("group-1", @pipeline)

    in_params(:pipeline_name => "pipeline", :stage_name => "stage", :action => "settings", :controller => "admin/stages")

    allow(view).to receive(:admin_stage_update_path).and_return("admin_stage_update_url")
    allow(view).to receive(:admin_stage_edit_path).and_return("admin_stage_edit_url")
  end

  describe "stage approval" do

    it "should have manual, success and custom approval options" do
      render

      Capybara.string(response.body).find('form').tap do |form|
        expect(form).to have_selector("label[for='auto']", :text => "On Success")
        expect(form).to have_selector("input#auto[type='radio'][name='stage[approval][type]'][value='success']")
        expect(form).to have_selector("label[for='manual']", :text => "Manual")
        expect(form).to have_selector("input#manual[type='radio'][name='stage[approval][type]'][value='manual']")

        expect(form).to have_selector("input#stage_approval_allowOnlyOnSuccess[type='checkbox']")
        expect(form).to have_selector("label[for='stage_approval_allowOnlyOnSuccess']", :text => "Allow Only On Success")
        expect(form.find("span.stage_approval_allowOnlyOnSuccess.contextual_help.has_go_tip_right")['title']).to eq("Only allow stage to be scheduled if the previous stage run is successful.")

      end
    end
  end
end
