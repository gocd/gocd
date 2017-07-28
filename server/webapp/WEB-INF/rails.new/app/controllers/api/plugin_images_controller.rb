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

module Api
  class PluginImagesController < ::ApplicationController
    def show
      image = default_plugin_info_finder.getImage(params[:plugin_id], params[:hash])
      if image
        response.content_type = image.getContentType
        response.charset = false
        expires_in 1.year, public: false
        render text: image.getDataAsBytes if stale?(etag: image.getHash)
      else
        head(:not_found)
      end
    end

  end
end
