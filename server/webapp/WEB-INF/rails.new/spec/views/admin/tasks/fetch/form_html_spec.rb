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


describe "admin/tasks/fetch/new.html.erb" do
  include GoUtil, TaskMother, FormUI

  before :each do
    assign(:cruise_config, config = BasicCruiseConfig.new)
    set(config, "md5", "abcd1234")
    view.stub(:admin_task_create_path).and_return("task_create_path")
    view.stub(:admin_task_update_path).and_return("task_update_path")

    assign(:on_cancel_task_vms, @vms =  java.util.Arrays.asList([vm_for(exec_task('rm')), vm_for(ant_task), vm_for(nant_task), vm_for(rake_task), vm_for(fetch_task_with_exec_on_cancel_task)].to_java(TaskViewModel)))
    assign(:stage, StageConfigMother.stageConfig("stage2"))
    upstream = PipelineConfigMother.createPipelineConfig("upstream", "stage1", ["job", "job1", "job2"].to_java(java.lang.String))
    downstream = PipelineConfigMother.createPipelineConfig("downstream", "stage2", ["job"].to_java(java.lang.String))
    downstream.addMaterialConfig(DependencyMaterialConfig.new(CaseInsensitiveString.new("upstream"), CaseInsensitiveString.new("stage1")))
    assign(:pipelines_for_fetch, [upstream, downstream])
    assign(:pipeline, downstream)
  end

  it "should render a simple fetch task for edit" do
    task = assign(:task, FetchTask.new(CaseInsensitiveString.new("upstream"), CaseInsensitiveString.new("stage1"), CaseInsensitiveString.new("job"), "src", "dest"))
    assign(:task_view_model, Spring.bean("taskViewService").getViewModel(task, 'edit'))

    render :template => "admin/tasks/plugin/edit.html.erb"

    assert_response_body
  end

  it "should render a simple fetch task for create" do
    task = assign(:task, FetchTask.new)
    assign(:task_view_model, Spring.bean("taskViewService").getViewModel(task, 'new'))

    render :template => "/admin/tasks/plugin/new.html.erb"

    assert_response_body
  end

  def assert_response_body
    Capybara.string(response.body).find('form').tap do |form|
      form.all("div.fieldset") do |divs|
        expect(divs[0]).to have_selector("label", :text => "Pipeline")
        expect(divs[0]).to have_selector("input[name='task[pipelineName]']")
        expect(divs[0]).to have_selector("label", :text => "Stage*")
        expect(divs[0]).to have_selector("input[name='task[stage]']")
        expect(divs[0]).to have_selector("label", :text => "Job*")
        expect(divs[0]).to have_selector("input[name='task[job]']")
        expect(divs[0]).to have_selector("label", :text => "Source*")
        expect(divs[0]).to have_selector("input[name='task[src]']")
        expect(divs[0]).to have_selector("label", :text => "Destination")
        expect(divs[0]).to have_selector("input[name='task[dest]']")
      end
    end
  end
end
