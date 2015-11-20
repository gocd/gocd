module Representable
  module Debug
    def self.extended(represented)
      represented.extend Representable
    end

    module Representable
      def update_properties_from(doc, options, format)
        puts
        puts "[Deserialize]........."
        puts "[Deserialize] document #{doc.inspect}"
        super
      end

      def create_representation_with(doc, options, format)
        puts
        puts "[Serialize]........."
        puts "[Serialize]"
        super
      end

      def representable_mapper(*args)
        super.extend(Mapper)
      end
    end

    module Binding
      def read(doc)
        value = super
        puts "                #read --> #{value.inspect}"
        value
      end

      def evaluate_option(name, *args, &block)
        puts "=====#{self[name]}" if name ==:prepare
        puts (evaled = self[name]) ?
          "                #evaluate_option [#{name}]: eval!!!" :
          "                #evaluate_option [#{name}]: skipping"
        value = super
        puts "                #evaluate_option [#{name}]: --> #{value}" if evaled
        puts "                #evaluate_option [#{name}]: -->= #{args.first}" if name == :setter
        value
      end

      def populator
        super.extend(Populator)
      end

      def serializer
        super.extend(Serializer)
      end
    end

    module Populator
      def deserialize(fragment)
        puts "                  Populator#deserialize: #{fragment.inspect}"
        puts "                                       : typed? is false, skipping Deserializer." if ! @binding.typed?
        super
      end

      def deserializer
        super.extend(Deserializer)
      end
    end

    module Deserializer
      def create_object(fragment, *args)
        value = super
          puts "                    Deserializer#create_object: --> #{value.inspect}"
        value
      end
    end

    module Serializer
      def marshal(object, user_options)
        puts "                    Serializer#marshal: --> #{object.inspect}"
        super
      end
    end

    module Mapper
      def uncompile_fragment(bin, doc)
        bin.extend(Binding)
        puts "              uncompile_fragment: #{bin.name}"
        super
      end

      def compile_fragment(bin, doc)
        bin.extend(Binding)
        puts "              compile_fragment: #{bin.name}"
        super
      end
    end
  end
end
