##########################################################################
# Copyright 2015 ThoughtWorks, Inc.
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
##########################################################################


module ApiV1
  class BackupsController < ApiV1::BaseController
    before_action :check_admin_user_and_401

    def create
      result = HttpLocalizedOperationResult.new
      backup = backup_service.startBackup(current_user, result)

      if result.isSuccessful
        render DEFAULT_FORMAT => BackupRepresenter.new(backup).to_hash(url_builder: self)
      else
        render_http_operation_result(result)
      end
    end

  end
end
