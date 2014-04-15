module Jasmine
  module Runners
    class HTTP
      attr_accessor :suites

      def initialize(client, results_processor, result_batch_size)
        @client = client
        @results_processor = results_processor
        @result_batch_size = result_batch_size
      end

      def run
        @client.connect
        load_suite_info
        wait_for_suites_to_finish_running
        results = @results_processor.process(results_hash, suites)
        @client.disconnect
        results
      end

      private

      def load_suite_info
        started = Time.now
        while !eval_js('return jsApiReporter && jsApiReporter.started') do
          raise "couldn't connect to Jasmine after 60 seconds" if (started + 60 < Time.now)
          sleep 0.1
        end

        @suites = eval_js("var result = jsApiReporter.suites(); if (window.Prototype && Object.toJSON) { return Object.toJSON(result) } else { return JSON.stringify(result) }")
      end

      def results_hash
        spec_results = {}
        spec_ids.each_slice(@result_batch_size) do |slice|
          spec_results.merge!(eval_js("var result = jsApiReporter.resultsForSpecs(#{json_generate(slice)}); if (window.Prototype && Object.toJSON) { return Object.toJSON(result) } else { return JSON.stringify(result) }"))
        end
        spec_results
      end

      def spec_ids
        map_spec_ids = lambda do |suites|
          suites.map do |suite_or_spec|
            if suite_or_spec['type'] == 'spec'
              suite_or_spec['id']
            else
              map_spec_ids.call(suite_or_spec['children'])
            end
          end
        end
        map_spec_ids.call(@suites).compact.flatten
      end

      def wait_for_suites_to_finish_running
        puts "Waiting for suite to finish in browser ..."
        while !eval_js('return jsApiReporter.finished') do
          sleep 0.1
        end
      end

      def eval_js(script)
        @client.eval_js(script)
      end

      def json_generate(obj)
        @client.json_generate(obj)
      end

    end
  end
end
