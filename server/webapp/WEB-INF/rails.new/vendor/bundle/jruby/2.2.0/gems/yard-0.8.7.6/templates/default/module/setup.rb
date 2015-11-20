include Helpers::ModuleHelper

def init
  sections :header, :box_info, :pre_docstring, T('docstring'), :children,
    :constant_summary, [T('docstring')], :inherited_constants,
    :attribute_summary, [:item_summary], :inherited_attributes,
    :method_summary, [:item_summary], :inherited_methods,
    :methodmissing, [T('method_details')],
    :attribute_details, [T('method_details')],
    :method_details_list, [T('method_details')]
end

def pre_docstring
  return if object.docstring.blank?
  erb(:pre_docstring)
end

def children
  @inner = [[:modules, []], [:classes, []]]
  object.children.each do |child|
    @inner[0][1] << child if child.type == :module
    @inner[1][1] << child if child.type == :class
  end
  @inner.map! {|v| [v[0], run_verifier(v[1].sort_by {|o| o.name.to_s })] }
  return if (@inner[0][1].size + @inner[1][1].size) == 0
  erb(:children)
end

def methodmissing
  mms = object.meths(:inherited => true, :included => true)
  return unless @mm = mms.find {|o| o.name == :method_missing && o.scope == :instance }
  erb(:methodmissing)
end

def method_listing(include_specials = true)
  return @smeths ||= method_listing.reject {|o| special_method?(o) } unless include_specials
  return @meths if @meths
  @meths = object.meths(:inherited => false, :included => !options.embed_mixins.empty?)
  if options.embed_mixins.size > 0
    @meths = @meths.reject {|m| options.embed_mixins_match?(m.namespace) == false }
  end
  @meths = sort_listing(prune_method_listing(@meths))
  @meths
end

def special_method?(meth)
  return true if meth.name(true) == '#method_missing'
  return true if meth.constructor?
  false
end

def attr_listing
  return @attrs if @attrs
  @attrs = []
  object.inheritance_tree(true).each do |superclass|
    next if superclass.is_a?(CodeObjects::Proxy)
    next if options.embed_mixins.size > 0 &&
      options.embed_mixins_match?(superclass) == false
    [:class, :instance].each do |scope|
      superclass.attributes[scope].each do |name, rw|
        attr = prune_method_listing([rw[:read], rw[:write]].compact, false).first
        @attrs << attr if attr
      end
    end
    break if options.embed_mixins.empty?
  end
  @attrs = sort_listing(@attrs)
end

def constant_listing
  return @constants if @constants
  @constants = object.constants(:included => false, :inherited => false)
  @constants += object.cvars
  @constants = run_verifier(@constants)
  @constants
end

def sort_listing(list)
  list.sort_by {|o| [o.scope.to_s, o.name.to_s.downcase] }
end

def inherited_attr_list(&block)
  object.inheritance_tree(true)[1..-1].each do |superclass|
    next if superclass.is_a?(YARD::CodeObjects::Proxy)
    next if options.embed_mixins.size > 0 && options.embed_mixins_match?(superclass) != false
    attribs = superclass.attributes[:instance]
    attribs = attribs.reject {|name, rw| object.child(:scope => :instance, :name => name) != nil }
    attribs = attribs.sort_by {|args| args.first.to_s }.map {|n, m| m[:read] || m[:write] }
    attribs = prune_method_listing(attribs, false)
    yield superclass, attribs if attribs.size > 0
  end
end

def inherited_constant_list(&block)
  object.inheritance_tree(true)[1..-1].each do |superclass|
    next if superclass.is_a?(YARD::CodeObjects::Proxy)
    next if options.embed_mixins.size > 0 && options.embed_mixins_match?(superclass) != false
    consts = superclass.constants(:included => false, :inherited => false)
    consts = consts.reject {|const| object.child(:type => :constant, :name => const.name) != nil }
    consts = consts.sort_by {|const| const.name.to_s }
    consts = run_verifier(consts)
    yield superclass, consts if consts.size > 0
  end
end

def docstring_full(obj)
  docstring = ""
  if obj.tags(:overload).size == 1 && obj.docstring.empty?
    docstring = obj.tag(:overload).docstring
  else
    docstring = obj.docstring
  end

  if docstring.summary.empty? && obj.tags(:return).size == 1 && obj.tag(:return).text
    docstring = Docstring.new(obj.tag(:return).text.gsub(/\A([a-z])/) {|x| x.upcase }.strip)
  end

  docstring
end

def docstring_summary(obj)
  docstring_full(obj).summary
end

def groups(list, type = "Method")
  if groups_data = object.groups
    list.each {|m| groups_data |= [m.group] if m.group && owner != m.namespace }
    others = list.select {|m| !m.group || !groups_data.include?(m.group) }
    groups_data.each do |name|
      items = list.select {|m| m.group == name }
      yield(items, name) unless items.empty?
    end
  else
    others = []
    group_data = {}
    list.each do |meth|
      if meth.group
        (group_data[meth.group] ||= []) << meth
      else
        others << meth
      end
    end
    group_data.each {|group, items| yield(items, group) unless items.empty? }
  end

  scopes(others) {|items, scope| yield(items, "#{scope.to_s.capitalize} #{type} Summary") }
end

def scopes(list)
  [:class, :instance].each do |scope|
    items = list.select {|m| m.scope == scope }
    yield(items, scope) unless items.empty?
  end
end

def mixed_into(object)
  unless globals.mixed_into
    globals.mixed_into = {}
    list = run_verifier Registry.all(:class, :module)
    list.each {|o| o.mixins.each {|m| (globals.mixed_into[m.path] ||= []) << o } }
  end

  globals.mixed_into[object.path] || []
end
