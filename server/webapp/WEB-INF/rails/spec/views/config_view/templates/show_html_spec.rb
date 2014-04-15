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

describe "Showing template definition" do

  it "should display tree of stages and jobs to help navigation" do
    template = com.thoughtworks.go.helper.PipelineTemplateConfigMother.createTemplate("t1")
    assigns[:template_config] = template
    render "config_view/templates/show"

    response.body.should have_tag(".layout.admin-entity") do
      with_tag("ul.stages") do
        with_tag("li") do
          with_tag("a[href='#definition_view_stage_1']", "defaultStage")
          with_tag("ul.jobs") do
            with_tag("li") do
              with_tag("a[href='#definition_view_stage_1_job_1']", "defaultJob")
            end
          end
        end
      end
    end
  end

  it "should display stage heading" do
    template = PipelineTemplateConfigMother.create_template("t1", [StageConfigMother.stage_config("stage1"), StageConfigMother.manual_stage("stage2")].to_java(StageConfig))
    assigns[:template_config] = template
    render "config_view/templates/show"

    response.body.should have_tag(".layout.admin-entity") do
      with_tag("#definition_view_stage_1") do
        with_tag("h3.entity_title", "stage1")
      end
      with_tag("#definition_view_stage_2") do
        with_tag("h3.entity_title", "stage2")
      end
    end
  end

  it "should display stage summary details" do
    stage2 = StageConfig.new(CaseInsensitiveString.new("stage2"), false, true, Approval.manualApproval(), true, BuildPlanMother.withBuildPlans(["job1"].to_java(java.lang.String)))
    template = PipelineTemplateConfigMother.create_template("t1", [StageConfigMother.stage_config("stage1"), stage2].to_java(StageConfig))
    assigns[:template_config] = template
    render "config_view/templates/show"

    response.body.should have_tag(".layout.admin-entity") do
      with_tag("#definition_view_stage_1") do
        with_tag("fieldset.stage_summary") do
          with_tag("ul") do
            with_tag("li.stage_type.field") do
              with_tag("label") do
                with_tag("span.key", "Stage Type")
                with_tag("span.hint", "'On Success' option will automatically schedule the stage after the preceding stage completes successfully. The 'Manual' option will require a user to manually\n                                              trigger the stage. For the first stage in a pipeline, setting type to 'on success' is the same as checking 'Automatic Pipeline Scheduling' on the pipeline config.")
              end
              with_tag("span.value", "On Success")
            end
            with_tag("li.fetch_materials.field") do
              with_tag("label") do
                with_tag("span.key", "Fetch Materials")
                with_tag("span.hint", "Perform material updates or checkouts")
              end
              with_tag("span.value", "Yes")
            end
            with_tag("li.never_cleanup_artifacts.field") do
              with_tag("label") do
                with_tag("span.key", "Never Cleanup Artifacts")
                with_tag("span.hint", "Never cleanup artifacts for this stage, if purging artifacts is configured at the Server Level")
              end
              with_tag("span.value", "No")
            end
            with_tag("li.clean_working_directory.field") do
              with_tag("label") do
                with_tag("span.key", "Clean Working Directory")
                with_tag("span.hint", "Remove all files/directories in the working directory on the agent")
              end
              with_tag("span.value", "No")
            end
          end
        end
      end
      with_tag("#definition_view_stage_2") do
        with_tag("fieldset.stage_summary") do
          with_tag("ul") do
            with_tag("li.stage_type.field") do
              with_tag("label") do
                with_tag("span.key", "Stage Type")
                with_tag("span.hint", "'On Success' option will automatically schedule the stage after the preceding stage completes successfully. The 'Manual' option will require a user to manually\n                                              trigger the stage. For the first stage in a pipeline, setting type to 'on success' is the same as checking 'Automatic Pipeline Scheduling' on the pipeline config.")
              end
              with_tag("span.value", "Manual")
            end
            with_tag("li.fetch_materials.field") do
              with_tag("label") do
                with_tag("span.key", "Fetch Materials")
                with_tag("span.hint", "Perform material updates or checkouts")
              end
              with_tag("span.value", "No")
            end
            with_tag("li.never_cleanup_artifacts.field") do
              with_tag("label") do
                with_tag("span.key", "Never Cleanup Artifacts")
                with_tag("span.hint", "Never cleanup artifacts for this stage, if purging artifacts is configured at the Server Level")
              end
              with_tag("span.value", "Yes")
            end
            with_tag("li.clean_working_directory.field") do
              with_tag("label") do
                with_tag("span.key", "Clean Working Directory")
                with_tag("span.hint", "Remove all files/directories in the working directory on the agent")
              end
              with_tag("span.value", "Yes")
            end
          end
        end
      end
    end
  end

  it "should display environment variables and permissions tab for a stage" do
    stage = StageConfigMother.stage_config("stage1")
    template = PipelineTemplateConfigMother.create_template("t1", [stage].to_java(StageConfig))
    assigns[:template_config] = template
    render "config_view/templates/show"

    response.body.should have_tag(".layout.admin-entity") do
      with_tag("#definition_view_stage_1") do
        with_tag("ul.nav.nav-tabs") do
          with_tag("li") do
            with_tag("a[href='#environment_variables_stage_1'][data-toggle='tab']", "Environment Variables")
          end
          with_tag("li") do
            with_tag("a[href='#permissions_stage_1'][data-toggle='tab']", "Permissions")
          end
        end
      end
    end
  end

  it "should display environment variables for a stage" do
    stage = StageConfigMother.stage_config("stage1")
    stage.setVariables(EnvironmentVariablesConfig.new([EnvironmentVariableConfig.new("env1", "value1"), EnvironmentVariableConfig.new("env2", "value2")]))
    template = PipelineTemplateConfigMother.create_template("t1", [stage].to_java(StageConfig))
    assigns[:template_config] = template
    render "config_view/templates/show"

    response.body.should have_tag("#definition_view_stage_1") do
      with_tag(".tab-content") do
        with_tag("#environment_variables_stage_1.tab-pane.active") do
          with_tag("table.variables") do
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

  it "should not show environment variables table if none are configured" do
    assigns[:template_config] = PipelineTemplateConfigMother.create_template("t1")
    render "config_view/templates/show"

    response.body.should have_tag("#definition_view_stage_1") do
      with_tag(".tab-content") do
        with_tag("#environment_variables_stage_1.tab-pane.active") do
          with_tag("table.variables") do
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

  it "should display permissions for a stage" do
    stage = StageConfigMother.stage_config("stage1")
    authConfig = com.thoughtworks.go.config.AuthConfig.new()
    authConfig.add(com.thoughtworks.go.config.AdminRole.new(CaseInsensitiveString.new("group1_admin")))
    authConfig.add(com.thoughtworks.go.config.AdminUser.new(CaseInsensitiveString.new("user1")))
    authConfig.add(com.thoughtworks.go.config.AdminUser.new(CaseInsensitiveString.new("user2")))
    stage.updateApproval(Approval.new(authConfig))
    stage.setVariables(EnvironmentVariablesConfig.new([EnvironmentVariableConfig.new("env1", "value1"), EnvironmentVariableConfig.new("env2", "value2")]))
    template = PipelineTemplateConfigMother.create_template("t1", [stage].to_java(StageConfig))
    assigns[:template_config] = template
    render "config_view/templates/show"

    response.body.should have_tag("#definition_view_stage_1") do
      with_tag(".tab-content") do
        with_tag("#permissions_stage_1.tab-pane") do
          with_tag("table.stage_operate_users.list_table") do
            with_tag("thead") do
              with_tag("tr") do
                with_tag("th", "Users")
              end
            end
            with_tag("tbody") do
              with_tag("tr") do
                with_tag("td.name_value_cell", "user1")
              end
              with_tag("tr") do
                with_tag("td.name_value_cell", "user2")
              end
            end
          end
          with_tag("table.stage_operate_roles.list_table") do
            with_tag("thead") do
              with_tag("tr") do
                with_tag("th", "Roles")
              end
            end
            with_tag("tbody") do
              with_tag("tr") do
                with_tag("td.name_value_cell", "group1_admin")
              end
            end
          end
        end
      end
    end
  end

  it "should not show permissions table if none are configured" do
    assigns[:template_config] = PipelineTemplateConfigMother.create_template("t1")
    render "config_view/templates/show"

    response.body.should have_tag("#permissions_stage_1") do
      with_tag(".information", "There are no operate permissions configured for this stage nor its pipeline group. All Go users can operate on this stage.")
    end
  end


  it "should show only users table if only user permissions are defined on stage" do
    stage = StageConfigMother.stage_config("stage1")
    authConfig = com.thoughtworks.go.config.AuthConfig.new()
    authConfig.add(com.thoughtworks.go.config.AdminUser.new(CaseInsensitiveString.new("user1")))
    stage.updateApproval(Approval.new(authConfig))
    assigns[:template_config] = PipelineTemplateConfigMother.create_template("t1", [stage].to_java(StageConfig))

    render "config_view/templates/show"

    response.body.should have_tag("#permissions_stage_1") do
      with_tag("table.stage_operate_users.list_table")
      without_tag("table.stage_operate_roles.list_table")
    end
  end

  it "should show only roles table if only role permissions are defined on stage" do
    stage = StageConfigMother.stage_config("stage1")
    authConfig = com.thoughtworks.go.config.AuthConfig.new()
    authConfig.add(com.thoughtworks.go.config.AdminRole.new(CaseInsensitiveString.new("role1")))
    stage.updateApproval(Approval.new(authConfig))
    assigns[:template_config] = PipelineTemplateConfigMother.create_template("t1", [stage].to_java(StageConfig))

    render "config_view/templates/show"

    response.body.should have_tag("#permissions_stage_1") do
      without_tag("table.stage_operate_users.list_table")
      with_tag("table.stage_operate_roles.list_table")
    end
  end

  it "should show breadcrumb for job containing stage it came from" do
    stage1 = StageConfigMother.custom("stage1", ["j1", "j2"].to_java(java.lang.String))
    stage2 = StageConfigMother.custom("stage2", ["j1"].to_java(java.lang.String))
    assigns[:template_config] = PipelineTemplateConfigMother.create_template("t1", [stage1, stage2].to_java(StageConfig))

    render "config_view/templates/show"

    response.body.should have_tag("#templates") do
      with_tag("#definition_view_stage_1_job_1") do
        with_tag("h3.entity_title") do
          with_tag("li:first-child a[href='#definition_view_stage_1']", "stage1")
          with_tag("li:last-child", "j1")
        end
      end
      with_tag("#definition_view_stage_1_job_2") do
        with_tag("h3.entity_title") do
          with_tag("li:first-child a[href='#definition_view_stage_1']", "stage1")
          with_tag("li:last-child", "j2")
        end
      end
      with_tag("#definition_view_stage_2_job_1") do
        with_tag("h3.entity_title") do
          with_tag("li:first-child a[href='#definition_view_stage_2']", "stage2")
          with_tag("li:last-child", "j1")
        end
      end

    end

  end

end


