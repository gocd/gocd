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

require 'rails_helper'

describe StageConfigLoader do

  before(:all) do
    @klass = Class.new(AdminController) do
      cattr_accessor :filter_names
      attr_accessor :pipeline, :stage, :params

      self.filter_names = []

      def self.before_action filter_name, *args
        filter_names << filter_name
      end

      include StageConfigLoader

      load_stage_except_for
    end
  end

  before(:each) do
    @controller = @klass.new
    @controller.pipeline = PipelineConfigMother.createPipelineConfig("foo-pipeline", "bar-stage", ["baz-job", "quux-job"].to_java(java.lang.String))
  end

  it "should load stage if exists" do
    @controller.params = {:pipeline_name => "foo-pipeline", :stage_name => "bar-stage"}
    expect(@controller.stage).to be_nil
    expect(@controller.send(@klass.filter_names.last)).to eq(true)
    expect(@controller.stage.name()).to eq(CaseInsensitiveString.new("bar-stage"))
  end

  it "should render error when no stage exists" do
    @controller.params = {:pipeline_name => "foo-pipeline", :stage_name => "quux-stage"}
    expect(@controller.stage).to be_nil

    expect(@controller).to receive_render_with({:template => "shared/config_error.html", :layout => "application", :status => 404})

    expect(@controller.send(@klass.filter_names.last)).to eq(false)

    expect(@controller.instance_variable_get('@message')).to eq("No stage named 'quux-stage' exists for pipeline 'foo-pipeline'.")
    expect(@controller.stage).to be_nil
  end

  it "should hookup load_pipeline before load_stage" do
    expect(@klass.filter_names).to eq([:load_pipeline, :load_pause_info, :load_stage])
  end
end
