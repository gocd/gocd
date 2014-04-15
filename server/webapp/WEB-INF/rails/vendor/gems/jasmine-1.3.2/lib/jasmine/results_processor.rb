module Jasmine
  class ResultsProcessor

    def initialize(config)
      @config = config
    end

    def process(results_hash, suites_hash)
      return Jasmine::Results.new(results_hash, suites_hash, example_locations)
    end

    def example_locations
      # example_locations = {}
      # example_name_parts = []
      # previous_indent_level = 0
      # @config.spec_files_full_paths.each do |filename|
        # line_number = 1
        # File.open(filename, "r") do |file|
          # file.readlines.each do |line|
            # match = /^(\s*)(describe|it)\s*\(\s*["'](.*)["']\s*,\s*function/.match(line)
             # if (match)
               # indent_level = match[1].length / 2
               # example_name = match[3]
               # example_name_parts[indent_level] = example_name

               # full_example_name = example_name_parts.slice(0, indent_level + 1).join(" ")
               # example_locations[full_example_name] = "#{filename}:#{line_number}: in `it'"
             # end
             # line_number += 1
          # end
        # end
      # end
      # example_locations
      {}
    end

  end
end
