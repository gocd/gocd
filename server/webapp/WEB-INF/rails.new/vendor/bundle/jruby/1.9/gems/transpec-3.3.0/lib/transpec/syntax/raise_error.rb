# coding: utf-8

require 'transpec/syntax'
require 'transpec/syntax/mixin/send'
require 'transpec/syntax/mixin/owned_matcher'

module Transpec
  class Syntax
    class RaiseError < Syntax
      include Mixin::Send, Mixin::OwnedMatcher

      def dynamic_analysis_target?
        super && receiver_node.nil? && method_name == :raise_error
      end

      def remove_error_specification_with_negative_expectation!
        return if expectation.positive?
        return unless specific_error?
        remove(parentheses_range)
        add_record(RecordBuilder.build(self))
      end

      def specific_error?
        !arg_nodes.empty?
      end

      def specific_class?
        specific_error? && arg_nodes.first.const_type?
      end

      def specific_message?
        if specific_class?
          arg_nodes.count >= 2
        else
          specific_error?
        end
      end

      class RecordBuilder < Transpec::RecordBuilder
        param_names :raise_error

        def old_syntax
          syntax = 'expect { }.not_to raise_error('

          if raise_error.specific_class?
            syntax << 'SpecificErrorClass'
            syntax << ', ' if raise_error.specific_message?
          end

          syntax << 'message' if raise_error.specific_message?

          syntax << ')'
        end

        def new_syntax
          'expect { }.not_to raise_error'
        end
      end
    end
  end
end
