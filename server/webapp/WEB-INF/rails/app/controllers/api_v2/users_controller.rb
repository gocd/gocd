##########################################################################
# Copyright 2018 ThoughtWorks, Inc.
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

module ApiV2
  class UsersController < ApiV2::BaseController
    include ApiV2::UsersHelper

    before_action :check_admin_user_and_403
    before_action :load_user, only: [:show, :update, :destroy]

    def index
      render DEFAULT_FORMAT => UsersRepresenter.new(user_service.allUsers).to_hash(url_builder: self)
    end

    def show
      render DEFAULT_FORMAT => UserRepresenter.new(@user_to_operate).to_hash(url_builder: self)
    end

    def update
      result = HttpLocalizedOperationResult.new
      @user_to_operate = save_user(result, @user_to_operate)
      if result.isSuccessful
        render DEFAULT_FORMAT => UserRepresenter.new(@user_to_operate).to_hash(url_builder: self)
      else
        render_http_operation_result(result)
      end
    end

    def create
      result = HttpLocalizedOperationResult.new

      user = nil
      created = false
      user_service.withEnableUserMutex do
        user = user_service.findUserByName(params[:login_name])
        if user.instance_of?(com.thoughtworks.go.domain.NullUser)
          user = save_user(result, com.thoughtworks.go.domain.User.new(params[:login_name]))
          created = true
        end
      end

      unless created
        return render_message("The user `#{params[:login_name]}` already exists.", :conflict)
      end

      if result.httpCode == 200
        response.location = apiv1_user_url(login_name: params[:login_name])
        render DEFAULT_FORMAT => UserRepresenter.new(user).to_hash(url_builder: self), status: :created
      else
        render_http_operation_result(result)
      end
    end

    def destroy
      result = HttpLocalizedOperationResult.new
      user_service.deleteUser(params[:login_name], string_username.to_s, result)
      render_http_operation_result(result)
    end

    def bulk_delete
      result = HttpLocalizedOperationResult.new
      bulk_deletion_obstruct = user_service.deleteUsers(Array.wrap(params[:users]), string_username.to_s, result)
      if result.isSuccessful
        render_http_operation_result(result)
      elsif bulk_deletion_obstruct
        render_http_operation_result(result, BulkDeletionFailureResultRepresenter.new(bulk_deletion_obstruct).to_hash(url_builder: self))
      else
        render_http_operation_result(result)
      end
    end

    private

  end
end
