require 'tempfile'
require 'minitest/test'

require 'simplecov'

if ENV.include?('COVERAGE') && SimpleCov.usable?
  if defined?(TracePoint)
    require_relative 'racc_coverage_helper'

    RaccCoverage.start(%w(ruby18.y ruby19.y ruby20.y ruby21.y ruby22.y ruby23.y ruby24.y),
                       File.expand_path('../../lib/parser', __FILE__))

    # Report results faster.
    at_exit { RaccCoverage.stop }
  end

  SimpleCov.start do
    self.formatter = SimpleCov::Formatter::MultiFormatter[
      SimpleCov::Formatter::HTMLFormatter,
    ]

    add_group 'Grammars' do |source_file|
      source_file.filename =~ %r{\.y$}
    end

    # Exclude the testsuite itself.
    add_filter '/test/'

    # Exclude generated files.
    add_filter do |source_file|
      source_file.filename =~ %r{/lib/parser/(lexer|ruby\d+|macruby|rubymotion)\.rb$}
    end
  end
end

# minitest/autorun must go after SimpleCov to preserve
# correct order of at_exit hooks.
require 'minitest/autorun'

$LOAD_PATH.unshift(File.expand_path('../../lib', __FILE__))
require 'parser'
