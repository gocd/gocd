require 'spec_helper'
require 'rspec/core/caller_filter'

module RSpec
  describe CallerFilter do
    def ruby_files_in_lib(lib)
      # http://rubular.com/r/HYpUMftlG2
      path = $LOAD_PATH.find { |p| p.match(/\/rspec-#{lib}(-[a-f0-9]+)?\/lib/) }

      unless path
        pending "Cannot locate rspec-#{lib} files."
      end

      Dir["#{path}/**/*.rb"].sort.tap do |files|
        # Just a sanity check...
        expect(files.count).to be > 10
      end
    end

    describe "the filtering regex" do
      def unmatched_from(files)
        files.reject { |file| file.match(CallerFilter::LIB_REGEX) }
      end

      %w[ core mocks expectations ].each do |lib|
        it "matches all ruby files in rspec-#{lib}" do
          files = ruby_files_in_lib(lib)

          files.reject! do |file|
            # We don't care about this file -- it only has a single require statement
            # and won't show up in any backtraces.
            file.end_with?('lib/rspec-expectations.rb') ||

            # This has a single require and a single deprecation, and won't be
            # in backtraces.
            file.end_with?('lib/spec/mocks.rb') ||

            # Autotest files are only loaded by the autotest executable
            # and not by the rspec command and thus won't be in backtraces.
            file.include?('autotest')
          end

          expect(unmatched_from files).to eq([])
        end
      end

      it "does not match other ruby files" do
        files = %w[
          /path/to/lib/rspec/some-extension/foo.rb
          /path/to/spec/rspec/core/some_spec.rb
        ]

        expect(unmatched_from files).to eq(files)
      end
    end
  end
end

