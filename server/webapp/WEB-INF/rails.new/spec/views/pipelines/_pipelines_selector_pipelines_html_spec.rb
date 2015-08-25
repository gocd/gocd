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

describe "/pipelines/_pipeline_selector_pipelines.html.erb" do
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
    it "should render checkboxes for all groups and pipelines" do
      render :partial => "pipelines/pipeline_selector_pipelines", :locals => {:scope => {}}

      @groups.each do |group|
        group_name = group.getGroup()
        Capybara.string(response.body).find("#selector_group_#{group_name}").tap do |selector_group_node|
          expect(selector_group_node).to have_selector("input#select_group_#{group_name}[type='checkbox'][name='selector[group][]'][value='#{group_name}']")
          expect(selector_group_node).to have_selector(".label[for='select_group_#{group_name}']", :text => group_name)
        end
        group.each do |pipeline|
          pipeline_name = pipeline.name().to_s
          Capybara.string(response.body).find("#selector_pipeline_#{pipeline_name}").tap do |selector_pipeline_node|
            expect(selector_pipeline_node).to have_selector("input#select_pipeline_#{pipeline_name}[type='checkbox'][name='selector[pipeline][]'][value='#{pipeline_name}']")
            expect(selector_pipeline_node).to have_selector(".label[for='select_pipeline_#{pipeline_name}']", :text => pipeline_name)
          end
        end
      end
    end

    it "should check checkboxes that are selected" do
      assign(:pipeline_selections, PipelineSelections.new(["pipeline-x3", "pipeline-x4"]))

      render :partial => "pipelines/pipeline_selector_pipelines", :locals => {:scope => {}}

      expect(response).to have_selector("input#select_group_group-1[checked]")
      expect(response).to have_selector("input#select_group_group-2[checked]")
      expect(response).to_not have_selector("input#select_group_group-x[checked]")

      [@group1, @group2].each do |group|
        group.each do |pipeline|
          expect(response).to have_selector("input#select_pipeline_#{pipeline.name()}[checked]")
        end
      end
      expect(response).to have_selector("input#select_pipeline_pipeline-x1[checked]")
      expect(response).to have_selector("input#select_pipeline_pipeline-x2[checked]")
      expect(response).to_not have_selector("input#select_pipeline_pipeline-x3[checked]")
      expect(response).to_not have_selector("input#select_pipeline_pipeline-x4[checked]")
    end
  end
end
