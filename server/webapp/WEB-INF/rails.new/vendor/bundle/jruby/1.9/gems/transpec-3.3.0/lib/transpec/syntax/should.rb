# coding: utf-8

require 'transpec/syntax'
require 'transpec/syntax/mixin/should_base'
require 'transpec/syntax/mixin/monkey_patch'
require 'transpec/syntax/mixin/expectizable'
require 'transpec/util'

module Transpec
  class Syntax
    class Should < Syntax
      include Mixin::ShouldBase, Mixin::MonkeyPatch, Mixin::Expectizable, Util

      define_dynamic_analysis do |rewriter|
        register_syntax_availability_analysis_request(
          rewriter,
          :expect_available?,
          [:expect]
        )
      end

      def dynamic_analysis_target?
        super && receiver_node && [:should, :should_not].include?(method_name)
      end

      def expectize!(negative_form = 'not_to')
        fail ContextError.new("##{method_name}", '#expect', selector_range) unless expect_available?

        if proc_subject?
          replace(range_of_subject_method_taking_block, 'expect')
        else
          wrap_subject_in_expect!
        end

        replace(should_range, positive? ? 'to' : negative_form)

        @current_syntax_type = :expect
        add_record(RecordBuilder.build(self, negative_form))
      end

      def proc_subject?
        return true if proc_literal?(subject_node)
        return false unless subject_node.block_type?
        send_node = subject_node.children.first
        receiver_node, method_name, = *send_node
        receiver_node.nil? && method_name == :expect
      end

      def name_of_subject_method_taking_block
        range_of_subject_method_taking_block.source
      end

      private

      def expect_available?
        syntax_available?(__method__)
      end

      def range_of_subject_method_taking_block
        send_node = subject_node.children.first
        send_node.loc.expression
      end

      class RecordBuilder < Transpec::RecordBuilder
        param_names :should, :negative_form_of_to

        def old_syntax
          syntax = if should.proc_subject?
                     "#{should.name_of_subject_method_taking_block} { }.should"
                   else
                     'obj.should'
                   end

          syntax << '_not' unless should.positive?
          syntax
        end

        def new_syntax
          syntax = if should.proc_subject?
                     'expect { }.'
                   else
                     'expect(obj).'
                   end

          syntax << (should.positive? ? 'to' : negative_form_of_to)
        end
      end
    end
  end
end
