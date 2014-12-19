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

class Admin::BackupController < ApplicationController

  layout "admin"

  protect_from_forgery :except => :delete_all #NOT_IN_PRODUCTION don't remove this line, the build will remove this line when packaging the war

  def index
    @tab_name = "backup"
    @backup_location = backup_service.backupLocation()
    @last_backup_time = backup_service.lastBackupTime()
    @last_backup_user = backup_service.lastBackupUser()
    @available_disk_space_on_artifacts_directory = backup_service.availableDiskSpace()
  end

  def perform_backup
    backup_service.startBackup(current_user, op_result = HttpLocalizedOperationResult.new())
    redirect_with_flash(op_result.message(Spring.bean("localizer")), :action => :index, :class => op_result.isSuccessful() ? "success" : "error")
  end

  # Used only in Twist tests. Don't remove the #NOT_IN_PRODUCTION comments, these are used to strip out lines when building the distributable'
  def delete_all #NOT_IN_PRODUCTION
    backup_service.deleteAll() #NOT_IN_PRODUCTION
    render :text => 'Deleted' #NOT_IN_PRODUCTION
  end #NOT_IN_PRODUCTION
end