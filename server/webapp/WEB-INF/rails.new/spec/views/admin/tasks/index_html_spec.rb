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

require File.join(File.dirname(__FILE__), "/../../../spec_helper")

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
    assign(:cruise_config, config = CruiseConfig.new)
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

    response.body.should have_tag("table.list_table") do
      with_tag("tr") do
        with_tag("th", "Order")
        with_tag("th", "Task Type")
        with_tag("th", "Run If Conditions")
        with_tag("th", "Properties")
        with_tag("th", "On Cancel")
        with_tag("th", "Remove")
      end
      with_tag("tr") do
        without_tag("td form[method='post'][action=?]", admin_task_decrement_index_path(:pipeline_name => "foo-pipeline", :stage_name => "bar-stage", :job_name => "baz-job", :task_index => 0))
        with_tag("td form[method='post'][action=?]", admin_task_increment_index_path(:pipeline_name => "foo-pipeline", :stage_name => "bar-stage", :job_name => "baz-job", :task_index => 0)) do
          with_tag("button[title=?]", "Move Down") do
            with_tag("div.promote_down")  
          end
        end
        with_tag("td a[href='#']", "Custom Command")
        with_tag("td.run_ifs", "Passed")
        with_tag("td.properties ul") do
          with_tag("li.command") do
            with_tag("span.name", "Command:")
            with_tag("span.value", "ls")
          end
          with_tag("li.arguments") do
            with_tag("span.name", "Arguments:")
            with_tag("span.value", "-la")
          end
          with_tag("li.working_dir") do
            with_tag("span.name", "Working Directory:")
            with_tag("span.value", "hero/ka/directory")
          end
        end
        with_tag("td.has_on_cancel", "Custom Command")
        assert_has_delete_button_for_task "0"
      end
      with_tag("tr") do
        with_tag("td form[method='post'][action=?]", admin_task_decrement_index_path(:pipeline_name => "foo-pipeline", :stage_name => "bar-stage", :job_name => "baz-job", :task_index => 1)) do
          with_tag("button[title=?]", "Move Up") do
            with_tag("div.promote_up")
          end
        end
        with_tag("td form[method='post'][action=?]", admin_task_increment_index_path(:pipeline_name => "foo-pipeline", :stage_name => "bar-stage", :job_name => "baz-job", :task_index => 1)) do
          with_tag("button[title=?]", "Move Down") do
            with_tag("div.promote_down")
          end
        end
        with_tag("td a[href='#']", "Ant")
        with_tag("td.run_ifs", "Passed")
        with_tag("td.properties ul") do
          with_tag("li.target") do
            with_tag("span.name", "Target:")
            with_tag("span.value", "compile")
          end
          with_tag("li.buildfile") do
            with_tag("span.name", "Build File:")
            with_tag("span.value", "build.xml")
          end
          with_tag("li.workingdirectory") do
            with_tag("span.name", "Working Directory:")
            with_tag("span.value", "default/wd")
          end
        end
        with_tag("td.has_on_cancel", "No")
        assert_has_delete_button_for_task "1"
      end
      with_tag("tr") do
        with_tag("td form[method='post'][action=?]", admin_task_decrement_index_path(:pipeline_name => "foo-pipeline", :stage_name => "bar-stage", :job_name => "baz-job", :task_index => 3)) do
          with_tag("button[title=?]", "Move Up") do
            with_tag("div.promote_up")  
          end
        end
        without_tag("td form[method='post'][action=?]", admin_task_increment_index_path(:pipeline_name => "foo-pipeline", :stage_name => "bar-stage", :job_name => "baz-job", :task_index => 3))
        with_tag("td a[href='#']", "NAnt")
        with_tag("td.run_ifs", "Passed")
        with_tag("td.properties ul") do
          with_tag("li.target") do
            with_tag("span.name", "Target:")
            with_tag("span.value", "compile")
          end
          with_tag("li.buildfile") do
            with_tag("span.name", "Build File:")
            with_tag("span.value", "default.build")
          end
          with_tag("li.workingdirectory") do
            with_tag("span.name", "Working Directory:")
            with_tag("span.value", "default/wd")
          end
        end
        with_tag("td.has_on_cancel", "No")
        assert_has_delete_button_for_task "1"
      end
    end
  end

  describe "Add new task" do

    it "should list all the tasks that can be added" do
      template.should_receive(:admin_task_new_path).with(:type => FetchTask.new.getTaskType())
      template.should_receive(:admin_task_new_path).with(:type => ExecTask.new.getTaskType())
      template.should_receive(:admin_task_new_path).with(:type => RakeTask.new.getTaskType())
      template.should_receive(:admin_task_new_path).with(:type => AntTask.new.getTaskType())
      template.should_receive(:admin_task_new_path).with(:type => NantTask.new.getTaskType())

      render

      response.body.should have_tag("#new_task_popup ul") do
        with_tag("li a[href='#']", "More...")
        with_tag("li a[href='#']", "Rake")
        with_tag("li a[href='#']", "NAnt")
        with_tag("li a[href='#']", "Ant")
        with_tag("li a[href='#']", "Fetch Artifact")
      end
    end

    it "should add a lookup icon next to custom command" do
      template.should_receive(:admin_task_new_path).with(:type => "fetch")
      template.should_receive(:admin_task_new_path).with(:type => "exec")
      template.should_receive(:admin_task_new_path).with(:type => "rake")
      template.should_receive(:admin_task_new_path).with(:type => "ant")
      template.should_receive(:admin_task_new_path).with(:type => "nant")

      template.should_receive(:task_css_class).with("exec").and_return("foo")
      template.should_receive(:task_css_class).with("fetch").and_return("")
      template.should_receive(:task_css_class).with("rake").and_return("")
      template.should_receive(:task_css_class).with("ant").and_return("")
      template.should_receive(:task_css_class).with("nant").and_return("")

      render

      response.body.should have_tag("#new_task_popup ul") do
        with_tag("li a.foo", "More...")
        with_tag("li a[class=?]", "")
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

        fake_task_view_service = mock("task_view_service")
        template.stub(:task_view_service).and_return(fake_task_view_service)

        fake_task_view_service.stub(:getViewModel).with(@task_1, "list-entry").and_return(@tvm_1)
        fake_task_view_service.stub(:getViewModel).with(@task_2, "list-entry").and_return(@tvm_2)
        fake_task_view_service.stub(:getViewModel).with(@task_3, "list-entry").and_return(@tvm_3)
        fake_task_view_service.stub(:getViewModel).with(@builtin_task_1, "list-entry").and_return(@builtin_tvm_1)
      end

      it "should show display value of plugin, and not 'pluggable task'" do
        assign(:tasks, [@task_1])
        @tvm_1.stub(:getTypeForDisplay).and_return("CURL")

        render

        response.body.should have_tag("table.list_table") do
          with_tag("tr") do
            with_tag("td a[href='#']", "CURL")
            with_tag("td.has_on_cancel", "No")
          end
        end
      end

      it "for plugin on-cancel task of a plugin task, it should show display value of plugin, and not 'pluggable task'" do
        @task_1.setCancelTask(@task_2)

        assign(:tasks, [@task_1])
        @tvm_1.stub(:getTypeForDisplay).and_return("CURL")
        @tvm_2.stub(:getTypeForDisplay).and_return("MAVEN")

        render

        response.body.should have_tag("table.list_table") do
          with_tag("tr") do
            with_tag("td.has_on_cancel", "MAVEN")
          end
        end
      end

      it "for missing plugin task, it should add missing class" do
        assign(:tasks, [@task_1, @task_3])
        @tvm_1.stub(:getTypeForDisplay).and_return("CURL")
        @tvm_3.stub(:getTypeForDisplay).and_return("MISSING")

        render

        response.body.should have_tag("table.list_table") do
          with_tag("tr.missing_plugin") do
            with_tag("label.missing_plugin_link[title=?]", "Associated plugin 'MISSING' not found. Please contact the Go admin to install the plugin.")
          end
        end
      end

      it "should have missing plugin class in on cancel task name if respective plugin is missing" do
        @builtin_task_1.setCancelTask(@task_3)

        assign(:tasks, [@builtin_task_1])
        @tvm_3.stub(:getTypeForDisplay).and_return("MISSING")

        render

        response.body.should have_tag('table.list_table') do
          with_tag("td.has_on_cancel") do
            with_tag("label.missing_plugin_link[title=?]", "Associated plugin 'MISSING' not found. Please contact the Go admin to install the plugin.")
          end
        end
      end

      it "should have missing plugin class in on-cancel task if both task & on-cancel task are pluggable task of a missing plugin" do
        @task_3.setCancelTask(@task_3)

        assign(:tasks, [@task_3])
        @tvm_3.stub(:getTypeForDisplay).and_return("MISSING")

        render

        response.body.should have_tag('table.list_table') do
          with_tag("tr.missing_plugin") do
            with_tag("label.missing_plugin_link[title=?]", "Associated plugin 'MISSING' not found. Please contact the Go admin to install the plugin.")
            with_tag("td.has_on_cancel") do
              with_tag("label.missing_plugin_link[title=?]", "Associated plugin 'MISSING' not found. Please contact the Go admin to install the plugin.")
            end
          end
        end
      end

      it "for plugin on-cancel task of a builtin task, it should show display value of plugin, and not 'pluggable task'" do
        @builtin_task_1.setCancelTask(@task_2)

        assign(:tasks, [@builtin_task_1])
        @tvm_2.stub(:getTypeForDisplay).and_return("MAVEN")

        render

        response.body.should have_tag("table.list_table") do
          with_tag("tr") do
            with_tag("td a[href='#']", "Ant")
            with_tag("td.has_on_cancel", "MAVEN")
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

  def assert_has_delete_button_for_task index
    with_tag("td form[action='/admin/pipelines/foo-pipeline/stages/bar-stage/job/baz-job/tasks/#{index}'][method='post']") do
      with_tag("span#trigger_delete_task_#{index}.icon_remove.delete_parent")
      with_tag("input[type='hidden'][name='config_md5'][value='abcd1234']")
      with_tag("input[type='hidden'][name='_method'][value='delete']")
      with_tag("div#warning_prompt[style='display:none;']", /Are you sure you want to delete the task at index '#{index.to_i + 1}' \?/)
    end
  end
end
