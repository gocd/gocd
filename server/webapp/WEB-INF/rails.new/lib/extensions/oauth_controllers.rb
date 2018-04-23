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

Go::Application.config.after_initialize do

  Oauth2Provider::ClientsController.class_eval do
    layout 'admin'
    prepend_before_action :set_tab_name, :set_view_title

    private

    def set_tab_name
      @tab_name = "oauth-clients"
    end

    def set_view_title
      @view_title = "Administration"
    end
  end

  Oauth2Provider::UserTokensController.class_eval do
    layout 'my-cruise'

    prepend_before_action :set_tab_name

    def set_tab_name
      @current_tab_name = "preferences"
    end
  end
end