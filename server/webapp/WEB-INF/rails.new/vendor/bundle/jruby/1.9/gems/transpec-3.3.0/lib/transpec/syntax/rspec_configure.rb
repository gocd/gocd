# coding: utf-8

require 'transpec/syntax'
require 'transpec/syntax/mixin/rspec_rails'
require 'transpec/syntax/mixin/send'
require 'transpec/dynamic_analyzer'
require 'transpec/util'

module Transpec
  class Syntax
    class RSpecConfigure < Syntax
      require 'transpec/syntax/rspec_configure/config_modification'
      require 'transpec/syntax/rspec_configure/expectations'
      require 'transpec/syntax/rspec_configure/mocks'

      include Mixin::Send, Mixin::RSpecRails, ConfigModification

      define_dynamic_analysis do |rewriter|
        run_order = "#{DynamicAnalyzer::ANALYSIS_MODULE}.ephemeral_data[:rspec_configure_run_order]"
        code = "#{run_order} ||= 0\n" \
               "#{run_order} += 1"
        rewriter.register_request(node, :run_order, code)
      end

      def dynamic_analysis_target?
        return false unless super
        const_name(receiver_node) == 'RSpec' && method_name == :configure && parent_node.block_type?
      end

      def expose_dsl_globally=(value)
        comment = <<-END.gsub(/^\s+\|/, '').chomp
          |Setting this config option `false` removes rspec-core's monkey patching of the
          |top level methods like `describe`, `shared_examples_for` and `shared_context`
          |on `main` and `Module`. The methods are always available through the `RSpec`
          |module like `RSpec.describe` regardless of this setting.
          |For backwards compatibility this defaults to `true`.
          |
          |https://relishapp.com/rspec/rspec-core/v/3-0/docs/configuration/global-namespace-dsl
        END
        set_config_value!(:expose_dsl_globally, value, comment)
      end

      def infer_spec_type_from_file_location!
        return if infer_spec_type_from_file_location?
        return unless rspec_rails?

        # Based on the deprecation warning in RSpec 2.99:
        # https://github.com/rspec/rspec-rails/blob/ab6313b/lib/rspec/rails/infer_type_configuration.rb#L13-L22
        # and the Myron's post:
        # http://myronmars.to/n/dev-blog/2014/05/notable-changes-in-rspec-3#filetype_inference_disabled_by_default
        comment = <<-END.gsub(/^\s+\|/, '').chomp
          |rspec-rails 3 will no longer automatically infer an example group's spec type
          |from the file location. You can explicitly opt-in to the feature using this
          |config option.
          |To explicitly tag specs without using automatic inference, set the `:type`
          |metadata manually:
          |
          |    describe ThingsController, :type => :controller do
          |      # Equivalent to being in spec/controllers
          |    end
        END

        add_config!(:infer_spec_type_from_file_location!, nil, comment)
      end

      def infer_spec_type_from_file_location?
        !find_config_node(:infer_spec_type_from_file_location!).nil?
      end

      def convert_deprecated_options! # rubocop:disable MethodLength
        replace_config!(:backtrace_clean_patterns,  :backtrace_exclusion_patterns)
        replace_config!(:backtrace_clean_patterns=, :backtrace_exclusion_patterns=)
        replace_config!(:color_enabled=, :color=)

        if rspec_version.config_output_stream_available?
          replace_config!(:output,  :output_stream)
          replace_config!(:output=, :output_stream=)
          replace_config!(:out,     :output_stream)
          replace_config!(:out=,    :output_stream=)
        end

        if rspec_version.config_pattern_available?
          replace_config!(:filename_pattern,  :pattern)
          replace_config!(:filename_pattern=, :pattern=)
        end

        if rspec_version.config_backtrace_formatter_available?
          replace_config!(:backtrace_cleaner, :backtrace_formatter)
        end

        if rspec_version.config_predicate_color_enabled_available?
          replace_config!(:color?, :color_enabled?)
        end

        if rspec_version.config_predicate_warnings_available?
          replace_config!(:warnings, :warnings?)
        end
      end

      def expectations
        @expectations ||= Expectations.new(self)
      end

      def mocks
        @mocks ||= Mocks.new(self)
      end

      alias_method :block_node, :parent_node

      def block_arg_name
        first_block_arg_name(block_node)
      end
    end
  end
end
