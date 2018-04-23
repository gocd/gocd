require "delegate"
require "active_support/core_ext/string/strip"

module ActionDispatch
  module Routing
    class RouteWrapper < SimpleDelegator
      def endpoint
        app.dispatcher? ? "#{controller}##{action}" : rack_app.inspect
      end

      def constraints
        requirements.except(:controller, :action)
      end

      def rack_app
        app.app
      end

      def path
        super.spec.to_s
      end

      def name
        super.to_s
      end

      def reqs
        @reqs ||= begin
          reqs = endpoint
          reqs += " #{constraints}" unless constraints.empty?
          reqs
        end
      end

      def controller
        parts.include?(:controller) ? ":controller" : requirements[:controller]
      end

      def action
        parts.include?(:action) ? ":action" : requirements[:action]
      end

      def internal?
        internal
      end

      def engine?
        rack_app.respond_to?(:routes)
      end
    end

    ##
    # This class is just used for displaying route information when someone
    # executes `rails routes` or looks at the RoutingError page.
    # People should not use this class.
    class RoutesInspector # :nodoc:
      def initialize(routes)
        @engines = {}
        @routes = routes
      end

      def format(formatter, filter = nil)
        routes_to_display = filter_routes(normalize_filter(filter))
        routes = collect_routes(routes_to_display)
        if routes.none?
          formatter.no_routes(collect_routes(@routes))
          return formatter.result
        end

        formatter.header routes
        formatter.section routes

        @engines.each do |name, engine_routes|
          formatter.section_title "Routes for #{name}"
          formatter.section engine_routes
        end

        formatter.result
      end

      private

        def normalize_filter(filter)
          if filter.is_a?(Hash) && filter[:controller]
            { controller: /#{filter[:controller].downcase.sub(/_?controller\z/, '').sub('::', '/')}/ }
          elsif filter
            { controller: /#{filter}/, action: /#{filter}/, verb: /#{filter}/, name: /#{filter}/, path: /#{filter}/ }
          end
        end

        def filter_routes(filter)
          if filter
            @routes.select do |route|
              route_wrapper = RouteWrapper.new(route)
              filter.any? { |default, value| route_wrapper.send(default) =~ value }
            end
          else
            @routes
          end
        end

        def collect_routes(routes)
          routes.collect do |route|
            RouteWrapper.new(route)
          end.reject(&:internal?).collect do |route|
            collect_engine_routes(route)

            { name: route.name,
              verb: route.verb,
              path: route.path,
              reqs: route.reqs }
          end
        end

        def collect_engine_routes(route)
          name = route.endpoint
          return unless route.engine?
          return if @engines[name]

          routes = route.rack_app.routes
          if routes.is_a?(ActionDispatch::Routing::RouteSet)
            @engines[name] = collect_routes(routes.routes)
          end
        end
    end

    class ConsoleFormatter
      def initialize
        @buffer = []
      end

      def result
        @buffer.join("\n")
      end

      def section_title(title)
        @buffer << "\n#{title}:"
      end

      def section(routes)
        @buffer << draw_section(routes)
      end

      def header(routes)
        @buffer << draw_header(routes)
      end

      def no_routes(routes)
        @buffer <<
        if routes.none?
          <<-MESSAGE.strip_heredoc
          You don't have any routes defined!

          Please add some routes in config/routes.rb.
          MESSAGE
        else
          "No routes were found for this controller"
        end
        @buffer << "For more information about routes, see the Rails guide: http://guides.rubyonrails.org/routing.html."
      end

      private
        def draw_section(routes)
          header_lengths = ["Prefix", "Verb", "URI Pattern"].map(&:length)
          name_width, verb_width, path_width = widths(routes).zip(header_lengths).map(&:max)

          routes.map do |r|
            "#{r[:name].rjust(name_width)} #{r[:verb].ljust(verb_width)} #{r[:path].ljust(path_width)} #{r[:reqs]}"
          end
        end

        def draw_header(routes)
          name_width, verb_width, path_width = widths(routes)

          "#{"Prefix".rjust(name_width)} #{"Verb".ljust(verb_width)} #{"URI Pattern".ljust(path_width)} Controller#Action"
        end

        def widths(routes)
          [routes.map { |r| r[:name].length }.max || 0,
           routes.map { |r| r[:verb].length }.max || 0,
           routes.map { |r| r[:path].length }.max || 0]
        end
    end

    class HtmlTableFormatter
      def initialize(view)
        @view = view
        @buffer = []
      end

      def section_title(title)
        @buffer << %(<tr><th colspan="4">#{title}</th></tr>)
      end

      def section(routes)
        @buffer << @view.render(partial: "routes/route", collection: routes)
      end

      # the header is part of the HTML page, so we don't construct it here.
      def header(routes)
      end

      def no_routes(*)
        @buffer << <<-MESSAGE.strip_heredoc
          <p>You don't have any routes defined!</p>
          <ul>
            <li>Please add some routes in <tt>config/routes.rb</tt>.</li>
            <li>
              For more information about routes, please see the Rails guide
              <a href="http://guides.rubyonrails.org/routing.html">Rails Routing from the Outside In</a>.
            </li>
          </ul>
          MESSAGE
      end

      def result
        @view.raw @view.render(layout: "routes/table") {
          @view.raw @buffer.join("\n")
        }
      end
    end
  end
end
