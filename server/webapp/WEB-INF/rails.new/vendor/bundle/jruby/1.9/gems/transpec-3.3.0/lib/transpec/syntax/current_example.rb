# coding: utf-8

require 'transpec/syntax'
require 'transpec/syntax/mixin/send'
require 'transpec/rspec_dsl'
require 'transpec/util'

module Transpec
  class Syntax
    class CurrentExample < Syntax
      include Mixin::Send, RSpecDSL, Util

      METHODS_YIELD_EXAMPLE = (EXAMPLE_METHODS + HOOK_METHODS + HELPER_METHODS).freeze

      def dynamic_analysis_target?
        super &&
          receiver_node.nil? &&
          [:example, :running_example].include?(method_name) &&
          block_node_taken_by_method(node).nil?
      end

      def convert!
        if block_node
          insert_after(block_node.loc.begin, " |#{block_arg_name}|") unless block_has_argument?
          replace(selector_range, block_arg_name.to_s) unless method_name == block_arg_name
          block_node.metadata[:added_example_block_arg] = true
        else
          replace(selector_range, 'RSpec.current_example')
        end

        add_record(RecordBuilder.build(self))
      end

      def block_node
        return @block_node if instance_variable_defined?(:@block_node)

        @block_node = node.each_ancestor(:block).find do |block_node|
          method_name = method_name_of_block_node(block_node)
          METHODS_YIELD_EXAMPLE.include?(method_name)
        end
      end

      def block_method_name
        method_name_of_block_node(block_node)
      end

      private

      def block_has_argument?
        block_arg_node || block_node.metadata[:added_example_block_arg]
      end

      def block_arg_node
        args_node = block_node.children[1]
        args_node.children.first
      end

      def block_arg_name
        if block_arg_node
          block_arg_node.children.first
        else
          :example
        end
      end

      def method_name_of_block_node(block_node)
        send_node = block_node.children.first
        send_node.children[1]
      end

      class RecordBuilder < Transpec::RecordBuilder
        include RSpecDSL

        param_names :current_example

        def old_syntax
          if current_example.block_node
            "#{base_dsl} { example }"
          else
            'def helper_method example; end'
          end
        end

        def new_syntax
          if current_example.block_node
            "#{base_dsl} { |example| example }"
          else
            'def helper_method RSpec.current_example; end'
          end
        end

        def base_dsl
          dsl = "#{current_example.block_method_name}"
          dsl << '(:name)' if HELPER_METHODS.include?(current_example.block_method_name)
          dsl
        end
      end
    end
  end
end
