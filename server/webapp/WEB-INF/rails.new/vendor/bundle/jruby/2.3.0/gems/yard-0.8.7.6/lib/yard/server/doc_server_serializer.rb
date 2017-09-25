require 'webrick/httputils'

module YARD
  module Server
    # A custom {Serializers::Base serializer} which returns resource URLs instead of
    # static relative paths to files on disk.
    class DocServerSerializer < Serializers::FileSystemSerializer
      include WEBrick::HTTPUtils

      def initialize(command = nil)
        super(:basepath => '', :extension => '')
      end

      def serialized_path(object)
        case object
        when CodeObjects::RootObject
          "toplevel"
        when CodeObjects::ExtendedMethodObject
          name = object.name.to_s
          serialized_path(object.namespace) + ':' + urlencode(object.name.to_s)
        when CodeObjects::MethodObject
          serialized_path(object.namespace) +
            (object.scope == :instance ? ":" : ".") + urlencode(object.name.to_s)
        when CodeObjects::ConstantObject, CodeObjects::ClassVariableObject
          serialized_path(object.namespace) + "##{object.name}-#{object.type}"
        when CodeObjects::ExtraFileObject
          super(object).gsub(/^file\./, 'file/')
        else
          super(object)
        end
      end

      private

      def urlencode(name)
        if name.respond_to?(:force_encoding)
          name = name.dup.force_encoding('binary')
        end
        escape(name)
      end
    end
  end
end
