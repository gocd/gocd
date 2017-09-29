# coding: utf-8

require 'transpec/syntax'
require 'transpec/syntax/mixin/send'
require 'transpec/syntax/mixin/owned_matcher'
require 'transpec/syntax/mixin/messaging_host'

module Transpec
  class Syntax
    class Receive < Syntax
      include Mixin::Send, Mixin::OwnedMatcher, Mixin::MessagingHost

      def dynamic_analysis_target?
        super && receiver_node.nil? && method_name == :receive
      end

      def remove_useless_and_return!
        removed = super
        return unless removed
        add_record(UselessAndReturnRecordBuilder.build(self))
      end

      def add_receiver_arg_to_any_instance_implementation_block!
        added = super
        return unless added
        add_record(AnyInstanceBlockRecordBuilder.build(self))
      end

      def any_instance?
        expectation.any_instance?
      end

      def any_instance_block_node
        return unless any_instance?
        super || expectation.block_node
      end

      class UselessAndReturnRecordBuilder < Mixin::UselessAndReturn::RecordBuilder
        def base_syntax
          "#{host.expectation.method_name_for_instance}(obj).to receive(:message)"
        end
      end

      class AnyInstanceBlockRecordBuilder < Mixin::AnyInstanceBlock::RecordBuilder
        def base_syntax
          "#{host.expectation.method_name}(Klass).to receive(:message)"
        end
      end
    end
  end
end
