#
# Copyright 2021 ThoughtWorks, Inc.
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
#

require 'rails_helper'

describe Admin::ConfigurationController do
  include ExtraSpecAssertions

  before(:each) do
    @admin_service = double("admin_service")
    @config_repository = double("config_repository")
    @go_config_service = double('go config service')
    allow(@go_config_service).to receive(:checkConfigFileValid).and_return(GoConfigValidity.valid())
    allow(controller).to receive(:admin_service).and_return(@admin_service)
    allow(controller).to receive(:config_repository).and_return(@config_repository)
    allow(controller).to receive(:go_config_service).and_return(@go_config_service)
  end

  describe "tab_name" do
    before :each do
      cruise_config_revision = GoConfigRevision.new("config-content", "md5", "loser", "2.3.0", TimeProvider.new)
      expect(@go_config_service).to receive(:fileLocation).and_return('/foo/bar')
      expect(@go_config_service).to receive(:getConfigAtVersion).with('current').and_return(cruise_config_revision)
      expect(@config_repository).to receive(:getRevision).with('md5').and_return(cruise_config_revision)
    end

    it "should set tab name for show" do
      get :show
      expect(assigns[:tab_name]).to eq('configuration-xml')
    end

    it "should set tab name for edit" do
      get :edit
      expect(assigns[:tab_name]).to eq('configuration-xml')
    end
  end

  describe "view_title" do
    before :each do
      cruise_config_revision = GoConfigRevision.new("config-content", "md5", "loser", "2.3.0", TimeProvider.new)
      expect(@go_config_service).to receive(:fileLocation).and_return('/foo/bar')
      expect(@go_config_service).to receive(:getConfigAtVersion).with('current').and_return(cruise_config_revision)
      expect(@config_repository).to receive(:getRevision).with('md5').and_return(cruise_config_revision)
    end

    it "should set tab name for show" do

      get :show

      expect(assigns[:view_title]).to eq('Administration')
      assert_template layout: "admin"
    end

    it "should set tab name for edit" do
      get :edit

      expect(assigns[:view_title]).to eq('Administration')
      assert_template layout: "admin"
    end
  end

  describe "routes" do
    it "view" do
      expect(config_view_path).to eq("/admin/config_xml")
      expect({:get => "/admin/config_xml"}).to route_to(:controller => "admin/configuration", :action => "show")
    end

    it "edit" do
      expect(config_edit_path).to eq("/admin/config_xml/edit")
      expect({:get => "/admin/config_xml/edit"}).to route_to(:controller => "admin/configuration", :action => "edit")
    end

    it "update" do
      expect(config_update_path).to eq("/admin/config_xml")
      expect({:put => "/admin/config_xml"}).to route_to(:controller => "admin/configuration", :action => "update")
    end
  end

  describe "show" do
    it "should render view with config" do
      cruise_config_revision = GoConfigRevision.new("config-content", "md5", "loser", "2.3.0", TimeProvider.new)
      expect(@go_config_service).to receive(:fileLocation).and_return('/foo/bar')
      expect(@go_config_service).to receive(:getConfigAtVersion).with('current').and_return(cruise_config_revision)
      expect(@config_repository).to receive(:getRevision).with('md5').and_return(cruise_config_revision)
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
      cruise_config_revision = GoConfigRevision.new("config-content", "md5", "loser", "2.3.0", TimeProvider.new)
      expect(@go_config_service).to receive(:fileLocation).and_return('/foo/bar')
      expect(@go_config_service).to receive(:getConfigAtVersion).with('current').and_return(cruise_config_revision)
      expect(@config_repository).to receive(:getRevision).with('md5').and_return(cruise_config_revision)
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

      put :update, params:{:go_config => param_map}

      expect(flash[:success]).to eq('Saved successfully.')
      assert_redirect config_view_path
    end

    it "should render edit page when config save fails" do
      expect(@go_config_service).to receive(:fileLocation).and_return('/foo/bar')

      current_config = GoConfigRevision.new("config-content", "current-md5", "loser", "2.3.0", TimeProvider.new)
      expect(@go_config_service).to receive(:getConfigAtVersion).with('current').and_return(current_config)

      submitted_copy = {"content" => "edited-content", "md5" => "md5"}
      config_validity = double('config validity')
      expect(config_validity).to receive(:isValid).and_return(false)
      expect(config_validity).to receive(:errorMessage).and_return('Wrong config xml')
      allow(controller).to receive(:switch_to_split_pane?).once.with(config_validity).and_return(false)
      expect(@admin_service).to receive(:updateConfig).with(submitted_copy, an_instance_of(HttpLocalizedOperationResult)).and_return(config_validity)
      cruise_config_revision = double('cruise config revision')
      expect(@config_repository).to receive(:getRevision).with(submitted_copy['md5']).and_return(cruise_config_revision)

      put :update, params:{:go_config => submitted_copy}

      expect(response).to render_template "edit"
      expect(flash.now[:error]).to eq('Save failed, see errors below')
      expect(assigns[:errors][0]).to eq("Wrong config xml")
      expect(assigns[:go_config].content).to eq("edited-content")
      expect(assigns[:go_config].md5).to eq("md5")
      expect(assigns[:go_config].location).to eq("/foo/bar")
      expect(assigns[:go_config_revision]).to eq(cruise_config_revision)
    end

    it "should render split pane when config save fails because of merge conflict" do
      expect(@go_config_service).to receive(:fileLocation).and_return('/foo/bar')

      current_config = GoConfigRevision.new("config-content", "md5", "loser", "2.3.0", TimeProvider.new)
      expect(@go_config_service).to receive(:getConfigAtVersion).with('current').and_return(current_config)
      expect(@config_repository).to receive(:getRevision).with('md5').and_return(current_config)

      submitted_copy = {"content" => "content-which-caused-conflict", "md5" => "md5"}

      config_validity = double('config validity')
      expect(config_validity).to receive(:isValid).and_return(false)
      expect(config_validity).to receive(:errorMessage).and_return('Conflict in merging')
      allow(controller).to receive(:switch_to_split_pane?).once.with(config_validity).and_return(true)
      expect(@admin_service).to receive(:updateConfig).with(submitted_copy, an_instance_of(HttpLocalizedOperationResult)).and_return(config_validity)

      put :update, params: {:go_config => submitted_copy}

      expect(response).to render_template "split_pane"
      expect(flash.now[:error]).to eq("Someone has modified the configuration and your changes are in conflict. Please review, amend and retry.")
      expect(assigns[:errors][0]).to eq("Conflict in merging")
      expect(assigns[:flash_help_link]).to eq("<a class='' href='#{CurrentGoCDVersion.docs_url('/configuration/configuration_reference.html')}' target='_blank'>Help Topic: Configuration</a>")
      expect(assigns[:conflicted_config].content).to eq(submitted_copy['content'])
      expect(assigns[:conflicted_config].md5).to eq(submitted_copy['md5'])
      expect(assigns[:conflicted_config].location).to eq(submitted_copy['location'])
      expect(assigns[:go_config].content).to eq(current_config.getContent)
      expect(assigns[:go_config].md5).to eq(current_config.getMd5)
      expect(assigns[:go_config].location).to eq('/foo/bar')
      expect(assigns[:go_config_revision]).to eq(current_config)
    end

    it "should render display configuration merged successfully when a merge happens" do
      submitted_copy = {"content" => "config_content_1", "md5" => "md5"}
      config_validity = double('config_validity')
      expect(config_validity).to receive(:isValid).and_return(true)
      expect(config_validity).to receive(:wasMerged).and_return(true)
      expect(@admin_service).to receive(:updateConfig).with(submitted_copy, an_instance_of(HttpLocalizedOperationResult)).and_return(config_validity)

      put :update, params:{:go_config => submitted_copy}

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
