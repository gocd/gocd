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

module Admin
  class GarageController < AdminController
    layout false

    def index
      @garage_data = garage_service.getData
    end

    def gc
      result = HttpLocalizedOperationResult.new
      garage_service.gc(result)
      if result.isSuccessful
        flash[:notice] = {:gc => "#{result.message(Spring.bean('localizer'))}"}
      else
        flash[:error] = {:gc => result.message(Spring.bean('localizer'))}
      end
      redirect_to garage_index_path
    end

  end
end
