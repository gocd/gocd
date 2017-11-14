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

shared_examples_for :material_controller_routes do
  describe "routes should resolve and generate" do
    it "new" do
      expect({:get => "/admin/pipelines/pipeline.name/materials/#{@short_material_type}/new"}).to route_to(:controller => "admin/materials/#{@short_material_type}", :action => "new", :pipeline_name => "pipeline.name")
      expect(send("admin_#{@short_material_type}_new_path", :pipeline_name => "foo.bar")).to eq("/admin/pipelines/foo.bar/materials/#{@short_material_type}/new")
    end

    it "create" do
      expect({:post => "/admin/pipelines/pipeline.name/materials/#{@short_material_type}"}).to route_to(:controller => "admin/materials/#{@short_material_type}", :action => "create", :pipeline_name => "pipeline.name")
      expect(send("admin_#{@short_material_type}_create_path", :pipeline_name => "foo.bar")).to eq("/admin/pipelines/foo.bar/materials/#{@short_material_type}")
    end

    it "update" do
      expect({:put => "/admin/pipelines/pipeline.name/materials/#{@short_material_type}/finger_print"}).to route_to(:controller => "admin/materials/#{@short_material_type}", :action => "update", :pipeline_name => "pipeline.name", :finger_print => "finger_print")
      expect(send("admin_#{@short_material_type}_update_path", :pipeline_name => "foo.bar", :finger_print => "abc")).to eq("/admin/pipelines/foo.bar/materials/#{@short_material_type}/abc")
    end

    it "edit" do
      expect({:get => "/admin/pipelines/pipeline.name/materials/#{@short_material_type}/finger_print/edit"}).to route_to(:controller => "admin/materials/#{@short_material_type}", :action => "edit", :pipeline_name => "pipeline.name", :finger_print => "finger_print")
      expect(send("admin_#{@short_material_type}_edit_path", :pipeline_name => "foo.bar", :finger_print => "finger_print")).to eq("/admin/pipelines/foo.bar/materials/#{@short_material_type}/finger_print/edit")
    end

    it "delete" do
      expect({:delete => "/admin/pipelines/pipeline.name/materials/finger_print"}).to route_to(:controller => "admin/materials", :action => "destroy", :stage_parent => "pipelines", :pipeline_name => "pipeline.name", :finger_print => "finger_print")
      expect(send("admin_material_delete_path", :pipeline_name => "foo.bar", :finger_print => "finger_print")).to eq("/admin/pipelines/foo.bar/materials/finger_print")
    end
  end
end
