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

require 'rails_helper'

describe Admin::BackupController do

  include ExtraSpecAssertions

  before :each do
    allow(controller).to receive(:backup_service).and_return(@backup_service = double('backup_server'))
  end

  describe "index" do

    before :each do
      expect(@backup_service).to receive(:lastBackupTime).and_return(@time = java.util.Date.new)
      expect(@backup_service).to receive(:backupLocation).and_return(@location = "/var/lib/go-server/logs/server-backups")
      expect(@backup_service).to receive(:availableDiskSpace).and_return(@space = "424242")
      expect(@backup_service).to receive(:lastBackupUser).and_return(@user = "loser")
    end

    it "should populate the tab name" do
      get "index"

      expect(assigns[:tab_name]).to eq("backup")
      assert_template layout: "admin"
    end

    it "should populate the backup location" do
      get :index

      expect(assigns[:backup_location]).to eq(@location)
    end

    it "should populate the last backup time" do
      get :index

      expect(assigns[:last_backup_time]).to eq(@time)
    end

    it "should populate the user that triggered the last backup" do
      get :index

      expect(assigns[:last_backup_user]).to eq(@user)
    end

    it "should populate available disk space on artifact directory" do
      get :index

      expect(assigns[:available_disk_space_on_artifacts_directory]).to eq(@space)
    end
  end

  describe "perform_backup" do

    it "should return success if the backup is successful" do
      allow(controller).to receive(:backup_service).and_return(backup_service = double("backup_service"))

      expect(backup_service).to receive(:startBackup).with(an_instance_of(Username), an_instance_of(HttpLocalizedOperationResult)) do |u, r|
        r.setMessage(LocalizedMessage.string("BACKUP_COMPLETED_SUCCESSFULLY"))
      end

      post :perform_backup

      assert_redirected_with_flash(backup_server_path, "Backup completed successfully.", "success")
    end

    it "should return error if the backup has failed" do
      allow(controller).to receive(:backup_service).and_return(backup_service = double("backup_service"))

      expect(backup_service).to receive(:startBackup).with(an_instance_of(Username), an_instance_of(HttpLocalizedOperationResult)) do |user, result|
        result.badRequest(LocalizedMessage.string("BACKUP_UNSUCCESSFUL", ["Ran out of disk space"].to_java(java.lang.String)))
      end

      post :perform_backup

      assert_redirected_with_flash(backup_server_path, "Failed to perform backup. Reason: Ran out of disk space", "error")
    end
  end
end
