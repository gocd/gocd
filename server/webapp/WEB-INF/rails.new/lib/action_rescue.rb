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

module ActionRescue
  def rescue_action(exception)
    Rails.logger.error(format_exception(exception))
    if exception.is_a?(ActionController::InvalidAuthenticityToken)
      logger.error("REQUEST: #{request.inspect} SESSION: #{session.inspect} REQUEST_OBJECT_ID: #{request.object_id} SESSION_OBJECT_ID: #{session.object_id}") if ENV['LOG_REQUEST_AND_SESSION_FOR_INVALID_AUTH_TOKEN']
      redirect_to root_url
      return
    end
    render_error_template l.string("INTERNAL_SERVER_ERROR"), 500
  end

  def format_exception(exception)
    "#{exception}\n#{stacktrace_for(exception)}"
  end

  def stacktrace_for(exception)
    if exception.is_a?(org.jruby.NativeException)
      collect_stacktrace {|print_writer| exception.printBacktrace(print_writer) }
    elsif exception.is_a?(java.lang.Throwable)
      collect_stacktrace {|print_writer| exception.printStackTrace(print_writer) }
    elsif exception.respond_to?(:backtrace)
      exception.backtrace.join("\n")
    else
      "could not extract stacktrace from exception: #{exception.class} (#{exception})"
    end
  end

  def collect_stacktrace
    java.io.StringWriter writer = java.io.StringWriter.new();
    yield print_writer
    return writer.getBuffer().toString();
  end
end