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

describe PipelinesController do
  describe "build_cause" do
    it "should route to build_cause action" do
      expect({:get => "/pipelines/name_of_pipeline/15/build_cause"}).to route_to(:controller => "pipelines", :action => "build_cause", :pipeline_name => "name_of_pipeline", :pipeline_counter => "15", :no_layout => true)
    end

    it "should route to build_cause action with dots in pipline name" do
      expect({:get => "/pipelines/blah.pipe-line/1/build_cause"}).to route_to(:controller => "pipelines", :action => "build_cause", :pipeline_name => "blah.pipe-line", :pipeline_counter => "1", :no_layout => true)
    end

    it "should have a named route" do
      expect(:get => build_cause_path(:pipeline_name => "foo", :pipeline_counter => 20)).to route_to(:controller => "pipelines", :action => "build_cause", :pipeline_name => "foo", :pipeline_counter => "20", "no_layout"=>true)
    end
  end

  describe "index" do
    it "should resolve" do
      expect({:get => "/pipelines"}).to route_to(:controller => "pipelines", :action => "index", :format => "html")
      expect({:get => "/pipelines.json"}).to route_to(:controller => "pipelines", :action => "index", :format => "json")
    end
  end

  describe "action show_for_trigger" do
    it "should resolve POST to /pipelines/show_for_trigger as a call" do
      expect({:post => "/pipelines/show_for_trigger"}).to route_to(:controller => 'pipelines', :action => 'show_for_trigger', :no_layout => true)
    end
  end


  describe "action show" do
    it "should resolve using both GET and POST" do
      expect({:get => "/pipelines/show"}).to route_to(:controller => "pipelines", :action => "show")
      expect({:post => "/pipelines/show"}).to route_to(:controller => "pipelines", :action => "show")
    end

  end

  it "should resolve get to /pipelines/material_search as a call" do
    expect({:get => "/pipelines/material_search"}).to route_to(:controller => "pipelines", :action => "material_search", :no_layout => true)
  end
end
