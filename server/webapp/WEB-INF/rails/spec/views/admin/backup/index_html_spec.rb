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

require File.join(File.dirname(__FILE__), "/../../../spec_helper")

describe "admin/backup/index.html.erb" do

  before :each do
    template.stub!(:perform_backup_path).and_return("perform_backup_url")
    assigns[:last_backup_time] = @last_backup_time = java.util.Date.new()
    assigns[:last_backup_user] = @last_backup_user = "loser"
  end

  it "should contain server back up button" do
    render "admin/backup/index.html.erb"

    response.should have_tag("div.backup_server") do
      with_tag("button#backup_server") do
        with_tag("span", 'PERFORM BACKUP')
      end
    end
  end

  it "should display warning message when postgresql is used" do
    template.stub!(:external_db?).and_return(true)
    render "admin/backup/index.html.erb"
    response.body.should have_tag(".postgresql.warnings") do
      with_tag("span.info", "When postgreSQL is used, this operation will not perform a database backup. We recommend taking a manual backup of the database as needed.")
    end
  end

  it "should not display warning message when postgresql is not used" do
    template.stub!(:external_db?).and_return(false)
    render "admin/backup/index.html.erb"
    response.body.should_not have_tag(".postgresql.warning")
  end

  it "should contain server back up form" do
    render "admin/backup/index.html.erb"

    response.should have_tag("div.should_perform_backup_content") do
      with_tag("div.warning_message", "Jobs that are building may get rescheduled if the backup process takes a long time. Proceed with backup?")
      with_tag("form[id='backup_server_form'][method='post'][action='perform_backup_url']") do
        with_tag("button[type='submit']") do
          with_tag("span", 'PROCEED')
        end
        with_tag("button") do
          with_tag("span", 'Cancel')
        end
      end
    end
  end

  it "should show the location of the server backup directory" do
    assigns[:backup_location] = location = "/var/lib/go-server/artifacts/server-backups"

    render "admin/backup/index.html.erb"

    response.body.should have_tag(".backup_storage_message.information span.info", "Backups are stored in #{location}")
    response.body.should have_tag(".backup_storage_message.information span.info strong", location)
  end

  it "should show the last performed backup time" do

    render "admin/backup/index.html.erb"

    response.body.should have_tag(".last_backup") do
      with_tag("span","Last backup was taken by 'loser' on #{@last_backup_time.iso8601}")
    end
  end

  it "should show a warning if the last performed backup time is unknown" do
    assigns[:last_backup_time] = nil

    render "admin/backup/index.html.erb"

    response.body.should have_tag(".last_backup") do
      with_tag("span", "Go has not performed a backup yet.")
    end
  end
end