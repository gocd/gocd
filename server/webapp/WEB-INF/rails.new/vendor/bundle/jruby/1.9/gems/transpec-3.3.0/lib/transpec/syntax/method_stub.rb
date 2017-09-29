# coding: utf-8

require 'transpec/syntax'
require 'transpec/syntax/mixin/monkey_patch_any_instance'
require 'transpec/syntax/mixin/messaging_host'
require 'transpec/util'
require 'English'

module Transpec
  class Syntax
    class MethodStub < Syntax
      include Mixin::MonkeyPatchAnyInstance, Mixin::MessagingHost, Util

      CLASSES_DEFINING_OWN_STUB_METHOD = [
        'Typhoeus', # https://github.com/typhoeus/typhoeus/blob/6a59c62/lib/typhoeus.rb#L66-L85
        'Excon',    # https://github.com/geemus/excon/blob/6af4f9c/lib/excon.rb#L143-L178
        'Factory'   # https://github.com/thoughtbot/factory_girl/blob/v3.6.2/lib/factory_girl/syntax/vintage.rb#L112
      ]

      define_dynamic_analysis do |rewriter|
        register_syntax_availability_analysis_request(
          rewriter,
          :allow_to_receive_available?,
          [:allow, :receive]
        )
      end

      def dynamic_analysis_target?
        super &&
          receiver_node &&
          [:stub, :stub!, :stub_chain, :unstub, :unstub!].include?(method_name) &&
          arg_node
      end

      def conversion_target?
        return false unless dynamic_analysis_target?

        # Check if the method is RSpec's one or not.
        if runtime_data.run?(send_analysis_target_node)
          # If we have runtime data, check with it.
          defined_by_rspec?
        else
          # Otherwise check with a static whitelist.
          const_name = const_name(receiver_node)
          !CLASSES_DEFINING_OWN_STUB_METHOD.include?(const_name)
        end
      end

      def allow_to_receive_available?
        syntax_available?(__method__)
      end

      def hash_arg?
        arg_node.hash_type?
      end

      def unstub?
        [:unstub, :unstub!].include?(method_name)
      end

      def allowize!
        return if method_name == :stub_chain && !rspec_version.receive_message_chain_available?

        unless allow_to_receive_available?
          fail ContextError.new("##{method_name}", '#allow', selector_range)
        end

        source, type = replacement_source_and_conversion_type(rspec_version)
        return unless source

        replace(expression_range, source)

        add_record(AllowRecordBuilder.build(self, type))
      end

      def convert_deprecated_method!
        return unless replacement_for_deprecated_method
        replace(selector_range, replacement_for_deprecated_method)
        add_record(DeprecatedMethodRecordBuilder.build(self))
      end

      def remove_no_message_allowance!
        return unless allow_no_message?
        super
        add_record(NoMessageAllowanceRecordBuilder.build(self))
      end

      def remove_useless_and_return!
        return unless super
        add_record(Mixin::UselessAndReturn::MonkeyPatchRecordBuilder.build(self))
      end

      def add_receiver_arg_to_any_instance_implementation_block!
        return unless super
        add_record(Mixin::AnyInstanceBlock::MonkeyPatchRecordBuilder.build(self))
      end

      def replacement_for_deprecated_method
        case method_name
        when :stub!   then 'stub'
        when :unstub! then 'unstub'
        else nil
        end
      end

      private

      def replacement_source_and_conversion_type(rspec_version)
        if method_name == :stub_chain
          [build_allow_to(:receive_message_chain), :allow_to_receive_message_chain]
        else
          if hash_arg?
            if rspec_version.receive_messages_available?
              [build_allow_to(:receive_messages), :allow_to_receive_messages]
            else
              [build_multiple_allow_to_receive_with_hash(arg_node), :allow_to_receive]
            end
          else
            [build_allow_to_receive(arg_node, nil, !unstub?), :allow_to_receive]
          end
        end
      end

      def build_multiple_allow_to_receive_with_hash(hash_node)
        expressions = []

        hash_node.children.each_with_index do |pair_node, index|
          key_node, value_node = *pair_node
          expression = build_allow_to_receive(key_node, value_node, false)
          expression.prepend(indentation_of_line(node)) if index > 0
          expressions << expression
        end

        expressions.join($RS)
      end

      def build_allow_to_receive(message_node, value_node = nil, keep_form_around_arg = true)
        expression =  allow_source
        expression << range_in_between_receiver_and_selector.source
        expression << 'to receive'
        expression << (keep_form_around_arg ? range_in_between_selector_and_arg.source : '(')
        expression << message_source(message_node)
        expression << (keep_form_around_arg ? range_after_arg.source : ')')
        expression << ".and_return(#{value_node.loc.expression.source})" if value_node
        expression << '.and_call_original' if unstub?
        expression
      end

      def build_allow_to(method)
        expression =  allow_source
        expression << range_in_between_receiver_and_selector.source
        expression << "to #{method}"
        expression << parentheses_range.source
        expression
      end

      def allow_source
        if any_instance?
          "allow_any_instance_of(#{any_instance_target_class_source})"
        else
          "allow(#{subject_range.source})"
        end
      end

      def message_source(node)
        message_source = node.loc.expression.source
        message_source.prepend(':') if node.sym_type? && !message_source.start_with?(':')
        message_source
      end

      class AllowRecordBuilder < RecordBuilder
        param_names :method_stub, :conversion_type

        def old_syntax
          syntax = method_stub.any_instance? ? 'Klass.any_instance' : 'obj'
          syntax << ".#{method_stub.method_name}"

          if method_stub.method_name == :stub_chain
            syntax << '(:message1, :message2)'
          else
            syntax << (method_stub.hash_arg? ? '(:message => value)' : '(:message)')
          end
        end

        def new_syntax
          syntax = method_stub.any_instance? ? 'allow_any_instance_of(Klass)' : 'allow(obj)'
          syntax << '.to '

          case conversion_type
          when :allow_to_receive
            syntax << 'receive(:message)'
            syntax << '.and_return(value)' if method_stub.hash_arg?
            syntax << '.and_call_original' if method_stub.unstub?
          when :allow_to_receive_messages
            syntax << 'receive_messages(:message => value)'
          when :allow_to_receive_message_chain
            syntax << 'receive_message_chain(:message1, :message2)'
          end

          syntax
        end
      end

      class DeprecatedMethodRecordBuilder < RecordBuilder
        param_names :method_stub

        def old_syntax
          syntax = method_stub.any_instance? ? 'Klass.any_instance' : 'obj'
          syntax << ".#{method_stub.method_name}(:message)"
        end

        def new_syntax
          syntax = 'obj.'
          syntax << method_stub.replacement_for_deprecated_method
          syntax << '(:message)'
        end
      end

      class NoMessageAllowanceRecordBuilder < RecordBuilder
        param_names :method_stub

        def old_syntax
          syntax = base_syntax
          syntax << '.any_number_of_times' if method_stub.any_number_of_times?
          syntax << '.at_least(0)' if method_stub.at_least_zero?
          syntax
        end

        def new_syntax
          base_syntax
        end

        def base_syntax
          "obj.#{method_stub.method_name}(:message)"
        end
      end
    end
  end
end
