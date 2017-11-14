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

describe ComparisonController do
  it "should generate & resolve list" do
    expect(:get => '/compare/foo.bar/list/compare_with/10').to route_to(:controller => "comparison", :action => "list", :pipeline_name => 'foo.bar', :other_pipeline_counter => "10", :format=> "json")
  end

  it "should generate the route for show action" do
    expect(:get => '/compare/foo.bar/10/with/9').to route_to(:controller => "comparison", :action => "show", :pipeline_name => 'foo.bar', :from_counter => "10", :to_counter => "9")
  end

  it "should resolve route to show action" do
    expect(:get => '/compare/foo.bar/10/with/9').to route_to(:controller => "comparison", :pipeline_name=>"foo.bar", :action => "show", :from_counter => "10", :to_counter => "9")
  end

  it "should generate the route for page action" do
    expect(:get => '/compare/foo.bar/page/1').to route_to(:controller => "comparison", :action => "page", :pipeline_name => 'foo.bar', :page => "1")
  end

  it "should resolve route to page action" do
    expect(:get => '/compare/foo.bar/page/1').to route_to(:controller => "comparison", :action => "page", :pipeline_name=>"foo.bar", :page => "1")
  end

  it "should generate the route for browse pipeline timeline" do
    expect(:get => '/compare/foo.bar/timeline/5').to route_to(:controller => "comparison", :action => "timeline", :pipeline_name => "foo.bar", :page => "5")
  end

  it "should resolve route to browse pipeline timeline" do
    expect(:get => '/compare/foo.bar/timeline/5').to route_to(:controller => "comparison", :action => "timeline", :pipeline_name => "foo.bar", :page => "5")
  end
end
