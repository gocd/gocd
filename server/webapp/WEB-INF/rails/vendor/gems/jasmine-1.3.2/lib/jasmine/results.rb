module Jasmine
  class Results

    attr_reader :suites
    def initialize(result_hash, suite_hash, example_locations)
      @suites = suite_hash
      @results = result_hash
      @example_locations = example_locations
    end

    def for_spec_id(id)
      @results[id]
    end

    def example_location_for(spec_description)
      @example_locations[spec_description]
    end
  end
end
