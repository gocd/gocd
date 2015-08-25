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

describe "/pipelines/_pipelines_selector.html.erb" do
  include PipelineModelMother

  before do
    @group1 = PipelineConfigMother.createGroup("group-1",
                                               [PipelineConfigMother.pipelineConfig("pipeline-1"),
                                                PipelineConfigMother.pipelineConfig("pipeline-2"),
                                                PipelineConfigMother.pipelineConfig("pipeline-3"),
                                                PipelineConfigMother.pipelineConfig("pipeline-4")].to_java(PipelineConfig))
    @groupx = PipelineConfigMother.createGroup("group-x",
                                               [PipelineConfigMother.pipelineConfig("pipeline-x1"),
                                                PipelineConfigMother.pipelineConfig("pipeline-x2"),
                                                PipelineConfigMother.pipelineConfig("pipeline-x3"),
                                                PipelineConfigMother.pipelineConfig("pipeline-x4")].to_java(PipelineConfig))

    @group2 = PipelineConfigMother.createGroup("group-2", [PipelineConfigMother.pipelineConfig("pipeline-2-1")].to_java(PipelineConfig))

    @groups = [@group1, @group2, @groupx]
    assign(:pipeline_configs, @groups)
    assign(:pipeline_selections, PipelineSelections.new())
  end

  describe "/pipelines/pipeline_selector_pipelines.html.erb" do
    it "should have all and none buttons" do
      render

      expect(response).to have_selector("div.select_all_none_panel a#select_all_pipelines", text: "All")
      expect(response).to have_selector("div.select_all_none_panel a#select_no_pipelines", text: "None")
    end

    it "should have a 'show new pipelines' checkbox" do
      render

      expect(response).to have_selector("div.select_all_none_panel div#show_new_pipelines_container input#show_new_pipelines[type='checkbox'][name='show_new_pipelines']")
      expect(response).to have_selector("div.select_all_none_panel div#show_new_pipelines_container label#show_new_pipelines_label[for='show_new_pipelines']", text: "Show newly created pipelines")
    end

    it "should have the 'show new pipelines' box checked if blacklisting pipelines is enabled for the user" do
      assign(:pipeline_selections, PipelineSelections.new([], nil, nil, true))

      render

      expect(response).to have_selector("div.select_all_none_panel input#show_new_pipelines[checked='checked']")
    end
  end
end
