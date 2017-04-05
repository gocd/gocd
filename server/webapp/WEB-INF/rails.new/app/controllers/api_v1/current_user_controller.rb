##########################################################################
# Copyright 2017 ThoughtWorks, Inc.
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
  class CurrentUserController < ApiV1::BaseController
    include ApiV1::UsersHelper

    before_action :check_user_and_404
    before_action :load_current_user

    def show
      render_user(@user_to_operate)
    end

    def update
      result           = HttpLocalizedOperationResult.new
      user = save_user(result, @user_to_operate)
      if result.isSuccessful
        render_user(user)
      else
        render_http_operation_result(result)
      end
    end

    private

    def render_user(user)
      render DEFAULT_FORMAT => ApiV1::UserRepresenter.new(user).to_hash(url_builder: self)
    end

  end
end
