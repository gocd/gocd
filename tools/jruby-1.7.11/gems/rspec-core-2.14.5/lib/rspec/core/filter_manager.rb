module RSpec
  module Core
    # Manages the filtering of examples and groups by matching tags declared on
    # the command line or options files, or filters declared via
    # `RSpec.configure`, with hash key/values submitted within example group
    # and/or example declarations. For example, given this declaration:
    #
    #     describe Thing, :awesome => true do
    #       it "does something" do
    #         # ...
    #       end
    #     end
    #
    # That group (or any other with `:awesome => true`) would be filtered in
    # with any of the following commands:
    #
    #     rspec --tag awesome:true
    #     rspec --tag awesome
    #     rspec -t awesome:true
    #     rspec -t awesome
    #
    # Prefixing the tag names with `~` negates the tags, thus excluding this group with
    # any of:
    #
    #     rspec --tag ~awesome:true
    #     rspec --tag ~awesome
    #     rspec -t ~awesome:true
    #     rspec -t ~awesome
    #
    # ## Options files and command line overrides
    #
    # Tag declarations can be stored in `.rspec`, `~/.rspec`, or a custom
    # options file.  This is useful for storing defaults. For example, let's
    # say you've got some slow specs that you want to suppress most of the
    # time. You can tag them like this:
    #
    #     describe Something, :slow => true do
    #
    # And then store this in `.rspec`:
    #
    #     --tag ~slow:true
    #
    # Now when you run `rspec`, that group will be excluded.
    #
    # ## Overriding
    #
    # Of course, you probably want to run them sometimes, so you can override
    # this tag on the command line like this:
    #
    #     rspec --tag slow:true
    #
    # ## RSpec.configure
    #
    # You can also store default tags with `RSpec.configure`. We use `tag` on
    # the command line (and in options files like `.rspec`), but for historical
    # reasons we use the term `filter` in `RSpec.configure:
    #
    #     RSpec.configure do |c|
    #       c.filter_run_including :foo => :bar
    #       c.filter_run_excluding :foo => :bar
    #     end
    #
    # These declarations can also be overridden from the command line.
    #
    # @see RSpec.configure
    # @see Configuration#filter_run_including
    # @see Configuration#filter_run_excluding
    class FilterManager
      DEFAULT_EXCLUSIONS = {
        :if     => lambda { |value| !value },
        :unless => lambda { |value| value }
      }

      STANDALONE_FILTERS = [:locations, :line_numbers, :full_description]

      module Describable
        PROC_HEX_NUMBER = /0x[0-9a-f]+@/
        PROJECT_DIR = File.expand_path('.')

        def description
          reject { |k, v| RSpec::Core::FilterManager::DEFAULT_EXCLUSIONS[k] == v }.inspect.gsub(PROC_HEX_NUMBER, '').gsub(PROJECT_DIR, '.').gsub(' (lambda)','')
        end

        def empty_without_conditional_filters?
          reject { |k, v| RSpec::Core::FilterManager::DEFAULT_EXCLUSIONS[k] == v }.empty?
        end
      end

      module BackwardCompatibility
        def merge(orig, opposite, *updates)
          _warn_deprecated_keys(updates.last)
          super
        end

        def reverse_merge(orig, opposite, *updates)
          _warn_deprecated_keys(updates.last)
          super
        end

        # Supports a use case that probably doesn't exist: overriding the
        # if/unless procs.
        def _warn_deprecated_keys(updates)
          _warn_deprecated_key(:unless, updates) if updates.has_key?(:unless)
          _warn_deprecated_key(:if, updates)     if updates.has_key?(:if)
        end

        # Emits a deprecation warning for keys that will not be supported in
        # the future.
        def _warn_deprecated_key(key, updates)
          RSpec.deprecate("FilterManager#exclude(#{key.inspect} => #{updates[key].inspect})")
          @exclusions[key] = updates.delete(key)
        end
      end

      attr_reader :exclusions, :inclusions

      def initialize
        @exclusions = DEFAULT_EXCLUSIONS.dup.extend(Describable)
        @inclusions = {}.extend(Describable)
        extend(BackwardCompatibility)
      end

      def add_location(file_path, line_numbers)
        # locations is a hash of expanded paths to arrays of line
        # numbers to match against. e.g.
        #   { "path/to/file.rb" => [37, 42] }
        locations = @inclusions.delete(:locations) || Hash.new {|h,k| h[k] = []}
        locations[File.expand_path(file_path)].push(*line_numbers)
        @inclusions.replace(:locations => locations)
        @exclusions.clear
      end

      def empty?
        inclusions.empty? && exclusions.empty_without_conditional_filters?
      end

      def prune(examples)
        examples.select {|e| !exclude?(e) && include?(e)}
      end

      def exclude(*args)
        merge(@exclusions, @inclusions, *args)
      end

      def exclude!(*args)
        replace(@exclusions, @inclusions, *args)
      end

      def exclude_with_low_priority(*args)
        reverse_merge(@exclusions, @inclusions, *args)
      end

      def exclude?(example)
        @exclusions.empty? ? false : example.any_apply?(@exclusions)
      end

      def include(*args)
        unless_standalone(*args) { merge(@inclusions, @exclusions, *args) }
      end

      def include!(*args)
        unless_standalone(*args) { replace(@inclusions, @exclusions, *args) }
      end

      def include_with_low_priority(*args)
        unless_standalone(*args) { reverse_merge(@inclusions, @exclusions, *args) }
      end

      def include?(example)
        @inclusions.empty? ? true : example.any_apply?(@inclusions)
      end

      private

      def unless_standalone(*args)
        is_standalone_filter?(args.last) ? @inclusions.replace(args.last) : yield unless already_set_standalone_filter?
      end

      def merge(orig, opposite, *updates)
        orig.merge!(updates.last).each_key {|k| opposite.delete(k)}
      end

      def replace(orig, opposite, *updates)
        updates.last.each_key {|k| opposite.delete(k)}
        orig.replace(updates.last)
      end

      def reverse_merge(orig, opposite, *updates)
        updated = updates.last.merge(orig)
        opposite.each_pair {|k,v| updated.delete(k) if updated[k] == v}
        orig.replace(updated)
      end

      def already_set_standalone_filter?
        is_standalone_filter?(inclusions)
      end

      def is_standalone_filter?(filter)
        STANDALONE_FILTERS.any? {|key| filter.has_key?(key)}
      end
    end
  end
end
