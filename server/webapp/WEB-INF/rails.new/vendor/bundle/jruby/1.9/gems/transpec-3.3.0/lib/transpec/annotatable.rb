# coding: utf-8

module Transpec
  module Annotatable
    attr_reader :message, :source_range

    def initialize(message, source_range)
      @message = message
      @source_range = source_range
    end

    def source_buffer
      source_range.source_buffer
    end
  end
end
