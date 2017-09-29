# coding: utf-8

require 'transpec/annotatable'

module Transpec
  class Record
    include Comparable

    TYPES = [:conversion, :addition, :removal, :modification]
    COMPARISON_ATTRIBUTES = [:type_sort_key, :old_syntax, :new_syntax].freeze

    attr_reader :old_syntax_type, :new_syntax_type, :annotation

    def initialize(old_syntax, new_syntax, options = {})
      # Keep these syntax data as symbols for:
      #   * Better memory footprint
      #   * Better summarizing performance in Report
      @old_syntax_type = old_syntax && old_syntax.to_sym
      @new_syntax_type = new_syntax && new_syntax.to_sym

      @type = options[:type]
      @annotation = options[:annotation]

      fail ArgumentError, "Invalid type: #{type}" unless TYPES.include?(type)
    end

    def type
      @type ||= if old_syntax_type && new_syntax_type
                  :conversion
                elsif new_syntax_type
                  :addition
                else
                  :removal
                end
    end

    def old_syntax
      old_syntax_type && old_syntax_type.to_s
    end

    def new_syntax
      new_syntax_type && new_syntax_type.to_s
    end

    def ==(other)
      self.class == other.class &&
        old_syntax_type == other.old_syntax_type &&
        new_syntax_type == other.new_syntax_type
    end

    alias_method :eql?, :==

    def hash
      old_syntax_type.hash ^ new_syntax_type.hash
    end

    def to_s
      string = type.to_s.capitalize

      string << case type
                when :conversion, :modification
                  " from `#{old_syntax_type}` to `#{new_syntax_type}`"
                when :addition
                  " of `#{new_syntax_type}`"
                when :removal
                  " of `#{old_syntax_type}`"
                end
    end

    def <=>(other)
      COMPARISON_ATTRIBUTES.each do |attribute|
        result = send(attribute) <=> other.send(attribute)
        return result unless result.zero?
      end
      0
    end

    private

    def type_sort_key
      case type
      when :conversion   then 1
      when :modification then 2
      when :addition     then 3
      when :removal      then 4
      end
    end
  end

  # This class is intended to be inherited to build complex record.
  # The reasons why you should inherit this class rather than Record are:
  #   * You need to care about String and Symbol around Record#old_syntax and #new_syntax.
  #   * All record instances are kept in a Report until the end of Transpec process.
  #     This mean that if a custom record keeps a syntax object as an ivar,
  #     the AST kept by the syntax object won't be GCed.
  class RecordBuilder
    def self.build(*args)
      new(*args).build
    end

    def self.param_names(*names)
      names.each_with_index do |name, index|
        define_method(name) do
          @initializer_params[index]
        end
      end
    end

    def build
      Record.new(old_syntax, new_syntax, { type: type, annotation: annotation })
    end

    private

    def initialize(*params)
      @initializer_params = params
    end

    def old_syntax
      nil
    end

    def new_syntax
      nil
    end

    def type
      nil
    end

    def annotation
      nil
    end
  end

  class Annotation
    include Annotatable
  end

  class AccuracyAnnotation < Annotation
    def initialize(source_range)
      message = "The `#{source_range.source}` has been converted " \
                'but it might possibly be incorrect ' \
                'due to a lack of runtime information. ' \
                "It's recommended to review the change carefully."
      super(message, source_range)
    end
  end
end
