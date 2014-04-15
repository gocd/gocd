class Java::OrgJrubyAst::Node
  POS = org.jruby.lexer.yacc.SimpleSourcePosition.new("-",-1)
  def [](ix)
    self.child_nodes[ix]
  end
  
  def first
    self.child_nodes[0] if self.child_nodes.size > 0
  end
  
  def last
    self.child_nodes[self.child_nodes.size-1] if self.child_nodes.size > 0
  end
  
  def +(other)
    blk = org.jruby.ast.BlockNode.new POS
    blk.add self
    blk.add other
    blk
  end
  
  attr_accessor :locals
  
  def run
    unless defined?(JRuby)
      require 'jruby'
    end
    root = self
    unless org.jruby.ast.RootNode === root
      pos = POS
      scope1 = org.jruby.parser.LocalStaticScope.new(nil)
      scope1.setVariables(java.lang.String[self.locals || 40].new)
      scope = org.jruby.runtime.DynamicScope.new(scope1, nil)
      root = org.jruby.ast.RootNode.new(pos, scope, self)
    end

    JRuby::runtime.eval root
  end
  
  def inspect(indent = 0)
    s = ' '*indent + self.class.name.split('::').last

    if self.respond_to?(:name)
      s << " |#{self.name}|"
    end
    if self.respond_to?(:value)
      s << " ==#{self.value.inspect}"
    end

    if self.respond_to?(:index)
      s << " &#{self.index.inspect}"
    end

    if self.respond_to?(:depth)
      s << " >#{self.depth.inspect}"
    end
    
    [:receiver_node, :args_node, :var_node, :head_node, :value_node, :iter_node, :body_node, :next_node, :condition, :then_body, :else_body].each do |mm|
      if self.respond_to?(mm)
        begin 
          s << "\n#{self.send(mm).inspect(indent+2)}" if self.send(mm)
        rescue
          s << "\n#{' '*(indent+2)}#{self.send(mm).inspect}" if self.send(mm)
        end
      end
    end

    if Java::OrgJrubyAst::ListNode === self
      (0...self.size).each do |n|
        begin
          s << "\n#{self.get(n).inspect(indent+2)}" if self.get(n)
        rescue
          s << "\n#{' '*(indent+2)}#{self.get(n).inspect}" if self.get(n)
        end
      end
    end
    s
  end
  
  def to_yaml_node(out)
    content = []
    content << {'name' => self.name} if self.respond_to?(:name)
    content << {'value' => self.value} if self.respond_to?(:value)
    [:receiver_node, :args_node, :value_node, :iter_node, :body_node, :next_node].each do |mm|
      if self.respond_to?(mm)
        content << self.send(mm) if self.send(mm)
      end
    end
    if Java::OrgJrubyAst::ListNode === self
      (0...self.size).each do |n|
        content << self.get(n) if self.get(n)
      end
    end
    out.map({ }.taguri, { self.class.name.split('::').last => content }, nil)
  end
end

