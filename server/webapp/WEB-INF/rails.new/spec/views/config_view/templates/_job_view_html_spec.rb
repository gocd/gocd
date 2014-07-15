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

describe "Showing job level details while viewing templates" do
  include TaskMother

  it "should render job settings" do
    job = JobConfig.new('build_job')
    job.addResource('buildr')
    job.addResource('ruby')
    job.addTask(ant_task)
    render :partial => 'config_view/templates/job_view', :locals => {:scope => {:job => job, :index => 1, :stage_id => 'stage_id', :stage_name => "stage_build"}}

    response.body.should have_tag("#stage_id_job_1") do
      with_tag(".summary") do
        with_tag("fieldset.job_summary ul") do
          with_tag("li.field.resources") do
            with_tag("label") do
              with_tag("span.key", "Resources")
              with_tag("span.hint", "Agent resources that this job requires to run")
            end
            with_tag("span.value", "buildr | ruby")
          end
          with_tag("li.field.job_timeout") do
            with_tag("label") do
              with_tag("span.key", "Job Timeout")
              with_tag("span.hint", "If this job is inactive for more than the specified period (in minutes), Go will cancel it.")
            end
            with_tag("span.value", "Use default")
          end
          with_tag("li.field.run_on_all_agents") do
            with_tag("label") do
              with_tag("span.key", "Run on all agents")
            end
            with_tag("span.value", "No")
          end
        end
      end
    end
  end

  it "should render resources as none when no resources are configured" do
    job = JobConfig.new('build_job')
    job.addTask(ant_task)

    render :partial => 'config_view/templates/job_view', :locals => {:scope => {:job => job, :index => 1, :stage_id => 'stage_id', :stage_name => "stage_build"}}

    response.body.should have_tag("#stage_id_job_1") do
      with_tag(".summary") do
        with_tag("fieldset.job_summary ul") do
          with_tag("li.field.resources") do
            with_tag("label") do
              with_tag("span.key", "Resources")
              with_tag("span.hint", "Agent resources that this job requires to run")
            end
            with_tag("span.value", "None")
          end
        end
      end
    end
  end

  it "should render job timeout it is configured to never timeout" do
    job = JobConfig.new('build_job')
    job.setTimeout('0')
    job.addTask(ant_task)
    render :partial => 'config_view/templates/job_view', :locals => {:scope => {:job => job, :index => 1, :stage_id => 'stage_id', :stage_name => "stage_build"}}

    response.body.should have_tag("#stage_id_job_1") do
      with_tag(".summary") do
        with_tag("fieldset.job_summary ul") do
          with_tag("li.field.job_timeout") do
            with_tag("label") do
              with_tag("span.key", "Job Timeout")
              with_tag("span.hint", "If this job is inactive for more than the specified period (in minutes), Go will cancel it.")
            end
            with_tag("span.value", "Never")
          end
        end
      end
    end
  end

  it "should render overridden job timeout" do
    job = JobConfig.new('build_job')
    job.setTimeout('30')
    job.addTask(ant_task)
    render :partial => 'config_view/templates/job_view', :locals => {:scope => {:job => job, :index => 1, :stage_id => 'stage_id', :stage_name => "stage_build"}}

    response.body.should have_tag("#stage_id_job_1") do
      with_tag(".summary") do
        with_tag("fieldset.job_summary ul") do
          with_tag("li.field.job_timeout") do
            with_tag("label") do
              with_tag("span.key", "Job Timeout")
              with_tag("span.hint", "If this job is inactive for more than the specified period (in minutes), Go will cancel it.")
            end
            with_tag("span.value", "Cancel after '30' minute(s) of inactivity")
          end
        end
      end
    end
  end

  it "should render when job is configured to run on all agents" do
    job = JobConfig.new('build_job')
    job.setRunOnAllAgents(true)
    job.addTask(ant_task)
    render :partial => 'config_view/templates/job_view', :locals => {:scope => {:job => job, :index => 1, :stage_id => 'stage_id', :stage_name => "stage_build"}}

    response.body.should have_tag("#stage_id_job_1") do
      with_tag(".summary") do
        with_tag("fieldset.job_summary ul") do
          with_tag("li.field.run_on_all_agents") do
            with_tag("label") do
              with_tag("span.key", "Run on all agents")
            end
            with_tag("span.value", "Yes")
          end
        end
      end
    end
  end

  it "should render tabs to show job details like tasks, artifacts, environment variables and custom tabs" do
    job = JobConfig.new('build_job')
    job.addTask(ant_task)
    render :partial => 'config_view/templates/job_view', :locals => {:scope => {:job => job, :index => 1, :stage_id => 'stage_id', :stage_name => "stage_build"}}

    response.body.should have_tag("#stage_id_job_1") do
      with_tag("ul.nav.nav-tabs") do
        with_tag("li.active") do
          with_tag("a[href='#tasks_stage_id_job_1'][data-toggle='tab']", "Tasks")
        end
        with_tag("li") do
          with_tag("a[href='#artifacts_stage_id_job_1'][data-toggle='tab']", "Artifacts")
        end
        with_tag("li") do
          with_tag("a[href='#environment_variables_stage_id_job_1'][data-toggle='tab']", "Environment Variables")
        end
        with_tag("li") do
          with_tag("a[href='#custom_tabs_stage_id_job_1'][data-toggle='tab']", "Custom Tabs")
        end
      end
    end
  end

  it "should render artifacts tab for a job" do
    artifact_plans = ArtifactPlans.new()
    artifact_plans.add(ArtifactPlan.new(ArtifactType.unit, "build-result", "build-output"))
    test_artifact = TestArtifactPlan.new()
    test_artifact.setSrc('test-result')
    test_artifact.setDest('test-output')
    artifact_plans.add(test_artifact)
    job = JobConfig.new(CaseInsensitiveString.new("jobName"), Resources.new(), artifact_plans)
    job.addTask(ant_task)

    render :partial => 'config_view/templates/job_view', :locals => {:scope => {:job => job, :index => 1, :stage_id => 'stage_id', :stage_name => "stage_build"}}

    response.body.should have_tag("#stage_id_job_1") do
      with_tag(".tab-content") do
        with_tag("#artifacts_stage_id_job_1.tab-pane") do
          with_tag("table.artifacts.list_table") do
            with_tag("thead") do
              with_tag("tr") do
                with_tag("th", "Source")
                with_tag("th", "Destination")
                with_tag("th", "Type")
              end
            end
            with_tag("tbody") do
              with_tag("tr") do
                with_tag("td.name_value_cell", "build-result")
                with_tag("td.name_value_cell", "build-output")
                with_tag("td.name_value_cell", "Build Artifact")
              end
              with_tag("tr") do
                with_tag("td.name_value_cell", "test-result")
                with_tag("td.name_value_cell", "test-output")
                with_tag("td.name_value_cell", "Test Artifact")
              end
            end
          end
        end
      end
    end

  end

  it "should render artifacts tab when not configured for a job" do
    job = JobConfig.new('job1')
    job.addTask(ant_task)
    render :partial => 'config_view/templates/job_view', :locals => {:scope => {:job => job, :index => 1, :stage_id => 'stage_id', :stage_name => "stage_build"}}

    response.body.should have_tag("#stage_id_job_1") do
      with_tag(".tab-content") do
        with_tag("#artifacts_stage_id_job_1.tab-pane") do
          with_tag("table.artifacts.list_table") do
            with_tag("thead") do
              with_tag("tr") do
                with_tag("th", "Source")
                with_tag("th", "Destination")
                with_tag("th", "Type")
              end
            end
            with_tag("tbody") do
              with_tag("tr") do
                with_tag("td.name_value_cell[align='center'][colspan='3']", "No artifacts have been configured")
              end
            end
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

    response.body.should have_tag("#stage_id_job_1") do
      with_tag(".tab-content") do
        with_tag("#environment_variables_stage_id_job_1.tab-pane") do
          with_tag("table.variables.list_table") do
            with_tag("thead") do
              with_tag("tr") do
                with_tag("th", "Name")
                with_tag("th", "Value")
              end
            end
            with_tag("tbody") do
              with_tag("tr") do
                with_tag("td.name_value_cell", "env1")
                with_tag("td.name_value_cell", "value1")
              end
              with_tag("tr") do
                with_tag("td.name_value_cell", "env2")
                with_tag("td.name_value_cell", "value2")
              end
            end
          end
        end
      end
    end
  end

  it "should render environment variables tab when no variables are configured" do
    job = JobConfig.new('job1')
    job.addTask(ant_task)
    render :partial => 'config_view/templates/job_view', :locals => {:scope => {:job => job, :index => 1, :stage_id => 'stage_id', :stage_name => "stage_build"}}

    response.body.should have_tag("#stage_id_job_1") do
      with_tag(".tab-content") do
        with_tag("#environment_variables_stage_id_job_1.tab-pane") do
          with_tag("table.variables.list_table") do
            with_tag("thead") do
              with_tag("tr") do
                with_tag("th", "Name")
                with_tag("th", "Value")
              end
            end
            with_tag("tbody") do
              with_tag("tr") do
                with_tag("td.name_value_cell[align='center'][colspan='2']", "No environment variables have been configured")
              end
            end
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

    response.body.should have_tag("#stage_id_job_1") do
      with_tag(".tab-content") do
        with_tag("#custom_tabs_stage_id_job_1.tab-pane") do
          with_tag("table.custom_tabs.list_table") do
            with_tag("thead") do
              with_tag("tr") do
                with_tag("th", "Tab Name")
                with_tag("th", "Path")
              end
            end
            with_tag("tbody") do
              with_tag("tr") do
                with_tag("td.name_value_cell", "test-reports")
                with_tag("td.name_value_cell", "reports/rspec")
              end
              with_tag("tr") do
                with_tag("td.name_value_cell", "cobertura")
                with_tag("td.name_value_cell", "reports/code-coverage")
              end
            end
          end
        end
      end
    end
  end

  it "should render custom tab when no tabs are configured" do
    job = JobConfig.new('job1')
    job.addTask(ant_task)
    render :partial => 'config_view/templates/job_view', :locals => {:scope => {:job => job, :index => 1, :stage_id => 'stage_id', :stage_name => "stage_build"}}

    response.body.should have_tag("#stage_id_job_1") do
      with_tag(".tab-content") do
        with_tag("#custom_tabs_stage_id_job_1.tab-pane") do
          with_tag("table.custom_tabs.list_table") do
            with_tag("thead") do
              with_tag("tr") do
                with_tag("th", "Tab Name")
                with_tag("th", "Path")
              end
            end
            with_tag("tbody") do
              with_tag("tr") do
                with_tag("td.name_value_cell[align='center'][colspan='2']", "No custom tabs have been configured")
              end
            end
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

      response.body.should have_tag('#stage_id_job_1') do
        with_tag(".tab-content") do
          with_tag('#tasks_stage_id_job_1') do
            with_tag('ul') do
              with_tag('li') do
                with_tag('span') do
                  with_tag('span.working_dir', 'default/wd$')
                  with_tag('span.command', 'ant')
                  with_tag('span.arguments', '-f "build.xml" compile')
                end
                with_tag('span.condition', 'Run if Passed')
                without_tag('ul li span.on_cancel')
              end
              with_tag('li') do
                with_tag('span.working_dir', 'hero/ka/directory$')
                with_tag('span.command', 'ls')
                with_tag('span.arguments', '-la')
              end
            end
          end
        end
      end
    end

    it "should display fetch artifact task of a job" do
      job = JobConfig.new('job1')
      job.addTask(fetch_task)
      render :partial => 'config_view/templates/job_view', :locals => {:scope => {:job => job, :index => 1, :stage_id => 'stage_id', :stage_name => "stage_build"}}
      response.body.should have_tag('#stage_id_job_1') do
        with_tag(".tab-content") do
          with_tag('#tasks_stage_id_job_1') do
            with_tag('ul') do
              with_tag('li.fetch') do
                with_tag('span', "#{"Fetch Artifact"} -")

                with_tag('span') do
                  with_tag("span[title=#{"Pipeline Name"}]", "pipeline")
                  with_tag("span.path_separator", ">")
                  with_tag("span[title=#{"Stage Name"}]", "stage")
                  with_tag("span.path_separator", ">")
                  with_tag("span[title=#{"Job Name"}]", "job")
                  with_tag("span.delimiter", ':')
                  with_tag("span[title=#{"Source"}]", "src")
                  with_tag("span.direction_arrow", "->")
                  with_tag("span[title=#{"Destination"}]", "dest")
                end
              end
            end
          end
        end
      end
    end

    it "should display fetch artifact task of a job when it fetches from same pipeline" do
      job = JobConfig.new('job1')
      task = fetch_task(nil)
      job.addTask(task)
      render :partial => 'config_view/templates/job_view', :locals => {:scope => {:job => job, :index => 1, :stage_id => 'stage_id', :stage_name => "stage_build"}}

      response.body.should have_tag('#stage_id_job_1') do
        with_tag(".tab-content") do
          with_tag('#tasks_stage_id_job_1') do
            with_tag('ul') do
              with_tag('li.fetch') do
                with_tag('span', "#{"Fetch Artifact"} -")

                with_tag('span') do
                  with_tag("span[title=#{"Pipeline Name"}]", "[#{"Current pipeline"}]")
                  with_tag("span.path_separator", ">")
                  with_tag("span[title=#{"Stage Name"}]", "stage")
                  with_tag("span.path_separator", ">")
                  with_tag("span[title=#{"Job Name"}]", "job")
                  with_tag("span.delimiter", ':')
                  with_tag("span[title=#{"Source"}]", "src")
                  with_tag("span.direction_arrow", "->")
                  with_tag("span[title=#{"Destination"}]", "dest")
                end
              end
            end
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

      response.body.should have_tag('#stage_id_job_1') do
        with_tag(".tab-content") do
          with_tag('#tasks_stage_id_job_1') do
            with_tag('ul') do
              with_tag('li') do
                with_tag('span.condition', 'Run if Passed, Failed')
              end
            end
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

      response.body.should have_tag('#stage_id_job_1') do
        with_tag(".tab-content") do
          with_tag('#tasks_stage_id_job_1') do
            with_tag('ul') do
              with_tag('li') do
                with_tag('span.condition', 'Run if Failed, Passed')
              end
            end
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

      response.body.should have_tag('#stage_id_job_1') do
        with_tag(".tab-content") do
          with_tag('#tasks_stage_id_job_1') do
            with_tag('ul') do
              with_tag('li') do
                with_tag('span') do
                  with_tag('span.working_dir', 'default/wd$')
                  with_tag('span.command', 'ant')
                  with_tag('span.arguments', '-f "build.xml" compile')
                end
                with_tag('span.condition', 'Run if Passed')
                with_tag('ul') do
                  with_tag('li') do
                    with_tag('span') do
                      with_tag('span.on_cancel', "On Cancel")
                      with_tag('span.working_dir', 'default/wd$')
                      with_tag('span.command', 'nant')
                      with_tag('span.arguments', '-buildfile:"default.build" compile')
                    end
                  end
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

      response.body.should have_tag("#stage_id_job_1") do
        with_tag(".tab-content") do
          with_tag('#tasks_stage_id_job_1') do
            with_tag("span", "No tasks have been configured")
          end
        end
      end
    end
  end
end