require 'active_support/core_ext/module/attr_internal'
require 'active_support/core_ext/class/attribute_accessors'
require 'active_support/ordered_options'
require 'action_view/log_subscriber'

module ActionView #:nodoc:
  # = Action View Base
  #
  # Action View templates can be written in several ways. If the template file has a <tt>.erb</tt> extension then it uses a mixture of ERB
  # (included in Ruby) and HTML. If the template file has a <tt>.builder</tt> extension then Jim Weirich's Builder::XmlMarkup library is used.
  #
  # == ERB
  #
  # You trigger ERB by using embeddings such as <% %>, <% -%>, and <%= %>. The <%= %> tag set is used when you want output. Consider the
  # following loop for names:
  #
  #   <b>Names of all the people</b>
  #   <% @people.each do |person| %>
  #     Name: <%= person.name %><br/>
  #   <% end %>
  #
  # The loop is setup in regular embedding tags <% %> and the name is written using the output embedding tag <%= %>. Note that this
  # is not just a usage suggestion. Regular output functions like print or puts won't work with ERB templates. So this would be wrong:
  #
  #   <%# WRONG %>
  #   Hi, Mr. <% puts "Frodo" %>
  #
  # If you absolutely must write from within a function use +concat+.
  #
  # <%- and -%> suppress leading and trailing whitespace, including the trailing newline, and can be used interchangeably with <% and %>.
  #
  # === Using sub templates
  #
  # Using sub templates allows you to sidestep tedious replication and extract common display structures in shared templates. The
  # classic example is the use of a header and footer (even though the Action Pack-way would be to use Layouts):
  #
  #   <%= render "shared/header" %>
  #   Something really specific and terrific
  #   <%= render "shared/footer" %>
  #
  # As you see, we use the output embeddings for the render methods. The render call itself will just return a string holding the
  # result of the rendering. The output embedding writes it to the current template.
  #
  # But you don't have to restrict yourself to static includes. Templates can share variables amongst themselves by using instance
  # variables defined using the regular embedding tags. Like this:
  #
  #   <% @page_title = "A Wonderful Hello" %>
  #   <%= render "shared/header" %>
  #
  # Now the header can pick up on the <tt>@page_title</tt> variable and use it for outputting a title tag:
  #
  #   <title><%= @page_title %></title>
  #
  # === Passing local variables to sub templates
  #
  # You can pass local variables to sub templates by using a hash with the variable names as keys and the objects as values:
  #
  #   <%= render "shared/header", { headline: "Welcome", person: person } %>
  #
  # These can now be accessed in <tt>shared/header</tt> with:
  #
  #   Headline: <%= headline %>
  #   First name: <%= person.first_name %>
  #
  # If you need to find out whether a certain local variable has been assigned a value in a particular render call,
  # you need to use the following pattern:
  #
  #   <% if local_assigns.has_key? :headline %>
  #     Headline: <%= headline %>
  #   <% end %>
  #
  # Testing using <tt>defined? headline</tt> will not work. This is an implementation restriction.
  #
  # === Template caching
  #
  # By default, Rails will compile each template to a method in order to render it. When you alter a template,
  # Rails will check the file's modification time and recompile it in development mode.
  #
  # == Builder
  #
  # Builder templates are a more programmatic alternative to ERB. They are especially useful for generating XML content. An XmlMarkup object
  # named +xml+ is automatically made available to templates with a <tt>.builder</tt> extension.
  #
  # Here are some basic examples:
  #
  #   xml.em("emphasized")                                 # => <em>emphasized</em>
  #   xml.em { xml.b("emph & bold") }                      # => <em><b>emph &amp; bold</b></em>
  #   xml.a("A Link", "href" => "http://onestepback.org")  # => <a href="http://onestepback.org">A Link</a>
  #   xml.target("name" => "compile", "option" => "fast")  # => <target option="fast" name="compile"\>
  #                                                        # NOTE: order of attributes is not specified.
  #
  # Any method with a block will be treated as an XML markup tag with nested markup in the block. For example, the following:
  #
  #   xml.div do
  #     xml.h1(@person.name)
  #     xml.p(@person.bio)
  #   end
  #
  # would produce something like:
  #
  #   <div>
  #     <h1>David Heinemeier Hansson</h1>
  #     <p>A product of Danish Design during the Winter of '79...</p>
  #   </div>
  #
  # A full-length RSS example actually used on Basecamp:
  #
  #   xml.rss("version" => "2.0", "xmlns:dc" => "http://purl.org/dc/elements/1.1/") do
  #     xml.channel do
  #       xml.title(@feed_title)
  #       xml.link(@url)
  #       xml.description "Basecamp: Recent items"
  #       xml.language "en-us"
  #       xml.ttl "40"
  #
  #       @recent_items.each do |item|
  #         xml.item do
  #           xml.title(item_title(item))
  #           xml.description(item_description(item)) if item_description(item)
  #           xml.pubDate(item_pubDate(item))
  #           xml.guid(@person.firm.account.url + @recent_items.url(item))
  #           xml.link(@person.firm.account.url + @recent_items.url(item))
  #
  #           xml.tag!("dc:creator", item.author_name) if item_has_creator?(item)
  #         end
  #       end
  #     end
  #   end
  #
  # More builder documentation can be found at http://builder.rubyforge.org.
  class Base
    include Helpers, ::ERB::Util, Context

    # Specify the proc used to decorate input tags that refer to attributes with errors.
    cattr_accessor :field_error_proc
    @@field_error_proc = Proc.new{ |html_tag, instance| "<div class=\"field_with_errors\">#{html_tag}</div>".html_safe }

    # How to complete the streaming when an exception occurs.
    # This is our best guess: first try to close the attribute, then the tag.
    cattr_accessor :streaming_completion_on_exception
    @@streaming_completion_on_exception = %("><script>window.location = "/500.html"</script></html>)

    # Specify whether rendering within namespaced controllers should prefix
    # the partial paths for ActiveModel objects with the namespace.
    # (e.g., an Admin::PostsController would render @post using /admin/posts/_post.erb)
    cattr_accessor :prefix_partial_path_with_controller_namespace
    @@prefix_partial_path_with_controller_namespace = true

    # Specify default_formats that can be rendered.
    cattr_accessor :default_formats

    class_attribute :_routes
    class_attribute :logger

    class << self
      delegate :erb_trim_mode=, :to => 'ActionView::Template::Handlers::ERB'

      def cache_template_loading
        ActionView::Resolver.caching?
      end

      def cache_template_loading=(value)
        ActionView::Resolver.caching = value
      end

      def xss_safe? #:nodoc:
        true
      end
    end

    attr_accessor :view_renderer
    attr_internal :config, :assigns

    delegate :lookup_context, :to => :view_renderer
    delegate :formats, :formats=, :locale, :locale=, :view_paths, :view_paths=, :to => :lookup_context

    def assign(new_assigns) # :nodoc:
      @_assigns = new_assigns.each { |key, value| instance_variable_set("@#{key}", value) }
    end

    def initialize(context = nil, assigns = {}, controller = nil, formats = nil) #:nodoc:
      @_config = ActiveSupport::InheritableOptions.new

      if context.is_a?(ActionView::Renderer)
        @view_renderer = context
      else
        lookup_context = context.is_a?(ActionView::LookupContext) ?
          context : ActionView::LookupContext.new(context)
        lookup_context.formats  = formats if formats
        lookup_context.prefixes = controller._prefixes if controller
        @view_renderer = ActionView::Renderer.new(lookup_context)
      end

      assign(assigns)
      assign_controller(controller)
      _prepare_context
    end

    ActiveSupport.run_load_hooks(:action_view, self)
  end
end
