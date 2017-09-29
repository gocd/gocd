# coding: utf-8

require 'transpec/annotatable'

module Transpec
  class ConversionError < StandardError
    include Annotatable
  end

  class ContextError < ConversionError
    def initialize(old_syntax, new_syntax, source_range)
      message = build_message(old_syntax, new_syntax)
      super(message, source_range)
    end

    private

    def build_message(old_syntax, new_syntax)
      "Cannot convert #{old_syntax} into #{new_syntax} " \
      "since #{new_syntax} is not available in the context."
    end
  end
end
