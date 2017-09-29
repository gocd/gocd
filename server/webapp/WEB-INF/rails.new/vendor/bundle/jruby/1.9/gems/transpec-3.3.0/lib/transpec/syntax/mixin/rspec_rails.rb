# coding: utf-8

require 'active_support/concern'

module Transpec
  class Syntax
    module Mixin
      module RSpecRails
        extend ActiveSupport::Concern

        included do
          define_dynamic_analysis do |rewriter|
            rewriter.register_request(node, :rspec_rails?, 'defined?(RSpec::Rails)', :context)
          end
        end

        def rspec_rails?
          if runtime_data.present?(node, :rspec_rails?)
            runtime_data[node, :rspec_rails?]
          else
            project.depend_on_rspec_rails?
          end
        end
      end
    end
  end
end
