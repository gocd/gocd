# coding: utf-8

require 'transpec/syntax'
require 'transpec/syntax/mixin/send'

module Transpec
  class Syntax
    class BeClose < Syntax
      include Mixin::Send

      def dynamic_analysis_target?
        super && receiver_node.nil? && method_name == :be_close
      end

      def convert_to_be_within!
        _receiver_node, _method_name, expected_node, delta_node = *node

        be_within_source = 'be_within('
        be_within_source << delta_node.loc.expression.source
        be_within_source << ').of('
        be_within_source << expected_node.loc.expression.source
        be_within_source << ')'

        replace(expression_range, be_within_source)

        add_record
      end

      private

      def add_record
        super('be_close(expected, delta)', 'be_within(delta).of(expected)')
      end
    end
  end
end
