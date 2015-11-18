# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

require 'json'
require 'tempfile'
require 'weakref'

module ObjectSpace

  def count_nodes(nodes = {})
    ObjectSpace.each_object(Module) do |mod|
      mod.methods(false).each do |name|
        count_nodes_method mod.method(name), nodes
      end

      mod.private_methods(false).each do |name|
        count_nodes_method mod.method(name), nodes
      end
    end

    ObjectSpace.each_object(Proc) do |proc|
      count_nodes_method proc, nodes
    end

    ObjectSpace.each_object(Method) do |method|
      count_nodes_method method, nodes
    end

    ObjectSpace.each_object(UnboundMethod) do |umethod|
      count_nodes_method umethod, nodes
    end

    nodes
  end

  module_function :count_nodes

  class << self

    def count_nodes_method(method, nodes)
      node_stack = [Truffle::Primitive.ast(method)]

      until node_stack.empty?
        node = node_stack.pop
        next if node.nil?

        name = node.first
        children = node.drop(1)
        nodes[name] ||= 0
        nodes[name] += 1
        node_stack.push(*children)
      end
    end
    private :count_nodes_method

  end

  def count_objects_size(hash = {})
    total = 0
    ObjectSpace.each_object(Class) do |klass|
      per_klass = memsize_of_all(klass)
      hash[klass.name.to_sym] = per_klass unless klass.name.nil?
      total += per_klass
    end
    hash[:TOTAL] = total
    hash
  end
  module_function :count_objects_size

  def count_tdata_objects(hash = {})
    ObjectSpace.each_object do |object|
      object_type = Truffle::Primitive.object_type_of(object)
      hash[object_type] ||= 0
      hash[object_type] += 1
    end
    hash
  end
  module_function :count_tdata_objects

  def dump(object, output: :string)
    case output
      when :string
        json = {
          address: "0x" + object.object_id.to_s(16),
          class: "0x" + object.class.object_id.to_s(16),
          memsize: memsize_of(object),
          flags: { }
        }
        case object
          when String
            json.merge!({
              type: "STRING",
              bytesize: object.bytesize,
              value: object,
              encoding: object.encoding.name
            })
          when Array
            json.merge!({
              type: "ARRAY",
              length: object.size
            })
          when Hash
            json.merge!({
              type: "HASH",
              size: object.size
            })
          else
            json.merge!({
              type: "OBJECT",
              length: object.instance_variables.size
            })
        end
        JSON.generate(json)
      when :file
        f = Tempfile.new(['rubyobj', '.json'])
        f.write dump(object, output: :string)
        f.close
        f.path
      when :stdout
        puts dump(object, output: :string)
        nil
    end
  end
  module_function :dump

  def dump_all(output: :file)
    case output
      when :string
        objects = []
        ObjectSpace.each_object do |object|
          objects.push dump(object)
        end
        objects.join("\n")
      when :file
        f = Tempfile.new(['ruby', '.json'])
        f.write dump_all(output: :string)
        f.close
        f.path
      when :stdout
        puts dump_all(output: :string)
        nil
      when IO
        output.write dump_all(output: :string)
        nil
    end
  end
  module_function :dump_all

  def memsize_of(object)
    Truffle::ObjSpace.memsize_of(object)
  end
  module_function :memsize_of

  def memsize_of_all(klass = BasicObject)
    total = 0
    ObjectSpace.each_object(klass) do |object|
      total += ObjectSpace.memsize_of(object)
    end
    total
  end
  module_function :memsize_of_all

  def reachable_objects_from(object)
    Truffle::ObjSpace.adjacent_objects(object)
  end
  module_function :reachable_objects_from

  def reachable_objects_from_root
    {"roots" => Truffle::ObjSpace.root_objects}
  end
  module_function :reachable_objects_from_root

  def trace_object_allocations
    trace_object_allocations_start

    begin
      yield
    ensure
      trace_object_allocations_stop
    end
  end
  module_function :trace_object_allocations

  def trace_object_allocations_clear
    ALLOCATIONS.clear
  end
  module_function :trace_object_allocations_clear

  def trace_object_allocations_debug_start
    trace_object_allocations_start
  end
  module_function :trace_object_allocations_debug_start

  def trace_object_allocations_start
    Truffle::ObjSpace.trace_allocations_start
  end
  module_function :trace_object_allocations_start

  def trace_object_allocations_stop
    Truffle::ObjSpace.trace_allocations_stop
  end
  module_function :trace_object_allocations_stop

  def allocation_class_path(object)
    allocation = ALLOCATIONS[object]
    return nil if allocation.nil?
    allocation.class_path
  end
  module_function :allocation_class_path

  def allocation_generation(object)
    allocation = ALLOCATIONS[object]
    return nil if allocation.nil?
    allocation.generation
  end
  module_function :allocation_generation

  def allocation_method_id(object)
    allocation = ALLOCATIONS[object]
    return nil if allocation.nil?
    allocation.method_id
  end
  module_function :allocation_method_id

  def allocation_sourcefile(object)
    allocation = ALLOCATIONS[object]
    return nil if allocation.nil?
    allocation.sourcefile
  end
  module_function :allocation_sourcefile

  def allocation_sourceline(object)
    allocation = ALLOCATIONS[object]
    return nil if allocation.nil?
    allocation.sourceline
  end
  module_function :allocation_sourceline

  Allocation = Struct.new(:class_path, :method_id, :sourcefile, :sourceline, :generation)

  ALLOCATIONS = {}.compare_by_identity

  def trace_allocation(object, class_path, method_id, sourcefile, sourceline, generation)
    ALLOCATIONS[object] = Allocation.new(class_path, method_id, sourcefile, sourceline, generation)
  end
  module_function :trace_allocation

end
