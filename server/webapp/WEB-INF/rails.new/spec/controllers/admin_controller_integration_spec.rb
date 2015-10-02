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

describe AdminController, "integration test" do
  #this the place for integration testing admin-config-update, we should try to cover failure, 406, 409, 401 etc here,
  #     and we should eventually start using one of the real controllers(stages/jobs/pipelines controller so we can test with views) -jj

  before do
    @config_helper = com.thoughtworks.go.util.GoConfigFileHelper.new()
    @db_helper = Spring.bean("databaseAccessHelper")
    config_dao = Spring.bean("goConfigDao")
    @db_helper.onSetUp()
    @config_helper.usingCruiseConfigDao(config_dao).initializeConfigFile()
    @config_helper.onSetUp()
    controller.go_config_service.forceNotifyListeners()

    @command = Class.new(::ConfigUpdate::SaveAsPipelineAdmin) do
      include ConfigUpdate::StageNode
      include ConfigUpdate::NodeAsSubject

      def updatedNode(cruise_config)
        load_from_pipeline_stage_named(load_pipeline_or_template(cruise_config), CaseInsensitiveString.new(params[:stage]['name']))
      end

      def update(stage)
        stage.setConfigAttributes(params[:stage])
      end
    end
  end

  after do
    @config_helper.onTearDown()
    @db_helper.onTearDown()
  end

  it "should save config and call data loading lambda" do
    @config_helper.addPipeline("pipeline-name", "stage-name")
    params = {:stage => {'name' => "new-stage-name"}, :stage_name => "stage-name", :pipeline_name => "pipeline-name", :stage_parent => "pipelines"}
    @config_helper.addAdmins(["admin"].to_java(java.lang.String))
    save_successful = false
    stage = nil
    controller.send(:save, controller.go_config_service.getConfigForEditing().getMd5(), {:action => "random_action", :controller => "random_controller"}, @command.new(params, CaseInsensitiveString.new("admin"), controller.security_service), "Saved successfully.", proc do
      stage = controller.instance_variable_get('@node')
    end) do
      save_successful = true
    end
    stage.name().should == CaseInsensitiveString.new("new-stage-name")
    save_successful.should be_true
  end

end
