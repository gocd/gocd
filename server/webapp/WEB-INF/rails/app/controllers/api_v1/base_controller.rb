#
# Copyright 2019 ThoughtWorks, Inc.
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

module ApiV1
  class BaseController < ::ApplicationController

    include AuthenticationHelper
    self.etag_with_template_digest = false

    FORMATS = [:json_hal_v1]
    DEFAULT_FORMAT = FORMATS.last
    DEFAULT_ACCEPTS_HEADER = Mime[DEFAULT_FORMAT].to_s

    skip_before_action :verify_authenticity_token
    before_action :verify_content_type_on_post
    before_action :set_default_response_format

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
      render DEFAULT_FORMAT => json, location: url
    end

    rescue_from RecordNotFound, with: :render_not_found_error
    rescue_from BadRequest, with: :render_bad_request
    rescue_from UnprocessableEntity, with: :render_unprocessable_entity_error
    rescue_from FailedDependency, with: :render_failed_dependency_error

    class << self
      def default_accepts_header
        DEFAULT_ACCEPTS_HEADER
      end
    end

    protected
    include ApiEtagHelper

    def handle_create_or_update_response(result, updated_entity)
      json = entity_representer.new(updated_entity).to_hash(url_builder: self)
      if result.isSuccessful
        response.etag = [etag_for(updated_entity)]
        render DEFAULT_FORMAT => json
      else
        render_http_operation_result(result, {data: json})
      end
    end

    def to_tristate(var)
      TriState.from(var.to_s)
    rescue => e
      raise BadRequest.new(e.message)
    end

    def render_http_operation_result(result, data = {})
      status = result.httpCode()
      if result.instance_of?(HttpOperationResult)
        render_message(result.detailedMessage(), status, data)
      else
        render_message(result.message(), status, data)
      end
    end

    def render_message(message, status = :ok, data = {})
      render DEFAULT_FORMAT => {message: message.strip}.merge(data), status: status
    end

    def render_unprocessable_entity_error(exception)
      render_message("Your request could not be processed. #{exception.message}", :unprocessable_entity)
    end

    def render_failed_dependency_error(exception)
      render_message("Your request could not be processed. #{exception.message}", 424)
    end
  end
end
