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

describe JobConfigLoader do
  before(:all) do
    @klass = Class.new(AdminController) do
      cattr_accessor :filter_names
      attr_accessor :pipeline, :stage, :job, :params

      self.filter_names = []

      def self.before_action filter_name, *args
        filter_names << filter_name
      end

      include JobConfigLoader

      load_job_except_for
    end
  end

  before(:each) do
    @controller = @klass.new
    @controller.pipeline = PipelineConfigMother.createPipelineConfig("foo-pipeline", "bar-stage", ["baz-job", "quux-job"].to_java(java.lang.String))
    @controller.stage = @controller.pipeline.get(0)
  end

  it "should load stage if exists" do
    @controller.params = {:pipeline_name => "foo-pipeline", :stage_name => "bar-stage", :job_name => "baz-job"}
    expect(@controller.job).to be_nil
    expect(@controller.send(@klass.filter_names.last)).to eq(true)
    expect(@controller.job.name()).to eq(CaseInsensitiveString.new("baz-job"))
  end

  it "should render error when no stage exists" do
    @controller.params = {:pipeline_name => "foo-pipeline", :stage_name => "quux-stage", :job_name => "bang-job"}
    expect(@controller.job).to be_nil
    expect(@controller).to receive_render_with({:template => "shared/config_error.html", :layout => "application", :status => 404})

    expect(@controller.send(@klass.filter_names.last)).to eq(false)

    expect(@controller.instance_variable_get('@message')).to eq("No job named 'bang-job' exists for stage 'bar-stage' of pipeline 'foo-pipeline'.")
    expect(@controller.job).to be_nil
  end

  it "should hookup pipeline loader and stage loader before job loader" do
    expect(@klass.filter_names).to eq([:load_pipeline, :load_pause_info, :load_stage, :load_job])
  end
end
