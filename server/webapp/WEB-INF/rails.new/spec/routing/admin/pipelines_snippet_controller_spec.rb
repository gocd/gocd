##########################################################################
# Copyright 2017 ThoughtWorks, Inc.
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
##########################################################################

require 'rails_helper'

describe Admin::PipelinesSnippetController do
  it "should resolve the route to partial config page" do
    expect({:get => "/admin/pipelines/snippet"}).to route_to(:controller => "admin/pipelines_snippet", :action => "index")
    expect(pipelines_snippet_path).to eq("/admin/pipelines/snippet")
  end

  it "should resolve route to get group xml" do
    expect({:get => "/admin/pipelines/snippet/foo.bar"}).to route_to(:controller => "admin/pipelines_snippet", :action => "show", :group_name => "foo.bar")
    expect(pipelines_snippet_show_path(:group_name => 'foo.bar')).to eq("/admin/pipelines/snippet/foo.bar")
  end

  it "should resolve route to save group xml" do
    expect({:put => "/admin/pipelines/snippet/foo.bar"}).to route_to(:controller => "admin/pipelines_snippet", :action => "update", :group_name => "foo.bar")
    expect(pipelines_snippet_update_path(:group_name => 'foo.bar')).to eq("/admin/pipelines/snippet/foo.bar")
  end

  it "should resolve route to edit group xml" do
    expect({:get => "/admin/pipelines/snippet/foo.bar/edit"}).to route_to(:controller => "admin/pipelines_snippet", :action => "edit", :group_name => "foo.bar")
    expect(pipelines_snippet_edit_path(:group_name => 'foo.bar')).to eq("/admin/pipelines/snippet/foo.bar/edit")
  end
end
