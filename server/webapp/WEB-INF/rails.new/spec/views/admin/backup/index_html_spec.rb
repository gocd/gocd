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

describe "admin/backup/index.html.erb" do

  before :each do
    allow(view).to receive(:perform_backup_path).and_return("perform_backup_url")
    @last_backup_time = java.util.Date.new()
    @last_backup_user = "loser"
    assign(:last_backup_time, @last_backup_time)
    assign(:last_backup_user, @last_backup_user)
  end

  it "should contain server back up button" do
    render

    Capybara.string(response.body).find('div.backup_server').tap do |div|
      div.find("button#backup_server").tap do |button|
        expect(button).to have_selector("span", :text => 'PERFORM BACKUP')
      end
    end
  end

  it "should contain server back up form" do
    render

    Capybara.string(response.body).find('div.should_perform_backup_content').tap do |div|
      expect(div).to have_selector("div.warning_message", :text => "Jobs that are building may get rescheduled if the backup process takes a long time. Proceed with backup?")
      div.find("form[id='backup_server_form'][method='post'][action='perform_backup_url']") do |form|
        form.find("button[type='submit']") do |submit_button|
          expect(submit_button).to have_selector("span", :text => 'PROCEED')
        end
        form.find("button") do |cancel_button|
          expect(cancel_button).to have_selector("span", :text => 'Cancel')
        end
      end
    end
  end

  it "should show the location of the server backup directory" do
    location = "/var/lib/go-server/artifacts/server-backups"
    assign(:backup_location, location)

    render

    expect(response.body).to have_selector(".backup_storage_message.information span.info", :text => "Backups are stored in #{location}")
    expect(response.body).to have_selector(".backup_storage_message.information span.info strong", :text => location)
  end

  it "should show the last performed backup time" do

    render

    Capybara.string(response.body).find('.last_backup').tap do |div|
      expect(div).to have_selector("span", :text => "Last backup was taken by 'loser' on #{@last_backup_time.iso8601}")
    end
  end

  it "should show a warning if the last performed backup time is unknown" do
    assign(:last_backup_time, nil)

    render

    Capybara.string(response.body).find('.last_backup').tap do |div|
      expect(div).to have_selector("span", :text => "Go has not performed a backup yet.")
    end
  end
end
