# coding: utf-8

module Transpec
  class Config
    NEGATIVE_FORMS_OF_TO = ['not_to', 'to_not'].freeze
    FORMS_OF_BE_FALSEY = ['be_falsey', 'be_falsy'].freeze
    BOOLEAN_MATCHER_TYPES = [:conditional, :exact].freeze

    PREDICATES = [
      [:forced,                                                false],
      [:skip_dynamic_analysis,                                 false],
      [:add_explicit_type_metadata_to_example_group,           false],
      [:add_receiver_arg_to_any_instance_implementation_block, true],
      [:parenthesize_matcher_arg,                              true]
    ].freeze

    DEFAULT_CONVERSIONS = {
              should: true,
            oneliner: true,
      should_receive: true,
                stub: true,
          have_items: true,
                 its: true,
             pending: true,
          deprecated: true,
       example_group: false,
          hook_scope: false,
      stub_with_hash: false # to allow(obj).to receive(:message).and_return(value) prior to RSpec 3
    }.freeze

    PREDICATES.each do |predicate, _|
      attr_accessor predicate
      alias_method predicate.to_s + '?', predicate
    end

    attr_reader :conversion
    attr_accessor :negative_form_of_to, :boolean_matcher_type, :form_of_be_falsey, :rspec_command

    def self.valid_conversion_type?(type)
      conversion_types.include?(type.to_sym)
    end

    def self.conversion_types
      DEFAULT_CONVERSIONS.keys
    end

    def initialize
      PREDICATES.each do |predicate, default_value|
        instance_variable_set('@' + predicate.to_s, default_value)
      end

      @conversion = SymbolKeyHash.new
      @conversion.update(DEFAULT_CONVERSIONS)

      self.negative_form_of_to = 'not_to'
      self.boolean_matcher_type = :conditional
      self.form_of_be_falsey = 'be_falsey'
    end

    def convert?(type)
      @conversion[type]
    end

    def negative_form_of_to=(form)
      validate!(form.to_s, NEGATIVE_FORMS_OF_TO, 'Negative form of "to"')
      @negative_form_of_to = form.to_s.freeze
    end

    def boolean_matcher_type=(type)
      validate!(type.to_sym, BOOLEAN_MATCHER_TYPES, 'Boolean matcher type')
      @boolean_matcher_type = type.to_sym
    end

    def form_of_be_falsey=(form)
      validate!(form.to_s, FORMS_OF_BE_FALSEY, 'Form of "be_falsey"')
      @form_of_be_falsey = form.to_s.freeze
    end

    private

    def validate!(arg, valid_values, subject)
      return if valid_values.include?(arg)
      message = "#{subject} must be either "
      message << valid_values.map(&:inspect).join(' or ')
      fail ArgumentError, message
    end

    class SymbolKeyHash < Hash
      def [](key)
        super(key.to_sym)
      end

      def []=(key, value)
        super(key.to_sym, value)
      end
    end
  end
end
