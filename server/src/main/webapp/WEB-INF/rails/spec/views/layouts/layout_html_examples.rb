#
# Copyright 2021 ThoughtWorks, Inc.
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
#

require 'rails_helper'

shared_examples :layout do
  it "should have title" do
    render :inline => '<% @view_title = "My Title" -%>', :layout => @layout_name

    expect(response.body).to have_title("My Title - Go")
  end

  it "should disable mycruise tab when mycruise is not available" do
    allow(view).to receive(:mycruise_available?).and_return(false)
    render :inline => "<span>foo</span>", :layout => @layout_name
    expect(response.body).to_not(have_selector("ul.current_user"))
  end

  it "should render page error" do
    session[:notice] = FlashMessageModel.new("Some error message", "error")
    render :inline => "<span>foo</span>", :layout => @layout_name
    Capybara.string(response.body).all('div.flash').tap do |flash|
      expect(flash[0]).to have_selector("p.error", :text => "Some error message")
    end
  end
end
