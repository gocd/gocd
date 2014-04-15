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

require File.expand_path(File.dirname(__FILE__) + '/../../spec_helper')

describe "/pipelines/pipelines_selector.html.erb" do
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

    assigns[:pipeline_configs] = @groups = [@group1, @group2, @groupx]
    assigns[:pipeline_selections] = PipelineSelections.new()
  end

  it "should have the same contents as the jsunit fixture" do
    render :partial => "pipelines/pipelines_selector", :locals => {:scope => {}}
    assert_fixture_equal("pipelines_selector_test.html", response.body)
  end


  describe "/pipelines/pipeline_selector_pipelines.html.erb" do
    it "should render checkboxes for all groups and pipelines" do

      render :partial => "pipelines/pipeline_selector_pipelines", :locals => {:scope => {}}
      @groups.each do |group|
        group_name=group.getGroup()
        response.should have_tag "#selector_group_#{group_name}" do
          with_tag  "input#select_group_#{group_name}[type='checkbox'][name='selector[group][]'][value='#{group_name}']"
          puts "checking for #{group_name}"
          with_tag  ".label[for='select_group_#{group_name}']", group_name
        end

        group.each do |pipeline|
          pipeline_name=pipeline.name().to_s
          response.should have_tag "#selector_pipeline_#{pipeline_name}" do
            with_tag "input#select_pipeline_#{pipeline_name}[type='checkbox'][name='selector[pipeline][]'][value='#{pipeline_name}']"
            with_tag ".label[for='select_pipeline_#{pipeline_name}']", pipeline_name
          end
        end
      end
    end

    it "should check checkboxes that are selected" do
      assigns[:pipeline_selections] = PipelineSelections.new(["pipeline-x3", "pipeline-x4"])
      render :partial => "pipelines/pipeline_selector_pipelines", :locals => {:scope => {}}

      response.should  have_tag "input#select_group_group-1[checked]"
      response.should  have_tag "input#select_group_group-2[checked]"
      response.should_not  have_tag "input#select_group_group-x[checked]"

      [@group1, @group2].each do |group|
        group.each do |pipeline|
          with_tag "input#select_pipeline_#{pipeline.name()}[checked]"
        end
      end
      response.should  have_tag "input#select_pipeline_pipeline-x1[checked]"
      response.should  have_tag "input#select_pipeline_pipeline-x2[checked]"
      response.should_not  have_tag "input#select_pipeline_pipeline-x3[checked]"
      response.should_not  have_tag "input#select_pipeline_pipeline-x4[checked]"
    end

  end
end