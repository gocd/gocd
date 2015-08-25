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

describe Admin::BackupController do

  describe :routes do

    it "should resolve the route to the backup admin ui page" do
      {:get => "/admin/backup"}.should route_to(:controller => "admin/backup", :action => "index")
      backup_server_path.should == "/admin/backup"
    end

    it "should resolve the route for the server backup" do
      {:post => "/admin/backup"}.should route_to(:controller => "admin/backup", :action => "perform_backup")
      perform_backup_path.should == "/admin/backup"
    end
  end

  describe :index do

    before :each do
      controller.stub(:backup_service).and_return(@backup_service = double('backup_server'))
      @backup_service.should_receive(:lastBackupTime).and_return(@time = java.util.Date.new)
      @backup_service.should_receive(:backupLocation).and_return(@location = "/var/lib/go-server/logs/server-backups")
      @backup_service.should_receive(:availableDiskSpace).and_return(@space = "424242")
      @backup_service.should_receive(:lastBackupUser).and_return(@user = "loser")
    end

    it "should populate the tab name" do
      get "index"

      assigns[:tab_name].should == "backup"
      assert_template layout: "admin"
    end

    it "should populate the backup location" do
      get :index

      assigns[:backup_location].should == @location
    end

    it "should populate the last backup time" do
      get :index

      assigns[:last_backup_time].should == @time
    end

    it "should populate the user that triggered the last backup" do
      get :index

      assigns[:last_backup_user].should == @user
    end

    it "should populate available disk space on artifact directory" do
      get :index

      assigns[:available_disk_space_on_artifacts_directory].should == @space
    end
  end

  describe :perform_backup do

    it "should return success if the backup is successful" do
      controller.stub(:backup_service).and_return(backup_service = double("backup_service"))

      backup_service.should_receive(:startBackup).with(an_instance_of(Username), an_instance_of(HttpLocalizedOperationResult)) do |u, r|
        r.setMessage(LocalizedMessage.string("BACKUP_COMPLETED_SUCCESSFULLY"))
      end

      post :perform_backup

      assert_redirected_with_flash(backup_server_path, "Backup completed successfully.", "success")
    end

    it "should return error if the backup has failed" do
      controller.stub(:backup_service).and_return(backup_service = double("backup_service"))

      backup_service.should_receive(:startBackup).with(an_instance_of(Username), an_instance_of(HttpLocalizedOperationResult)) do |user, result|
        result.badRequest(LocalizedMessage.string("BACKUP_UNSUCCESSFUL", ["Ran out of disk space"].to_java(java.lang.String)))
      end

      post :perform_backup

      assert_redirected_with_flash(backup_server_path, "Failed to perform backup. Reason: Ran out of disk space", "error")
    end
  end
end
