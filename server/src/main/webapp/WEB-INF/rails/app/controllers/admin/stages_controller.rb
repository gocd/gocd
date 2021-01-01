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

module Admin
  class StagesController < AdminController
    include AuthenticationHelper
    before_action :check_admin_user_and_403
    layout "application"

    def config_change
      @changes = go_config_service.configChangesFor(params[:later_md5], params[:earlier_md5], result = HttpLocalizedOperationResult.new)
      @config_change_error_message = result.isSuccessful ? ('This is the first entry in the config versioning. Please refer config tab to view complete configuration during this run.' if @changes == nil) : result.message()
    end
  end
end
