# coding: utf-8

require 'transpec/dynamic_analyzer/constants'
require 'transpec/dynamic_analyzer/node_util'
require 'json'
require 'ostruct'

module Transpec
  class DynamicAnalyzer
    class RuntimeData
      include NodeUtil

      attr_reader :data

      def self.load(string_or_io)
        data = JSON.load(string_or_io, nil, object_class: CompatibleOpenStruct)
        new(data)
      end

      def initialize(data = CompatibleOpenStruct.new)
        error_message = data[RUNTIME_DATA_ERROR_MESSAGE_KEY]
        fail AnalysisError, error_message if error_message

        @data = data
      end

      def [](node, key = nil)
        node_data = data[node_id(node)]
        return nil unless node_data
        return node_data unless key
        node_data[key]
      end

      def run?(node)
        !self[node].nil?
      end

      def present?(node, key)
        node_data = self[node]
        return false unless node_data
        node_data.respond_to?(key)
      end

      class CompatibleOpenStruct < OpenStruct
        # OpenStruct#[] is available from Ruby 2.0.
        unless method_defined?(:[])
          def [](key)
            __send__(key)
          end
        end

        unless method_defined?(:[]=)
          def []=(key, value)
            __send__("#{key}=", value)
          end
        end
      end
    end
  end
end
