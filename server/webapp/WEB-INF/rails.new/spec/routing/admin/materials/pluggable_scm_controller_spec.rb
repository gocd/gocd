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

describe Admin::Materials::PluggableScmController do
  describe "routes should resolve and generate" do
    it "show_existing" do
      expect({:get => '/admin/pipelines/pipeline.name/materials/pluggable_scm/show_existing'}).to route_to(:controller => 'admin/materials/pluggable_scm', :action => 'show_existing', :pipeline_name => 'pipeline.name')
      expect(send('admin_pluggable_scm_show_existing_path', :pipeline_name => 'foo.bar')).to eq('/admin/pipelines/foo.bar/materials/pluggable_scm/show_existing')
    end

    it "choose_existing" do
      expect({:post => '/admin/pipelines/pipeline.name/materials/pluggable_scm/choose_existing'}).to route_to(:controller => 'admin/materials/pluggable_scm', :action => 'choose_existing', :pipeline_name => 'pipeline.name')
      expect(send('admin_pluggable_scm_choose_existing_path', :pipeline_name => 'foo.bar')).to eq('/admin/pipelines/foo.bar/materials/pluggable_scm/choose_existing')
    end

    it "new" do
      expect({:get => '/admin/pipelines/pipeline.name/materials/pluggable_scm/new/plugin.id-1'}).to route_to(:controller => 'admin/materials/pluggable_scm', :action => 'new', :pipeline_name => 'pipeline.name', :plugin_id => 'plugin.id-1')
      expect(send('admin_pluggable_scm_new_path', :pipeline_name => 'foo.bar', :plugin_id => 'plugin-id')).to eq('/admin/pipelines/foo.bar/materials/pluggable_scm/new/plugin-id')
    end

    it "create" do
      expect({:post => '/admin/pipelines/pipeline.name/materials/pluggable_scm/plugin.id-1'}).to route_to(:controller => 'admin/materials/pluggable_scm', :action => 'create', :pipeline_name => 'pipeline.name', :plugin_id => 'plugin.id-1')
      expect(send('admin_pluggable_scm_create_path', :pipeline_name => 'foo.bar', :plugin_id => 'plugin-id')).to eq('/admin/pipelines/foo.bar/materials/pluggable_scm/plugin-id')
    end

    it "edit" do
      expect({:get => '/admin/pipelines/pipeline.name/materials/pluggable_scm/finger_print/edit'}).to route_to(:controller => 'admin/materials/pluggable_scm', :action => 'edit', :pipeline_name => 'pipeline.name', :finger_print => 'finger_print')
      expect(send('admin_pluggable_scm_edit_path', :pipeline_name => 'foo.bar', :finger_print => 'finger_print')).to eq('/admin/pipelines/foo.bar/materials/pluggable_scm/finger_print/edit')
    end

    it "update" do
      expect({:put => '/admin/pipelines/pipeline.name/materials/pluggable_scm/finger_print'}).to route_to(:controller => 'admin/materials/pluggable_scm', :action => 'update', :pipeline_name => 'pipeline.name', :finger_print => 'finger_print')
      expect(send('admin_pluggable_scm_update_path', :pipeline_name => 'foo.bar', :finger_print => 'finger_print')).to eq('/admin/pipelines/foo.bar/materials/pluggable_scm/finger_print')
    end

    it "check_connection" do
      expect({:post => '/admin/materials/pluggable_scm/check_connection/plugin.id-1'}).to route_to(:controller => 'admin/materials/pluggable_scm', :action => 'check_connection', :plugin_id => 'plugin.id-1')
      expect(send('admin_pluggable_scm_check_connection_path', :plugin_id => 'plugin-id')).to eq('/admin/materials/pluggable_scm/check_connection/plugin-id')
    end

    it "pipelines_used_in" do
      expect({:get => '/admin/materials/pluggable_scm/scm-id/pipelines_used_in'}).to route_to(:controller => 'admin/materials/pluggable_scm', :action => 'pipelines_used_in', :scm_id => 'scm-id')
      expect(send('scm_pipelines_used_in_path', :scm_id => 'scm-id')).to eq('/admin/materials/pluggable_scm/scm-id/pipelines_used_in')
    end
  end
end
