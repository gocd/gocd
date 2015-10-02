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
describe "/agents/index.html.erb" do
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
    @agent6 = missing_agent(:uuid => "UUID_host6", :hostname => "foo_bar_host", :environments => ["uat", "blah"])
    @agent7 = cancelled_agent(:locator => LOCATOR2, :uuid => "UUID_host7")
    @agent8 = lost_contact_agent(:locator => LOCATOR3, :uuid => "UUID_host8")
    @agent9 = lost_contact_agent(:locator => '', :uuid => "UUID_host9")
    assign(:agents, AgentsViewModel.new([@agent1, @agent2, @agent3, @agent4, @agent5, @agent6, @agent7, @agent8, @agent9].to_java(AgentViewModel)))

    allow(view).to receive(:can_view_admin_page?).and_return(true)
    allow(view).to receive(:edit_agent_path).and_return("foo")
    allow(view).to receive(:has_view_or_operate_permission_on_pipeline?).and_return(true)
    allow(view).to receive(:is_user_an_admin?).and_return(true)
    stub_context_path(view)
  end

  it "should have form with submit path including all params" do
    allow(view).to receive(:has_operate_permission_for_agents?).and_return(true)
    in_params(:column => "status", :order => "DESC", :filter => "foobar")

    render
    page = Capybara::Node::Simple.new(response.body)
    expect(page).to have_xpath("//form[@id='agents_form' and @action='/agents/edit_agents?column=status&filter=foobar&order=DESC']")
  end

  describe "agents page has filtering capability" do

    before do
      allow(view).to receive(:has_operate_permission_for_agents?).and_return(true)
      in_params(:column => "status", :order => "ASC", :filter => "foo:bar, moo:boo")
    end

    it "should have filter textbox" do
      in_params(:column => "status", :order => "ASC", :filter => "foo:bar, moo:boo", :autoRefresh => false)

      render
      Capybara.string(response.body).find("div.filter_agents").tap do |div|
        expect(div).to have_selector("input[type='text'][name='filter'][value='foo:bar, moo:boo']")
        expect(div).to have_selector("input[type='hidden'][name='column'][value='status']")
        expect(div).to have_selector("input[type='hidden'][name='order'][value='ASC']")
        expect(div).to have_selector("input[type='hidden'][name='autoRefresh'][value='false']")
        expect(div).to have_selector("button[type='submit']")
        expect(div).to have_selector("a[href='/agents?column=status&filter=&order=ASC'][id='clear_filter']", "Clear")
      end
    end

    it "should maintain sorting order with filtering" do
      render
      Capybara.string(response.body).find("table#agent_details").tap do |table|
        expect(table).to have_selector("th.hostname a[href='/agents?column=hostname&filter=foo%3Abar%2C+moo%3Aboo&order=ASC']")
        expect(table).to have_selector("th.location a[href='/agents?column=location&filter=foo%3Abar%2C+moo%3Aboo&order=ASC']")
        expect(table).to have_selector("th.operating_system a[href='/agents?column=operating_system&filter=foo%3Abar%2C+moo%3Aboo&order=ASC']")
        expect(table).to have_selector("th.ip_address a[href='/agents?column=ip_address&filter=foo%3Abar%2C+moo%3Aboo&order=ASC']")
        expect(table).to have_selector("th.status a[href='/agents?column=status&filter=foo%3Abar%2C+moo%3Aboo&order=DESC']")
        expect(table).to have_selector("th.usable_space a[href='/agents?column=usable_space&filter=foo%3Abar%2C+moo%3Aboo&order=ASC']")
        expect(table).to have_selector("th.resources a[href='/agents?column=resources&filter=foo%3Abar%2C+moo%3Aboo&order=ASC']")
        expect(table).to have_selector("th.environments a[href='/agents?column=environments&filter=foo%3Abar%2C+moo%3Aboo&order=ASC']")
      end
    end

    it "should have help content for filtering in a hidden drop down" do
      render
      Capybara.string(response.body).find("div.filter_agents").tap do |div|
        div.find(".filter_help_instructions").tap do |f|
          expect(f).to have_selector("p", :text => "Available tags")
          expect(f).to have_selector("p", :text => "name:")
          expect(f).to have_selector("p", :text => "os:")
          expect(f).to have_selector("p", :text => "ip:")
          expect(f).to have_selector("p", :text => "status:")
          expect(f).to have_selector("p", :text => "environment:")
          expect(f).to have_selector("p", :text => "resource:")
          expect(f).to have_selector("p", :text => "More...")
        end
      end
    end

    it "should have contextual help for exact matching" do
      render

      Capybara.string(response.body).find("div.filter_agents").tap do |div|
        div.find("#filter_help.enhanced_dropdown").tap do |f|
          expect(f).to have_selector("p.heading", :text => "Values")
          expect(f).to have_selector("p", :text => "Put filter values in quotes for exact match")
        end
      end
    end
  end


  describe "has agent operate permissions" do

    before do
      allow(view).to receive(:has_operate_permission_for_agents?).and_return(true)
    end

    it "should show the host name" do
      render
      expect(response).to have_selector("td a[href='#{agent_detail_path(:uuid => 'UUID_host1')}']", :text => 'host1')
    end

    it "should show the sandbox directory" do
      render
      Capybara.string(response.body).all("table.agents tr.agent_instance").tap do |f|
        expect(f[0]).to have_selector("td.location", :text => "/var/lib/cruise-agent")
        expect(f[1]).to have_selector("td.location", :text => "C:\\Program Files\\Cruise Agent")
      end
    end

    it "should show the ip address" do
      render
      Capybara.string(response.body).all("table.agents tr.agent_instance").tap do |f|
        expect(f[1]).to have_selector("td.ip_address", :text => "127.0.0.1")
        expect(f[2]).to have_selector("td.ip_address", :text => "10.6.6.6")
      end
    end

    it "should show free disk space" do
      render
      Capybara.string(response.body).all("table.agents tr.agent_instance").tap do |f|
        expect(f[2]).to have_selector("td.usable_space", :text => /10.0 GB/)
        expect(f[4]).to have_selector("td.usable_space", :text => /12.0 GB/)
        expect(f[5]).to have_selector("td.usable_space", :text => "Unknown")
      end
    end

    it "should show status" do
      render
      Capybara.string(response.body).all("table.agents tr.agent_instance").tap do |f|
        expect(f[3]).to have_selector("td.status", :text => "building")
        expect(f[1]).to have_selector("td.status", :text => "pending")
      end
    end

    it "should show resources" do
      render
      Capybara.string(response.body).all("table.agents tr.agent_instance").tap do |f|
        expect(f[2]).to have_selector("td.resources", :text => "db | dbSync")
        expect(f[4]).to have_selector("td.resources", :text => "nant | vs.net")
        expect(f[5]).to have_selector("td.resources", :text => "no resources specified")
      end
    end

    it "should show sort links" do
      params[:column] = 'hostname'
      params[:order] = 'ASC'
      render

      Capybara.string(response.body).find("table.agents tr.agent_header").tap do |f|
        expect(f).to have_selector("th.selector")
        expect(f).to have_selector("th.hostname a[href='/agents?column=hostname&order=DESC']")
        expect(f).to have_selector("th.location a[href='/agents?column=location&order=ASC']")
        expect(f).to have_selector("th.status a[href='/agents?column=status&order=ASC']")
        expect(f).to have_selector("th.usable_space a[href='/agents?column=usable_space&order=ASC']")
        expect(f).to have_selector("th.resources a[href='/agents?column=resources&order=ASC']")
        expect(f).to have_selector("th.ip_address a[href='/agents?column=ip_address&order=ASC']")
        expect(f).to have_selector("th.operating_system a[href='/agents?column=operating_system&order=ASC']")
        expect(f).to have_selector("th.environments a[href='/agents?column=environments&order=ASC']")
      end
    end

    it "columns should be sortable" do
      render

      xml = HTML::Document.new(response.body, true, false).root
      xml.find_all(:tag => 'a', :parent => {:tag => 'th'}).each do |tag|
        scanned_name = tag.attributes['href'].sub(/.*column=(\w+).*/, '\1')
        raise "Didn't find comparator for column named #{scanned_name}" unless (comparator = AgentsController::ORDERS[scanned_name.to_s])
        comparator.is_a?(java.util.Comparator)
      end
    end

    it "should display titles" do
      render
    end

    it "should add agent status as css_class on the row" do
      render
      expect(response).to have_selector("tr.agent_instance.Building")
      expect(response).to have_selector("tr.agent_instance.Pending")
    end

    it "should add values of table cells as title" do
      render

      expect(response).to have_selector("td.hostname[title='host1'] a", :text => "host1")
      expect(response).to have_selector("td.location[title='/var/lib/cruise-agent']", :text => "/var/lib/cruise-agent")
      expect(response).to have_selector("td.ip_address[title='127.0.0.1']", :text => "127.0.0.1")
      Capybara.string(response.body).find("td.status[title='#{LOCATOR1}']").tap do |f|
        expect(f).to have_selector("a[href='/go/tab/build/detail/#{LOCATOR1}']", :text => "building")
      end
      expect(response).to have_selector("td.usable_space[title='10.0 GB']", :text => "10.0 GB")
      expect(response).to have_selector("td.resources[title='db | dbSync']", :text => "db | dbSync")
    end

    it "should use agent's uuid as id to agent row" do
      render
      Capybara.string(response.body).find("tr.agent_instance#UUID_host5").tap do |f|
        expect(f).to have_selector("td.hostname", :text => "foo_baz_host")
      end
      Capybara.string(response.body).find("tr.agent_instance#UUID_host6").tap do |f|
        expect(f).to have_selector("td.hostname", :text => "foo_bar_host")
      end
    end

    it "should show job locator even when cancelled" do
      render
      Capybara.string(response.body).find("td.status[title='#{LOCATOR2}']").tap do |f|
        expect(f).to have_selector("a[href='/go/tab/build/detail/#{LOCATOR2}']", :text => "building (cancelled)")
      end
    end

    it "should show job locator even when building agent goes into lost contact" do
      render
      Capybara.string(response.body).all("table.agents tr.agent_instance").tap do |f|
        expect(f[8]).to have_selector("td.status[title='lost contact at #{@agent9.getLastHeardTime().iso8601()}']", :text => "lost contact")
        expect(f[7]).to have_selector("td.status[title='lost contact at #{@agent8.getLastHeardTime().iso8601()} while building #{LOCATOR3}: job rescheduled']", :text => "lost contact")
        f[7].find("td.status[title='lost contact at #{@agent8.getLastHeardTime().iso8601()} while building #{LOCATOR3}: job rescheduled']") do |td|
          expect(td).to have_selector("a[href='/go/tab/build/detail/#{LOCATOR3}']", :text => "lost contact")
        end
      end
    end

    it "should show a table of agents" do
      render
      expect(response).to have_selector('table.agents')
    end

    it "should set title" do
      assign(:user, double('username', :anonymous? => true))
      render :template => "agents/index.html.erb", :layout => 'layouts/application'
      page = Capybara::Node::Simple.new(response.body)
      expect(page.title).to include("Agents - Go")
    end

    it "should not include relative url root in urls" do
      params[:column] = 'status'
      params[:order] = 'asc'
      render
      Capybara.string(response.body).find("a.sorted_asc").tap do |f|
        expect(f).to_not have_selector("a[href='/rails']")
      end
    end

    it "should populate environments" do
      render
      expect(response).to have_selector("tr#UUID_host6 td.environments", :text => "blah | uat")
      expect(response).to have_selector("tr#UUID_host5 td.environments", :text => "no environments specified")
    end

    it "should populate operating system" do
      render
      expect(response).to have_selector("tr#UUID_host1 td.operating_system", :text => "Linux")
      expect(response).to have_selector("tr#UUID_host5 td.operating_system", :text => "Windows")
    end

    it "should include a checkbox for each agent" do
      render
      Capybara.string(response.body).find("tr#UUID_host6").tap do |tr|
        tr.find("td.selector").tap do |f|
          expect(f).to have_selector("input[type='checkbox'][name='selected[]'][value='UUID_host6'][class='agent_select']")
        end
      end
    end

    it "should remember selected state for agent checkboxes" do
      params[:selected] = ['UUID_host6']
      render
      Capybara.string(response.body).find("tr#UUID_host6").tap do |tr|
        tr.find("td.selector").tap do |f|
          expect(f).to have_selector("input[type='checkbox'][name='selected[]'][value='UUID_host6'][class='agent_select'][checked='true']")
        end
      end
    end

    it "should include enable and diasable buttons" do
      params[:column] = "pavan"
      params[:order] = "shilpa"
      params[:foo] = "bar"
      render
      Capybara.string(response.body).find("div.edit_panel").tap do |div|
        div.find("form[action='/agents/edit_agents?column=pavan&order=shilpa']").tap do |f|
          expect(f).to have_selector("input[type='hidden'][name='operation'][id='agent_edit_operation']")
          expect(f).to have_selector("button[type='submit'][value='Enable']")
          expect(f).to have_selector("button[type='submit'][value='Disable']")
        end
      end
    end

    it "should not render page error if it is not there" do
      render
      expect(response).to_not have_selector("div.flash")
    end

    it "should allow adding new resources to selected agents" do
      render

      Capybara.string(response.body).find("form[action='/agents/edit_agents']").tap do |form|
        form.find("div#resources_panel").tap do |f|
          expect(f).to have_selector("input[type='text'][name='add_resource']")
          expect(f).to have_selector("button[type='submit'][name='resource_operation'][value='Add']")
        end
      end
    end

    it "should have resource validation message set" do
      render
      expect(response).to have_selector('div.validation_message', :text => "Invalid character. Please use a-z, A-Z, 0-9, fullstop, underscore, hyphen and pipe.")
    end

    it "should have panel to show resources selector" do
      render
      expect(response).to have_selector('div.resources_selector')
    end

  end


  describe "does not have agent operate permissions" do

    before do
      allow(view).to receive(:has_operate_permission_for_agents?).and_return(false)
    end

    it "should not show the checkbox to edit if not an admin" do
      render
      expect(response).to_not have_selector("input[type='checkbox']")
    end

    it "should not show the operate buttons to edit if not an admin" do
      render
      expect(response).to_not have_selector("div[class='edit_panel']")
    end
  end
end

