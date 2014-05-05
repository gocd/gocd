module ActiveResource
  class ResourceInvalid < ClientError  #:nodoc:
  end

  # Active Resource validation is reported to and from this object, which is used by Base#save
  # to determine whether the object in a valid state to be saved. See usage example in Validations.  
  class Errors
    include Enumerable
    attr_reader :errors

    delegate :empty?, :to => :errors
    
    def initialize(base) # :nodoc:
      @base, @errors = base, {}
    end

    # Add an error to the base Active Resource object rather than an attribute.
    #
    # ==== Examples
    #   my_folder = Folder.find(1)
    #   my_folder.errors.add_to_base("You can't edit an existing folder")
    #   my_folder.errors.on_base
    #   # => "You can't edit an existing folder"
    #
    #   my_folder.errors.add_to_base("This folder has been tagged as frozen")
    #   my_folder.valid?
    #   # => false
    #   my_folder.errors.on_base
    #   # => ["You can't edit an existing folder", "This folder has been tagged as frozen"]
    #
    def add_to_base(msg)
      add(:base, msg)
    end

    # Adds an error to an Active Resource object's attribute (named for the +attribute+ parameter)
    # with the error message in +msg+.
    #
    # ==== Examples
    #   my_resource = Node.find(1)
    #   my_resource.errors.add('name', 'can not be "base"') if my_resource.name == 'base'
    #   my_resource.errors.on('name')
    #   # => 'can not be "base"!'
    #
    #   my_resource.errors.add('desc', 'can not be blank') if my_resource.desc == ''
    #   my_resource.valid?
    #   # => false
    #   my_resource.errors.on('desc')
    #   # => 'can not be blank!'
    #
    def add(attribute, msg)
      @errors[attribute.to_s] = [] if @errors[attribute.to_s].nil?
      @errors[attribute.to_s] << msg
    end

    # Returns true if the specified +attribute+ has errors associated with it.
    #
    # ==== Examples
    #   my_resource = Disk.find(1)
    #   my_resource.errors.add('location', 'must be Main') unless my_resource.location == 'Main'
    #   my_resource.errors.on('location')
    #   # => 'must be Main!'
    #
    #   my_resource.errors.invalid?('location')
    #   # => true
    #   my_resource.errors.invalid?('name')
    #   # => false
    def invalid?(attribute)
      !@errors[attribute.to_s].nil?
    end

    # A method to return the errors associated with +attribute+, which returns nil, if no errors are 
    # associated with the specified +attribute+, the error message if one error is associated with the specified +attribute+,
    # or an array of error messages if more than one error is associated with the specified +attribute+.
    #
    # ==== Examples
    #   my_person = Person.new(params[:person])
    #   my_person.errors.on('login')
    #   # => nil
    #
    #   my_person.errors.add('login', 'can not be empty') if my_person.login == ''
    #   my_person.errors.on('login')
    #   # => 'can not be empty'
    #
    #   my_person.errors.add('login', 'can not be longer than 10 characters') if my_person.login.length > 10
    #   my_person.errors.on('login')
    #   # => ['can not be empty', 'can not be longer than 10 characters']
    def on(attribute)
      errors = @errors[attribute.to_s]
      return nil if errors.nil?
      errors.size == 1 ? errors.first : errors
    end
    
    alias :[] :on

    # A method to return errors assigned to +base+ object through add_to_base, which returns nil, if no errors are 
    # associated with the specified +attribute+, the error message if one error is associated with the specified +attribute+,
    # or an array of error messages if more than one error is associated with the specified +attribute+.
    #
    # ==== Examples
    #   my_account = Account.find(1)
    #   my_account.errors.on_base
    #   # => nil
    #
    #   my_account.errors.add_to_base("This account is frozen")
    #   my_account.errors.on_base
    #   # => "This account is frozen"
    #
    #   my_account.errors.add_to_base("This account has been closed")
    #   my_account.errors.on_base
    #   # => ["This account is frozen", "This account has been closed"]
    #
    def on_base
      on(:base)
    end

    # Yields each attribute and associated message per error added.
    #
    # ==== Examples
    #   my_person = Person.new(params[:person])
    #
    #   my_person.errors.add('login', 'can not be empty') if my_person.login == ''
    #   my_person.errors.add('password', 'can not be empty') if my_person.password == ''
    #   messages = ''
    #   my_person.errors.each {|attr, msg| messages += attr.humanize + " " + msg + "<br />"}
    #   messages
    #   # => "Login can not be empty<br />Password can not be empty<br />"
    #
    def each
      @errors.each_key { |attr| @errors[attr].each { |msg| yield attr, msg } }
    end

    # Yields each full error message added. So Person.errors.add("first_name", "can't be empty") will be returned
    # through iteration as "First name can't be empty".
    #
    # ==== Examples
    #   my_person = Person.new(params[:person])
    #
    #   my_person.errors.add('login', 'can not be empty') if my_person.login == ''
    #   my_person.errors.add('password', 'can not be empty') if my_person.password == ''
    #   messages = ''
    #   my_person.errors.each_full {|msg| messages += msg + "<br/>"}
    #   messages
    #   # => "Login can not be empty<br />Password can not be empty<br />"
    #
    def each_full
      full_messages.each { |msg| yield msg }
    end

    # Returns all the full error messages in an array.
    #
    # ==== Examples
    #   my_person = Person.new(params[:person])
    #
    #   my_person.errors.add('login', 'can not be empty') if my_person.login == ''
    #   my_person.errors.add('password', 'can not be empty') if my_person.password == ''
    #   messages = ''
    #   my_person.errors.full_messages.each {|msg| messages += msg + "<br/>"}
    #   messages
    #   # => "Login can not be empty<br />Password can not be empty<br />"
    #
    def full_messages
      full_messages = []

      @errors.each_key do |attr|
        @errors[attr].each do |msg|
          next if msg.nil?

          if attr == "base"
            full_messages << msg
          else
            full_messages << [attr.humanize, msg].join(' ')
          end
        end
      end
      full_messages
    end

    def clear
      @errors = {}
    end

    # Returns the total number of errors added. Two errors added to the same attribute will be counted as such
    # with this as well.
    #
    # ==== Examples
    #   my_person = Person.new(params[:person])
    #   my_person.errors.size
    #   # => 0
    #
    #   my_person.errors.add('login', 'can not be empty') if my_person.login == ''
    #   my_person.errors.add('password', 'can not be empty') if my_person.password == ''
    #   my_person.error.size
    #   # => 2
    #
    def size
      @errors.values.inject(0) { |error_count, attribute| error_count + attribute.size }
    end

    alias_method :count, :size
    alias_method :length, :size
    
    # Grabs errors from an array of messages (like ActiveRecord::Validations)
    def from_array(messages)
      clear
      humanized_attributes = @base.attributes.keys.inject({}) { |h, attr_name| h.update(attr_name.humanize => attr_name) }
      messages.each do |message|
        attr_message = humanized_attributes.keys.detect do |attr_name|
          if message[0, attr_name.size + 1] == "#{attr_name} "
            add humanized_attributes[attr_name], message[(attr_name.size + 1)..-1]
          end
        end
        
        add_to_base message if attr_message.nil?
      end
    end

    # Grabs errors from the json response.
    def from_json(json)
      array = ActiveSupport::JSON.decode(json)['errors'] rescue []
      from_array array
    end

    # Grabs errors from the XML response.
    def from_xml(xml)
      array = Array.wrap(Hash.from_xml(xml)['errors']['error']) rescue []
      from_array array
    end
  end
  
  # Module to support validation and errors with Active Resource objects. The module overrides
  # Base#save to rescue ActiveResource::ResourceInvalid exceptions and parse the errors returned 
  # in the web service response. The module also adds an +errors+ collection that mimics the interface 
  # of the errors provided by ActiveRecord::Errors.
  #
  # ==== Example
  #
  # Consider a Person resource on the server requiring both a +first_name+ and a +last_name+ with a 
  # <tt>validates_presence_of :first_name, :last_name</tt> declaration in the model:
  #
  #   person = Person.new(:first_name => "Jim", :last_name => "")
  #   person.save                   # => false (server returns an HTTP 422 status code and errors)
  #   person.valid?                 # => false
  #   person.errors.empty?          # => false
  #   person.errors.count           # => 1
  #   person.errors.full_messages   # => ["Last name can't be empty"]
  #   person.errors.on(:last_name)  # => "can't be empty"
  #   person.last_name = "Halpert"  
  #   person.save                   # => true (and person is now saved to the remote service)
  #
  module Validations
    def self.included(base) # :nodoc:
      base.class_eval do
        alias_method_chain :save, :validation
      end
    end

    # Validate a resource and save (POST) it to the remote web service.
    def save_with_validation
      save_without_validation
      true
    rescue ResourceInvalid => error
      case self.class.format
      when ActiveResource::Formats[:xml]
        errors.from_xml(error.response.body)
      when ActiveResource::Formats[:json]
        errors.from_json(error.response.body)
      end
      false
    end

    # Checks for errors on an object (i.e., is resource.errors empty?).
    # 
    # ==== Examples
    #   my_person = Person.create(params[:person])
    #   my_person.valid?
    #   # => true
    #
    #   my_person.errors.add('login', 'can not be empty') if my_person.login == ''
    #   my_person.valid?
    #   # => false
    def valid?
      errors.empty?
    end

    # Returns the Errors object that holds all information about attribute error messages.
    def errors
      @errors ||= Errors.new(self)
    end
  end
end
