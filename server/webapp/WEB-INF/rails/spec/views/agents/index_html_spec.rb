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

describe "/agents/index" do
  before do
    stub_server_health_messages
  end
  include AgentsHelper
  include AgentMother
  include GoUtil

  before(:all) do
    unless defined? LOCATOR1
      LOCATOR1 = 'blue/2/stage/3/job/'
      LOCATOR2 = 'pink/2/stage/3/job/'
      LOCATOR3 = 'french/2/stage/3/job/'
      LAST_HEARD_TIME = java.util.Date.new
    end
  end

  before(:each) do
    @agent1 = idle_agent(:hostname => 'host1', :location => '/var/lib/cruise-agent', :operating_system => "Linux", :uuid => "UUID_host1", :agent_launcher_version => "12.3")
    @agent2 = pending_agent(:location => 'C:\Program Files\Cruise Agent', :ip_address => '127.0.0.1')
    @agent3 = cancelled_agent(:locator => LOCATOR2, :ip_address => "10.6.6.6", :space => 10*1024*1024*1024, :resources => "db,dbSync")
    @agent4 = building_agent(:locator => LOCATOR1, :uuid => "UUID_host4", :resources => "java")
    @agent5 = idle_agent(:space => 12*1024*1024*1024, :hostname => "foo_baz_host", :uuid => "UUID_host5", :resources => "vs.net,nant", :operating_system => "Windows")
    @agent6 = missing_agent(:uuid => "UUID_host6", :hostname => "foo_bar_host", :environments=> ["uat", "blah"])
    @agent7 = cancelled_agent(:locator => LOCATOR2, :uuid => "UUID_host7")
    @agent8 = lost_contact_agent(:locator => LOCATOR3, :uuid => "UUID_host8")
    @agent9 = lost_contact_agent(:locator => '', :uuid => "UUID_host9")
    assigns[:agents] = AgentsViewModel.new([@agent1, @agent2, @agent3, @agent4, @agent5, @agent6, @agent7, @agent8, @agent9].to_java(AgentViewModel))

    template.stub!(:can_view_admin_page?).and_return(true)
    template.stub!(:edit_agent_path).and_return("foo")
    template.stub!(:has_view_or_operate_permission_on_pipeline?).and_return(true)
    template.stub!(:is_user_an_admin?).and_return(true)
    stub_context_path(template)
    template.stub!(:default_url_options).and_return({})
  end

  describe "agents page has filtering capability" do

    before do
      template.stub(:has_operate_permission_for_agents?).and_return(true)
      in_params(:column => "status", :order => "ASC", :filter => "foo:bar, moo:boo")
    end

    it "should have filter textbox" do
      template.stub!(:default_url_options).and_return({:order => 'ASC', :column => 'status', :autoRefresh => 'false'})
      render "agents/index.html"

      response.should have_tag("div.filter_agents") do
        with_tag("input[type='text'][name='filter'][value=?]", "foo:bar, moo:boo")
        with_tag("input[type='hidden'][name='column'][value='status']")
        with_tag("input[type='hidden'][name='order'][value='ASC']")
        with_tag("input[type='hidden'][name='autoRefresh'][value='false']")
        with_tag("button[type='submit']")
        with_tag("a[href='/agents?filter='][id='clear_filter']", "Clear")
      end
    end

    it "should maintain sorting order with filtering" do
      render "agents/index.html"

      response.body.should have_tag("table#agent_details") do
        with_tag("th.hostname a[href=?]", "/agents?column=hostname&amp;filter=foo%3Abar%2C+moo%3Aboo&amp;order=ASC")
        with_tag("th.location a[href=?]", "/agents?column=location&amp;filter=foo%3Abar%2C+moo%3Aboo&amp;order=ASC")
        with_tag("th.operating_system a[href=?]", "/agents?column=operating_system&amp;filter=foo%3Abar%2C+moo%3Aboo&amp;order=ASC")
        with_tag("th.ip_address a[href=?]", "/agents?column=ip_address&amp;filter=foo%3Abar%2C+moo%3Aboo&amp;order=ASC")
        with_tag("th.status a[href=?]", "/agents?column=status&amp;filter=foo%3Abar%2C+moo%3Aboo&amp;order=DESC")
        with_tag("th.usable_space a[href=?]", "/agents?column=usable_space&amp;filter=foo%3Abar%2C+moo%3Aboo&amp;order=ASC")
        with_tag("th.resources a[href=?]", "/agents?column=resources&amp;filter=foo%3Abar%2C+moo%3Aboo&amp;order=ASC")
        with_tag("th.environments a[href=?]", "/agents?column=environments&amp;filter=foo%3Abar%2C+moo%3Aboo&amp;order=ASC")
      end
    end

    it "should have help content for filtering in a hidden drop down" do
      render "agents/index.html"

      response.body.should have_tag("div.filter_agents") do
        with_tag("#filter_help.enhanced_dropdown") do
          with_tag(".filter_help_instructions") do
            with_tag("p","Available tags")
            with_tag("p","name:")
            with_tag("p","os:")
            with_tag("p","ip:")
            with_tag("p","status:")
            with_tag("p","environment:")
            with_tag("p","resource:")
            with_tag("a","More...")
          end
        end

      end
    end

    it "should have contextual help for exact matching" do
      render "agents/index.html"

      response.body.should have_tag("div.filter_agents") do
        with_tag("#filter_help.enhanced_dropdown") do
          with_tag("p.heading", "Values")
          with_tag("p", "Put filter values in quotes for exact match")
        end
      end
    end
  end



  describe "has agent operate permissions" do

    before do
      template.stub(:has_operate_permission_for_agents?).and_return(true)
    end

    it "should show the host name" do
      render 'agents/index.html'
      response.should have_tag("td a[href='#{agent_detail_path(:uuid => 'UUID_host1')}']", 'host1')
    end

    it "should show the sandbox directory" do
      render 'agents/index'
      response.should have_tag('table.agents tr.agent_instance') do
        with_tag 'td.location', '/var/lib/cruise-agent'
        with_tag 'td.location', 'C:\Program Files\Cruise Agent'
      end
    end

    it "should show the ip address" do
      render 'agents/index'
      response.should have_tag('table.agents tr.agent_instance') do
        with_tag('td.ip_address', '127.0.0.1')
        with_tag('td.ip_address', '10.6.6.6')
      end
    end

    it "should show free disk space" do
      render 'agents/index'
      response.should have_tag('table.agents tr.agent_instance') do
        with_tag('td.usable_space', /10.0 GB/)
        with_tag('td.usable_space', /12.0 GB/)
        with_tag('td.usable_space', "Unknown")
      end
    end

    it "should show status" do
      render 'agents/index'
      response.should have_tag('table.agents tr.agent_instance') do
        with_tag('td.status', 'building')
        with_tag('td.status', 'pending')
      end
    end

    it "should show resources" do
      render 'agents/index'
      response.should have_tag('table.agents tr.agent_instance') do
        with_tag('td.resources', 'db | dbSync')
        with_tag('td.resources', 'nant | vs.net')
        with_tag('td.resources', 'no resources specified')
      end
    end

    it "should show sort links" do
      params[:column] = 'hostname'
      params[:order] = 'ASC'
      render 'agents/index'
      response.should have_tag('table.agents tr.agent_header') do
        with_tag('th.selector')
        with_tag('th.hostname a[href=/agents?column=hostname&amp;order=DESC]')
        with_tag('th.location a[href=/agents?column=location&amp;order=ASC]')
        with_tag('th.status a[href=/agents?column=status&amp;order=ASC]')
        with_tag('th.usable_space a[href=/agents?column=usable_space&amp;order=ASC]')
        with_tag('th.resources a[href=/agents?column=resources&amp;order=ASC]')
        with_tag('th.ip_address a[href=/agents?column=ip_address&amp;order=ASC]')
        with_tag('th.operating_system a[href=/agents?column=operating_system&amp;order=ASC]')
        with_tag('th.environments a[href=/agents?column=environments&amp;order=ASC]')
      end
    end

    it "columns should be sortable" do
      render 'agents/index'
      xml = HTML::Document.new(response.body, true, false).root
      xml.find_all(:tag => 'a', :parent => {:tag => 'th'}).each do |tag|
        scanned_name = tag.attributes['href'].scan(/column=(\w+)/).to_s
        raise "Didn't find comparator for column named #{scanned_name}" unless (comparator = AgentsController::ORDERS[scanned_name.to_s])
        comparator.is_a?(java.util.Comparator)
      end
    end

    it "should display titles" do

      render 'agents/index'

    end

    it "should add agent status as css_class on the row" do
      render 'agents/index'
      response.should have_tag("tr.agent_instance.Building")
      response.should have_tag("tr.agent_instance.Pending")
    end

    it "should add values of table cells as title" do
      render 'agents/index'

      response.should have_tag("td.hostname[title=host1] a", "host1")
      response.should have_tag("td.location[title=/var/lib/cruise-agent]", "/var/lib/cruise-agent")
      response.should have_tag("td.ip_address[title=127.0.0.1]", "127.0.0.1")
      response.should have_tag("td.status[title=#{LOCATOR1}]") do
        with_tag "a[href=/go/tab/build/detail/#{LOCATOR1}]", "building"
      end
      response.should have_tag("td.usable_space[title='10.0 GB']", "10.0 GB")
      response.should have_tag("td.resources[title='db | dbSync']", "db | dbSync")
    end

    it "should use agent's uuid as id to agent row" do
      render 'agents/index'
      response.should have_tag("tr.agent_instance#UUID_host5") do
        with_tag("td.hostname", "foo_baz_host")
      end
      response.should have_tag("tr.agent_instance#UUID_host6") do
        with_tag("td.hostname", "foo_bar_host")
      end
    end

    it "should show job locator even when cancelled" do
      render 'agents/index'
      response.should have_tag("td.status[title=#{LOCATOR2}]") do
        with_tag "a[href=/go/tab/build/detail/#{LOCATOR2}]", "building (cancelled)"
      end
    end

    it "should show job locator even when building agent goes into lost contact" do
      render 'agents/index'
      response.should have_tag "table.agents tr.agent_instance" do
        with_tag "td.status[title='lost contact at #{@agent9.getLastHeardTime().iso8601()}']", "lost contact"
        with_tag "td.status[title='lost contact at #{@agent8.getLastHeardTime().iso8601()} while building #{LOCATOR3}: job rescheduled']", "lost contact"
      end
      response.should have_tag "td.status[title='lost contact at #{@agent8.getLastHeardTime().iso8601()} while building #{LOCATOR3}: job rescheduled']", "lost contact" do
        with_tag "a[href=/go/tab/build/detail/#{LOCATOR3}]", "lost contact"
      end
    end

    it "should show a table of agents" do
      render 'agents/index'
      response.should have_tag('table.agents')
    end

    it "should set title" do
      assigns[:user] = stub('username', :anonymous? => true)
      render 'agents/index', :layout => 'application'

      response.should have_tag('title', /Agents\s+-\s+Go/)
    end

    it "should not include relative url root in urls" do
      params[:column] = 'status'
      params[:order] = 'asc'
      render 'agents/index'
      response.should have_tag('a.sorted_asc') do |tags|
        tags[0]["href"].should_not include("/rails")
      end
    end

    it "should populate environments" do
      render 'agents/index'
      response.should have_tag("tr#UUID_host6 td.environments", "uat | blah")
      response.should have_tag("tr#UUID_host5 td.environments", "no environments specified")
    end

    it "should populate operating system" do
      render 'agents/index'
      response.should have_tag("tr#UUID_host1 td.operating_system", "Linux")
      response.should have_tag("tr#UUID_host5 td.operating_system", "Windows")
    end

    it "should include a checkbox for each agent" do

      render 'agents/index'
      response.should have_tag('tr#UUID_host6') do |tr|
        tr.should have_tag("td.selector") do |td|
          td.should have_tag("input[type='checkbox'][name='selected[]'][value='UUID_host6'][class='agent_select']")
        end
      end
    end

    it "should remember selected state for agent checkboxes" do
      params[:selected] = ['UUID_host6']
      render 'agents/index'
      response.should have_tag('tr#UUID_host6') do |tr|
        tr.should have_tag("td.selector") do |td|
          td.should have_tag("input[type='checkbox'][name='selected[]'][value='UUID_host6'][class='agent_select'][checked='true']")
        end
      end
    end

    it "should include enable and diasable buttons" do
      params[:column] = "pavan"
      params[:order] = "shilpa"
      params[:foo] = "bar"

      render 'agents/index'

      response.should have_tag("div.edit_panel") do |panel|
        panel.should have_tag("form[action='/agents/edit_agents?column=pavan&amp;order=shilpa']") do |form|
          form.should have_tag("input[type='hidden'][name='operation'][id='agent_edit_operation']")
          form.should have_tag("button[type='submit'][value='Enable']")
          form.should have_tag("button[type='submit'][value='Disable']")
        end
      end
    end

    it "should not render page error if it is not there" do
      render 'agents/index'
      response.should_not have_tag("div.flash")
    end

    it "should allow adding new resources to selected agents" do
      render 'agents/index'
      response.should have_tag("form[action='/agents/edit_agents']") do |form|
        form.should have_tag("div#resources_panel") do |panel|
          panel.should have_tag("input[type='text'][name='add_resource']")
          panel.should have_tag("button[type='submit'][name='resource_operation'][value='Add']")
        end
      end
    end

    it "should have the same contents as the jsunit fixture" do
      render 'agents/index'

      body = response.body.gsub(/\<script.+?\<\/script\>/mi, '')

      resp_doc = REXML::Document.new('<temp>' + body + '</temp>')
      REXML::XPath.each(resp_doc, "//td[@class='location']") do  |loc_field|
        loc_field.attributes["title"] = "LOCATION"
        span = REXML::XPath.first(loc_field, "./span")
        span.children.each do |text|
          span.delete text
        end
        span.text = "LOCATION"
      end
      assert_fixture_equal("micro_content_on_agents_test.html", REXML::XPath.first(resp_doc,'temp').children.to_s)
    end

    it "should have resource validation message set" do
      render 'agents/index'
      response.body.should have_tag('div.validation_message', "Invalid character. Please use a-z, A-Z, 0-9, fullstop, underscore, hyphen and pipe.")
    end

    it "should have panel to show resources selector" do
      render 'agents/index'
      response.should have_tag('div.resources_selector')
    end

  end


  describe "does not have agent operate permissions" do

    before do
      template.stub(:has_operate_permission_for_agents?).and_return(false)
    end

    it "should not show the checkbox to edit if not an admin" do
      render 'agents/index'
      response.body.should_not have_tag("input[type='checkbox']")
    end

    it "should not show the operate buttons to edit if not an admin" do
      render 'agents/index'
      response.body.should_not have_tag("div[class='edit_panel']")
    end
  end
end

