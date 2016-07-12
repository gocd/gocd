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

require 'roar/decorator'
require 'roar/json'
require 'roar/json/hal'

module ApiV2
  class BaseRepresenter < Roar::Decorator
    include Representable::Hash
    include Representable::Hash::AllowSymbols

    include Roar::JSON::HAL
    include JavaImports

    SkipParseOnBlank = lambda { |fragment, *args|
      fragment.blank?
    }

    class_attribute :collection_items
    self.collection_items = []

    class <<self
      def property(name, options={})
        if options.delete(:case_insensitive_string)
          options.merge!({
                           getter: lambda { |options|
                             self.send(name).to_s if self.send(name)
                           },
                           setter: lambda { |value, options|
                             self.send(:"#{name}=", com.thoughtworks.go.config.CaseInsensitiveString.new(value)) if value
                           }
                         })
        end

        if options[:collection]
          self.collection_items << name
        end

        if options[:expect_hash]
          options[:skip_parse] = lambda { |fragment, options|

            if fragment.respond_to?(:has_key?) || fragment.instance_of?(String)
              false
            elsif fragment.nil?
              true
        else
              raise ApiV2::UnprocessableEntity, "Expected #{name} to contain an object, got a #{fragment.class} instead!"
        end
          }
        end

        unless options.delete(:skip_nil)
          options.merge!(render_nil: true)
        end

        super(name, options)
      end

      def error_representer(error_translation_map={}, &blk)
        self.property :errors, exec_context: :decorator, decorator: ApiV2::Config::ErrorRepresenter, skip_parse: true, skip_render: lambda { |object, options| object.empty? }
        class_attribute :error_translation_map
        self.error_translation_map = block_given? ? blk : error_translation_map

        class_eval <<-RUBY, __FILE__, __LINE__ + 1
          def errors
            translation_map = if self.error_translation_map.respond_to?(:call)
              translation_map = self.error_translation_map.call(represented)
            else
              self.error_translation_map
            end

            translation_map ||= {}

            represented.errors.inject({}) do |memo, (key, value)|
              translated_key = translation_map[key] || key
              memo[translated_key] = value
              memo
            end
          end
        RUBY
      end

    end

    def to_hash(*options)
      super.deep_symbolize_keys unless @represented.nil?
    end

    def from_hash(data, options={})
      super(with_default_values(data), options)
    end

    private
    def with_default_values(hash)
      hash ||= {}

      if hash.respond_to?(:has_key?)
        hash = hash.deep_symbolize_keys
      end

      self.collection_items.inject(hash) do |memo, item|
        memo[item] ||= []
        memo
      end
    end
  end
end
