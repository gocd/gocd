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

describe "config_view/templates/_job_view.html.erb" do
  include TaskMother

  it "should render job settings" do
    job = JobConfig.new('build_job')
    job.addResource('buildr')
    job.addResource('ruby')
    job.addTask(ant_task)
    render :partial => 'config_view/templates/job_view', :locals => {:scope => {:job => job, :index => 1, :stage_id => 'stage_id', :stage_name => "stage_build"}}
    Capybara.string(response.body).find("#stage_id_job_1").tap do |job|
      job.find(".summary fieldset.job_summary ul").tap do |list|
        list.find("li.field.resources").tap do |resources|
          resources.find("label").tap do |label|
            expect(label).to have_selector("span.key", :text => "Resources")
            expect(label).to have_selector("span.hint", :text => "Agent resources that this job requires to run")
          end
          expect(resources).to have_selector("span.value", :text => "buildr | ruby")
        end
        list.find("li.field.job_timeout").tap do |timeout|
          timeout.find("label").tap do |label|
            expect(label).to have_selector("span.key", :text => "Job Timeout")
            expect(label).to have_selector("span.hint", :text => "If this job is inactive for more than the specified period (in minutes), Go will cancel it.")
          end
          expect(timeout).to have_selector("span.value", :text => "Use default")
        end
        list.find("li.field.run_on_all_agents").tap do |run|
          run.find("label").tap do |label|
            expect(label).to have_selector("span.key", :text => "Run on all agents")
          end
          expect(run).to have_selector("span.value", :text => "No")
        end
        list.find("li.field.run_multiple_instances").tap do |run|
          run.find("label").tap do |label|
            expect(label).to have_selector("span.key", :text => "Run multiple instances")
          end
          expect(run).to have_selector("span.value", :text => "No")
        end
      end
    end
  end

  it "should render resources as none when no resources are configured" do
    job = JobConfig.new('build_job')
    job.addTask(ant_task)
    render :partial => 'config_view/templates/job_view', :locals => {:scope => {:job => job, :index => 1, :stage_id => 'stage_id', :stage_name => "stage_build"}}
    Capybara.string(response.body).find("#stage_id_job_1").tap do |job|
      job.find(".summary fieldset.job_summary ul").tap do |list|
        list.find("li.field.resources").tap do |item|
          item.find("label").tap do |label|
            expect(label).to have_selector("span.key", :text => "Resources")
            expect(label).to have_selector("span.hint", :text => "Agent resources that this job requires to run")
          end
          expect(item).to have_selector("span.value", :text => "None")
        end
      end
    end
  end

  it "should render job timeout it is configured to never timeout" do
    job = JobConfig.new('build_job')
    job.setTimeout('0')
    job.addTask(ant_task)
    render :partial => 'config_view/templates/job_view', :locals => {:scope => {:job => job, :index => 1, :stage_id => 'stage_id', :stage_name => "stage_build"}}
    Capybara.string(response.body).find("#stage_id_job_1").tap do |job|
      job.find(".summary fieldset.job_summary ul").tap do |list|
        list.find("li.field.job_timeout").tap do |item|
          item.find("label").tap do |label|
            expect(label).to have_selector("span.key", :text => "Job Timeout")
            expect(label).to have_selector("span.hint", :text => "If this job is inactive for more than the specified period (in minutes), Go will cancel it.")
          end
          expect(item).to have_selector("span.value", :text => "Never")
        end
      end
    end
  end

  it "should render overridden job timeout" do
    job = JobConfig.new('build_job')
    job.setTimeout('30')
    job.addTask(ant_task)
    render :partial => 'config_view/templates/job_view', :locals => {:scope => {:job => job, :index => 1, :stage_id => 'stage_id', :stage_name => "stage_build"}}
    Capybara.string(response.body).find("#stage_id_job_1").tap do |job|
      job.find(".summary fieldset.job_summary ul").tap do |list|
        list.find("li.field.job_timeout").tap do |item|
          item.find("label").tap do |label|
            expect(label).to have_selector("span.key", :text => "Job Timeout")
            expect(label).to have_selector("span.hint", :text => "If this job is inactive for more than the specified period (in minutes), Go will cancel it.")
          end
          expect(item).to have_selector("span.value", :text => "Cancel after '30' minute(s) of inactivity")
        end
      end
    end
  end

  it "should render when job is configured to run on all agents" do
    job = JobConfig.new('build_job')
    job.setRunOnAllAgents(true)
    job.addTask(ant_task)
    render :partial => 'config_view/templates/job_view', :locals => {:scope => {:job => job, :index => 1, :stage_id => 'stage_id', :stage_name => "stage_build"}}
    Capybara.string(response.body).find("#stage_id_job_1").tap do |job|
      job.find(".summary fieldset.job_summary ul").tap do |list|
        list.find("li.field.run_on_all_agents").tap do |item|
          item.find("label").tap do |label|
            expect(label).to have_selector("span.key", :text => "Run on all agents")
          end
          expect(item).to have_selector("span.value", :text => "Yes")
        end
      end
    end
  end

  it "should render when job is configured to run multiple instances" do
    job = JobConfig.new('build_job')
    job.setRunInstanceCount(2)
    job.addTask(ant_task)
    render :partial => 'config_view/templates/job_view', :locals => {:scope => {:job => job, :index => 1, :stage_id => 'stage_id', :stage_name => "stage_build"}}
    Capybara.string(response.body).find("#stage_id_job_1").tap do |job|
      job.find(".summary fieldset.job_summary ul").tap do |list|
        list.find("li.field.run_multiple_instances").tap do |item|
          item.find("label").tap do |label|
            expect(label).to have_selector("span.key", :text => "Run multiple instances")
          end
          expect(item).to have_selector("span.value", :text => "Yes")
        end
      end
    end
  end

  it "should render tabs to show job details like tasks, artifacts, environment variables and custom tabs" do
    job = JobConfig.new('build_job')
    job.addTask(ant_task)
    render :partial => 'config_view/templates/job_view', :locals => {:scope => {:job => job, :index => 1, :stage_id => 'stage_id', :stage_name => "stage_build"}}
    Capybara.string(response.body).find("#stage_id_job_1").tap do |job|
      job.find("ul.nav.nav-tabs").tap do |list|
        list.find("li.active").tap do |active_item|
          expect(active_item).to have_selector("a[href='#tasks_stage_id_job_1'][data-toggle='tab']", :text => "Tasks")
        end
        list.all("li:not(.active)").tap do |items|
          expect(items[0]).to have_selector("a[href='#artifacts_stage_id_job_1'][data-toggle='tab']", :text => "Artifacts")
          expect(items[1]).to have_selector("a[href='#environment_variables_stage_id_job_1'][data-toggle='tab']", :text => "Environment Variables")
          expect(items[2]).to have_selector("a[href='#custom_tabs_stage_id_job_1'][data-toggle='tab']", :text => "Custom Tabs")
        end
      end
    end
  end

  it "should render artifacts tab for a job" do
    artifact_plans = ArtifactPlans.new()
    artifact_plans.add(ArtifactPlan.new("build-result", "build-output"))
    test_artifact = TestArtifactPlan.new('test-result', 'test-output')
    artifact_plans.add(test_artifact)
    job = JobConfig.new(CaseInsensitiveString.new("jobName"), Resources.new(), artifact_plans)
    job.addTask(ant_task)
    render :partial => 'config_view/templates/job_view', :locals => {:scope => {:job => job, :index => 1, :stage_id => 'stage_id', :stage_name => "stage_build"}}
    Capybara.string(response.body).find("#stage_id_job_1").tap do |job|
      job.find(".tab-content #artifacts_stage_id_job_1.tab-pane table.artifacts.list_table").tap do |table|
        table.find("thead").tap do |head|
          head.find("tr").tap do |row|
            expect(row).to have_selector("th", :text => "Source")
            expect(row).to have_selector("th", :text => "Destination")
            expect(row).to have_selector("th", :text => "Type")
          end
        end
        table.find("tbody").tap do |body|
          body.all("tr").tap do |rows|
            expect(rows[0]).to have_selector("td.name_value_cell", :text => "build-result")
            expect(rows[0]).to have_selector("td.name_value_cell", :text => "build-output")
            expect(rows[0]).to have_selector("td.name_value_cell", :text => "Build Artifact")
            expect(rows[1]).to have_selector("td.name_value_cell", :text => "test-result")
            expect(rows[1]).to have_selector("td.name_value_cell", :text => "test-output")
            expect(rows[1]).to have_selector("td.name_value_cell", :text => "Test Artifact")
          end
        end
      end
    end
  end

  it "should render artifacts tab when not configured for a job" do
    job = JobConfig.new('job1')
    job.addTask(ant_task)
    render :partial => 'config_view/templates/job_view', :locals => {:scope => {:job => job, :index => 1, :stage_id => 'stage_id', :stage_name => "stage_build"}}
    Capybara.string(response.body).find("#stage_id_job_1").tap do |job|
      job.find(".tab-content #artifacts_stage_id_job_1.tab-pane table.artifacts.list_table").tap do |table|
        table.find("thead").tap do |head|
          head.find("tr").tap do |row|
            expect(row).to have_selector("th", :text => "Source")
            expect(row).to have_selector("th", :text => "Destination")
            expect(row).to have_selector("th", :text => "Type")
          end
        end
        table.find("tbody").tap do |body|
          body.find("tr").tap do |row|
            expect(row).to have_selector("td.name_value_cell[align='center'][colspan='3']", :text => "No artifacts have been configured")
          end
        end
      end
    end
  end

  it "should display environment variables for a job" do
    job = JobConfig.new('job1')
    job.addVariable('env1', 'value1')
    job.addVariable('env2', 'value2')
    job.addTask(ant_task)
    render :partial => 'config_view/templates/job_view', :locals => {:scope => {:job => job, :index => 1, :stage_id => 'stage_id', :stage_name => "stage_build"}}
    Capybara.string(response.body).find("#stage_id_job_1").tap do |job|
      job.find(".tab-content #environment_variables_stage_id_job_1.tab-pane table.variables.list_table").tap do |table|
        table.find("thead").tap do |head|
          head.find("tr").tap do |row|
            expect(row).to have_selector("th", :text => "Name")
            expect(row).to have_selector("th", :text => "Value")
          end
        end
        table.find("tbody").tap do |body|
          body.all("tr").tap do |rows|
            expect(rows[0]).to have_selector("td.name_value_cell", :text => "env1")
            expect(rows[0]).to have_selector("td.name_value_cell", :text => "value1")
            expect(rows[1]).to have_selector("td.name_value_cell", :text => "env2")
            expect(rows[1]).to have_selector("td.name_value_cell", :text => "value2")
          end
        end
      end
    end
  end

  it "should display masked value for secure environment variables for a job" do
    job = JobConfig.new('job1')
    environment_variable_config_new = EnvironmentVariableConfig.new("env2", "value2")
    environment_variable_config_new.setIsSecure(true)
    job.setVariables(EnvironmentVariablesConfig.new([EnvironmentVariableConfig.new("env1", "value1"), environment_variable_config_new]))
    job.addTask(ant_task)
    render :partial => 'config_view/templates/job_view', :locals => {:scope => {:job => job, :index => 1, :stage_id => 'stage_id', :stage_name => "stage_build"}}
    Capybara.string(response.body).find("#stage_id_job_1").tap do |job|
      job.find(".tab-content #environment_variables_stage_id_job_1.tab-pane table.variables.list_table").tap do |table|
        table.find("thead").tap do |head|
          head.find("tr").tap do |row|
            expect(row).to have_selector("th", :text => "Name")
            expect(row).to have_selector("th", :text => "Value")
          end
        end
        table.find("tbody").tap do |body|
          body.all("tr").tap do |rows|
            expect(rows[0]).to have_selector("td.name_value_cell", :text => "env1")
            expect(rows[0]).to have_selector("td.name_value_cell", :text => "value1")
            expect(rows[1]).to have_selector("td.name_value_cell", :text => "env2")
            expect(rows[1]).to have_selector("td.name_value_cell", :text => "****")
          end
        end
      end
    end
  end

  it "should render environment variables tab when no variables are configured" do
    job = JobConfig.new('job1')
    job.addTask(ant_task)
    render :partial => 'config_view/templates/job_view', :locals => {:scope => {:job => job, :index => 1, :stage_id => 'stage_id', :stage_name => "stage_build"}}
    Capybara.string(response.body).find("#stage_id_job_1").tap do |job|
      job.find(".tab-content #environment_variables_stage_id_job_1.tab-pane table.variables.list_table").tap do |table|
        table.find("thead").tap do |head|
          head.find("tr").tap do |row|
            expect(row).to have_selector("th", :text => "Name")
            expect(row).to have_selector("th", :text => "Value")
          end
        end
        table.find("tbody").tap do |body|
          body.find("tr").tap do |row|
            expect(row).to have_selector("td.name_value_cell[align='center'][colspan='2']", :text => "No environment variables have been configured")
          end
        end
      end
    end
  end

  it "should display custom tab for a job" do
    job = JobConfig.new('job1')
    job.addTab('test-reports', 'reports/rspec')
    job.addTab('cobertura', 'reports/code-coverage')
    job.addTask(ant_task)
    render :partial => 'config_view/templates/job_view', :locals => {:scope => {:job => job, :index => 1, :stage_id => 'stage_id', :stage_name => "stage_build"}}
    Capybara.string(response.body).find("#stage_id_job_1").tap do |job|
      job.find(".tab-content #custom_tabs_stage_id_job_1.tab-pane table.custom_tabs.list_table").tap do |table|
        table.find("thead").tap do |head|
          head.find("tr").tap do |row|
            expect(row).to have_selector("th", :text => "Tab Name")
            expect(row).to have_selector("th", :text => "Path")
          end
        end
        table.find("tbody").tap do |body|
          body.all("tr").tap do |rows|
            expect(rows[0]).to have_selector("td.name_value_cell", :text => "test-reports")
            expect(rows[0]).to have_selector("td.name_value_cell", :text => "reports/rspec")
            expect(rows[1]).to have_selector("td.name_value_cell", :text => "cobertura")
            expect(rows[1]).to have_selector("td.name_value_cell", :text => "reports/code-coverage")
          end
        end
      end
    end
  end

  it "should render custom tab when no tabs are configured" do
    job = JobConfig.new('job1')
    job.addTask(ant_task)
    render :partial => 'config_view/templates/job_view', :locals => {:scope => {:job => job, :index => 1, :stage_id => 'stage_id', :stage_name => "stage_build"}}
    Capybara.string(response.body).find("#stage_id_job_1").tap do |job|
      job.find(".tab-content #custom_tabs_stage_id_job_1.tab-pane table.custom_tabs.list_table").tap do |table|
        table.find("thead").tap do |head|
          head.find("tr").tap do |row|
            expect(row).to have_selector("th", :text => "Tab Name")
            expect(row).to have_selector("th", :text => "Path")
          end
        end
        table.find("tbody").tap do |body|
          body.find("tr").tap do |row|
            expect(row).to have_selector("td.name_value_cell[align='center'][colspan='2']", :text => "No custom tabs have been configured")
          end
        end
      end
    end
  end

  describe "render tasks" do

    it "should display tasks of a job" do
      job = JobConfig.new('job1')
      job.addTask(ant_task)
      job.addTask(simple_exec_task)
      render :partial => 'config_view/templates/job_view', :locals => {:scope => {:job => job, :index => 1, :stage_id => 'stage_id', :stage_name => "stage_build"}}
      Capybara.string(response.body).find("#stage_id_job_1").tap do |job|
        job.find(".tab-content #tasks_stage_id_job_1").tap do |tasks|
          tasks.find("ul.tasks_view_list").tap do |list|
            list.all("li").tap do |items|
              items[0].find("code").tap do |code|
                expect(code).to have_selector("span.working_dir", :text => "default/wd$")
                expect(code).to have_selector("span.command", :text => "ant")
                expect(code).to have_selector("span.arguments", :text => "-f \"build.xml\" compile")
              end
              expect(items[0]).to have_selector("span.condition", :text => "Run if Passed")
              expect(items[0]).to_not have_selector("ul li span.on_cancel")
              items[1].find("code").tap do |code|
                expect(code).to have_selector("span.working_dir", :text => "hero/ka/directory$")
                expect(code).to have_selector("span.command", :text => "ls")
                expect(code).to have_selector("span.arguments", :text => "-la")
              end
              expect(items[1]).to have_selector("span.condition", :text => "Run if Passed")
            end
          end
        end
      end
    end

    it "should display fetch artifact task of a job" do
      job = JobConfig.new('job1')
      job.addTask(fetch_task_with_exec_on_cancel_task)
      render :partial => 'config_view/templates/job_view', :locals => {:scope => {:job => job, :index => 1, :stage_id => 'stage_id', :stage_name => "stage_build"}}
      Capybara.string(response.body).find("#stage_id_job_1").tap do |job|
        job.find(".tab-content #tasks_stage_id_job_1 ul.tasks_view_list").tap do |list|
          list.all("li.fetch code").tap do |items|
            item = items[0]
            expect(item).to have_selector("span", :text => "Fetch Artifact -")
            expect(item).to have_selector("span[title='Pipeline Name']", :text => "pipeline")
            expect(item).to have_selector("span.path_separator", :text => ">")
            expect(item).to have_selector("span[title='Stage Name']", :text => "stage")
            expect(item).to have_selector("span.path_separator", :text => ">")
            expect(item).to have_selector("span[title='Job Name']", :text => "job")
            expect(item).to have_selector("span.delimiter", :text => ":")
            expect(item).to have_selector("span[title='Source']", :text => "src")
            expect(item).to have_selector("span.direction_arrow", :text => "->")
            expect(item).to have_selector("span[title='Destination']", :text => "dest")
          end
        end
      end
    end

    it "should display fetch artifact task of a job when it fetches from same pipeline" do
      job = JobConfig.new('job1')
      task = fetch_task_with_exec_on_cancel_task(nil)
      job.addTask(task)
      render :partial => 'config_view/templates/job_view', :locals => {:scope => {:job => job, :index => 1, :stage_id => 'stage_id', :stage_name => "stage_build"}}
      Capybara.string(response.body).find("#stage_id_job_1").tap do |job|
        job.find(".tab-content #tasks_stage_id_job_1 ul.tasks_view_list").tap do |list|
          list.all("li.fetch code").tap do |items|
            item = items[0]
            expect(item).to have_selector("span", :text => "Fetch Artifact -")
            expect(item).to have_selector("span[title='Pipeline Name']", :text => "Current pipeline")
            expect(item).to have_selector("span.path_separator", :text => ">")
            expect(item).to have_selector("span[title='Stage Name']", :text => "stage")
            expect(item).to have_selector("span.path_separator", :text => ">")
            expect(item).to have_selector("span[title='Job Name']", :text => "job")
            expect(item).to have_selector("span.delimiter", :text => ":")
            expect(item).to have_selector("span[title='Source']", :text => "src")
            expect(item).to have_selector("span.direction_arrow", :text => "->")
            expect(item).to have_selector("span[title='Destination']", :text => "dest")
          end
        end
      end
    end

    it "should display multiple run if conditions" do
      job = JobConfig.new('job1')
      task = simple_exec_task
      task.getConditions().add(com.thoughtworks.go.config.RunIfConfig::PASSED)
      task.getConditions().add(com.thoughtworks.go.config.RunIfConfig::FAILED)
      job.addTask(task)
      render :partial => 'config_view/templates/job_view', :locals => {:scope => {:job => job, :index => 1, :stage_id => 'stage_id', :stage_name => "stage_build"}}
      Capybara.string(response.body).find("#stage_id_job_1").tap do |job|
        job.find(".tab-content #tasks_stage_id_job_1 ul").tap do |list|
          list.find("li").tap do |item|
            expect(item).to have_selector("span.condition", :text => "Run if Passed, Failed")
          end
        end
      end
    end

    it "should display as run if Passed or failed when run if condition is any" do
      job = JobConfig.new('job1')
      task = simple_exec_task
      task.getConditions().add(com.thoughtworks.go.config.RunIfConfig::ANY)
      job.addTask(task)
      render :partial => 'config_view/templates/job_view', :locals => {:scope => {:job => job, :index => 1, :stage_id => 'stage_id', :stage_name => "stage_build"}}
      Capybara.string(response.body).find("#stage_id_job_1").tap do |job|
        job.find(".tab-content #tasks_stage_id_job_1 ul").tap do |list|
          list.find("li").tap do |item|
            expect(item).to have_selector("span.condition", :text => "Run if Failed, Passed")
          end
        end
      end
    end

    it "should display on cancel task details of a task" do
      job = JobConfig.new('job1')
      task = ant_task
      task.setCancelTask(nant_task)
      job.addTask(task)
      render :partial => 'config_view/templates/job_view', :locals => {:scope => {:job => job, :index => 1, :stage_id => 'stage_id', :stage_name => "stage_build"}}
      Capybara.string(response.body).find("#stage_id_job_1").tap do |job|
        job.find(".tab-content #tasks_stage_id_job_1 ul.tasks_view_list").tap do |list|
          list.find("li.ant").tap do |item|
            expect(item).to have_selector("span.condition", :text => "Run if Passed")
            item.all("code").tap do |codes|
              code = codes[0]
              expect(code).to have_selector("span.working_dir", :text => "default/wd$")
              expect(code).to have_selector("span.command", :text => "ant")
              expect(code).to have_selector("span.arguments", :text => "-f \"build.xml\" compile")
            end
            item.find("ul").tap do |list2|
              list2.find("li").tap do |item2|
                item2.find("code").tap do |code2|
                  expect(code2).to have_selector("span.on_cancel", :text => "On Cancel")
                  expect(code2).to have_selector("span.working_dir", :text => "default/wd$")
                  expect(code2).to have_selector("span.command", :text => "nant")
                  expect(code2).to have_selector("span.arguments", :text => "-buildfile:\"default.build\" compile")
                end
              end
            end
          end
        end
      end
    end

    it "should display message saying that no tasks have been configured when there are none" do
      job = JobConfig.new('job1')
      render :partial => 'config_view/templates/job_view', :locals => {:scope => {:job => job, :index => 1, :stage_id => 'stage_id', :stage_name => "stage_build"}}
      Capybara.string(response.body).find("#stage_id_job_1").tap do |job|
        job.find(".tab-content #tasks_stage_id_job_1").tap do |tasks|
          expect(tasks).to have_selector("span", :text => "No tasks have been configured")
        end
      end
    end

  end
end
