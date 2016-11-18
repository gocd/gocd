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

describe "shared/_selectors.html.erb" do

  it "should contain a selector for selected resources" do
    render :partial => 'shared/selectors', :locals => {:scope => {:selections => [ TriStateSelection.new('resource-1', 'add') ]}}

    Capybara.string(response.body).find("div.selectors").tap do |div|
      div.find("select[name='selections[resource-1]']").tap do |f|
        expect(f).to have_selector("option[value='add'][selected='true']", :text => "add")
        expect(f).to have_selector("option[value='remove']", :text => "remove")
      end
    end

    expect(response).to_not have_selector("option[value='nochange']", :text => "nochange")
  end

  it "should contain the resource name" do
    render :partial => 'shared/selectors', :locals => {:scope => {:selections => [ TriStateSelection.new('resource-1', 'add') ]}}

    expect(response).to have_selector("span.selector", :text => "resource-1")
  end

  it "should contain a selector for unchanged resources" do
    render :partial => 'shared/selectors', :locals => {:scope => {:selections => [ TriStateSelection.new('resource-1', 'nochange') ]}}

    Capybara.string(response.body).find("div.selectors").tap do |div|
      div.find("select[name='selections[resource-1]']").tap do |f|
        expect(f).to have_selector("option[value='add']")
        expect(f).to have_selector("option[value='remove']")
        expect(f).to have_selector("option[value='nochange'][selected='true']")
      end
    end
  end

  it "should contain a selector for removed resources" do
    render :partial => 'shared/selectors', :locals => {:scope => {:selections => [ TriStateSelection.new('resource-1', 'remove') ]}}

    Capybara.string(response.body).find("div.selectors").tap do |div|
      div.find("select[name='selections[resource-1]']").tap do |f|
        expect(f).to have_selector("option[value='add']")
        expect(f).to have_selector("option[value='remove'][selected='true']")
      end
    end

    expect(response).to_not have_selector("option[value='nochange']")
  end

  it "should add an id to selector" do
    selected_resource = TriStateSelection.new('resource-1', 'add')

    render :partial => 'shared/selectors', :locals => {:scope => {:selections => [ selected_resource ]}}
    Capybara.string(response.body).find("div.selectors").tap do |div|
        expect(div).to have_selector("select[id='field_for_#{selected_resource.object_id}']")
    end
  end

  it "should show a view for the selector" do
    selected_resource = TriStateSelection.new('resource-1', 'add')
    render :partial => 'shared/selectors', :locals => {:scope => {:selections => [ selected_resource ]}}

    Capybara.string(response.body).find("div.selectors").tap do |div|
      expect(div).to have_selector("span[id='view_for_#{selected_resource.object_id}']")
    end
  end

  it "should contain scripts to hook up the tristates" do
    selected_resource = TriStateSelection.new('resource-1', 'add')
    render :partial => 'shared/selectors', :locals => {:scope => {:selections => [ selected_resource ]}}

    expect(response).to have_selector("script", :text => /new TriStateCheckbox\(\$\('view_for_#{selected_resource.object_id}'\), \$\('field_for_#{selected_resource.object_id}'\), true\);/, :visible => false)
  end
end
