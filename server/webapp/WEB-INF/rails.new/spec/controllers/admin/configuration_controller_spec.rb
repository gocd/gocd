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

describe Admin::ConfigurationController do

  before(:each) do
    @admin_service = double("admin_service")
    @config_repository = double("config_repository")
    controller.stub(:admin_service).and_return(@admin_service)
    controller.stub(:config_repository).and_return(@config_repository)
  end

  describe "tab_name" do
    before :each do
      @config = {"content" => "config-content", "md5" => "md5", "location" => "/foo/bar"}
      cruise_config_revision = double('cruise config revision')
      @config_repository.should_receive(:getRevision).with(@config['md5']).and_return(cruise_config_revision)
    end

    it "should set tab name for show" do
      @admin_service.should_receive(:populateModel).with(anything).and_return(@config)
      get :show
      assigns[:tab_name].should == 'configuration-xml'
    end

    it "should set tab name for edit" do
      @admin_service.should_receive(:configurationMapForSourceXml).and_return(@config)
      get :edit
      assigns[:tab_name].should == 'configuration-xml'
    end
  end

  describe "view_title" do
    before :each do
      @config = {"content" => "config-content", "md5" => "md5", "location" => "/foo/bar"}
      cruise_config_revision = double('cruise config revision')
      @config_repository.should_receive(:getRevision).with(@config['md5']).and_return(cruise_config_revision)
    end

    it "should set tab name for show" do
      @admin_service.should_receive(:populateModel).with(anything).and_return(@config)

      get :show

      assigns[:view_title].should == 'Administration'
      assert_template layout: "admin"
    end

    it "should set tab name for edit" do
      @admin_service.should_receive(:configurationMapForSourceXml).and_return(@config)

      get :edit

      assigns[:view_title].should == 'Administration'
      assert_template layout: "admin"
    end
  end

  describe "routes" do
    it "view" do
      config_view_path.should == "/admin/config_xml"
      {:get => "/admin/config_xml"}.should route_to(:controller => "admin/configuration", :action => "show")
    end

    it "edit" do
      config_edit_path.should == "/admin/config_xml/edit"
      {:get => "/admin/config_xml/edit"}.should route_to(:controller => "admin/configuration", :action => "edit")
    end

    it "update" do
      config_update_path.should == "/admin/config_xml"
      {:put => "/admin/config_xml"}.should route_to(:controller => "admin/configuration", :action => "update")
    end
  end

  describe :show do
    it "should render view with config" do
      config = {"content" => "config-content", "md5" => "md5", "location" => "/foo/bar"}
      @admin_service.should_receive(:populateModel).with(anything).and_return(config)
      cruise_config_revision = double('cruise config revision')
      @config_repository.should_receive(:getRevision).with(config['md5']).and_return(cruise_config_revision)

      get :show

      response.should render_template "show"
      assigns[:go_config].content.should == "config-content"
      assigns[:go_config].md5.should == "md5"
      assigns[:go_config].location.should == "/foo/bar"
      assigns[:go_config_revision].should == cruise_config_revision
    end
  end

  describe :edit do
    it "should render edit" do
      config = {"content" => "config-content", "md5" => "md5", "location" => "/foo/bar"}
      @admin_service.should_receive(:configurationMapForSourceXml).and_return(config)
      cruise_config_revision = double('cruise config revision')
      @config_repository.should_receive(:getRevision).with(config['md5']).and_return(cruise_config_revision)

      get :edit

      response.should render_template "edit"
      assigns[:go_config].content.should == "config-content"
      assigns[:go_config].md5.should == "md5"
      assigns[:go_config].location.should == "/foo/bar"
      assigns[:go_config_revision].should == cruise_config_revision
    end
  end

  describe :update do
    it "should update the configuration" do
      param_map = {"content" => "config_content", "md5" => "md5"}
      @admin_service.should_receive(:updateConfig).with(param_map, an_instance_of(HttpLocalizedOperationResult)).and_return(GoConfigValidity::valid())

      put :update, :go_config => param_map

      flash[:success].should == 'Saved successfully.'
      assert_redirect config_view_path
    end

    it "should render edit page when config save fails" do
      current_config = {"content" => "config-content", "md5" => "current-md5", "location" => "/foo/bar"}
      submitted_copy = {"content" => "edited-content", "md5" => "md5"}
      config_validity = double('config validity')
      config_validity.should_receive(:isValid).and_return(false)
      config_validity.should_receive(:errorMessage).and_return('Wrong config xml')
      controller.stub(:switch_to_split_pane?).once.with(config_validity).and_return(false)
      @admin_service.should_receive(:configurationMapForSourceXml).and_return(current_config)
      @admin_service.should_receive(:updateConfig).with(submitted_copy, an_instance_of(HttpLocalizedOperationResult)).and_return(config_validity)
      cruise_config_revision = double('cruise config revision')
      @config_repository.should_receive(:getRevision).with(submitted_copy['md5']).and_return(cruise_config_revision)

      put :update, {:go_config => submitted_copy}

      response.should render_template "edit"
      flash.now[:error].should == 'Save failed, see errors below'
      assigns[:errors][0].should == "Wrong config xml"
      assigns[:go_config].content.should == "edited-content"
      assigns[:go_config].md5.should == "md5"
      assigns[:go_config].location.should == "/foo/bar"
      assigns[:go_config_revision].should == cruise_config_revision
    end

    it "should render split pane when config save fails because of merge conflict" do
      current_config = {"content" => "config-content", "md5" => "md5", "location" => "/foo/bar"}
      submitted_copy = {"content" => "content-which-caused-conflict", "md5" => "md5"}
      @admin_service.should_receive(:configurationMapForSourceXml).and_return(current_config)
      config_validity = double('config validity')
      config_validity.should_receive(:isValid).and_return(false)
      config_validity.should_receive(:errorMessage).and_return('Conflict in merging')
      controller.stub(:switch_to_split_pane?).once.with(config_validity).and_return(true)
      @admin_service.should_receive(:updateConfig).with(submitted_copy, an_instance_of(HttpLocalizedOperationResult)).and_return(config_validity)
      cruise_config_revision = double('cruise config revision')
      @config_repository.should_receive(:getRevision).with(current_config['md5']).and_return(cruise_config_revision)

      put :update, {:go_config => submitted_copy}

      response.should render_template "split_pane"
      flash.now[:error].should == "Someone has modified the configuration and your changes are in conflict. Please review, amend and retry."
      assigns[:errors][0].should == "Conflict in merging"
      assigns[:flash_help_link].should == "<a class='' href='http://www.go.cd/documentation/user/current/configuration/configuration_reference.html' target='_blank'>Help Topic: Configuration</a>"
      assigns[:conflicted_config].content.should == submitted_copy['content']
      assigns[:conflicted_config].md5.should == submitted_copy['md5']
      assigns[:conflicted_config].location.should == submitted_copy['location']
      assigns[:go_config].content.should == current_config['content']
      assigns[:go_config].md5.should == current_config['md5']
      assigns[:go_config].location.should == current_config['location']
      assigns[:go_config_revision].should == cruise_config_revision
    end

    it "should render display configuration merged successfully when a merge happens" do
      submitted_copy = {"content" => "config_content_1", "md5" => "md5"}
      config_validity = double('config_validity')
      config_validity.should_receive(:isValid).and_return(true)
      config_validity.should_receive(:wasMerged).and_return(true)
      @admin_service.should_receive(:updateConfig).with(submitted_copy, an_instance_of(HttpLocalizedOperationResult)).and_return(config_validity)

      put :update, {:go_config => submitted_copy}

      flash[:success].should == 'Saved successfully. The configuration was modified by someone else, but your changes were merged successfully.'
      assert_redirect config_view_path
    end
  end

  describe :switch_to_split_pane? do
    it "should return false when config validity is valid" do
      config_validity = double('config validity')
      config_validity.should_receive(:isValid).and_return(true)
      config_validity.should_receive(:isMergeConflict).never
      config_validity.should_receive(:isPostValidationError).never
      actual = @controller.send(:switch_to_split_pane?, config_validity)
      actual.should == false
    end

    it "should return true when config validity says a merge failure" do
      config_validity = double('config validity')
      config_validity.should_receive(:isValid).and_return(false)
      config_validity.should_receive(:isMergeConflict).and_return(true)
      config_validity.should_receive(:isPostValidationError).never
      actual = @controller.send(:switch_to_split_pane?, config_validity)
      actual.should == true
    end

    it "should return true when config validity says a post validation error" do
      config_validity = double('config validity')
      config_validity.should_receive(:isValid).and_return(false)
      config_validity.should_receive(:isMergeConflict).and_return(false)
      config_validity.should_receive(:isPostValidationError).and_return(true)
      actual = @controller.send(:switch_to_split_pane?, config_validity)
      actual.should == true
    end
  end
end
