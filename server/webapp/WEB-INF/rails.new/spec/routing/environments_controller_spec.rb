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

describe EnvironmentsController do
  describe "index, create and update" do
    it "should match /environments defaulting to html format" do
      expect(:get => "/environments").to route_to({:controller => "environments", :action => 'index', :format => :html})
    end

    it "should match /environments defaulting to json format" do
      expect(:post => "/environments.json").to route_to({:controller => "environments", :action => 'index', :format => "json"})
    end

    it "should match /new" do
      expect(:get => "/environments/new").to route_to({:controller => "environments", :action => 'new', :no_layout => true})
    end

    it "should match /create" do
      expect(:post => "/environments/create").to route_to({:controller => "environments", :action => 'create', :no_layout => true})
    end

    it "should match /update" do
      expect(:put => "/environments/foo").to route_to({:no_layout=>true, :controller => "environments", :action => 'update', :name => 'foo'})
      expect(:put => "/environments/foo.bar.baz").to route_to({:no_layout=>true, :controller => "environments", :action => 'update', :name => 'foo.bar.baz'})
    end

    it "should match /show" do
      expect(:get => "/environments/foo/show").to route_to({:controller => "environments", :action => 'show', :name => 'foo'})
      expect(:get => "/environments/foo.bar.baz/show").to route_to({:controller => "environments", :action => 'show', :name => 'foo.bar.baz'})
    end

    it "should match /edit/pipelines" do
      expect(:get => "/environments/foo/edit/pipelines").to route_to({:controller => "environments", :action => 'edit_pipelines', :name => 'foo', :no_layout => true})
      expect(:get => "/environments/foo.bar.baz/edit/pipelines").to route_to({:controller => "environments", :action => 'edit_pipelines', :name => 'foo.bar.baz', :no_layout => true})
    end

    it "should match /edit/agents" do
      expect(:get => "/environments/foo/edit/agents").to route_to({:controller => "environments", :action => 'edit_agents', :name => 'foo', :no_layout => true})
      expect(:get => "/environments/foo.bar.baz/edit/agents").to route_to({:controller => "environments", :action => 'edit_agents', :name => 'foo.bar.baz', :no_layout => true})
    end

    it "should match /edit/variables" do
      expect(:get => "/environments/foo/edit/variables").to route_to({:controller => "environments", :action => 'edit_variables', :name => 'foo', :no_layout => true})
      expect(:get => "/environments/foo.bar.baz/edit/variables").to route_to({:controller => "environments", :action => 'edit_variables', :name => 'foo.bar.baz', :no_layout => true})
    end
  end
end
