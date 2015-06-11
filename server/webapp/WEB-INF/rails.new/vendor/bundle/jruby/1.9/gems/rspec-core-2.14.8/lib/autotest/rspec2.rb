require 'autotest'
require 'rspec/core/deprecation'

# Derived from the `Autotest` class, extends the `autotest` command to work
# with RSpec.
#
# @note this will be extracted to a separate gem when we release rspec-3.
class Autotest::Rspec2 < Autotest

  RSPEC_EXECUTABLE = File.expand_path('../../../exe/rspec', __FILE__)

  def initialize
    super()
    clear_mappings
    setup_rspec_project_mappings

    # Example for Ruby 1.8: http://rubular.com/r/AOXNVDrZpx
    # Example for Ruby 1.9: http://rubular.com/r/85ag5AZ2jP
    self.failed_results_re = /^\s*\d+\).*\n\s+(?:\e\[\d*m)?Failure.*(\n(?:\e\[\d*m)?\s+#\s(.*)?:\d+(?::.*)?(?:\e\[\d*m)?)+$/m
    self.completed_re = /\n(?:\e\[\d*m)?\d* examples?/m
  end

  # Adds conventional spec-to-file mappings to Autotest configuation.
  def setup_rspec_project_mappings
    add_mapping(%r%^spec/.*_spec\.rb$%) { |filename, _|
      filename
    }
    add_mapping(%r%^lib/(.*)\.rb$%) { |_, m|
      ["spec/#{m[1]}_spec.rb"]
    }
    add_mapping(%r%^spec/(spec_helper|shared/.*)\.rb$%) {
      files_matching %r%^spec/.*_spec\.rb$%
    }
  end

  # Overrides Autotest's implementation to read rspec output
  def consolidate_failures(failed)
    filters = new_hash_of_arrays
    failed.each do |spec, trace|
      if trace =~ /(.*spec\.rb)/
        filters[$1] << spec
      end
    end
    return filters
  end

  # Overrides Autotest's implementation to generate the rspec command to run
  def make_test_cmd(files_to_test)
    files_to_test.empty? ? '' :
      %|#{prefix}"#{ruby}"#{suffix} -S "#{RSPEC_EXECUTABLE}" --tty #{normalize(files_to_test).keys.flatten.map { |f| %|"#{f}"|}.join(' ')}|
  end

  # Generates a map of filenames to Arrays for Autotest
  def normalize(files_to_test)
    files_to_test.keys.inject({}) do |result, filename|
      result.merge!(File.expand_path(filename) => [])
    end
  end

  private

  def suffix
    using_bundler? ? "" : defined?(:Gem) ? " -rrubygems" : ""
  end

  def using_bundler?
    prefix =~ /bundle exec/
  end

  def gemfile?
    File.exist?('./Gemfile')
  end
end
