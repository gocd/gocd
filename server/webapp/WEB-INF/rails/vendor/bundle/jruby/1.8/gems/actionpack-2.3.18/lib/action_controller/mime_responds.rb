module ActionController #:nodoc:
  module MimeResponds #:nodoc:
    def self.included(base)
      base.module_eval do
        include ActionController::MimeResponds::InstanceMethods
      end
    end

    module InstanceMethods
      # Without web-service support, an action which collects the data for displaying a list of people
      # might look something like this:
      #
      #   def index
      #     @people = Person.find(:all)
      #   end
      #
      # Here's the same action, with web-service support baked in:
      #
      #   def index
      #     @people = Person.find(:all)
      #
      #     respond_to do |format|
      #       format.html
      #       format.xml { render :xml => @people.to_xml }
      #     end
      #   end
      #
      # What that says is, "if the client wants HTML in response to this action, just respond as we
      # would have before, but if the client wants XML, return them the list of people in XML format."
      # (Rails determines the desired response format from the HTTP Accept header submitted by the client.)
      #
      # Supposing you have an action that adds a new person, optionally creating their company
      # (by name) if it does not already exist, without web-services, it might look like this:
      #
      #   def create
      #     @company = Company.find_or_create_by_name(params[:company][:name])
      #     @person  = @company.people.create(params[:person])
      #
      #     redirect_to(person_list_url)
      #   end
      #
      # Here's the same action, with web-service support baked in:
      #
      #   def create
      #     company  = params[:person].delete(:company)
      #     @company = Company.find_or_create_by_name(company[:name])
      #     @person  = @company.people.create(params[:person])
      #
      #     respond_to do |format|
      #       format.html { redirect_to(person_list_url) }
      #       format.js
      #       format.xml  { render :xml => @person.to_xml(:include => @company) }
      #     end
      #   end
      #
      # If the client wants HTML, we just redirect them back to the person list. If they want Javascript
      # (format.js), then it is an RJS request and we render the RJS template associated with this action.
      # Lastly, if the client wants XML, we render the created person as XML, but with a twist: we also
      # include the person's company in the rendered XML, so you get something like this:
      #
      #   <person>
      #     <id>...</id>
      #     ...
      #     <company>
      #       <id>...</id>
      #       <name>...</name>
      #       ...
      #     </company>
      #   </person>
      #
      # Note, however, the extra bit at the top of that action:
      #
      #   company  = params[:person].delete(:company)
      #   @company = Company.find_or_create_by_name(company[:name])
      #
      # This is because the incoming XML document (if a web-service request is in process) can only contain a
      # single root-node. So, we have to rearrange things so that the request looks like this (url-encoded):
      #
      #   person[name]=...&person[company][name]=...&...
      #
      # And, like this (xml-encoded):
      #
      #   <person>
      #     <name>...</name>
      #     <company>
      #       <name>...</name>
      #     </company>
      #   </person>
      #
      # In other words, we make the request so that it operates on a single entity's person. Then, in the action,
      # we extract the company data from the request, find or create the company, and then create the new person
      # with the remaining data.
      #
      # Note that you can define your own XML parameter parser which would allow you to describe multiple entities
      # in a single request (i.e., by wrapping them all in a single root node), but if you just go with the flow
      # and accept Rails' defaults, life will be much easier.
      #
      # If you need to use a MIME type which isn't supported by default, you can register your own handlers in
      # environment.rb as follows.
      #
      #   Mime::Type.register "image/jpg", :jpg
      def respond_to(*types, &block)
        raise ArgumentError, "respond_to takes either types or a block, never both" unless types.any? ^ block
        block ||= lambda { |responder| types.each { |type| responder.send(type) } }
        responder = Responder.new(self)
        block.call(responder)
        responder.respond
      end
    end

    class Responder #:nodoc:
      def initialize(controller)
        @controller = controller
        @request    = controller.request
        @response   = controller.response

        if ActionController::Base.use_accept_header
          @mime_type_priority = Array(Mime::Type.lookup_by_extension(@request.parameters[:format]) || @request.accepts)
        else
          @mime_type_priority = [@request.format]
        end

        @order     = []
        @responses = {}
      end

      def custom(mime_type, &block)
        mime_type = mime_type.is_a?(Mime::Type) ? mime_type : Mime::Type.lookup(mime_type.to_s)

        @order << mime_type

        @responses[mime_type] ||= Proc.new do
          @response.template.template_format = mime_type.to_sym
          @response.content_type = mime_type.to_s
          block_given? ? block.call : @controller.send(:render, :action => @controller.action_name)
        end
      end

      def any(*args, &block)
        if args.any?
          args.each { |type| send(type, &block) }
        else
          custom(@mime_type_priority.first, &block)
        end
      end
      
      def self.generate_method_for_mime(mime)
        sym = mime.is_a?(Symbol) ? mime : mime.to_sym
        const = sym.to_s.upcase
        class_eval <<-RUBY, __FILE__, __LINE__ + 1
          def #{sym}(&block)                # def html(&block)
            custom(Mime::#{const}, &block)  #   custom(Mime::HTML, &block)
          end                               # end
        RUBY
      end

      Mime::SET.each do |mime|
        generate_method_for_mime(mime)
      end

      def method_missing(symbol, &block)
        mime_constant = Mime.const_get(symbol.to_s.upcase)
      
        if Mime::SET.include?(mime_constant)
          self.class.generate_method_for_mime(mime_constant)
          send(symbol, &block)
        else
          super
        end
      end

      def respond
        for priority in @mime_type_priority
          if priority == Mime::ALL
            @responses[@order.first].call
            return
          else
            if @responses[priority]
              @responses[priority].call
              return # mime type match found, be happy and return
            end
          end
        end

        if @order.include?(Mime::ALL)
          @responses[Mime::ALL].call
        else
          @controller.send :head, :not_acceptable
        end
      end
    end
  end
end
