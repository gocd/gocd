class Module
  # Provides a +delegate+ class method to easily expose contained objects'
  # public methods as your own.
  #
  # The macro receives one or more method names (specified as symbols or
  # strings) and the name of the target object via the <tt>:to</tt> option
  # (also a symbol or string).
  #
  # Delegation is particularly useful with Active Record associations:
  #
  #   class Greeter < ActiveRecord::Base
  #     def hello
  #       'hello'
  #     end
  #
  #     def goodbye
  #       'goodbye'
  #     end
  #   end
  #
  #   class Foo < ActiveRecord::Base
  #     belongs_to :greeter
  #     delegate :hello, to: :greeter
  #   end
  #
  #   Foo.new.hello   # => "hello"
  #   Foo.new.goodbye # => NoMethodError: undefined method `goodbye' for #<Foo:0x1af30c>
  #
  # Multiple delegates to the same target are allowed:
  #
  #   class Foo < ActiveRecord::Base
  #     belongs_to :greeter
  #     delegate :hello, :goodbye, to: :greeter
  #   end
  #
  #   Foo.new.goodbye # => "goodbye"
  #
  # Methods can be delegated to instance variables, class variables, or constants
  # by providing them as a symbols:
  #
  #   class Foo
  #     CONSTANT_ARRAY = [0,1,2,3]
  #     @@class_array  = [4,5,6,7]
  #
  #     def initialize
  #       @instance_array = [8,9,10,11]
  #     end
  #     delegate :sum, to: :CONSTANT_ARRAY
  #     delegate :min, to: :@@class_array
  #     delegate :max, to: :@instance_array
  #   end
  #
  #   Foo.new.sum # => 6
  #   Foo.new.min # => 4
  #   Foo.new.max # => 11
  #
  # It's also possible to delegate a method to the class by using +:class+:
  #
  #   class Foo
  #     def self.hello
  #       "world"
  #     end
  #
  #     delegate :hello, to: :class
  #   end
  #
  #   Foo.new.hello # => "world"
  #
  # Delegates can optionally be prefixed using the <tt>:prefix</tt> option. If the value
  # is <tt>true</tt>, the delegate methods are prefixed with the name of the object being
  # delegated to.
  #
  #   Person = Struct.new(:name, :address)
  #
  #   class Invoice < Struct.new(:client)
  #     delegate :name, :address, to: :client, prefix: true
  #   end
  #
  #   john_doe = Person.new('John Doe', 'Vimmersvej 13')
  #   invoice = Invoice.new(john_doe)
  #   invoice.client_name    # => "John Doe"
  #   invoice.client_address # => "Vimmersvej 13"
  #
  # It is also possible to supply a custom prefix.
  #
  #   class Invoice < Struct.new(:client)
  #     delegate :name, :address, to: :client, prefix: :customer
  #   end
  #
  #   invoice = Invoice.new(john_doe)
  #   invoice.customer_name    # => 'John Doe'
  #   invoice.customer_address # => 'Vimmersvej 13'
  #
  # If the target is +nil+ and does not respond to the delegated method a
  # +NoMethodError+ is raised, as with any other value. Sometimes, however, it
  # makes sense to be robust to that situation and that is the purpose of the
  # <tt>:allow_nil</tt> option: If the target is not +nil+, or it is and
  # responds to the method, everything works as usual. But if it is +nil+ and
  # does not respond to the delegated method, +nil+ is returned.
  #
  #   class User < ActiveRecord::Base
  #     has_one :profile
  #     delegate :age, to: :profile
  #   end
  #
  #   User.new.age # raises NoMethodError: undefined method `age'
  #
  # But if not having a profile yet is fine and should not be an error
  # condition:
  #
  #   class User < ActiveRecord::Base
  #     has_one :profile
  #     delegate :age, to: :profile, allow_nil: true
  #   end
  #
  #   User.new.age # nil
  #
  # Note that if the target is not +nil+ then the call is attempted regardless of the
  # <tt>:allow_nil</tt> option, and thus an exception is still raised if said object
  # does not respond to the method:
  #
  #   class Foo
  #     def initialize(bar)
  #       @bar = bar
  #     end
  #
  #     delegate :name, to: :@bar, allow_nil: true
  #   end
  #
  #   Foo.new("Bar").name # raises NoMethodError: undefined method `name'
  #
  def delegate(*methods)
    options = methods.pop
    unless options.is_a?(Hash) && to = options[:to]
      raise ArgumentError, 'Delegation needs a target. Supply an options hash with a :to key as the last argument (e.g. delegate :hello, to: :greeter).'
    end

    prefix, allow_nil = options.values_at(:prefix, :allow_nil)

    if prefix == true && to =~ /^[^a-z_]/
      raise ArgumentError, 'Can only automatically set the delegation prefix when delegating to a method.'
    end

    method_prefix = \
      if prefix
        "#{prefix == true ? to : prefix}_"
      else
        ''
      end

    file, line = caller.first.split(':', 2)
    line = line.to_i

    to = to.to_s
    to = 'self.class' if to == 'class'

    methods.each do |method|
      # Attribute writer methods only accept one argument. Makes sure []=
      # methods still accept two arguments.
      definition = (method =~ /[^\]]=$/) ? 'arg' : '*args, &block'

      # The following generated methods call the target exactly once, storing
      # the returned value in a dummy variable.
      #
      # Reason is twofold: On one hand doing less calls is in general better.
      # On the other hand it could be that the target has side-effects,
      # whereas conceptualy, from the user point of view, the delegator should
      # be doing one call.
      if allow_nil
        module_eval(<<-EOS, file, line - 3)
          def #{method_prefix}#{method}(#{definition})        # def customer_name(*args, &block)
            _ = #{to}                                         #   _ = client
            if !_.nil? || nil.respond_to?(:#{method})         #   if !_.nil? || nil.respond_to?(:name)
              _.#{method}(#{definition})                      #     _.name(*args, &block)
            end                                               #   end
          end                                                 # end
        EOS
      else
        exception = %(raise "#{self}##{method_prefix}#{method} delegated to #{to}.#{method}, but #{to} is nil: \#{self.inspect}")

        module_eval(<<-EOS, file, line - 2)
          def #{method_prefix}#{method}(#{definition})                                          # def customer_name(*args, &block)
            _ = #{to}                                                                           #   _ = client
            _.#{method}(#{definition})                                                          #   _.name(*args, &block)
          rescue NoMethodError => e                                                             # rescue NoMethodError => e
            location = "%s:%d:in `%s'" % [__FILE__, __LINE__ - 2, '#{method_prefix}#{method}']  #   location = "%s:%d:in `%s'" % [__FILE__, __LINE__ - 2, 'customer_name']
            if _.nil? && e.backtrace.first == location                                          #   if _.nil? && e.backtrace.first == location
              #{exception}                                                                      #     # add helpful message to the exception
            else                                                                                #   else
              raise                                                                             #     raise
            end                                                                                 #   end
          end                                                                                   # end
        EOS
      end
    end
  end
end
