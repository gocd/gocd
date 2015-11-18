class YARD::Handlers::C::MixinHandler < YARD::Handlers::C::Base
  MATCH = /rb_include_module\s*\(\s*(\w+?),\s*(\w+?)\s*\)/
  handles MATCH
  statement_class BodyStatement

  process do
    statement.source.scan(MATCH) do |klass_var, mixin_var|
      namespace = namespace_for_variable(klass_var)
      ensure_loaded!(namespace)
      namespace.mixins(:instance) << namespace_for_variable(mixin_var)
    end
  end
end
