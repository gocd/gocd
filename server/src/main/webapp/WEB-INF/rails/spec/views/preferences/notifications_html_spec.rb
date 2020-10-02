#
# Copyright 2020 ThoughtWorks, Inc.
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

describe "preferences/notifications.html.erb" do
  before :each do
    assign(:user, com.thoughtworks.go.server.domain.Username.new("foo"))
    assign(:view_title, "Preferences")
    view.extend(SparkUrlAware)
  end

  it 'should have a view title' do
    render

    expect(response).to have_selector("#page-title", :text => "Preferences")
  end

  it 'should have a warning if smtp is disabled' do
    render

    expect(response).to have_selector(".callout", text: "SMTP settings are currently not configured. If you are the administrator, you can configure email support at Mail Server Configuration.")
    expect(response).to have_selector(".callout a[href='/go/admin/config/server#!email-server']", text: "Mail Server Configuration")
  end

end
