#
# Copyright 2023 Thoughtworks, Inc.
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

module PrototypeHelper
  include ActionView::Helpers::JavaScriptHelper

    unless const_defined? :CALLBACKS
    CALLBACKS    = Set.new([ :create, :uninitialized, :loading, :loaded,
                             :interactive, :complete, :failure, :success ] +
                               (100..599).to_a)
    AJAX_OPTIONS = Set.new([ :before, :after, :condition, :url,
                             :asynchronous, :method, :insertion, :position,
                             :form, :with, :update, :script, :type ]).merge(CALLBACKS)
  end

  protected
  def options_for_ajax(options)
    js_options = build_callbacks(options)

    content_type = if options[:headers]
                     key, _ = options[:headers].find {|k, _| k.to_s.downcase == 'content-type'}
                     options[:headers].delete(key)
                   end

    js_options['requestHeaders'] = quote_hash(options[:headers]) if options[:headers]
    js_options['contentType'] = "'#{content_type}'" if content_type
    js_options['method'] = method_option_to_s(options[:method]) if options[:method]

    if protect_against_forgery? && !options[:form]
      js_options['parameters'] = "'#{request_forgery_protection_token}=' + encodeURIComponent('#{escape_javascript form_authenticity_token}')"
    end

    options_for_javascript(js_options)
  end

  def quote_hash(hash)
    "{#{hash.keys.map { |key| "'#{key}':'#{hash[key]}'" }.join(', ')}}"
  end

  def method_option_to_s(method)
    (method.is_a?(String) and !method.index("'").nil?) ? method : "'#{method}'"
  end

  def build_callbacks(options)
    callbacks = {}
    options.each do |callback, code|
      if CALLBACKS.include?(callback)
        name = 'on' + callback.to_s.capitalize
        callbacks[name] = "function(request){#{code}}"
      end
    end
    callbacks
  end

  def options_for_javascript(options)
    if options.empty?
      '{}'
    else
      "{#{options.keys.map { |k| "#{k}:#{options[k]}" }.sort.join(', ')}}"
    end
  end

end

