# coding: utf-8

require 'transpec/syntax'
require 'transpec/syntax/mixin/send'
require 'transpec/syntax/mixin/owned_matcher'

module Transpec
  class Syntax
    class Have < Syntax
      require 'transpec/syntax/have/dynamic_analysis'
      require 'transpec/syntax/have/record_builder'
      require 'transpec/syntax/have/source_builder'

      include Mixin::Send, Mixin::OwnedMatcher, DynamicAnalysis

      # String#count is not query method, and there's no way to determine
      # whether a method is query method.
      # Method#arity and Method#parameters return same results
      # for Array#count (0+ args) and String#count (1+ args).
      #
      # So I make #size a priority over #count so that #count won't be chosen
      # for String (String responds to #size).
      QUERY_METHOD_PRIORITIES = [:size, :count, :length].freeze

      def dynamic_analysis_target?
        super &&
          receiver_node.nil? &&
          [:have, :have_exactly, :have_at_least, :have_at_most].include?(method_name) &&
          node.sibling_index.zero?
      end

      def conversion_target?
        return false unless super
        return false if runtime_subject_data(:project_requires_collection_matcher?)
        !active_model_errors_on?
      end

      def convert_to_standard_expectation!(parenthesize_matcher_arg = true)
        replace(expectation.subject_range, replacement_subject_source) if explicit_subject?
        replace(matcher_range, source_builder.replacement_matcher_source(parenthesize_matcher_arg))
        add_record if explicit_subject?
      end

      def explicit_subject?
        expectation.respond_to?(:subject_node)
      end

      alias_method :have_node, :node
      alias_method :items_node, :parent_node

      def size_node
        have_node.children[2]
      end

      def items_method_has_arguments?
        items_node.children.size > 2
      end

      def items_name
        items_node.children[1]
      end

      def collection_accessor
        if runtime_subject_data(:collection_accessor)
          runtime_subject_data(:collection_accessor).to_sym
        else
          items_name
        end
      end

      def subject_is_owner_of_collection?
        return true if items_method_has_arguments?
        runtime_subject_data(:collection_accessor)
      end

      def active_model_errors_on?
        return false unless runtime_subject_data(:subject_includes_active_model_validations?)
        [:errors_on, :error_on].include?(items_name)
      end

      def collection_accessor_is_private?
        runtime_subject_data(:collection_accessor_is_private?)
      end

      def query_method
        if (available_query_methods = runtime_subject_data(:available_query_methods))
          (QUERY_METHOD_PRIORITIES & available_query_methods.map(&:to_sym)).first
        else
          default_query_method
        end
      end

      def default_query_method
        QUERY_METHOD_PRIORITIES.first
      end

      def replacement_subject_source(base_subject = nil)
        base_subject ||= expectation.subject_node
        source_builder.replacement_subject_source(base_subject)
      end

      # https://github.com/rspec/rspec-expectations/blob/v2.14.5/lib/rspec/matchers/built_in/have.rb#L8-L12
      def size_source
        if size_node.sym_type? && size_node.children.first == :no
          0
        elsif size_node.str_type?
          size_node.children.first.to_i
        else
          size_node.loc.expression.source
        end
      end

      def accurate_conversion?
        runtime_subject_data
      end

      def matcher_range
        expression_range.join(items_node.loc.expression)
      end

      private

      def source_builder
        @source_builder ||= SourceBuilder.new(self, size_source)
      end

      def runtime_subject_data(key = nil)
        unless instance_variable_defined?(:@runtime_subject_data)
          node = explicit_subject? ? expectation.subject_node : expectation.node
          @runtime_subject_data = runtime_data[node]
        end

        return @runtime_subject_data unless key

        if @runtime_subject_data
          @runtime_subject_data[key]
        else
          nil
        end
      end

      def add_record
        super(Have::RecordBuilder.build(self))
      end
    end
  end
end
