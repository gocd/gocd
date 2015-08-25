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

describe "admin/tasks/index.html.erb" do
  include GoUtil, TaskMother, FormUI

  before(:each) do
    @pipeline = PipelineConfigMother.createPipelineConfig("pipeline.name", "stage.name", ["job.1", "job.2", "job.3"].to_java(java.lang.String))
    stage = @pipeline.get(0)
    job = stage.getJobs().get(0)
    tasks = job.getTasks()
    tasks.add(exec_task)
    tasks.add(ant_task)
    tasks.add(rake_task)
    tasks.add(nant_task)
    assign(:cruise_config, config = BasicCruiseConfig.new)
    set(config, "md5", "abcd1234")

    assign(:pipeline, @pipeline)
    assign(:stage, stage)
    assign(:job, job)
    assign(:tasks, tasks)
    in_params(:pipeline_name => "foo-pipeline", :stage_name => "bar-stage", :job_name => "baz-job", :action => "index", :controller => "admin/tasks", :stage_parent => "pipelines")
    assign(:task_view_models, [tvm(ExecTask.new), tvm(RakeTask.new), tvm(AntTask.new),tvm(FetchTask.new), tvm(NantTask.new)])
  end

  it "should show tasks" do
    render

    Capybara.string(response.body).find('table.list_table').tap do |table|
      table.all("tr").tap do |trs|
        expect(trs[0]).to have_selector("th", :text => "Order")
        expect(trs[0]).to have_selector("th", :text => "Task Type")
        expect(trs[0]).to have_selector("th", :text => "Run If Conditions")
        expect(trs[0]).to have_selector("th", :text => "Properties")
        expect(trs[0]).to have_selector("th", :text => "On Cancel")
        expect(trs[0]).to have_selector("th", :text => "Remove")
      end
      table.all("tr").tap do |trs|
        expect(trs[1]).not_to have_selector("td form[method='post'][action='#{admin_task_decrement_index_path(:pipeline_name => "foo-pipeline", :stage_name => "bar-stage", :job_name => "baz-job", :task_index => 0)}']")
        trs[1].find("td form[method='post'][action='#{admin_task_increment_index_path(:pipeline_name => "foo-pipeline", :stage_name => "bar-stage", :job_name => "baz-job", :task_index => 0)}']") do |form|
          form.find("button[title='Move Down']") do |button|
            expect(button).to have_selector("div.promote_down")
          end
        end
        expect(trs[1]).to have_selector("td a[href='#']", :text => "Custom Command")
        expect(trs[1]).to have_selector("td.run_ifs", :text => "Passed")
        trs[1].find("td.properties ul") do |ul|
          ul.find("li.command") do |li|
            expect(li).to have_selector("span.name", :text => "Command:")
            expect(li).to have_selector("span.value", :text => "ls")
          end
          ul.find("li.arguments") do |li|
            expect(li).to have_selector("span.name", :text => "Arguments:")
            expect(li).to have_selector("span.value", :text => "-la")
          end
          ul.find("li.working_dir") do |li|
            expect(li).to have_selector("span.name", :text => "Working Directory:")
            expect(li).to have_selector("span.value", :text => "hero/ka/directory")
          end
        end
        expect(table).to have_selector("td.has_on_cancel", :text => "Custom Command")

        assert_has_delete_button_for_task trs[1], "0"
      end
      table.all("tr").tap do |trs|
        trs[2].find("td form[method='post'][action='#{admin_task_decrement_index_path(:pipeline_name => "foo-pipeline", :stage_name => "bar-stage", :job_name => "baz-job", :task_index => 1)}']") do |form|
          form.find("button[title='Move Up']") do |button|
            expect(button).to have_selector("div.promote_up")
          end
        end
        trs[2].find("td form[method='post'][action='#{admin_task_increment_index_path(:pipeline_name => "foo-pipeline", :stage_name => "bar-stage", :job_name => "baz-job", :task_index => 1)}']") do |form|
          form.find("button[title='Move Down']") do |button|
            expect(button).to have_selector("div.promote_down")
          end
        end
        expect(trs[2]).to have_selector("td a[href='#']", :text => "Ant")
        expect(trs[2]).to have_selector("td.run_ifs", :text => "Passed")
        trs[2].find("td.properties ul") do |ul|
          ul.find("li.target") do |li|
            expect(li).to have_selector("span.name", :text => "Target:")
            expect(li).to have_selector("span.value", :text => "compile")
          end
          ul.find("li.buildfile") do |li|
            expect(li).to have_selector("span.name", :text => "Build File:")
            expect(li).to have_selector("span.value", :text => "build.xml")
          end
          ul.find("li.workingdirectory") do |li|
            expect(li).to have_selector("span.name", :text => "Working Directory:")
            expect(li).to have_selector("span.value", :text => "default/wd")
          end
        end
        expect(trs[2]).to have_selector("td.has_on_cancel", :text => "No")

        assert_has_delete_button_for_task trs[2], "1"
      end
      table.all("tr").tap do |trs|
        trs[4].find("td form[method='post'][action='#{admin_task_decrement_index_path(:pipeline_name => "foo-pipeline", :stage_name => "bar-stage", :job_name => "baz-job", :task_index => 3)}']") do |form|
          form.find("button[title='Move Up']") do |button|
            expect(button).to have_selector("div.promote_up")
          end
        end
        expect(trs[4]).not_to have_selector("td form[method='post'][action='#{admin_task_increment_index_path(:pipeline_name => "foo-pipeline", :stage_name => "bar-stage", :job_name => "baz-job", :task_index => 3)}']")
        expect(trs[4]).to have_selector("td a[href='#']", :text => "NAnt")
        expect(trs[4]).to have_selector("td.run_ifs", :text => "Passed")
        trs[4].find("td.properties ul") do |ul|
          ul.find("li.target") do |li|
            expect(li).to have_selector("span.name", :text => "Target:")
            expect(li).to have_selector("span.value", :text => "compile")
          end
          ul.find("li.buildfile") do |li|
            expect(li).to have_selector("span.name", :text => "Build File:")
            expect(li).to have_selector("span.value", :text => "default.build")
          end
          ul.find("li.workingdirectory") do |li|
            expect(li).to have_selector("span.name", :text => "Working Directory:")
            expect(li).to have_selector("span.value", :text => "default/wd")
          end
        end
        expect(trs[4]).to have_selector("td.has_on_cancel", :text => "No")

        assert_has_delete_button_for_task trs[4], "3"
      end
    end
  end

  describe "Add new task" do

    it "should list all the tasks that can be added" do
      view.should_receive(:admin_task_new_path).with(:type => FetchTask.new.getTaskType())
      view.should_receive(:admin_task_new_path).with(:type => ExecTask.new.getTaskType())
      view.should_receive(:admin_task_new_path).with(:type => RakeTask.new.getTaskType())
      view.should_receive(:admin_task_new_path).with(:type => AntTask.new.getTaskType())
      view.should_receive(:admin_task_new_path).with(:type => NantTask.new.getTaskType())

      render

      Capybara.string(response.body).find('#new_task_popup ul').tap do |ul|
        expect(ul).to have_selector("li a[href='#']", :text => "More...")
        expect(ul).to have_selector("li a[href='#']", :text => "Rake")
        expect(ul).to have_selector("li a[href='#']", :text => "NAnt")
        expect(ul).to have_selector("li a[href='#']", :text => "Ant")
        expect(ul).to have_selector("li a[href='#']", :text => "Fetch Artifact")
      end
    end

    it "should add a lookup icon next to custom command" do
      view.should_receive(:admin_task_new_path).with(:type => "fetch")
      view.should_receive(:admin_task_new_path).with(:type => "exec")
      view.should_receive(:admin_task_new_path).with(:type => "rake")
      view.should_receive(:admin_task_new_path).with(:type => "ant")
      view.should_receive(:admin_task_new_path).with(:type => "nant")

      view.should_receive(:task_css_class).with("exec").and_return("foo")
      view.should_receive(:task_css_class).with("fetch").and_return("")
      view.should_receive(:task_css_class).with("rake").and_return("")
      view.should_receive(:task_css_class).with("ant").and_return("")
      view.should_receive(:task_css_class).with("nant").and_return("")

      render

      Capybara.string(response.body).find('#new_task_popup ul').tap do |ul|
        expect(ul).to have_selector("li a.foo", :text => "More...")
        expect(ul).to have_selector("li a[class='']")
      end
    end
  end

  describe "with plugin tasks" do
    include GoUtil, TaskMother, FormUI

    describe "show tasks" do
      before(:each) do
        @task_1 = plugin_task "curl.plugin", [ConfigurationPropertyMother.create("KEY1"), ConfigurationPropertyMother.create("key2")]
        @task_2 = plugin_task "maven.plugin", [ConfigurationPropertyMother.create("KEY3"), ConfigurationPropertyMother.create("key4")]
        @task_3 = plugin_task "missing.plugin", [ConfigurationPropertyMother.create("KEY5"), ConfigurationPropertyMother.create("key6")]
        @builtin_task_1 = ant_task
        @tvm_1 = pluggable_tvm_for(@task_1, "list-entry")
        @tvm_2 = pluggable_tvm_for(@task_2, "list-entry")
        @tvm_3 = pluggable_tvm_for_missing(@task_3)
        @builtin_tvm_1 = tvm_for_list_entry(@builtin_task_1)

        fake_task_view_service = double("task_view_service")
        view.stub(:task_view_service).and_return(fake_task_view_service)

        fake_task_view_service.stub(:getViewModel).with(@task_1, "list-entry").and_return(@tvm_1)
        fake_task_view_service.stub(:getViewModel).with(@task_2, "list-entry").and_return(@tvm_2)
        fake_task_view_service.stub(:getViewModel).with(@task_3, "list-entry").and_return(@tvm_3)
        fake_task_view_service.stub(:getViewModel).with(@builtin_task_1, "list-entry").and_return(@builtin_tvm_1)
      end

      it "should show display value of plugin, and not 'pluggable task'" do
        assign(:tasks, [@task_1])
        @tvm_1.stub(:getTypeForDisplay).and_return("CURL")

        render

        Capybara.string(response.body).find('table.list_table').tap do |table|
          table.all("tr").tap do |trs|
            expect(trs[1]).to have_selector("td a[href='#']", :text => "CURL")
            expect(trs[1]).to have_selector("td.has_on_cancel", :text => "No")
          end
        end
      end

      it "for plugin on-cancel task of a plugin task, it should show display value of plugin, and not 'pluggable task'" do
        @task_1.setCancelTask(@task_2)

        assign(:tasks, [@task_1])
        @tvm_1.stub(:getTypeForDisplay).and_return("CURL")
        @tvm_2.stub(:getTypeForDisplay).and_return("MAVEN")

        render

        Capybara.string(response.body).find('table.list_table').tap do |table|
          table.all("tr").tap do |trs|
            expect(trs[1]).to have_selector("td.has_on_cancel", :text => "MAVEN")
          end
        end
      end

      it "for missing plugin task, it should add missing class" do
        assign(:tasks, [@task_1, @task_3])
        @tvm_1.stub(:getTypeForDisplay).and_return("CURL")
        @tvm_3.stub(:getTypeForDisplay).and_return("MISSING")

        render

        Capybara.string(response.body).find('table.list_table').tap do |table|
          table.find("tr.missing_plugin").tap do |tr|
            expect(tr.find("label.missing_plugin_link")['title']).to eq("Associated plugin 'MISSING' not found. Please contact the Go admin to install the plugin.")
          end
        end
      end

      it "should have missing plugin class in on cancel task name if respective plugin is missing" do
        @builtin_task_1.setCancelTask(@task_3)

        assign(:tasks, [@builtin_task_1])
        @tvm_3.stub(:getTypeForDisplay).and_return("MISSING")

        render

        Capybara.string(response.body).find('table.list_table').tap do |table|
          table.find("td.has_on_cancel") do |td|
            expect(td.find("label.missing_plugin_link")['title']).to eq("Associated plugin &#39;MISSING&#39; not found. Please contact the Go admin to install the plugin.")
          end
        end
      end

      it "should have missing plugin class in on-cancel task if both task & on-cancel task are pluggable task of a missing plugin" do
        @task_3.setCancelTask(@task_3)

        assign(:tasks, [@task_3])
        @tvm_3.stub(:getTypeForDisplay).and_return("MISSING")

        render

        Capybara.string(response.body).find('table.list_table').tap do |table|
          table.find("tr.missing_plugin").tap do |tr|
            expect(tr.all("label.missing_plugin_link")[0]['title']).to eq("Associated plugin 'MISSING' not found. Please contact the Go admin to install the plugin.")
            tr.find("td.has_on_cancel") do |td|
              expect(td.find("label.missing_plugin_link")['title']).to eq("Associated plugin 'MISSING' not found. Please contact the Go admin to install the plugin.")
            end
          end
        end
      end

      it "for plugin on-cancel task of a builtin task, it should show display value of plugin, and not 'pluggable task'" do
        @builtin_task_1.setCancelTask(@task_2)

        assign(:tasks, [@builtin_task_1])
        @tvm_2.stub(:getTypeForDisplay).and_return("MAVEN")

        render

        Capybara.string(response.body).find('table.list_table').tap do |table|
          table.all("tr").tap do |trs|
            expect(trs[1]).to have_selector("td a[href='#']", :text => "Ant")
            expect(trs[1]).to have_selector("td.has_on_cancel", :text => "MAVEN")
          end
        end
      end
    end
  end

  def tvm(task)
    TaskViewModel.new(task, "new", "erb")
  end

  def tvm_for_list_entry(task)
    TaskViewModel.new(task, "admin/tasks/plugin/_task_entry_value_fields.html", "erb")
  end

  def pluggable_tvm_for(task, display_value)
    PluggableTaskViewModel.new task, "admin/tasks/pluggable_task/_list_entry.html", com.thoughtworks.go.plugins.presentation.Renderer::ERB, display_value, "Curl - Template"
  end

  def pluggable_tvm_for_missing(task)
    MissingPluggableTaskViewModel.new task, "admin/tasks/pluggable_task/_list_entry.html", com.thoughtworks.go.plugins.presentation.Renderer::ERB
  end

  def assert_has_delete_button_for_task tr, index
    tr.find("td form[action='/admin/pipelines/foo-pipeline/stages/bar-stage/job/baz-job/tasks/#{index}'][method='post']") do |form|
      expect(form).to have_selector("span#trigger_delete_task_#{index}.icon_remove.delete_parent")
      expect(form).to have_selector("input[type='hidden'][name='config_md5'][value='abcd1234']")
      expect(form).to have_selector("input[type='hidden'][name='_method'][value='delete']")
      expect(form).to have_selector("div#warning_prompt[style='display:none;']", :text => /Are you sure you want to delete the task at index '#{index.to_i + 1}' \?/)
    end
  end
end
