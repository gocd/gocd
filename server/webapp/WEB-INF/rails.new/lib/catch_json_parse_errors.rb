##########################GO-LICENSE-START################################
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
##########################GO-LICENSE-END##################################
class CatchJsonParseErrors
  def initialize (app)
    @app = app
  end

  def call (env)
    begin
      @app.call(env)
    rescue ActionDispatch::ParamsParser::ParseError => exception
      content_type_is_json?(env) ? build_response(exception) : raise(exception)
    end
  end

  private

  def content_type_is_json? (env)
    env['CONTENT_TYPE'] =~ /application\/json/
  end

  def error_message (exception)
    "Payload data is not valid JSON. Error message: #{exception}"
  end

  def build_response (exception)
    [400, {"Content-Type" => "application/json"}, [{error: error_message(exception)}.to_json]]
  end

end