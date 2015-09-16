require 'rhino'

begin
  require 'mocha/api'
rescue LoadError
  require 'mocha'
end

require 'redjs'

module RedJS
  Context = Rhino::Context
  Error = Rhino::JSError
end

module Rhino
  module SpecHelpers
    
    def context_factory
      @context_factory ||= Rhino::JS::ContextFactory.new
    end
    
    def context
      @context || context_factory.call { |ctx| @context = ctx }
      @context
    end

    def scope
      context.initStandardObjects(nil, false)
    end
    
  end
end

RSpec.configure do |config|
  config.filter_run_excluding :compat => /(0.5.0)|(0.6.0)/ # RedJS
  config.include Rhino::SpecHelpers
  config.deprecation_stream = 'spec/deprecations.log'
end
