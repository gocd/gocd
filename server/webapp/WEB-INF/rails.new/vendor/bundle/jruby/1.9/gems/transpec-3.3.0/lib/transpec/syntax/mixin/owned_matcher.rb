# coding: utf-8

require 'active_support/concern'

module Transpec
  class Syntax
    module Mixin
      module OwnedMatcher
        extend ActiveSupport::Concern

        included do
          attr_reader :expectation
        end

        module ClassMethods
          def standalone?
            false
          end
        end

        # rubocop:disable LineLength
        def initialize(node, expectation, runtime_data = nil, project = nil, source_rewriter = nil, report = nil)
          super(node, runtime_data, project, source_rewriter, report)
          @expectation = expectation
        end
        # rubocop:enable LineLength
      end
    end
  end
end
