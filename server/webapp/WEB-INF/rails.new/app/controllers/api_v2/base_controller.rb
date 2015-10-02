##########################GO-LICENSE-START################################
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
##########################GO-LICENSE-END##################################

module ApiV2
  class BaseController < ::ApplicationController

    class BadRequest < StandardError
    end

    include AuthenticationHelper

    FORMATS                = [:json_hal_v2]
    DEFAULT_FORMAT         = FORMATS.last
    DEFAULT_ACCEPTS_HEADER = Mime[DEFAULT_FORMAT].to_s

    skip_before_filter :verify_authenticity_token
    before_filter :verify_content_type_on_post
    before_filter :set_default_response_format

    def set_default_response_format
      request.format = DEFAULT_FORMAT unless params[:format]
    end

    def redirect_json_hal(url, status=302)
      json = {
        _links: {
          redirect: {
            href: url
          }
        }
      }

      response.status = status
      render json_hal_v2: json, location: url
    end

    rescue_from RecordNotFound, with: :render_not_found_error
    rescue_from BadRequest,     with: :render_bad_request

    class << self
      def default_accepts_header
        DEFAULT_ACCEPTS_HEADER
      end
    end

    protected

    def to_tristate(var)
      TriState.from(var.to_s)
    rescue => e
      raise BadRequest.new(e.message)
    end

    def render_http_operation_result(result)
      if result.instance_of?(HttpOperationResult)
        render json_hal_v2: { message: result.detailedMessage().strip }, status: result.httpCode()
      else
        render json_hal_v2: { message: result.message(Spring.bean('localizer')).strip }, status: result.httpCode()
      end
    end
  end
end
