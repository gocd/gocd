##########################GO-LICENSE-START################################
# Copyright 2018 ThoughtWorks, Inc.
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


describe "admin/tasks/fetch/new.html.erb" do
  include GoUtil
  include TaskMother
  include FormUI

  before :each do
    assign(:cruise_config, config = BasicCruiseConfig.new)
    set(config, "md5", "abcd1234")
    allow(view).to receive(:admin_task_create_path).and_return("task_create_path")
    allow(view).to receive(:admin_task_update_path).and_return("task_update_path")

    assign(:on_cancel_task_vms, @vms = java.util.Arrays.asList([vm_for(exec_task('rm')), vm_for(ant_task), vm_for(nant_task), vm_for(rake_task), vm_for(fetch_task_with_exec_on_cancel_task)].to_java(TaskViewModel)))
    assign(:stage, StageConfigMother.stageConfig("stage2"))
    upstream = PipelineConfigMother.createPipelineConfig("upstream", "stage1", ["job", "job1", "job2"].to_java(java.lang.String))
    downstream = PipelineConfigMother.createPipelineConfig("downstream", "stage2", ["job"].to_java(java.lang.String))
    downstream.addMaterialConfig(DependencyMaterialConfig.new(CaseInsensitiveString.new("upstream"), CaseInsensitiveString.new("stage1")))
    assign(:pipelines_for_fetch, [upstream, downstream])
    assign(:pipeline, downstream)
    assign(:artifact_plugin_to_fetch_view, [{:id => 'cd.go.docker', :name => 'foo', :view => '<input type="text"/>'}])
  end

  describe 'GoCD fetch task' do
    it 'create - should render as default view' do
      task = assign(:task, com.thoughtworks.go.config.FetchTaskAdapter.new)
      assign(:task_view_model, Spring.bean("taskViewService").getViewModel(task, 'new'))

      render :template => "/admin/tasks/plugin/new.html.erb"

      assert_gocd_artifact
    end

    it 'should render a simple fetch task for edit' do
      fetch_task = FetchTask.new(CaseInsensitiveString.new("upstream"), CaseInsensitiveString.new("stage1"), CaseInsensitiveString.new("job"), "src", "dest")
      task = assign(:task, com.thoughtworks.go.config.FetchTaskAdapter.new(fetch_task))
      assign(:task_view_model, Spring.bean("taskViewService").getViewModel(task, 'edit'))

      render :template => "admin/tasks/plugin/edit.html.erb"

      assert_gocd_artifact
    end
  end

  describe 'External fetch task' do
    it 'should render pluggable fetch task view' do
      external_fetch_task = FetchPluggableArtifactTask.new(CaseInsensitiveString.new("upstream"), CaseInsensitiveString.new("stage1"), CaseInsensitiveString.new("job"), "docker")
      task = assign(:task, com.thoughtworks.go.config.FetchTaskAdapter.new(external_fetch_task))
      assign(:task_view_model, Spring.bean("taskViewService").getViewModel(task, 'new'))

      render :template => "/admin/tasks/plugin/new.html.erb"

      assert_external_artifact
    end

    it 'should render a simple fetch task for edit' do
      external_fetch_task = FetchPluggableArtifactTask.new(CaseInsensitiveString.new("upstream"), CaseInsensitiveString.new("stage1"), CaseInsensitiveString.new("job"), "docker")
      task = assign(:task, com.thoughtworks.go.config.FetchTaskAdapter.new(external_fetch_task))
      assign(:task_view_model, Spring.bean("taskViewService").getViewModel(task, 'edit'))

      render :template => "admin/tasks/plugin/edit.html.erb"

      assert_external_artifact
    end
  end

  def assert_external_artifact
    Capybara.string(response.body).find('form').tap do |form|
      form.all("div.fieldset").tap do |divs|
        assert_elem(divs[0].find("#task_artifact_type_gocd"), {:title => 'GoCD', :name => 'task[selectedTaskType]', :type => 'radio', :value => 'gocd', :checked => nil})
        expect(divs[0].find("label[for=task_artifact_type_gocd]")).to have_text('GoCD')

        assert_elem(divs[0].find("#task_artifact_type_external"), {:title => 'External', :name => 'task[selectedTaskType]', :type => 'radio', :value => 'external', :checked => 'checked'})
        expect(divs[0].find("label[for=task_artifact_type_external]")).to have_text('External')

        assert_upstream_info divs[0]

        expect(divs[0]).to have_selector("label", :text => "Artifact ID*")
        expect(divs[0]).to have_selector("input[name='task[artifactId]']")
        expect(divs[0]).to have_selector("label", :text => "Plugin ID")
        expect(divs[0]).to have_selector("select[name='FetchArtifact[pluginId]']")
        expect(divs[0]).to have_selector("input[name='task[pluginId]']", :visible => false)
        expect(divs[0]).to have_selector("span[id=fetch_pluggable_artifact_data]", :visible => false)
      end
    end
  end

  def assert_gocd_artifact
    Capybara.string(response.body).find('form').tap do |form|
      form.all("div.fieldset").tap do |divs|
        assert_elem(divs[0].find("#task_artifact_type_gocd"), {:title => 'GoCD', :name => 'task[selectedTaskType]', :type => 'radio', :value => 'gocd', :checked => 'checked'})
        expect(divs[0].find("label[for=task_artifact_type_gocd]")).to have_text('GoCD')

        assert_elem(divs[0].find("#task_artifact_type_external"), {:title => 'External', :name => 'task[selectedTaskType]', :type => 'radio', :value => 'external', :checked => nil})
        expect(divs[0].find("label[for=task_artifact_type_external]")).to have_text('External')

        assert_upstream_info divs[0]

        expect(divs[0]).to have_selector("label", :text => "Source*")
        expect(divs[0]).to have_selector("input[name='task[src]']")
        expect(divs[0]).to have_selector("label", :text => "Destination")
        expect(divs[0]).to have_selector("input[name='task[dest]']")
      end
    end
  end


  def assert_upstream_info elem
    expect(elem).to have_selector("label", :text => "Pipeline")
    expect(elem).to have_selector("input[name='task[pipelineName]']")
    expect(elem).to have_selector("label", :text => "Stage*")
    expect(elem).to have_selector("input[name='task[stage]']")
    expect(elem).to have_selector("label", :text => "Job*")
    expect(elem).to have_selector("input[name='task[job]']")
  end

  def assert_elem(elem, attributes)
    expect(elem).not_to eq(nil)
    attributes.each do |k, v|
      expect(elem[k]).to eq(v), "expected attribute `#{k}` with value `#{v}`, got `#{elem[k]}`"
    end
  end
end
