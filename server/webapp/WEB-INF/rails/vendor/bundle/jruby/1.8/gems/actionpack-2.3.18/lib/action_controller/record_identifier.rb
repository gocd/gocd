module ActionController  
  # The record identifier encapsulates a number of naming conventions for dealing with records, like Active Records or 
  # Active Resources or pretty much any other model type that has an id. These patterns are then used to try elevate
  # the view actions to a higher logical level. Example:
  #
  #   # routes
  #   map.resources :posts
  #
  #   # view
  #   <% div_for(post) do %>     <div id="post_45" class="post">
  #     <%= post.body %>           What a wonderful world!
  #   <% end %>                  </div>
  #
  #   # controller
  #   def destroy
  #     post = Post.find(params[:id])
  #     post.destroy
  #
  #     respond_to do |format|
  #       format.html { redirect_to(post) } # Calls polymorphic_url(post) which in turn calls post_url(post)
  #       format.js do
  #         # Calls: new Effect.fade('post_45');
  #         render(:update) { |page| page[post].visual_effect(:fade) }
  #       end
  #     end
  #   end
  #
  # As the example above shows, you can stop caring to a large extent what the actual id of the post is. You just know
  # that one is being assigned and that the subsequent calls in redirect_to and the RJS expect that same naming 
  # convention and allows you to write less code if you follow it.
  module RecordIdentifier
    extend self

    JOIN = '_'.freeze
    NEW = 'new'.freeze

    # Returns plural/singular for a record or class. Example:
    #
    #   partial_path(post)                   # => "posts/post"
    #   partial_path(Person)                 # => "people/person"
    #   partial_path(Person, "admin/games")  # => "admin/people/person"
    def partial_path(record_or_class, controller_path = nil)
      name = model_name_from_record_or_class(record_or_class)

      if controller_path && controller_path.include?("/")
        "#{File.dirname(controller_path)}/#{name.partial_path}"
      else
        name.partial_path
      end
    end

    # The DOM class convention is to use the singular form of an object or class. Examples:
    #
    #   dom_class(post)   # => "post"
    #   dom_class(Person) # => "person"
    #
    # If you need to address multiple instances of the same class in the same view, you can prefix the dom_class:
    #
    #   dom_class(post, :edit)   # => "edit_post"
    #   dom_class(Person, :edit) # => "edit_person"
    def dom_class(record_or_class, prefix = nil)
      singular = singular_class_name(record_or_class)
      prefix ? "#{prefix}#{JOIN}#{singular}" : singular
    end

    # The DOM id convention is to use the singular form of an object or class with the id following an underscore.
    # If no id is found, prefix with "new_" instead. Examples:
    #
    #   dom_id(Post.find(45))       # => "post_45"
    #   dom_id(Post.new)            # => "new_post"
    #
    # If you need to address multiple instances of the same class in the same view, you can prefix the dom_id:
    #
    #   dom_id(Post.find(45), :edit) # => "edit_post_45"
    def dom_id(record, prefix = nil) 
      if record_id = record.id
        "#{dom_class(record, prefix)}#{JOIN}#{record_id}"
      else
        dom_class(record, prefix || NEW)
      end
    end

    # Returns the plural class name of a record or class. Examples:
    #
    #   plural_class_name(post)             # => "posts"
    #   plural_class_name(Highrise::Person) # => "highrise_people"
    def plural_class_name(record_or_class)
      model_name_from_record_or_class(record_or_class).plural
    end

    # Returns the singular class name of a record or class. Examples:
    #
    #   singular_class_name(post)             # => "post"
    #   singular_class_name(Highrise::Person) # => "highrise_person"
    def singular_class_name(record_or_class)
      model_name_from_record_or_class(record_or_class).singular
    end

    private
      def model_name_from_record_or_class(record_or_class)
        (record_or_class.is_a?(Class) ? record_or_class : record_or_class.class).model_name
      end
  end
end
