module Krypt::ASN1
  module Template
    include Comparable

    module Sequence
      include Template
      def self.included(base)
        Template.init_cons_definition(base) do
          :SEQUENCE
        end
      end
    end

    module Set
      include Template
      def self.included(base)
        Template.init_cons_definition(base) do
          :SET
        end
      end
    end

    module Choice
      include Template

      def self.included(base)
        Template._mod_included_callback(base)
        definition = {
          codec: :CHOICE,
          layout: []
        }
        base.instance_variable_set(:@definition, definition)
        base.extend Template::ChoiceAccessor
        base.extend Template::ChoiceDefinitions
        base.extend Template::Parser
        base.asn1_attr_accessor :value, :@value
        base.asn1_attr_accessor :tag, :@tag
        base.asn1_attr_accessor :type, :@type
      end
    end

    module SequenceOf
      include Template
      def self.included(base)
        Template.init_cons_of_definition(base) do
          :SEQUENCE_OF
        end
      end
    end

    module SetOf
      include Template
      def self.included(base)
        Template.init_cons_of_definition(base) do
          :SET_OF
        end
      end
    end

    private 

    def self.init_cons_definition(base)
      _mod_included_callback(base)
      definition = {
        codec: yield,
        layout: [],
        min_size: 0
      }
      base.instance_variable_set(:@definition, definition)
      base.extend Template::Accessor
      base.extend Template::ConstructedDefinitions
      base.extend Template::Parser
    end

    def self.init_cons_of_definition(base)
      _mod_included_callback(base)
      definition = { codec: yield }
      base.instance_variable_set(:@definition, definition)
      base.extend Template::Accessor
      base.extend Template::ConstructedOfDefinitions
      base.extend Template::Parser
      base.asn1_attr_accessor :value, :@value
    end

    module GeneralDefinitions
      def init_methods
        declare_prim(:asn1_boolean, Krypt::ASN1::BOOLEAN)
        declare_prim(:asn1_integer, Krypt::ASN1::INTEGER)
        declare_prim(:asn1_bit_string, Krypt::ASN1::BIT_STRING)
        declare_prim(:asn1_octet_string, Krypt::ASN1::OCTET_STRING)
        declare_prim(:asn1_null, Krypt::ASN1::NULL)
        declare_prim(:asn1_object_id, Krypt::ASN1::OBJECT_ID)
        declare_prim(:asn1_enumerated, Krypt::ASN1::ENUMERATED)
        declare_prim(:asn1_utf8_string, Krypt::ASN1::UTF8_STRING)
        declare_prim(:asn1_numeric_string, Krypt::ASN1::NUMERIC_STRING)
        declare_prim(:asn1_printable_string, Krypt::ASN1::PRINTABLE_STRING)
        declare_prim(:asn1_t61_string, Krypt::ASN1::T61_STRING)
        declare_prim(:asn1_videotex_string, Krypt::ASN1::VIDEOTEX_STRING)
        declare_prim(:asn1_ia5_string, Krypt::ASN1::IA5_STRING)
        declare_prim(:asn1_utc_time, Krypt::ASN1::UTC_TIME)
        declare_prim(:asn1_generalized_time, Krypt::ASN1::GENERALIZED_TIME)
        declare_prim(:asn1_graphic_string, Krypt::ASN1::GRAPHIC_STRING)
        declare_prim(:asn1_iso64_string, Krypt::ASN1::ISO64_STRING)
        declare_prim(:asn1_general_string, Krypt::ASN1::GENERAL_STRING)
        declare_prim(:asn1_universal_string, Krypt::ASN1::UNIVERSAL_STRING)
        declare_prim(:asn1_bmp_string, Krypt::ASN1::BMP_STRING)

        declare_special_typed(:asn1_template, :TEMPLATE)
        declare_special_typed(:asn1_sequence_of, :SEQUENCE_OF)
        declare_special_typed(:asn1_set_of, :SET_OF)

        declare_any
      end

      def self.add_to_definition(klass, deff)
        cur_def = klass.instance_variable_get(:@definition)
        cur_def[:layout] << deff
        codec = cur_def[:codec]
        if codec == :SEQUENCE || codec == :SET 
          increase_min_size(cur_def, deff[:options])
        end
      end

      private

      def self.increase_min_size(cur_def, cur_opts)
        cur_opts ||= {}
        default = cur_opts[:default]
        optional = cur_opts[:optional]
        unless optional || default != nil
          cur_def[:min_size] += 1
        end
      end
    end

    module Accessor
      def asn1_attr_accessor(name, iv_name)
        define_method name do
          _get_callback(iv_name)
        end
        define_method "#{name.to_s}=".to_sym do |value|
          _set_callback(iv_name, value)
        end
      end
    end

    module ChoiceAccessor
      def asn1_attr_accessor(name, iv_name)
        define_method name do
          _get_callback_choice(iv_name)
        end
        define_method "#{name.to_s}=".to_sym do |value|
          _set_callback_choice(iv_name, value)
        end
      end
    end

    module ChoiceDefinitions
      extend GeneralDefinitions
      class << self
        define_method :declare_prim do |meth, type|
          define_method meth do |opts=nil|
            GeneralDefinitions.add_to_definition(self, {
              codec: :PRIMITIVE,
              type: type,
              options: opts
            })
          end
        end

        define_method :declare_special_typed do |meth, codec|
          define_method meth do |type, opts=nil|
            raise ArgumentError.new "Type must not be nil" if type == nil
            GeneralDefinitions.add_to_definition(self, {
              codec: codec,
              type: type,
              options: opts
            })
          end
        end

        define_method :declare_any do
          define_method :asn1_any do |opts=nil|
            GeneralDefinitions.add_to_definition(self, {
              codec: :ANY,
              type: Krypt::ASN1::ASN1Data,
              options: opts
            })
          end
        end
      end

      init_methods
    end 

    module ConstructedDefinitions
      extend GeneralDefinitions
      class << self
        define_method :declare_prim do |meth, type|
          define_method meth do |name, opts=nil|
            raise ArgumentError.new "Name must not be nil" if name == nil
            iv_name = ('@' + name.to_s).to_sym
            asn1_attr_accessor name, iv_name

            GeneralDefinitions.add_to_definition(self, {
              codec: :PRIMITIVE,
              type: type,
              name: iv_name,
              options: opts
            })
          end
        end

        define_method :declare_special_typed do |meth, codec|
          define_method meth do |name, type, opts=nil|
            raise ArgumentError.new "Name must not be nil" if name == nil
            raise ArgumentError.new "Type must not be nil" if type == nil
            iv_name = ('@' + name.to_s).to_sym
            asn1_attr_accessor name, iv_name

            GeneralDefinitions.add_to_definition(self, {
              codec: codec,
              type: type,
              name: iv_name,
              options: opts
            })
          end
        end

        define_method :declare_any do
          define_method :asn1_any do |name, opts=nil|
            raise ArgumentError.new "Name must not be nil" if name == nil
            iv_name = ('@' + name.to_s).to_sym
            asn1_attr_accessor name, iv_name

            GeneralDefinitions.add_to_definition(self, {
              codec: :ANY,
              type: Krypt::ASN1::ASN1Data,
              name: iv_name,
              options: opts
            })
          end
        end
      end

      init_methods
    end

    module ConstructedOfDefinitions
      def asn1_type(type)
        raise ArgumentError.new "Type must not be nil" if type == nil
        cur_def = instance_variable_get(:@definition)
        cur_def[:type] = type
      end
    end
  end
end
