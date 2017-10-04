module RSpec
  module Core
    # Each ExampleGroup class and Example instance owns an instance of
    # Metadata, which is Hash extended to support lazy evaluation of values
    # associated with keys that may or may not be used by any example or group.
    #
    # In addition to metadata that is used internally, this also stores
    # user-supplied metadata, e.g.
    #
    #     describe Something, :type => :ui do
    #       it "does something", :slow => true do
    #         # ...
    #       end
    #     end
    #
    # `:type => :ui` is stored in the Metadata owned by the example group, and
    # `:slow => true` is stored in the Metadata owned by the example. These can
    # then be used to select which examples are run using the `--tag` option on
    # the command line, or several methods on `Configuration` used to filter a
    # run (e.g. `filter_run_including`, `filter_run_excluding`, etc).
    #
    # @see Example#metadata
    # @see ExampleGroup.metadata
    # @see FilterManager
    # @see Configuration#filter_run_including
    # @see Configuration#filter_run_excluding
    class Metadata < Hash
      class << self
        attr_accessor :line_number_filter_deprecation_issued
      end

      def self.relative_path(line)
        line = line.sub(File.expand_path("."), ".")
        line = line.sub(/\A([^:]+:\d+)$/, '\\1')
        return nil if line == '-e:1'
        line
      rescue SecurityError
        nil
      end

      # @private
      module MetadataHash

        # @private
        # Supports lazy evaluation of some values. Extended by
        # ExampleMetadataHash and GroupMetadataHash, which get mixed in to
        # Metadata for ExampleGroups and Examples (respectively).
        def [](key)
          store_computed(key) unless has_key?(key)
          super
        end

        def fetch(key, *args)
          store_computed(key) unless has_key?(key)
          super
        end

        private

        def store_computed(key)
          case key
          when :location
            store(:location, location)
          when :file_path, :line_number
            file_path, line_number = file_and_line_number
            store(:file_path, file_path)
            store(:line_number, line_number)
          when :execution_result
            store(:execution_result, {})
          when :describes, :described_class
            klass = described_class
            store(:described_class, klass)
            # TODO (2011-11-07 DC) deprecate :describes as a key
            store(:describes, klass)
          when :full_description
            store(:full_description, full_description)
          when :description
            store(:description, build_description_from(*self[:description_args]))
          when :description_args
            store(:description_args, [])
          end
        end

        def location
          "#{self[:file_path]}:#{self[:line_number]}"
        end

        def file_and_line_number
          first_caller_from_outside_rspec =~ /(.+?):(\d+)(|:\d+)/
          return [Metadata::relative_path($1), $2.to_i]
        end

        def first_caller_from_outside_rspec
          self[:caller].detect {|l| l !~ /\/lib\/rspec\/core/}
        end

        def method_description_after_module?(parent_part, child_part)
          return false unless parent_part.is_a?(Module)
          child_part =~ /^(#|::|\.)/
        end

        def build_description_from(first_part = '', *parts)
          description, _ = parts.inject([first_part.to_s, first_part]) do |(desc, last_part), this_part|
            this_part = this_part.to_s
            this_part = (' ' + this_part) unless method_description_after_module?(last_part, this_part)
            [(desc + this_part), this_part]
          end

          description
        end
      end

      # Mixed in to Metadata for an Example (extends MetadataHash) to support
      # lazy evaluation of some values.
      module ExampleMetadataHash
        include MetadataHash

        def described_class
          self[:example_group].described_class
        end

        def full_description
          build_description_from(self[:example_group][:full_description], *self[:description_args])
        end
      end

      # Mixed in to Metadata for an ExampleGroup (extends MetadataHash) to
      # support lazy evaluation of some values.
      module GroupMetadataHash
        include MetadataHash

        def described_class
          warn_about_first_description_arg_behavioral_change_in_rspec_3

          value_for_rspec_2 = described_class_for_rspec_2
          value_for_rspec_3 = described_class_for_rspec_3

          if value_for_rspec_2 != value_for_rspec_3
            RSpec.warn_deprecation(<<-EOS.gsub(/^\s+\|/, ''))
              |The semantics of `described_class` in a nested `describe <SomeClass>`
              |example group are changing in RSpec 3. In RSpec 2.x, `described_class`
              |would return the outermost described class (#{value_for_rspec_2.inspect}).
              |In RSpec 3, it will return the innermost described class (#{value_for_rspec_3.inspect}).
              |In general, we recommend not describing multiple classes or objects in a
              |nested manner as it creates confusion.
              |
              |To make your code compatible with RSpec 3, change from `described_class` to a reference
              |to `#{value_for_rspec_3.inspect}`, or change the arg of the inner `describe` to a string.
              |(Called from #{CallerFilter.first_non_rspec_line})
            EOS
          end

          value_for_rspec_2
        end

        def described_class_for_rspec_2
          container_stack.each do |g|
            [:described_class, :describes].each do |key|
              if g.has_key?(key)
                value = g[key]
                return value unless value.nil?
              end
            end
          end

          container_stack.reverse.each do |g|
            candidate = g[:description_args].first
            return candidate unless String === candidate || Symbol === candidate
          end

          nil
        end

        def described_class_for_rspec_3
          container_stack.each do |g|
            [:described_class, :describes].each do |key|
              if g.has_key?(key)
                value = g[key]
                return value unless value.nil?
              end
            end

            candidate = g[:description_args].first
            return candidate unless NilClass === candidate || String === candidate || Symbol === candidate
          end

          nil
        end

        def warn_about_first_description_arg_behavioral_change_in_rspec_3
          return unless behavior_change = self[:description_arg_behavior_changing_in_rspec_3]
          RSpec.warn_deprecation(behavior_change.warning)
        end

        def first_description_arg
          self[:description_args].first
        end

        def full_description
          build_description_from(*container_stack.reverse.map {|a| a[:description_args]}.flatten)
        end

        def container_stack
          @container_stack ||= begin
                                 groups = [group = self]
                                 while group.has_key?(:example_group)
                                   groups << group[:example_group]
                                   group = group[:example_group]
                                 end
                                 groups
                               end
        end
      end

      def initialize(parent_group_metadata=nil)
        if parent_group_metadata
          update(parent_group_metadata)
          store(:example_group, {:example_group => parent_group_metadata[:example_group].extend(GroupMetadataHash)}.extend(GroupMetadataHash))
        else
          store(:example_group, {}.extend(GroupMetadataHash))
        end

        yield self if block_given?
      end

      # @private
      def process(*args)
        user_metadata = args.last.is_a?(Hash) ? args.pop : {}
        ensure_valid_keys(user_metadata)

        self[:example_group].store(:description_args, args)
        self[:example_group].store(:caller, user_metadata.delete(:caller) || caller)
        self[:example_group][:description_arg_behavior_changing_in_rspec_3] = user_metadata.delete(:description_arg_behavior_changing_in_rspec_3)

        update(user_metadata)
      end

      # @private
      def for_example(description, user_metadata)
        dup.extend(ExampleMetadataHash).configure_for_example(description, user_metadata)
      end

      # @private
      def any_apply?(filters)
        filters.any? {|k,v| filter_applies?(k,v)}
      end

      # @private
      def all_apply?(filters)
        filters.all? {|k,v| filter_applies?(k,v)}
      end

      # @private
      def filter_applies?(key, value, metadata=self)
        return metadata.filter_applies_to_any_value?(key, value) if Array === metadata[key] && !(Proc === value)

        if key == :line_numbers
          unless Metadata.line_number_filter_deprecation_issued
            RSpec.deprecate("Filtering by `:line_numbers`",
                            :replacement => "filtering by `:locations`")
          end

          return metadata.line_number_filter_applies?(value)
        end

        return metadata.location_filter_applies?(value)          if key == :locations
        return metadata.filters_apply?(key, value)               if Hash === value

        return false unless metadata.has_key?(key)

        case value
        when Regexp
          metadata[key] =~ value
        when Proc
          case value.arity
          when 0 then value.call
          when 2 then value.call(metadata[key], metadata)
          else value.call(metadata[key])
          end
        else
          metadata[key].to_s == value.to_s
        end
      end

      # @private
      def filters_apply?(key, value)
        value.all? {|k, v| filter_applies?(k, v, self[key])}
      end

      # @private
      def filter_applies_to_any_value?(key, value)
        self[key].any? {|v| filter_applies?(key, v, {key => value})}
      end

      # @private
      def location_filter_applies?(locations)
        # it ignores location filters for other files
        line_number = example_group_declaration_line(locations)
        line_number ? line_number_filter_applies?(line_number) : true
      end

      # @private
      def line_number_filter_applies?(line_numbers)
        preceding_declaration_lines = line_numbers.map {|n| RSpec.world.preceding_declaration_line(n)}
        !(relevant_line_numbers & preceding_declaration_lines).empty?
      end

      protected

      def configure_for_example(description, user_metadata)
        store(:description_args, [description]) if description
        store(:caller, user_metadata.delete(:caller) || caller)
        update(user_metadata)
      end

      private

      RESERVED_KEYS = [
        :description,
        :example_group,
        :execution_result,
        :file_path,
        :full_description,
        :line_number,
        :location
      ]

      def ensure_valid_keys(user_metadata)
        RESERVED_KEYS.each do |key|
          if user_metadata.has_key?(key)
            raise <<-EOM
            #{"*"*50}
:#{key} is not allowed

RSpec reserves some hash keys for its own internal use,
including :#{key}, which is used on:

            #{CallerFilter.first_non_rspec_line}.

Here are all of RSpec's reserved hash keys:

            #{RESERVED_KEYS.join("\n  ")}
            #{"*"*50}
            EOM
          end
        end
      end

      def example_group_declaration_line(locations)
        locations[File.expand_path(self[:example_group][:file_path])] if self[:example_group]
      end

      # TODO - make this a method on metadata - the problem is
      # metadata[:example_group] is not always a kind of GroupMetadataHash.
      def relevant_line_numbers(metadata=self)
        [metadata[:line_number]] + (metadata[:example_group] ? relevant_line_numbers(metadata[:example_group]) : [])
      end

    end
  end
end
