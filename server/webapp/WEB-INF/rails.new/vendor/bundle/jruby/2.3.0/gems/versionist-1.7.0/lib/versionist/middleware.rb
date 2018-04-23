module Versionist
  # When your routes don't include an explicit format in the URL (i.e. `match 'foos.(:format)' => foos#index`),
  # Rails inspects the `Accept` header to determine the requested format. Since an `Accept` header can have multiple values,
  # Rails uses the first one present to determine the format. If your custom version header happens to be the first value
  # in the `Accept` header, it would incorrectly be interpretted as the format. This middleware moves your custom version header
  # (if found) to the end of the `Accept` header so as to not interfere with this format logic in Rails.
  class Middleware

    ACCEPT = "Accept"
    HTTP_ACCEPT = "HTTP_ACCEPT"
    
    def initialize(app)
      @app = app
    end

    def call(env)
      dup._call(env)
    end

    def _call(env)
      request = ::Rack::Request.new(env)
      potential_matches = Versionist.configuration.header_versions.select {|hv| hv.config[:header][:name] == ACCEPT && env[HTTP_ACCEPT].try(:include?, hv.config[:header][:value])}
      if !potential_matches.empty?
        strategy = potential_matches.max {|a,b| a.config[:header][:value].length <=> b.config[:header][:value].length}
      end
      if !strategy.nil?
        entries = env[HTTP_ACCEPT].split(',')
        index = -1
        entries.each_with_index do |e, i|
          e.strip!
          index = i if e == strategy.config[:header][:value]
        end
        if (index != -1)
          version = entries.delete_at(index)
          entries << version
        end
        env[HTTP_ACCEPT] = entries.join(", ")
      end
      @app.call(env)
    end
  end
end
