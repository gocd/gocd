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

describe Admin::ConfigurationController do
  include ExtraSpecAssertions

  before(:each) do
    @admin_service = double("admin_service")
    @config_repository = double("config_repository")
    allow(controller).to receive(:admin_service).and_return(@admin_service)
    allow(controller).to receive(:config_repository).and_return(@config_repository)
  end

  describe "tab_name" do
    before :each do
      @config = {"content" => "config-content", "md5" => "md5", "location" => "/foo/bar"}
      cruise_config_revision = double('cruise config revision')
      expect(@config_repository).to receive(:getRevision).with(@config['md5']).and_return(cruise_config_revision)
    end

    it "should set tab name for show" do
      expect(@admin_service).to receive(:populateModel).with(anything).and_return(@config)
      get :show
      expect(assigns[:tab_name]).to eq('configuration-xml')
    end

    it "should set tab name for edit" do
      expect(@admin_service).to receive(:configurationMapForSourceXml).and_return(@config)
      get :edit
      expect(assigns[:tab_name]).to eq('configuration-xml')
    end
  end

  describe "view_title" do
    before :each do
      @config = {"content" => "config-content", "md5" => "md5", "location" => "/foo/bar"}
      cruise_config_revision = double('cruise config revision')
      expect(@config_repository).to receive(:getRevision).with(@config['md5']).and_return(cruise_config_revision)
    end

    it "should set tab name for show" do
      expect(@admin_service).to receive(:populateModel).with(anything).and_return(@config)

      get :show

      expect(assigns[:view_title]).to eq('Administration')
      assert_template layout: "admin"
    end

    it "should set tab name for edit" do
      expect(@admin_service).to receive(:configurationMapForSourceXml).and_return(@config)

      get :edit

      expect(assigns[:view_title]).to eq('Administration')
      assert_template layout: "admin"
    end
  end

  describe "show" do
    it "should render view with config" do
      config = {"content" => "config-content", "md5" => "md5", "location" => "/foo/bar"}
      expect(@admin_service).to receive(:populateModel).with(anything).and_return(config)
      cruise_config_revision = double('cruise config revision')
      expect(@config_repository).to receive(:getRevision).with(config['md5']).and_return(cruise_config_revision)

      get :show

      expect(response).to render_template "show"
      expect(assigns[:go_config].content).to eq("config-content")
      expect(assigns[:go_config].md5).to eq("md5")
      expect(assigns[:go_config].location).to eq("/foo/bar")
      expect(assigns[:go_config_revision]).to eq(cruise_config_revision)
    end
  end

  describe "edit" do
    it "should render edit" do
      config = {"content" => "config-content", "md5" => "md5", "location" => "/foo/bar"}
      expect(@admin_service).to receive(:configurationMapForSourceXml).and_return(config)
      cruise_config_revision = double('cruise config revision')
      expect(@config_repository).to receive(:getRevision).with(config['md5']).and_return(cruise_config_revision)

      get :edit

      expect(response).to render_template "edit"
      expect(assigns[:go_config].content).to eq("config-content")
      expect(assigns[:go_config].md5).to eq("md5")
      expect(assigns[:go_config].location).to eq("/foo/bar")
      expect(assigns[:go_config_revision]).to eq(cruise_config_revision)
    end
  end

  describe "update" do
    it "should update the configuration" do
      param_map = {"content" => "config_content", "md5" => "md5"}
      expect(@admin_service).to receive(:updateConfig).with(param_map, an_instance_of(HttpLocalizedOperationResult)).and_return(GoConfigValidity::valid())

      put :update, params: { :go_config => param_map }

      expect(flash[:success]).to eq('Saved successfully.')
      assert_redirect config_view_path
    end

    it "should render edit page when config save fails" do
      current_config = {"content" => "config-content", "md5" => "current-md5", "location" => "/foo/bar"}
      submitted_copy = {"content" => "edited-content", "md5" => "md5"}
      config_validity = double('config validity')
      expect(config_validity).to receive(:isValid).and_return(false)
      expect(config_validity).to receive(:errorMessage).and_return('Wrong config xml')
      allow(controller).to receive(:switch_to_split_pane?).once.with(config_validity).and_return(false)
      expect(@admin_service).to receive(:configurationMapForSourceXml).and_return(current_config)
      expect(@admin_service).to receive(:updateConfig).with(submitted_copy, an_instance_of(HttpLocalizedOperationResult)).and_return(config_validity)
      cruise_config_revision = double('cruise config revision')
      expect(@config_repository).to receive(:getRevision).with(submitted_copy['md5']).and_return(cruise_config_revision)

      put :update, params: { :go_config => submitted_copy }

      expect(response).to render_template "edit"
      expect(flash.now[:error]).to eq('Save failed, see errors below')
      expect(assigns[:errors][0]).to eq("Wrong config xml")
      expect(assigns[:go_config].content).to eq("edited-content")
      expect(assigns[:go_config].md5).to eq("md5")
      expect(assigns[:go_config].location).to eq("/foo/bar")
      expect(assigns[:go_config_revision]).to eq(cruise_config_revision)
    end

    it "should render split pane when config save fails because of merge conflict" do
      current_config = {"content" => "config-content", "md5" => "md5", "location" => "/foo/bar"}
      submitted_copy = {"content" => "content-which-caused-conflict", "md5" => "md5"}
      expect(@admin_service).to receive(:configurationMapForSourceXml).and_return(current_config)
      config_validity = double('config validity')
      expect(config_validity).to receive(:isValid).and_return(false)
      expect(config_validity).to receive(:errorMessage).and_return('Conflict in merging')
      allow(controller).to receive(:switch_to_split_pane?).once.with(config_validity).and_return(true)
      expect(@admin_service).to receive(:updateConfig).with(submitted_copy, an_instance_of(HttpLocalizedOperationResult)).and_return(config_validity)
      cruise_config_revision = double('cruise config revision')
      expect(@config_repository).to receive(:getRevision).with(current_config['md5']).and_return(cruise_config_revision)

      put :update, params: { :go_config => submitted_copy }

      expect(response).to render_template "split_pane"
      expect(flash.now[:error]).to eq("Someone has modified the configuration and your changes are in conflict. Please review, amend and retry.")
      expect(assigns[:errors][0]).to eq("Conflict in merging")
      expect(assigns[:flash_help_link]).to eq("<a class='' href='https://docs.gocd.org/current/configuration/configuration_reference.html' target='_blank'>Help Topic: Configuration</a>")
      expect(assigns[:conflicted_config].content).to eq(submitted_copy['content'])
      expect(assigns[:conflicted_config].md5).to eq(submitted_copy['md5'])
      expect(assigns[:conflicted_config].location).to eq(submitted_copy['location'])
      expect(assigns[:go_config].content).to eq(current_config['content'])
      expect(assigns[:go_config].md5).to eq(current_config['md5'])
      expect(assigns[:go_config].location).to eq(current_config['location'])
      expect(assigns[:go_config_revision]).to eq(cruise_config_revision)
    end

    it "should render display configuration merged successfully when a merge happens" do
      submitted_copy = {"content" => "config_content_1", "md5" => "md5"}
      config_validity = double('config_validity')
      expect(config_validity).to receive(:isValid).and_return(true)
      expect(config_validity).to receive(:wasMerged).and_return(true)
      expect(@admin_service).to receive(:updateConfig).with(submitted_copy, an_instance_of(HttpLocalizedOperationResult)).and_return(config_validity)

      put :update, params: { :go_config => submitted_copy }

      expect(flash[:success]).to eq('Saved successfully. The configuration was modified by someone else, but your changes were merged successfully.')
      assert_redirect config_view_path
    end
  end

  describe "switch_to_split_pane?" do
    it "should return false when config validity is valid" do
      config_validity = double('config validity')
      expect(config_validity).to receive(:isValid).and_return(true)
      expect(config_validity).to receive(:isMergeConflict).never
      expect(config_validity).to receive(:isPostValidationError).never
      actual = @controller.send(:switch_to_split_pane?, config_validity)
      expect(actual).to eq(false)
    end

    it "should return true when config validity says a merge failure" do
      config_validity = double('config validity')
      expect(config_validity).to receive(:isValid).and_return(false)
      expect(config_validity).to receive(:isMergeConflict).and_return(true)
      expect(config_validity).to receive(:isPostValidationError).never
      actual = @controller.send(:switch_to_split_pane?, config_validity)
      expect(actual).to eq(true)
    end

    it "should return true when config validity says a post validation error" do
      config_validity = double('config validity')
      expect(config_validity).to receive(:isValid).and_return(false)
      expect(config_validity).to receive(:isMergeConflict).and_return(false)
      expect(config_validity).to receive(:isPostValidationError).and_return(true)
      actual = @controller.send(:switch_to_split_pane?, config_validity)
      expect(actual).to eq(true)
    end
  end
end
