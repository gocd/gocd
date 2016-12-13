##########################################################################
# Copyright 2016 ThoughtWorks, Inc.
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
  module Admin
    class EncryptionController < ApiV1::BaseController
      before_action :check_admin_user_or_group_admin_user_and_401

      @@go_cipher = GoCipher.new

      def encrypt_value
        render DEFAULT_FORMAT => {encrypted_value: @@go_cipher.encrypt(params[:value])}
      rescue
        render_message("An error occurred while encrypting the value. Please check the logs for more details.", :internal_server_error)
      end
    end
  end
end