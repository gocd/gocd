module Rack
  module Jasmine

    class Runner
      def initialize(page)
        @page = page
      end

      def call(env)
        @path = env["PATH_INFO"]
        return not_found if @path != "/"
        [
          200,
          { 'Content-Type' => 'text/html'},
          [@page.render]
        ]
      end

      def not_found
        [404, {"Content-Type" => "text/plain",
               "X-Cascade" => "pass"},
               ["File not found: #{@path}\n"]]
      end
    end

  end
end
