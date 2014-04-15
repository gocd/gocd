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

load File.expand_path(File.dirname(__FILE__) + '/../auto_refresh_examples.rb')

describe 'environments/index.html.erb' do
  before do
    @foo = Environment.new("foo", [])
    @bar = Environment.new("bar", [])
  end


  it "should render partial 'environment' for each environment" do
    assigns[:environments] = [@foo, @bar]
    assigns[:show_add_environments] = true
    template.stub!(:environments_allowed?).and_return(true)
    template.should_receive(:render).with(:partial => "environment.html.erb", :locals => {:scope => { :environment => @foo, :show_edit_environments => true}}).and_return("(foo content)")
    template.should_receive(:render).with(:partial => "environment.html.erb", :locals => {:scope => { :environment => @bar, :show_edit_environments => true}}).and_return("(bar content)")
    render 'environments/index.html.erb'
    response.should have_tag("div.environments"), "(foo content)(bar content)"
  end

  it "should display 'no environments configured' message with link to configuration when there are no environments and using enterprise license" do
    assigns[:environments] = []
    template.stub!(:environments_allowed?).and_return(true)
    render 'environments/index.html.erb'
    response.should have_tag("div.unused_feature") do
      with_tag("a[href='/help/managing_environments.html']", "More Information")
    end
  end

  describe :auto_refresh do
    before do
      @partial = 'environments/index.html.erb'
      @ajax_refresher = /DashboardAjaxRefresher/
      assigns[:environments] = [@foo, @bar]
      assigns[:show_add_environments] = true
      template.stub!(:environments_allowed?).and_return(true)
      template.should_receive(:render).with(:partial => "environment.html.erb", :locals => {:scope => { :environment => @foo, :show_edit_environments => true}}).and_return("(foo content)")
      template.should_receive(:render).with(:partial => "environment.html.erb", :locals => {:scope => { :environment => @bar, :show_edit_environments => true}}).and_return("(bar content)")
    end

    it_should_behave_like "auto_refresh"    
  end

end
