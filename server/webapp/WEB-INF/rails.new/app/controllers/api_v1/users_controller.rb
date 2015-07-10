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
  class UsersController < ApiV1::BaseController
    before_action :check_admin_user_and_401
    before_action :load_user, only: [:show, :update, :destroy]

    def index
      render :json_hal_v1 => UsersRepresenter.new(user_service.allUsers).to_hash(url_builder: self)
    end

    def show
      render :json_hal_v1 => UserRepresenter.new(@user_to_operate).to_hash(url_builder: self)
    end

    def update
      result          = HttpLocalizedOperationResult.new
      checkin_aliases = if params[:checkin_aliases].is_a?(Array)
                          params[:checkin_aliases].join(',')
                        else
                          params[:checkin_aliases]
                        end

      @user_to_operate = user_service.update(@user_to_operate, to_tristate(params[:enabled]), to_tristate(params[:email_me]), params[:email], checkin_aliases, result)
      if result.isSuccessful
        render :json_hal_v1 => UserRepresenter.new(@user_to_operate).to_hash(url_builder: self)
      else
        render_http_operation_result(result)
      end
    end

    def destroy
      result = HttpLocalizedOperationResult.new
      user_service.deleteUser(params[:login_name], result)
      render_http_operation_result(result)
    end

    def load_user
      @user_to_operate = user_service.findUserByName(params[:login_name])

      if !@user_to_operate || @user_to_operate.instance_of?(com.thoughtworks.go.domain.NullUser)
        raise ApiV1::RecordNotFound
      end
    end
  end
end
