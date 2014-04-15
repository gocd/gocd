require "multi_json/vendor/ok_json" unless defined?(::OkJson)

module MultiJson
  module Engines
    class OkJson
      ParseError = ::OkJson::Error

      def self.decode(string, options = {}) #:nodoc:
        string = string.read if string.respond_to?(:read)
        result = ::OkJson.decode(string)
        options[:symbolize_keys] ? symbolize_keys(result) : result
      end

      def self.encode(object) #:nodoc:
        ::OkJson.encode(stringify_keys(object))
      end

      def self.symbolize_keys(object) #:nodoc:
        return object unless object.is_a?(Hash)
        object.inject({}) do |result, (key, value)|
          new_key   = key.is_a?(String) ? key.to_sym : key
          new_value = value.is_a?(Hash) ? symbolize_keys(value) : value
          result.merge! new_key => new_value
        end
      end

      def self.stringify_keys(object) #:nodoc:
        return object unless object.is_a?(Hash)
        object.inject({}) do |result, (key, value)|
          new_key   = key.is_a?(Symbol) ? key.to_s : key
          new_value = value.is_a?(Hash) ? stringify_keys(value) : value
          result.merge! new_key => new_value
        end
      end
    end
  end
end
