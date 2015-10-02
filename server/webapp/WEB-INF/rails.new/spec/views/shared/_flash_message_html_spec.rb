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

describe "/shared/_flash_message.html.erb" do
  describe "flash message obtained from session" do

    it "should read flash message from the session" do
      session[:notice] = FlashMessageModel.new("foo", "success")
      render :partial => "shared/flash_message.html.erb"
      expect(response.body).to have_selector("div#message_pane p.success", "foo")
      expect(session[:notice]).to be(nil)
    end

   it "should not show flash message when its not defined in session" do
      render :partial => "shared/flash_message.html.erb"
      expect(response.body).to_not have_selector("div#message_pane p", "foo")
    end

    it "should escape flash message" do
      session[:notice] = FlashMessageModel.new("<h2>", "success")
      render :partial => "shared/flash_message.html.erb"
      expect(response.body).to have_selector("div#message_pane p", "&lt;h2&gt;")
    end

    it "should render help links next to flash message" do
      assign(:flash_message, 'flash_key')
      flash = mock('flash')
      flash.should_receive(:flashClass).and_return('error')
      flash.should_receive(:to_s).and_return('some random message')
      template.stub(:load_flash_message).with('flash_key').and_return(flash)
      assign(:flash_help_link, "<a href='foo'>Foo</a>")

      render :partial => "shared/flash_message.html.erb"

      Capybara.string(response.body).find("p.error").tap do |error|
        expect(error).to have_selector("a[href='foo']", :text => "Foo")
      end
    end

    it "should not bomb when no help link exists" do
      assign(:flash_message, 'flash_key')
      flash = mock('flash')
      flash.should_receive(:flashClass).and_return('error')
      flash.should_receive(:to_s).and_return('some random message')
      template.stub(:load_flash_message).with('flash_key').and_return(flash)
      assign(:flash_help_link, nil)

      render :partial => "shared/flash_message.html.erb"

      Capybara.string(response.body).find("p.error").tap do |error|
        expect(error).not_to have_selector("a[href='foo']", :text => "Foo")
      end
    end

  end
end
