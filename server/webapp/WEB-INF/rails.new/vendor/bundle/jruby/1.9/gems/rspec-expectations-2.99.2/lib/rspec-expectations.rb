require "rspec/expectations"

RSpec.deprecate("`require 'rspec-expectations'`",
                :replacement => "`require 'rspec/expectations'`",
                # we explcitly pass a caller because CallerFilter ignores this file
                :call_site => caller[RUBY_PLATFORM == 'java' ? 2 : 0])
