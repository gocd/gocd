# coding: utf-8

require 'active_support/concern'
require 'transpec/syntax/mixin/send'
require 'transpec/util'

module Transpec
  class Syntax
    module Mixin
      module AnyInstanceBlock
        extend ActiveSupport::Concern
        include Send

        def need_to_add_receiver_arg_to_any_instance_implementation_block?
          first_arg_node = any_instance_block_first_arg_node
          return false unless first_arg_node
          first_arg_name = first_arg_node.children.first
          first_arg_name != :instance
        end

        def add_receiver_arg_to_any_instance_implementation_block!
          return unless need_to_add_receiver_arg_to_any_instance_implementation_block?
          insert_before(any_instance_block_first_arg_node.loc.expression, 'instance, ')
          true
        end

        private

        def any_instance_block_first_arg_node
          return nil unless any_instance_block_node
          any_instance_block_node.children[1].children[0]
        end

        def any_instance_block_node
          return unless any_instance?
          Util.each_backward_chained_node(node).find(&:block_type?)
        end

        class RecordBuilder < Transpec::RecordBuilder
          param_names :host

          def old_syntax
            "#{base_syntax} { |arg| }"
          end

          def new_syntax
            "#{base_syntax} { |instance, arg| }"
          end

          def base_syntax
            fail NotImplementedError
          end
        end

        class MonkeyPatchRecordBuilder < AnyInstanceBlock::RecordBuilder
          def base_syntax
            "Klass.any_instance.#{host.method_name}(:message)"
          end
        end
      end
    end
  end
end
