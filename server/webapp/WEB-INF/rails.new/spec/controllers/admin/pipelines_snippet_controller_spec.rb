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

describe Admin::PipelinesSnippetController do

  describe :routes do
    it "should resolve the route to partial config page" do
      {:get => "/admin/pipelines/snippet"}.should route_to(:controller => "admin/pipelines_snippet", :action => "index")
      pipelines_snippet_path.should == "/admin/pipelines/snippet"
    end

    it "should resolve route to get group xml" do
      {:get => "/admin/pipelines/snippet/foo.bar"}.should route_to(:controller => "admin/pipelines_snippet", :action => "show", :group_name => "foo.bar")
      pipelines_snippet_show_path(:group_name => 'foo.bar').should == "/admin/pipelines/snippet/foo.bar"
    end

    it "should resolve route to save group xml" do
      {:put => "/admin/pipelines/snippet/foo.bar"}.should route_to(:controller => "admin/pipelines_snippet", :action => "update", :group_name => "foo.bar")
      pipelines_snippet_update_path(:group_name => 'foo.bar').should == "/admin/pipelines/snippet/foo.bar"
    end

    it "should resolve route to edit group xml" do
      {:get => "/admin/pipelines/snippet/foo.bar/edit"}.should route_to(:controller => "admin/pipelines_snippet", :action => "edit", :group_name => "foo.bar")
      pipelines_snippet_edit_path(:group_name => 'foo.bar').should == "/admin/pipelines/snippet/foo.bar/edit"
    end
  end

  describe :actions do
    before :each do
      @security_service = double("Security Service")
      controller.stub(:security_service).and_return(@security_service)
      @pipeline_configs_service = double('Pipelines Config Service')
      @go_config_service = double('Go Config Service')
      controller.stub(:pipeline_configs_service).and_return(@pipeline_configs_service)
      controller.stub(:go_config_service).and_return(@go_config_service)
      controller.should_receive(:populate_config_validity).and_return(true)
      controller.should_receive(:load_context)
      @result = HttpLocalizedOperationResult.new
      HttpLocalizedOperationResult.stub(:new).and_return(@result)
    end


    describe :index do
      before :each do
        @user = Username.new(CaseInsensitiveString.new("group_admin"))
        controller.stub(:current_user).and_return(@user)
      end

      it "should display first group by default" do
        @security_service.should_receive(:modifiableGroupsForUser).with(@user).and_return(["first", "second"])
        get :index
        response.should redirect_to pipelines_snippet_show_path(:group_name => "first")
      end
    end

    describe :show do
      before :each do
        @user = Username.new(CaseInsensitiveString.new("group_admin"))
        controller.stub(:current_user).and_return(@user)
        @security_service.should_receive(:modifiableGroupsForUser).with(@user).and_return(["foo"])
      end

      it "should return a group xml for a valid group" do
        @result.should_receive(:is_successful).and_return(true)
        @pipeline_configs_service.should_receive(:getXml).with("valid_group", @user, @result).and_return("some valid xml as string")

        get :show, {:group_name => "valid_group"}

        assigns[:group_as_xml].should == "some valid xml as string"
        assigns[:group_name].should == "valid_group"
        assigns[:modifiable_groups].size.should == 1
        assigns[:modifiable_groups].should include "foo"
        response.should render_template :show
        assert_template layout: "admin"
      end

      it "should return unauthorized error if the user does not have access to the group" do
        @result.should_receive(:is_successful).and_return(false)
        @result.should_receive(:httpCode).and_return(401)
        @result.should_receive(:message).with(anything).and_return("Unauthorized")
        @config = BasicCruiseConfig.new
        group = "valid_group"
        @pipeline_configs_service.should_receive(:getXml).with(group, @user, @result).and_return(nil)
        get :show, {:group_name => group}

        response.should render_template 'shared/config_error'
        assert_response 401
      end
    end

    describe :edit do
      before :each do
        @user = Username.new(CaseInsensitiveString.new("group_admin"))
        controller.stub(:current_user).and_return(@user)
        @security_service.should_receive(:modifiableGroupsForUser).with(@user).and_return(["foo"])
      end

      it "should display the group xml" do
        group = "group"
        @pipeline_configs_service.should_receive(:getXml).with(group, @user, @result).and_return("some valid xml as string")
        @config = BasicCruiseConfig.new
        @config.should_receive(:getMd5).and_return('md5_value_for_configuration')
        @go_config_service.should_receive(:getConfigForEditing).and_return(@config)
        get :edit, {:group_name => group}

        assigns[:group_name].should == group
        assigns[:group_as_xml].should == "some valid xml as string"
        assigns[:config_md5].should == 'md5_value_for_configuration'
        assigns[:modifiable_groups].size.should == 1
        assigns[:modifiable_groups].should include "foo"
        response.should render_template :edit
        assert_template layout: "admin"
      end

      it "should return unauthorized error if the user does not have access to the group" do
        @result.should_receive(:is_successful).and_return(false)
        @result.should_receive(:httpCode).and_return(401)
        @result.should_receive(:message).with(anything).and_return("Unauthorized")
        @config = BasicCruiseConfig.new
        group = "valid_group"
        @go_config_service.should_receive(:getConfigForEditing).and_return(@config)
        @pipeline_configs_service.should_receive(:getXml).with(group, @user, @result).and_return(nil)
        get :edit, {:group_name => group}

        response.should render_template 'shared/config_error'
        assert_response 401
      end

    end

    describe :update do
      before :each do
        @user = Username.new(CaseInsensitiveString.new("group_admin"))
        controller.stub(:current_user).and_return(@user)
        @result = HttpLocalizedOperationResult.new
        HttpLocalizedOperationResult.stub(:new).and_return(@result)
      end

      it "should persist group xml and redirect to show" do
        @result.setMessage(LocalizedMessage.string("SAVED_SUCCESSFULLY"))
        controller.should_receive(:set_flash_message).with("Saved successfully.","success").and_return("Success!")
        pipeline_configs = double(PipelineConfigs.class)
        pipeline_configs.stub(:get_group).and_return("renamed_group")
        updated_xml = "updated pipelines xml"
        @result.should_receive(:is_successful).and_return(true)
        group_name = "group_name"
        cruise_config_operational_response = double('cruise_config_operational_response')
        cruise_config_operational_response.should_receive(:getConfigElement).and_return(pipeline_configs)
        validity = double('validity')
        validity.should_receive(:errorMessage).never
        validity.should_receive(:isMergeConflict).and_return(false)
        validity.should_receive(:isPostValidationError).and_return(false)
        cruise_config_operational_response.should_receive(:getValidity).and_return(validity)
        @pipeline_configs_service.should_receive(:updateXml).with(group_name, updated_xml, "md5", @user, @result).and_return(cruise_config_operational_response)
        put :update, {:group_name => group_name, :group_xml => updated_xml, :config_md5 => "md5"}

        response.should redirect_to pipelines_snippet_show_path("renamed_group", :fm => "Success!")
      end

      it "should render global error if update failed due to merge error or post validation error" do
        @security_service.should_receive(:modifiableGroupsForUser).with(@user).and_return(["foo"])
        updated_xml = "updated pipelines xml"
        @result.should_receive(:is_successful).and_return(false)
        @result.should_receive(:message).with(anything).and_return("failed")
        group_name = "group_name"
        cruise_config_operational_response = double('cruise_config_operational_response')
        cruise_config_operational_response.should_receive(:getConfigElement).and_return(nil)
        validity = double('validity')
        validity.should_receive(:errorMessage).and_return('error message')
        validity.should_receive(:isValid).never
        validity.should_receive(:isMergeConflict).and_return(true)
        validity.should_receive(:isPostValidationError).never
        cruise_config_operational_response.should_receive(:getValidity).and_return(validity)
        @pipeline_configs_service.should_receive(:updateXml).with(group_name, updated_xml, "md5", @user, @result).and_return(cruise_config_operational_response)
        put :update, {:group_name => group_name, :group_xml => updated_xml, :config_md5 => "md5"}

        response.should render_template 'edit'
        assigns[:config_md5].should == "md5"
        assigns[:group_name].should == group_name
        assigns[:group_as_xml].should == updated_xml
        assigns[:modifiable_groups].size.should == 1
        assigns[:modifiable_groups].should include "foo"
        assigns[:errors].should == ['error message']
        flash.now[:error].should == "failed"
        assert_template layout: "admin"
      end

      it "should render error on flash pane for pre merge validation errors or other errors" do
        @security_service.should_receive(:modifiableGroupsForUser).with(@user).and_return(["foo"])
        updated_xml = "updated pipelines xml"
        @result.should_receive(:is_successful).and_return(false)
        @result.should_receive(:message).with(anything).and_return("failed")
        group_name = "group_name"
        cruise_config_operational_response = double('cruise_config_operational_response')
        cruise_config_operational_response.should_receive(:getConfigElement).and_return(nil)
        validity = double('validity')
        validity.should_receive(:isValid).never
        validity.should_receive(:errorMessage).never
        validity.should_receive(:isPostValidationError).and_return(false)
        validity.should_receive(:isMergeConflict).and_return(false)
        cruise_config_operational_response.should_receive(:getValidity).and_return(validity)
        @pipeline_configs_service.should_receive(:updateXml).with(group_name, updated_xml, "md5", @user, @result).and_return(cruise_config_operational_response)
        put :update, {:group_name => group_name, :group_xml => updated_xml, :config_md5 => "md5"}

        response.should render_template 'edit'
        assigns[:config_md5].should == "md5"
        assigns[:group_name].should == group_name
        assigns[:group_as_xml].should == updated_xml
        assigns[:modifiable_groups].size.should == 1
        assigns[:modifiable_groups].should include "foo"
        assigns[:errors].should == nil
        flash.now[:error].should == 'failed'
      end
    end
  end
end
