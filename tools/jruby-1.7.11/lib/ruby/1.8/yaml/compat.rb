#
# Ruby 1.6 -> 1.8 compatibility
# (an isolated incident)
#
class Object; alias_method :object_id, :id; end unless Object.respond_to? :object_id

class Object; def instance_variable_set(k, v); self.instance_eval "#{k} = v"; end; end \
    unless Object.respond_to? :instance_variable_set

class Object; def instance_variable_get(k); self.instance_eval "#{k}"; end; end \
    unless Object.respond_to? :instance_variable_get

unless Object.respond_to? :allocate
    class Object
        def allocate
            name = self.class.name
            if Marshal::const_defined? :MAJOR_VERSION
                ostr = sprintf( "%c%co:%c%s\000", Marshal::MAJOR_VERSION, Marshal::MINOR_VERSION,
                                name.length + 5, name )
            else
                ostr = sprintf( "\004\006o:%c%s\000", name.length + 5, name )
            end
            ::Marshal.load( ostr )
        end
    end
end
