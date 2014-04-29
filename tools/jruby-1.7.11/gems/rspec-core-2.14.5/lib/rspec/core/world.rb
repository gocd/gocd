module RSpec
  module Core
    class World

      include RSpec::Core::Hooks

      attr_reader :example_groups, :filtered_examples
      attr_accessor :wants_to_quit

      def initialize(configuration=RSpec.configuration)
        @configuration = configuration
        @example_groups = [].extend(Extensions::Ordered::ExampleGroups)
        @filtered_examples = Hash.new { |hash,group|
          hash[group] = begin
            examples = group.examples.dup
            examples = filter_manager.prune(examples)
            examples.uniq
            examples.extend(Extensions::Ordered::Examples)
          end
        }
      end

      def reset
        example_groups.clear
        SharedExampleGroup.registry.clear
      end

      def filter_manager
        @configuration.filter_manager
      end

      def register(example_group)
        example_groups << example_group
        example_group
      end

      def inclusion_filter
        @configuration.inclusion_filter
      end

      def exclusion_filter
        @configuration.exclusion_filter
      end

      def configure_group(group)
        @configuration.configure_group(group)
      end

      def example_count
        example_groups.collect {|g| g.descendants}.flatten.inject(0) do |sum, g|
          sum += g.filtered_examples.size
        end
      end

      def preceding_declaration_line(filter_line)
        declaration_line_numbers.sort.inject(nil) do |highest_prior_declaration_line, line|
          line <= filter_line ? line : highest_prior_declaration_line
        end
      end

      def reporter
        @configuration.reporter
      end

      def announce_filters
        filter_announcements = []

        announce_inclusion_filter filter_announcements
        announce_exclusion_filter filter_announcements

        unless filter_manager.empty?
          if filter_announcements.length == 1
            reporter.message("Run options: #{filter_announcements[0]}")
          else
            reporter.message("Run options:\n  #{filter_announcements.join("\n  ")}")
          end
        end

        if @configuration.run_all_when_everything_filtered? && example_count.zero?
          reporter.message("#{everything_filtered_message}; ignoring #{inclusion_filter.description}")
          filtered_examples.clear
          inclusion_filter.clear
        end

        if example_count.zero?
          example_groups.clear
          if filter_manager.empty?
            reporter.message("No examples found.")
          elsif exclusion_filter.empty_without_conditional_filters?
            message = everything_filtered_message
            if @configuration.run_all_when_everything_filtered?
              message << "; ignoring #{inclusion_filter.description}"
            end
            reporter.message(message)
          elsif inclusion_filter.empty?
            reporter.message(everything_filtered_message)
          end
        end
      end

      def everything_filtered_message
        "\nAll examples were filtered out"
      end

      def announce_inclusion_filter(announcements)
        unless inclusion_filter.empty?
          announcements << "include #{inclusion_filter.description}"
        end
      end

      def announce_exclusion_filter(announcements)
        unless exclusion_filter.empty_without_conditional_filters?
          announcements << "exclude #{exclusion_filter.description}"
        end
      end

    private

      def declaration_line_numbers
        @line_numbers ||= example_groups.inject([]) do |lines, g|
          lines + g.declaration_line_numbers
        end
      end

    end
  end
end
