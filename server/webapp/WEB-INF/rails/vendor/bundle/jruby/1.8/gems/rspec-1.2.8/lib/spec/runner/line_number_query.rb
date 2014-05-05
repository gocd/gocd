module Spec
  module Runner
    # Parses a spec file and finds the nearest example for a given line number.
    class LineNumberQuery
      attr_reader :best_match

      def initialize(run_options)
        @best_match = {}
        @run_options = run_options
      end

      def spec_name_for(file, line_number)
        best_match.clear
        file = File.expand_path(file)
        determine_best_match(file, line_number)
        if best_match[:example_group]
          if best_match[:example]
            "#{best_match[:example_group].description} #{best_match[:example].description}"
          else
            best_match[:example_group].description
          end
        else
          nil
        end
      end

      def example_line_for(file, line_number)
        determine_best_match(file, line_number)
        best_match[:line]
      end
    
    protected
    
      def determine_best_match(file, line_number)
        best_match.clear
        file = File.expand_path(file)
        @run_options.example_groups.each do |example_group|
          next unless example_group.location
          consider_example_group_for_best_match(example_group, file, line_number)

          example_group.examples.each do |example|
            consider_example_for_best_match(example, example_group, file, line_number)
          end
        end
      end

      def consider_example_group_for_best_match(example_group, file, line_number)
        example_group_file, example_group_line = parse_location(example_group.location)
        if is_best_match?(file, line_number, example_group_file, example_group_line)
          best_match.clear
          best_match[:example_group] = example_group
          best_match[:line] = example_group_line
        end
      end

      def consider_example_for_best_match(example, example_group, file, line_number)
        example_file, example_line = parse_location(example.location)
        if is_best_match?(file, line_number, example_file, example_line)
          best_match.clear
          best_match[:example_group] = example_group
          best_match[:example] = example
          best_match[:line] = example_line
        end
      end

      def is_best_match?(file, line_number, example_file, example_line)
        file == File.expand_path(example_file) &&
                example_line <= line_number &&
                example_line > best_match[:line].to_i
      end
      
      def parse_location(location)
        location =~ /(.*)\:(\d*)(\:|$)/
        return $1, Integer($2)
      end
    end
  end
end
