# coding: utf-8

require 'transpec/base_rewriter'
require 'transpec/config'
require 'transpec/project'
require 'transpec/report'
require 'transpec/spec_suite'
require 'transpec/syntax'

Transpec::Syntax.require_all

module Transpec
  class Converter < BaseRewriter # rubocop:disable ClassLength
    include Syntax::Dispatcher

    attr_reader :spec_suite, :project, :config, :report

    alias_method :convert_file!, :rewrite_file!
    alias_method :convert_source, :rewrite_source
    alias_method :convert, :rewrite

    def initialize(spec_suite = nil, config = nil)
      @spec_suite = spec_suite || SpecSuite.new
      @config = config || Config.new
      @report = Report.new
    end

    def project
      spec_suite.project
    end

    def runtime_data
      spec_suite.runtime_data
    end

    def rspec_version
      project.rspec_version
    end

    def process(ast, source_rewriter)
      return unless ast

      ast.each_node do |node|
        begin
          dispatch_node(node, runtime_data, project, source_rewriter, report)
        rescue ConversionError => error
          report.conversion_errors << error
        end
      end
    end

    def process_should(should)
      return unless config.convert?(:should)
      should.expectize!(config.negative_form_of_to)
    end

    def process_oneliner_should(oneliner_should)
      negative_form = config.negative_form_of_to
      should_convert_have_items = config.convert?(:have_items) &&
                                  oneliner_should.have_matcher.conversion_target?

      if should_convert_have_items
        if config.convert?(:should)
          oneliner_should.convert_have_items_to_standard_expect!(negative_form)
        else
          oneliner_should.convert_have_items_to_standard_should!
        end
      elsif config.convert?(:oneliner) && rspec_version.oneliner_is_expected_available?
        oneliner_should.expectize!(negative_form)
      end
    end

    def process_should_receive(should_receive)
      if should_receive.useless_expectation?
        if config.convert?(:deprecated)
          if config.convert?(:stub)
            should_receive.allowize_useless_expectation!(config.negative_form_of_to)
          else
            should_receive.stubize_useless_expectation!
          end
        elsif config.convert?(:should_receive)
          should_receive.expectize!(config.negative_form_of_to)
        end
      elsif config.convert?(:should_receive)
        should_receive.expectize!(config.negative_form_of_to)
      end
    end

    def process_double(double)
      double.convert_to_double! if config.convert?(:deprecated)
    end

    def process_method_stub(method_stub)
      if config.convert?(:stub)
        if !method_stub.hash_arg? ||
           rspec_version.receive_messages_available? ||
           config.convert?(:stub_with_hash)
          method_stub.allowize!
        elsif config.convert?(:deprecated)
          method_stub.convert_deprecated_method!
        end
      elsif config.convert?(:deprecated)
        method_stub.convert_deprecated_method!
      end

      method_stub.remove_no_message_allowance! if config.convert?(:deprecated)
    end

    def process_operator(operator)
      return unless config.convert?(:should)
      return if operator.expectation.is_a?(Syntax::OnelinerShould) &&
                !rspec_version.oneliner_is_expected_available?
      operator.convert_operator!(config.parenthesize_matcher_arg?)
    end

    def process_be_boolean(be_boolean)
      return unless rspec_version.be_truthy_available?
      return unless config.convert?(:deprecated)

      case config.boolean_matcher_type
      when :conditional
        be_boolean.convert_to_conditional_matcher!(config.form_of_be_falsey)
      when :exact
        be_boolean.convert_to_exact_matcher!
      end
    end

    def process_be_close(be_close)
      be_close.convert_to_be_within! if config.convert?(:deprecated)
    end

    def process_raise_error(raise_error)
      return unless raise_error
      if config.convert?(:deprecated)
        raise_error.remove_error_specification_with_negative_expectation!
      end
    end

    def process_its(its)
      its.convert_to_describe_subject_it! if config.convert?(:its)
    end

    def process_example(example)
      return if !rspec_version.rspec_2_99? || !config.convert?(:pending)
      example.convert_pending_to_skip!
    end

    def process_pending(pending)
      return if !rspec_version.rspec_2_99? || !config.convert?(:pending)
      pending.convert_deprecated_syntax!
    end

    def process_current_example(current_example)
      return unless rspec_version.yielded_example_available?
      current_example.convert! if config.convert?(:deprecated)
    end

    def process_matcher_definition(matcher_definition)
      return unless rspec_version.non_should_matcher_protocol_available?
      matcher_definition.convert_deprecated_method! if config.convert?(:deprecated)
    end

    def process_example_group(example_group)
      if rspec_version.non_monkey_patch_example_group_available? && config.convert?(:example_group)
        example_group.convert_to_non_monkey_patch!
      end

      if rspec_version.implicit_spec_type_disablement_available? &&
         config.add_explicit_type_metadata_to_example_group?
        example_group.add_explicit_type_metadata!
      end
    end

    def process_rspec_configure(rspec_configure)
      if config.convert?(:deprecated)
        rspec_configure.convert_deprecated_options!
      end

      if spec_suite.main_rspec_configure_node?(rspec_configure.node)
        if rspec_version.non_monkey_patch_example_group_available? &&
           config.convert?(:example_group)
          rspec_configure.expose_dsl_globally = false
        end

        if need_to_modify_yield_receiver_to_any_instance_implementation_blocks_config?
          should_yield = config.add_receiver_arg_to_any_instance_implementation_block?
          rspec_configure.mocks.yield_receiver_to_any_instance_implementation_blocks = should_yield
        end

        if rspec_version.implicit_spec_type_disablement_available? &&
           !config.add_explicit_type_metadata_to_example_group?
          rspec_configure.infer_spec_type_from_file_location!
        end
      end
    end

    def process_have(have)
      return if !have || !config.convert?(:have_items)
      have.convert_to_standard_expectation!(config.parenthesize_matcher_arg)
    end

    def process_hook(hook)
      return if !config.convert?(:hook_scope) || !rspec_version.hook_scope_alias_available?
      hook.convert_scope_name!
    end

    def process_useless_and_return(messaging_host)
      return unless messaging_host
      return unless config.convert?(:deprecated)
      messaging_host.remove_useless_and_return!
    end

    def process_any_instance_block(messaging_host)
      return unless messaging_host
      return unless rspec_version.rspec_2_99?
      return unless config.convert?(:deprecated)
      return unless config.add_receiver_arg_to_any_instance_implementation_block?
      messaging_host.add_receiver_arg_to_any_instance_implementation_block!
    end

    def need_to_modify_yield_receiver_to_any_instance_implementation_blocks_config?
      rspec_version.rspec_2_99? && config.convert?(:deprecated) &&
        spec_suite.need_to_modify_yield_receiver_to_any_instance_implementation_blocks_config?
    end
  end
end
