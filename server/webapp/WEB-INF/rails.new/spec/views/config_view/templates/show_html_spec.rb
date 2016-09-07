##########################GO-LICENSE-START################################
# Copyright 2016 ThoughtWorks, Inc.
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

describe "config_view/templates/show.html.erb" do

  it "should display tree of stages and jobs to help navigation" do
    template = com.thoughtworks.go.helper.PipelineTemplateConfigMother.createTemplate("t1")
    assign(:template_config, template)
    render
    Capybara.string(response.body).all(".layout.admin-entity ul.stages li").tap do |stages|
      stage = stages[0]
      expect(stage).to have_selector("a[href='#definition_view_stage_1']", :text => "defaultStage")
      stage.find("ul.jobs li").tap do |job|
        expect(job).to have_selector("a[href='#definition_view_stage_1_job_1']", :text => "defaultJob")
      end
    end
  end

  it "should display stage heading" do
    template = PipelineTemplateConfigMother.create_template("t1", [StageConfigMother.stage_config("stage1"), StageConfigMother.manual_stage("stage2")].to_java(StageConfig))
    assign(:template_config, template)
    render
    Capybara.string(response.body).all(".layout.admin-entity div.content div#definition_view_stage_1 h3.entity_title ul li").tap do |item|
      expect(item[0]).to have_content("stage1")
    end
    Capybara.string(response.body).all(".layout.admin-entity div.content div#definition_view_stage_2 h3.entity_title ul li").tap do |item|
      expect(item[0]).to have_content("stage2")
    end
  end

  it "should display stage summary details" do
    stage2 = StageConfig.new(CaseInsensitiveString.new("stage2"), false, true, Approval.manualApproval(), true, BuildPlanMother.withBuildPlans(["job1"].to_java(java.lang.String)))
    template = PipelineTemplateConfigMother.create_template("t1", [StageConfigMother.stage_config("stage1"), stage2].to_java(StageConfig))
    assign(:template_config, template)
    render
    expected = [{:stage_name => "stage_1", :stage_type => "On Success", :fetch_materials => "Yes", :cleanup_artifacts => "No", :clean_directory => "No"},
                {:stage_name => "stage_2", :stage_type => "Manual", :fetch_materials => "No", :cleanup_artifacts => "Yes", :clean_directory => "Yes"}]

    expected.each do |e|
      Capybara.string(response.body).find(".layout.admin-entity").tap do |stages|
        stages.find("#definition_view_#{e[:stage_name]}").tap do |stage|
          stage.find("fieldset.stage_summary").tap do |summary|
            summary.find("ul").tap do |list|
              list.find("li.stage_type.field").tap do |field|
                field.find("label").tap do |label|
                  expect(label).to have_selector("span.key", :text => "Stage Type")
                  expect(label).to have_selector("span.hint", :text => "'On Success' option will automatically schedule the stage after the preceding stage completes successfully. The 'Manual' option will require a user to manually trigger the stage. For the first stage in a pipeline, setting type to 'on success' is the same as checking 'Automatic Pipeline Scheduling' on the pipeline config.")
                end
                expect(field).to have_selector("span.value", :text => e[:stage_type])
              end
              list.find("li.fetch_materials.field").tap do |field|
                field.find("label").tap do |label|
                  expect(label).to have_selector("span.key", :text => "Fetch Materials")
                  expect(label).to have_selector("span.hint", :text => "Perform material updates or checkouts")
                end
                expect(field).to have_selector("span.value", :text => e[:fetch_materials])
              end
              list.find("li.never_cleanup_artifacts.field").tap do |field|
                field.find("label").tap do |label|
                  expect(label).to have_selector("span.key", :text => "Never Cleanup Artifacts")
                  expect(label).to have_selector("span.hint", :text => "Never cleanup artifacts for this stage, if purging artifacts is configured at the Server Level")
                end
                expect(field).to have_selector("span.value", :text => e[:cleanup_artifacts])
              end
              list.find("li.clean_working_directory.field").tap do |field|
                field.find("label").tap do |label|
                  expect(label).to have_selector("span.key", :text => "Clean Working Directory")
                  expect(label).to have_selector("span.hint", :text => "Remove all files/directories in the working directory on the agent")
                end
                expect(field).to have_selector("span.value", :text => e[:clean_directory])
              end
            end
          end
        end
      end
    end
  end

  it "should display environment variables and permissions tab for a stage" do
    stage = StageConfigMother.stage_config("stage1")
    template = PipelineTemplateConfigMother.create_template("t1", [stage].to_java(StageConfig))
    assign(:template_config, template)
    render
    Capybara.string(response.body).find(".layout.admin-entity").tap do |layout|
      layout.find("#definition_view_stage_1").tap do |stage|
        stage.find("ul.nav.nav-tabs").tap do |list|
          list.all("li").tap do |items|
            expect(items[0]).to have_selector("a[href='#environment_variables_stage_1'][data-toggle='tab']", :text => "Environment Variables")
            expect(items[1]).to have_selector("a[href='#permissions_stage_1'][data-toggle='tab']", :text => "Permissions")
          end
        end
      end
    end
  end

  it "should display environment variables for a stage" do
    stage = StageConfigMother.stage_config("stage1")
    stage.setVariables(EnvironmentVariablesConfig.new([EnvironmentVariableConfig.new("env1", "value1"), EnvironmentVariableConfig.new("env2", "value2")]))
    template = PipelineTemplateConfigMother.create_template("t1", [stage].to_java(StageConfig))
    assign(:template_config, template)
    render
    Capybara.string(response.body).find("#definition_view_stage_1").tap do |stage|
      stage.find(".tab-content #environment_variables_stage_1.tab-pane.active table.variables").tap do |table|
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

  it 'should mask the value of secure environment variable for a stage' do
    stage = StageConfigMother.stage_config("stage1")
    environment_variable_config_new = EnvironmentVariableConfig.new("env2", "value2")
    environment_variable_config_new.setIsSecure(true)
    stage.setVariables(EnvironmentVariablesConfig.new([EnvironmentVariableConfig.new("env1", "value1"), environment_variable_config_new]))
    template = PipelineTemplateConfigMother.create_template("t1", [stage].to_java(StageConfig))
    assign(:template_config, template)
    render
    Capybara.string(response.body).find("#definition_view_stage_1").tap do |stage|
      stage.find(".tab-content #environment_variables_stage_1.tab-pane.active table.variables").tap do |table|
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

  it "should not show environment variables table if none are configured" do
    template = PipelineTemplateConfigMother.create_template("t1")
    assign(:template_config, template)
    render
    Capybara.string(response.body).find("#definition_view_stage_1") .tap do |stage|
      stage.find(".tab-content #environment_variables_stage_1.tab-pane.active table.variables").tap do |table|
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

  it "should display permissions for a stage" do
    stage = StageConfigMother.stage_config("stage1")
    authConfig = com.thoughtworks.go.config.AuthConfig.new()
    authConfig.add(com.thoughtworks.go.config.AdminRole.new(CaseInsensitiveString.new("group1_admin")))
    authConfig.add(com.thoughtworks.go.config.AdminUser.new(CaseInsensitiveString.new("user1")))
    authConfig.add(com.thoughtworks.go.config.AdminUser.new(CaseInsensitiveString.new("user2")))
    stage.updateApproval(Approval.new(authConfig))
    stage.setVariables(EnvironmentVariablesConfig.new([EnvironmentVariableConfig.new("env1", "value1"), EnvironmentVariableConfig.new("env2", "value2")]))
    template = PipelineTemplateConfigMother.create_template("t1", [stage].to_java(StageConfig))
    assign(:template_config, template)
    render
    Capybara.string(response.body).find("#definition_view_stage_1").tap do |stage|
      stage.find(".tab-content #permissions_stage_1.tab-pane").tap do |permissions|
        permissions.find("table.stage_operate_users.list_table").tap do |table|
          table.find("thead").tap do |head|
            head.find("tr").tap do |row|
              expect(row).to have_selector("th", :text => "Users")
            end
          end
          table.find("tbody").tap do |body|
            body.all("tr").tap do |rows|
              expect(rows[0]).to have_selector("td.name_value_cell", :text => "user1")
              expect(rows[1]).to have_selector("td.name_value_cell", :text => "user2")
            end
          end
        end
        permissions.find("table.stage_operate_roles.list_table").tap do |table|
          table.find("thead").tap do |head|
            head.find("tr").tap do |row|
              expect(row).to have_selector("th", :text => "Roles")
            end
          end
          table.find("tbody").tap do |body|
            body.find("tr").tap do |row|
              expect(row).to have_selector("td.name_value_cell", :text => "group1_admin")
            end
          end
        end
      end
    end
  end

  it "should not show permissions table if none are configured" do
    template = PipelineTemplateConfigMother.create_template("t1")
    assign(:template_config, template)
    render
    Capybara.string(response.body).find("#permissions_stage_1").tap do |stage|
      stage.find(".information", :text => "There are no operate permissions configured for this stage nor its pipeline group. All Go users can operate on this stage.")
    end
  end

  it "should show only users table if only user permissions are defined on stage" do
    stage = StageConfigMother.stage_config("stage1")
    authConfig = com.thoughtworks.go.config.AuthConfig.new()
    authConfig.add(com.thoughtworks.go.config.AdminUser.new(CaseInsensitiveString.new("user1")))
    stage.updateApproval(Approval.new(authConfig))
    template = PipelineTemplateConfigMother.create_template("t1", [stage].to_java(StageConfig))
    assign(:template_config, template)
    render
    Capybara.string(response.body).find("#permissions_stage_1").tap do |stage|
      expect(stage).to have_selector("table.stage_operate_users.list_table")
      expect(stage).to_not have_selector("table.stage_operate_roles.list_table")
    end
  end

  it "should show only roles table if only role permissions are defined on stage" do
    stage = StageConfigMother.stage_config("stage1")
    authConfig = com.thoughtworks.go.config.AuthConfig.new()
    authConfig.add(com.thoughtworks.go.config.AdminRole.new(CaseInsensitiveString.new("role1")))
    stage.updateApproval(Approval.new(authConfig))
    template = PipelineTemplateConfigMother.create_template("t1", [stage].to_java(StageConfig))
    assign(:template_config, template)
    render
    Capybara.string(response.body).find("#permissions_stage_1").tap do |stage|
      expect(stage).to_not have_selector("table.stage_operate_users.list_table")
      expect(stage).to have_selector("table.stage_operate_roles.list_table")
    end
  end

  it "should show breadcrumb for job containing stage it came from" do
    stage1 = StageConfigMother.custom("stage1", ["j1", "j2"].to_java(java.lang.String))
    stage2 = StageConfigMother.custom("stage2", ["j1"].to_java(java.lang.String))
    template = PipelineTemplateConfigMother.create_template("t1", [stage1, stage2].to_java(StageConfig))
    assign(:template_config, template)
    render
    Capybara.string(response.body).find("#templates").tap do |templates|
      templates.find("#definition_view_stage_1_job_1").tap do |job|
        job.find("h3.entity_title").tap do |heading|
          expect(heading).to have_selector("li:first-child a[href='#definition_view_stage_1']", :text => "stage1")
          expect(heading).to have_selector("li:last-child", "j1")
        end
      end
      templates.find("#definition_view_stage_1_job_2").tap do |job|
        job.find("h3.entity_title").tap do |heading|
          expect(heading).to have_selector("li:first-child a[href='#definition_view_stage_1']", :text => "stage1")
          expect(heading).to have_selector("li:last-child", "j2")
        end
      end
      templates.find("#definition_view_stage_2_job_1").tap do |job|
        job.find("h3.entity_title").tap do |heading|
          expect(heading).to have_selector("li:first-child a[href='#definition_view_stage_2']", :text => "stage2")
          expect(heading).to have_selector("li:last-child", "j1")
        end
      end
    end
  end

end


