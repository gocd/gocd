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

require File.join(File.dirname(__FILE__), "/../../../../spec_helper")


describe "admin/tasks/fetch/new.html.erb" do
  include GoUtil, TaskMother, FormUI

  before :each do
    assigns[:cruise_config] = config = CruiseConfig.new
    set(config, "md5", "abcd1234")
    template.stub(:admin_task_create_path).and_return("task_create_path")
    template.stub(:admin_task_update_path).and_return("task_update_path")

    assigns[:on_cancel_task_vms] = @vms =  java.util.Arrays.asList([vm_for(exec_task('rm')), vm_for(ant_task), vm_for(nant_task), vm_for(rake_task), vm_for(fetch_task)].to_java(TaskViewModel))
   assigns[:stage] = StageConfigMother.stageConfig("stage2")
    upstream = PipelineConfigMother.createPipelineConfig("upstream", "stage1", ["job", "job1", "job2"].to_java(java.lang.String))
    downstream = PipelineConfigMother.createPipelineConfig("downstream", "stage2", ["job"].to_java(java.lang.String))
    downstream.addMaterialConfig(DependencyMaterialConfig.new(CaseInsensitiveString.new("upstream"), CaseInsensitiveString.new("stage1")))
    assigns[:pipelines_for_fetch] = [upstream, downstream]
    assigns[:pipeline] = downstream
  end

  it "should render a simple fetch task for edit" do
    task = assigns[:task] = FetchTask.new(CaseInsensitiveString.new("upstream"), CaseInsensitiveString.new("stage1"), CaseInsensitiveString.new("job"), "src", "dest")
    assigns[:task_view_model] = Spring.bean("taskViewService").getViewModel(task, 'edit')

    render "/admin/tasks/plugin/edit"
    assert_response_body
  end

  it "should render a simple fetch task for create" do
    task = assigns[:task] = FetchTask.new
    assigns[:task_view_model] = Spring.bean("taskViewService").getViewModel(task, 'new')

    render "/admin/tasks/plugin/new"
    assert_response_body
  end

  def assert_response_body
    response.body.should have_tag("form") do
      with_tag("div.fieldset") do
        with_tag("label", "Pipeline")
        with_tag("input[name='task[pipelineName]']")
        with_tag("label", "Stage*")
        with_tag("input[name='task[stage]']")
        with_tag("label", "Job*")
        with_tag("input[name='task[job]']")
        with_tag("label", "Source*")
        with_tag("input[name='task[src]']")
        with_tag("label", "Destination")
        with_tag("input[name='task[dest]']")
      end
    end
  end

end