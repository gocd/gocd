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

describe ::ConfigUpdate::LoadConfig do
  include ::ConfigUpdate::LoadConfig

  def params
    @params
  end
  before(:each) do
    @params = {}
    cruise_config_mother = com.thoughtworks.go.helper.GoConfigMother.new
    @cruise_config = cruise_config_mother.cruiseConfigWithOnePipelineGroup()
    @template = PipelineTemplateConfig.new(CaseInsensitiveString.new("my_template"), [StageConfigMother.oneBuildPlanWithResourcesAndMaterials("stage", "job")].to_java(StageConfig))
    @cruise_config.addTemplate(@template)
  end

  describe "load_pipeline_group" do
    it "should load pipeline group for a given pipeline" do
      @params[:pipeline_name] = "pipeline1"
      expect(load_pipeline_group(@cruise_config)).to eq("group1")
    end

    it "should load pipeline group for a given pipeline" do
      @params[:group_name] = "group1"
      expect(load_pipeline_group(@cruise_config)).to eq("group1")
    end

    it "should fail when trying to load a group which does not exist" do
      @params[:group_name] = "group_which_cannot_be_found"
      expect{load_pipeline_group(@cruise_config)}.to raise_error("Unable to find group: group_which_cannot_be_found")
    end
  end

  it "should load all pipeline groups" do
    groups = load_all_pipeline_groups(@cruise_config)
    expect(groups.size()).to eq(1)
  end

  it "should load pipeline group" do
    @params[:group_name] = "group1"
    group = load_pipeline_group_config(@cruise_config)
    expect(group.getGroup()).to eq("group1")
  end

  it "should return nil if pipeline group not found" do
    @params[:group_name] = "some_random_group"
    group = load_pipeline_group_config(@cruise_config)
    expect(group).to eq(nil)
  end

  it "should template when :stage_parent = templates" do
    @params.merge!(:stage_parent => "templates", :pipeline_name => "my_template")
    expect(load_pipeline_or_template(@cruise_config)).to eq(@template)
  end

  it "should load pipeline when :stage_parent = pipelines" do
    @params.merge!(:stage_parent => "pipelines", :pipeline_name => "pipeline1")
    expect(load_pipeline_or_template(@cruise_config)).not_to be_nil
  end

  it "should return null when requested template is not found" do
    @params.merge!(:stage_parent => "templates", :pipeline_name => "foo_template")
    expect(load_pipeline_or_template(@cruise_config)).to be_nil
  end

  it "should return null when requested pipeline is not found" do
    @params.merge!(:stage_parent => "pipelines", :pipeline_name => "foo_pipeline")
    expect(load_pipeline_or_template(@cruise_config)).to be_nil
  end

  it "should return nil stage for nil pipeline/template" do
    @params.merge!(:stage_parent => "templates", :pipeline_name => "foo_template", :stage_name => "my_stage", :job_name => "my_job")
    expect(load_stage(@cruise_config)).to be_nil
  end

  it "should return nil job for nil stage" do
    @params.merge!(:stage_parent => "templates", :pipeline_name => "foo_template", :stage_name => "my_stage", :job_name => "my_job")
    expect(load_job(@cruise_config)).to be_nil
  end

  it "should return nil task for nil job" do
    @params.merge!(:stage_parent => "templates", :pipeline_name => "foo_template", :stage_name => "my_stage", :job_name => "my_job")
    expect(load_task_of_job(@cruise_config, 1)).to be_nil
  end

  it "should return nil material for nil pipeline" do
    @params.merge!(:stage_parent => "pipelines", :pipeline_name => "foo_pipeline", :finger_print => "abcd1234")
    expect(load_material_config_for_pipeline(@cruise_config)).to be_nil
  end

  it "should load job from a given stage and job_name" do
    @params.merge!(:stage_parent => "pipelines", :pipeline_name => "pipeline1", :stage_name => "stage")
    stage = load_stage(@cruise_config)
    expect(stage).not_to be_nil
    expect(load_job_from_stage_named(stage, CaseInsensitiveString.new("job"))).not_to be_nil
  end

end
