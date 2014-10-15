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

shared_examples_for :layout do
  it "should have title" do
    render :inline => '<% @view_title = "My Title" -%>', :layout => @layout_name

    response.should have_tag("html head title","My Title - Go")
  end

  it "should display all the tabs" do
    render :inline => '<div>content</div>', :layout => @layout_name
    response.should have_tag "li a", /AGENTS/
    response.should have_tag "li a", /ENVIRONMENTS/
  end

  it "should not display auth block when user not logged in" do
    render :inline => '<div>content</div>', :layout => @layout_name
    response.should_not have_tag('html body ul.principal')
  end

  it "should display username and logout botton if a user is logged in" do
    assigns[:user] = com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new("maulik suchak"))

    render :inline => '<div>content</div>', :layout => @layout_name
    response.should have_tag(".current_user a[href='#']", "maulik suchak")
    response.should have_tag(".current_user a[href='/tab/mycruise/user']", "Preferences")
    response.should have_tag(".current_user .logout a[href='/auth/logout']", "Sign out")

    response.should have_tag(".user .help a[href='http://www.go.cd/documentation/user/current']", "Help")
  end

  it "should not display username and logout botton if anonymous user is logged in" do
    assigns[:user] = com.thoughtworks.go.server.domain.Username::ANONYMOUS

    render :inline => '<div>content</div>', :layout => @layout_name

    response.should have_tag(".user .help a[href='http://www.go.cd/documentation/user/current']", "Help")
    response.should_not have_tag(".current_user a[href='#']", "maulik suchak")
    response.should_not have_tag(".current_user a[href='/tab/mycruise/user']", "Preferences")
    response.should_not have_tag(".current_user a[href='/auth/logout']", "Sign out")
  end

  it "should link all the tabs right" do
    template.stub!(:mycruise_available?).and_return(true)    
    render :inline => "<span>foo</span>", :layout => @layout_name
    response.should have_tag("a[href='/pipelines']", 'PIPELINES')
    response.should have_tag("a[href='/agents']", 'AGENTS')
    response.should have_tag("a[href='/environments']", 'ENVIRONMENTS')
    response.should have_tag("a[href='http://www.go.cd/documentation/user/current']", 'Help')
    response.should(have_tag("a[data-toggle='dropdown']", 'ADMIN'))
  end

  it "should disable mycruise tab when mycruise is not available" do
    template.stub!(:mycruise_available?).and_return(false)
    render :inline => "<span>foo</span>", :layout => @layout_name
    response.should_not(have_tag("ul.current_user"))
  end

  it "should disable admin tab when user is not an administrator" do
    template.stub!(:can_view_admin_page?).and_return(false)
    render :inline => "<span>foo</span>", :layout => @layout_name
    response.should have_tag('li span', "ADMIN")
  end

  it "should enable admin tab when user is an administrator" do
    render :inline => "<span>foo</span>", :layout => @layout_name
    response.should have_tag("li a[data-toggle='dropdown']", "ADMIN")
  end

  it "should render page error" do
      session[:notice] = FlashMessageModel.new("Some error message", "error")
      render :inline => "<span>foo</span>", :layout => @layout_name
      response.should have_tag("div.flash") do |flash|
        flash.should have_tag("p.error", :text => "Some error message")
      end
  end

  it "should have a place holder in the header for error and warning counts" do
      render :inline => "<span>foo</span>", :layout => @layout_name
      response.should have_tag("div#cruise_message_counts")
  end

  it "should have a place holder in the header for error and warning content" do
      render :inline => "<span>foo</span>", :layout => @layout_name
      response.should have_tag("div#cruise_message_body")
  end
end