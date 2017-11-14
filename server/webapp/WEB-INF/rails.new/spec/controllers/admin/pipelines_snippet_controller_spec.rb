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

require 'rails_helper'

describe Admin::PipelinesSnippetController do

  before :each do
    @security_service = double("Security Service")
    allow(controller).to receive(:security_service).and_return(@security_service)
    @pipeline_configs_service = double('Pipelines Configs Service')
    @go_config_service = double('Go Config Service')
    allow(controller).to receive(:pipeline_configs_service).and_return(@pipeline_configs_service)
    allow(controller).to receive(:go_config_service).and_return(@go_config_service)
  end

  describe "actions" do
    before :each do
      expect(controller).to receive(:populate_config_validity).and_return(true)
      expect(controller).to receive(:load_context)
      @result = double(HttpLocalizedOperationResult)
      allow(HttpLocalizedOperationResult).to receive(:new).and_return(@result)
    end


    describe "index" do
      before :each do
        @user = Username.new(CaseInsensitiveString.new("group_admin"))
        allow(controller).to receive(:current_user).and_return(@user)
      end

      it "should display first group by default" do
        expect(@security_service).to receive(:modifiableGroupsForUser).with(@user).and_return(["first", "second"])
        get :index
        expect(response).to redirect_to pipelines_snippet_show_path(:group_name => "first")
      end
    end

    describe "show" do
      before :each do
        @user = Username.new(CaseInsensitiveString.new("group_admin"))
        allow(controller).to receive(:current_user).and_return(@user)
        expect(@security_service).to receive(:modifiableGroupsForUser).with(@user).and_return(["foo"])
      end

      it "should return a group xml for a valid group" do
        expect(@result).to receive(:isSuccessful).and_return(true)
        expect(@pipeline_configs_service).to receive(:getXml).with("valid_group", @user, @result).and_return("some valid xml as string")

        get :show, params: { :group_name => "valid_group" }

        expect(assigns[:group_as_xml]).to eq("some valid xml as string")
        expect(assigns[:group_name]).to eq("valid_group")
        expect(assigns[:modifiable_groups].size).to eq(1)
        expect(assigns[:modifiable_groups]).to include "foo"
        expect(response).to render_template :show
        assert_template layout: "admin"
      end

      it "should return unauthorized error if the user does not have access to the group" do
        expect(@result).to receive(:isSuccessful).and_return(false)
        expect(@result).to receive(:httpCode).and_return(401)
        expect(@result).to receive(:message).with(anything).and_return("Unauthorized")
        @config = BasicCruiseConfig.new
        group = "valid_group"
        expect(@pipeline_configs_service).to receive(:getXml).with(group, @user, @result).and_return(nil)
        get :show, params: { :group_name => group }

        expect(response).to render_template 'shared/config_error'
        assert_response 401
      end
    end

    describe "edit" do
      before :each do
        @user = Username.new(CaseInsensitiveString.new("group_admin"))
        allow(controller).to receive(:current_user).and_return(@user)
        expect(@security_service).to receive(:modifiableGroupsForUser).with(@user).and_return(["foo"])
      end

      it "should display the group xml" do
        expect(@result).to receive(:isSuccessful).and_return(true)
        group = "group"
        expect(@pipeline_configs_service).to receive(:getXml).with(group, @user, @result).and_return("some valid xml as string")
        @config = BasicCruiseConfig.new
        expect(@config).to receive(:getMd5).and_return('md5_value_for_configuration')
        expect(@go_config_service).to receive(:getConfigForEditing).and_return(@config)
        get :edit, params: { :group_name => group }

        expect(assigns[:group_name]).to eq(group)
        expect(assigns[:group_as_xml]).to eq("some valid xml as string")
        expect(assigns[:config_md5]).to eq('md5_value_for_configuration')
        expect(assigns[:modifiable_groups].size).to eq(1)
        expect(assigns[:modifiable_groups]).to include "foo"
        expect(response).to render_template :edit
        assert_template layout: "admin"
      end

      it "should return unauthorized error if the user does not have access to the group" do
        expect(@result).to receive(:isSuccessful).and_return(false)
        expect(@result).to receive(:httpCode).and_return(401)
        expect(@result).to receive(:message).with(anything).and_return("Unauthorized")
        @config = BasicCruiseConfig.new
        group = "valid_group"
        expect(@go_config_service).to receive(:getConfigForEditing).and_return(@config)
        expect(@pipeline_configs_service).to receive(:getXml).with(group, @user, @result).and_return(nil)
        get :edit, params: { :group_name => group }

        expect(response).to render_template 'shared/config_error'
        assert_response 401
      end

    end

    describe "update" do
      before :each do
        @user = Username.new(CaseInsensitiveString.new("group_admin"))
        allow(controller).to receive(:current_user).and_return(@user)
        @result = double(HttpLocalizedOperationResult)
        allow(HttpLocalizedOperationResult).to receive(:new).and_return(@result)
      end

      it "should persist group xml and redirect to show" do
        expect(@result).to receive(:message).and_return("Saved successfully.")
        expect(controller).to receive(:set_flash_message).with("Saved successfully.","success").and_return("Success!")
        pipeline_configs = double(PipelineConfigs.class)
        allow(pipeline_configs).to receive(:get_group).and_return("renamed_group")
        updated_xml = "updated pipelines xml"
        expect(@result).to receive(:isSuccessful).and_return(true)
        group_name = "group_name"
        cruise_config_operational_response = double('cruise_config_operational_response')
        expect(cruise_config_operational_response).to receive(:getConfigElement).and_return(pipeline_configs)
        validity = double('validity')
        expect(validity).to receive(:errorMessage).never
        expect(validity).to receive(:isMergeConflict).and_return(false)
        expect(validity).to receive(:isPostValidationError).and_return(false)
        expect(cruise_config_operational_response).to receive(:getValidity).and_return(validity)
        expect(@pipeline_configs_service).to receive(:updateXml).with(group_name, updated_xml, "md5", @user, @result).and_return(cruise_config_operational_response)
        put :update, params: { :group_name => group_name, :group_xml => updated_xml, :config_md5 => "md5" }

        expect(response).to redirect_to pipelines_snippet_show_path("renamed_group", :fm => "Success!")
      end

      it "should render global error if update failed due to merge error or post validation error" do
        expect(@security_service).to receive(:modifiableGroupsForUser).with(@user).and_return(["foo"])
        updated_xml = "updated pipelines xml"
        expect(@result).to receive(:isSuccessful).and_return(false)
        expect(@result).to receive(:message).with(anything).and_return("failed")
        group_name = "group_name"
        cruise_config_operational_response = double('cruise_config_operational_response')
        expect(cruise_config_operational_response).to receive(:getConfigElement).and_return(nil)
        validity = double('validity')
        expect(validity).to receive(:errorMessage).and_return('error message')
        expect(validity).to receive(:isValid).never
        expect(validity).to receive(:isMergeConflict).and_return(true)
        expect(validity).to receive(:isPostValidationError).never
        expect(cruise_config_operational_response).to receive(:getValidity).and_return(validity)
        expect(@pipeline_configs_service).to receive(:updateXml).with(group_name, updated_xml, "md5", @user, @result).and_return(cruise_config_operational_response)
        put :update, params: { :group_name => group_name, :group_xml => updated_xml, :config_md5 => "md5" }

        expect(response).to render_template 'edit'
        expect(assigns[:config_md5]).to eq("md5")
        expect(assigns[:group_name]).to eq(group_name)
        expect(assigns[:group_as_xml]).to eq(updated_xml)
        expect(assigns[:modifiable_groups].size).to eq(1)
        expect(assigns[:modifiable_groups]).to include "foo"
        expect(assigns[:errors]).to eq(['error message'])
        expect(flash.now[:error]).to eq("failed")
        assert_template layout: "admin"
      end

      it "should render error on flash pane for pre merge validation errors or other errors" do
        expect(@security_service).to receive(:modifiableGroupsForUser).with(@user).and_return(["foo"])
        updated_xml = "updated pipelines xml"
        expect(@result).to receive(:isSuccessful).and_return(false)
        expect(@result).to receive(:message).with(anything).and_return("failed")
        group_name = "group_name"
        cruise_config_operational_response = double('cruise_config_operational_response')
        expect(cruise_config_operational_response).to receive(:getConfigElement).and_return(nil)
        validity = double('validity')
        expect(validity).to receive(:isValid).never
        expect(validity).to receive(:errorMessage).never
        expect(validity).to receive(:isPostValidationError).and_return(false)
        expect(validity).to receive(:isMergeConflict).and_return(false)
        expect(cruise_config_operational_response).to receive(:getValidity).and_return(validity)
        expect(@pipeline_configs_service).to receive(:updateXml).with(group_name, updated_xml, "md5", @user, @result).and_return(cruise_config_operational_response)
        put :update, params: { :group_name => group_name, :group_xml => updated_xml, :config_md5 => "md5" }

        expect(response).to render_template 'edit'
        expect(assigns[:config_md5]).to eq("md5")
        expect(assigns[:group_name]).to eq(group_name)
        expect(assigns[:group_as_xml]).to eq(updated_xml)
        expect(assigns[:modifiable_groups].size).to eq(1)
        expect(assigns[:modifiable_groups]).to include "foo"
        expect(assigns[:errors]).to eq(nil)
        expect(flash.now[:error]).to eq('failed')
      end
    end
  end
end
