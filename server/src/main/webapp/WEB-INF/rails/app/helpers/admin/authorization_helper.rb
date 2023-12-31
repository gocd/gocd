#
# Copyright 2024 Thoughtworks, Inc.
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
  module AuthorizationHelper
    def check_admin_user_and_403
      return unless security_service.isSecurityEnabled()
      unless security_service.isUserAdmin(current_user)
        Rails.logger.info("User '#{current_user.getUsername}' attempted to perform an unauthorized action!")
        render 'shared/config_error', status: 403
      end
    end
  end
end
