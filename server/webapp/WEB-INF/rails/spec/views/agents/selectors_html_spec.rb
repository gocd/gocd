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

require File.expand_path(File.dirname(__FILE__) + '/../../spec_helper')

describe "/shared/selectors" do

  it "should contain a selector for selected resources" do
    assigns[:selections] = [ TriStateSelection.new('resource-1', 'add') ]

    render :partial => 'shared/selectors', :locals => {:scope => {}}

    response.should have_tag("div.selectors") do
      with_tag "select[name='selections[resource-1]']" do
        with_tag "option[value='add'][selected='true']", "add"
        with_tag "option[value='remove']", "remove"
      end
    end
    response.should_not have_tag("option[value='nochange']", "nochange")
  end

  it "should contain the resource name" do
    assigns[:selections] = [ TriStateSelection.new('resource-1', 'add') ]

    render :partial => 'shared/selectors', :locals => {:scope => {}}

    response.should have_tag("span.selector", "resource-1")
  end

  it "should contain a selector for unchanged resources" do
    assigns[:selections] = [ TriStateSelection.new('resource-1', 'nochange') ]

    render :partial => 'shared/selectors', :locals => {:scope => {}}

    response.should have_tag("div.selectors") do
      with_tag "select[name='selections[resource-1]']" do
        with_tag "option[value='add']"
        with_tag "option[value='remove']"
        with_tag "option[value='nochange'][selected='true']"
      end
    end
  end

  it "should contain a selector for removed resources" do
    assigns[:selections] = [ TriStateSelection.new('resource-1', 'remove') ]

    render :partial => 'shared/selectors', :locals => {:scope => {}}

    response.should have_tag("div.selectors") do
      with_tag "select[name='selections[resource-1]']" do
        with_tag "option[value='add']"
        with_tag "option[value='remove'][selected='true']"
      end
    end
    response.should_not have_tag("option[value='nochange']", "nochange")
  end

  it "should add an id to selector" do
    selected_reosurce = TriStateSelection.new('resource-1', 'add')
    assigns[:selections] = [ selected_reosurce ]

    render :partial => 'shared/selectors', :locals => {:scope => {}}

    response.should have_tag("div.selectors") do
      with_tag "select[id='field_for_#{selected_reosurce.object_id}']"
    end
  end


  it "should show a view for the selector" do
    selected_reosurce = TriStateSelection.new('resource-1', 'add')
    assigns[:selections] = [ selected_reosurce ]

    render :partial => 'shared/selectors', :locals => {:scope => {}}

    response.should have_tag("div.selectors") do
      with_tag "span[id='view_for_#{selected_reosurce.object_id}']"
    end
  end

  it "should contain scripts to hook up the tristates" do
    selected_reosurce = TriStateSelection.new('resource-1', 'add')
    assigns[:selections] = [ selected_reosurce ]

    render :partial => 'shared/selectors', :locals => {:scope => {}}

    response.should have_tag("script", /new TriStateCheckbox\(\$\('view_for_#{selected_reosurce.object_id}'\), \$\('field_for_#{selected_reosurce.object_id}'\), true\);/)
  end

end
