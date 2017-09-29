# coding: utf-8

require 'transpec/syntax'
require 'transpec/syntax/mixin/expectizable'
require 'transpec/syntax/mixin/monkey_patch_any_instance'
require 'transpec/syntax/mixin/messaging_host'
require 'transpec/util'

module Transpec
  class Syntax
    class ShouldReceive < Syntax
      include Mixin::Expectizable, Mixin::MonkeyPatchAnyInstance, Mixin::MessagingHost, Util

      define_dynamic_analysis do |rewriter|
        register_syntax_availability_analysis_request(
          rewriter,
          :expect_to_receive_available?,
          [:expect, :receive]
        )

        register_syntax_availability_analysis_request(
          rewriter,
          :allow_to_receive_available?,
          [:allow, :receive]
        )
      end

      alias_method :useless_expectation?, :allow_no_message?

      def dynamic_analysis_target?
        super &&
          receiver_node &&
          [:should_receive, :should_not_receive].include?(method_name)
      end

      def expect_to_receive_available?
        syntax_available?(__method__)
      end

      def allow_to_receive_available?
        syntax_available?(__method__)
      end

      def positive?
        method_name == :should_receive
      end

      def expectize!(negative_form = 'not_to')
        unless expect_to_receive_available?
          fail ContextError.new("##{method_name}", '#expect', selector_range)
        end

        convert_to_syntax!('expect', negative_form)

        add_record(ExpectRecordBuilder.build(self, negative_form))
      end

      def allowize_useless_expectation!(negative_form = 'not_to')
        return unless useless_expectation?

        unless allow_to_receive_available?
          fail ContextError.new("##{method_name}", '#allow', selector_range)
        end

        convert_to_syntax!('allow', negative_form)
        remove_no_message_allowance!

        add_record(AllowRecordBuilder.build(self, negative_form))
      end

      def stubize_useless_expectation!
        return unless useless_expectation?

        replace(selector_range, 'stub')
        remove_no_message_allowance!

        add_record(StubRecordBuilder.build(self))
      end

      def remove_useless_and_return!
        return unless super
        add_record(Mixin::UselessAndReturn::MonkeyPatchRecordBuilder.build(self))
      end

      def add_receiver_arg_to_any_instance_implementation_block!
        return unless super
        add_record(Mixin::AnyInstanceBlock::MonkeyPatchRecordBuilder.build(self))
      end

      private

      def convert_to_syntax!(syntax, negative_form)
        if any_instance?
          wrap_subject_with_any_instance_of!(syntax)
        else
          wrap_subject_with_method!(syntax)
        end

        replace(selector_range, "#{positive? ? 'to' : negative_form} receive")

        correct_block_style!
      end

      def correct_block_style!
        return if broken_block_nodes.empty?

        broken_block_nodes.each do |block_node|
          map = block_node.loc
          next if map.begin.source == '{'
          replace(map.begin, '{')
          replace(map.end, '}')
        end
      end

      def wrap_subject_with_any_instance_of!(syntax)
        expression = "#{syntax}_any_instance_of(#{any_instance_target_class_source})"
        replace(subject_range, expression)
      end

      def broken_block_nodes
        @broken_block_nodes ||= [
          block_node_taken_by_with_method_with_no_normal_args,
          block_node_followed_by_fluent_method
        ].compact.uniq
      end

      # subject.should_receive(:method_name).once.with do |block_arg|
      # end
      #
      # (block
      #   (send
      #     (send
      #       (send
      #         (send nil :subject) :should_receive
      #         (sym :method_name)) :once) :with)
      #   (args
      #     (arg :block_arg)) nil)
      def block_node_taken_by_with_method_with_no_normal_args
        each_backward_chained_node(node, :child_as_second_arg) do |chained_node, child_node|
          next unless chained_node.block_type?
          return nil unless child_node.children[1] == :with
          return nil if child_node.children[2]
          return chained_node
        end
      end

      # subject.should_receive(:method_name) do |block_arg|
      # end.once
      #
      # (send
      #   (block
      #     (send
      #       (send nil :subject) :should_receive
      #       (sym :method_name))
      #     (args
      #       (arg :block_arg)) nil) :once)
      def block_node_followed_by_fluent_method
        each_backward_chained_node(node, :child_as_second_arg) do |chained_node, child_node|
          next unless chained_node.send_type?
          return child_node if child_node.block_type?
        end
      end

      class ExpectBaseRecordBuilder < RecordBuilder
        param_names :should_receive, :negative_form_of_to

        def syntax_name
          fail NotImplementedError
        end

        def old_syntax
          syntax = if should_receive.any_instance?
                     'Klass.any_instance.'
                   else
                     'obj.'
                   end
          syntax << "#{should_receive.method_name}(:message)"
        end

        def new_syntax
          syntax = if should_receive.any_instance?
                     "#{syntax_name}_any_instance_of(Klass)."
                   else
                     "#{syntax_name}(obj)."
                   end
          syntax << (should_receive.positive? ? 'to' : negative_form_of_to)
          syntax << ' receive(:message)'
        end
      end

      class ExpectRecordBuilder < ExpectBaseRecordBuilder
        def syntax_name
          'expect'
        end
      end

      class AllowRecordBuilder < ExpectBaseRecordBuilder
        def syntax_name
          'allow'
        end

        def old_syntax
          syntax = super
          syntax << '.any_number_of_times' if should_receive.any_number_of_times?
          syntax << '.at_least(0)' if should_receive.at_least_zero?
          syntax
        end
      end

      class StubRecordBuilder < RecordBuilder
        param_names :should_receive

        def old_syntax
          syntax = "obj.#{should_receive.method_name}(:message)"
          syntax << '.any_number_of_times' if should_receive.any_number_of_times?
          syntax << '.at_least(0)' if should_receive.at_least_zero?
          syntax
        end

        def new_syntax
          'obj.stub(:message)'
        end
      end
    end
  end
end
