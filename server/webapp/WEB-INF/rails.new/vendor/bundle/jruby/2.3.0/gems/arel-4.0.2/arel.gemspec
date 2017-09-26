# -*- encoding: utf-8 -*-
# stub: arel 4.0.2.20140205180311 ruby lib

Gem::Specification.new do |s|
  s.name = "arel"
  s.version = "4.0.2.20140205180311"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Aaron Patterson", "Bryan Halmkamp", "Emilio Tagua", "Nick Kallen"]
  s.date = "2014-02-05"
  s.description = "Arel is a SQL AST manager for Ruby. It\n\n1. Simplifies the generation of complex SQL queries\n2. Adapts to various RDBMS systems\n\nIt is intended to be a framework framework; that is, you can build your own ORM\nwith it, focusing on innovative object and collection modeling as opposed to\ndatabase compatibility and query generation."
  s.email = ["aaron@tenderlovemaking.com", "bryan@brynary.com", "miloops@gmail.com", "nick@example.org"]
  s.extra_rdoc_files = ["History.txt", "MIT-LICENSE.txt", "Manifest.txt", "README.markdown"]
  s.files = [".autotest", ".gemtest", ".travis.yml", "Gemfile", "History.txt", "MIT-LICENSE.txt", "Manifest.txt", "README.markdown", "Rakefile", "arel.gemspec", "lib/arel.rb", "lib/arel/alias_predication.rb", "lib/arel/attributes.rb", "lib/arel/attributes/attribute.rb", "lib/arel/compatibility/wheres.rb", "lib/arel/crud.rb", "lib/arel/delete_manager.rb", "lib/arel/deprecated.rb", "lib/arel/expression.rb", "lib/arel/expressions.rb", "lib/arel/factory_methods.rb", "lib/arel/insert_manager.rb", "lib/arel/math.rb", "lib/arel/nodes.rb", "lib/arel/nodes/and.rb", "lib/arel/nodes/ascending.rb", "lib/arel/nodes/binary.rb", "lib/arel/nodes/count.rb", "lib/arel/nodes/delete_statement.rb", "lib/arel/nodes/descending.rb", "lib/arel/nodes/equality.rb", "lib/arel/nodes/extract.rb", "lib/arel/nodes/false.rb", "lib/arel/nodes/function.rb", "lib/arel/nodes/grouping.rb", "lib/arel/nodes/in.rb", "lib/arel/nodes/infix_operation.rb", "lib/arel/nodes/inner_join.rb", "lib/arel/nodes/insert_statement.rb", "lib/arel/nodes/join_source.rb", "lib/arel/nodes/named_function.rb", "lib/arel/nodes/node.rb", "lib/arel/nodes/outer_join.rb", "lib/arel/nodes/over.rb", "lib/arel/nodes/select_core.rb", "lib/arel/nodes/select_statement.rb", "lib/arel/nodes/sql_literal.rb", "lib/arel/nodes/string_join.rb", "lib/arel/nodes/table_alias.rb", "lib/arel/nodes/terminal.rb", "lib/arel/nodes/true.rb", "lib/arel/nodes/unary.rb", "lib/arel/nodes/unqualified_column.rb", "lib/arel/nodes/update_statement.rb", "lib/arel/nodes/values.rb", "lib/arel/nodes/window.rb", "lib/arel/nodes/with.rb", "lib/arel/order_predications.rb", "lib/arel/predications.rb", "lib/arel/select_manager.rb", "lib/arel/sql/engine.rb", "lib/arel/sql_literal.rb", "lib/arel/table.rb", "lib/arel/tree_manager.rb", "lib/arel/update_manager.rb", "lib/arel/visitors.rb", "lib/arel/visitors/bind_visitor.rb", "lib/arel/visitors/depth_first.rb", "lib/arel/visitors/dot.rb", "lib/arel/visitors/ibm_db.rb", "lib/arel/visitors/informix.rb", "lib/arel/visitors/join_sql.rb", "lib/arel/visitors/mssql.rb", "lib/arel/visitors/mysql.rb", "lib/arel/visitors/oracle.rb", "lib/arel/visitors/order_clauses.rb", "lib/arel/visitors/postgresql.rb", "lib/arel/visitors/sqlite.rb", "lib/arel/visitors/to_sql.rb", "lib/arel/visitors/visitor.rb", "lib/arel/visitors/where_sql.rb", "lib/arel/window_predications.rb", "test/attributes/test_attribute.rb", "test/helper.rb", "test/nodes/test_and.rb", "test/nodes/test_as.rb", "test/nodes/test_ascending.rb", "test/nodes/test_bin.rb", "test/nodes/test_count.rb", "test/nodes/test_delete_statement.rb", "test/nodes/test_descending.rb", "test/nodes/test_distinct.rb", "test/nodes/test_equality.rb", "test/nodes/test_extract.rb", "test/nodes/test_false.rb", "test/nodes/test_grouping.rb", "test/nodes/test_infix_operation.rb", "test/nodes/test_insert_statement.rb", "test/nodes/test_named_function.rb", "test/nodes/test_node.rb", "test/nodes/test_not.rb", "test/nodes/test_or.rb", "test/nodes/test_over.rb", "test/nodes/test_select_core.rb", "test/nodes/test_select_statement.rb", "test/nodes/test_sql_literal.rb", "test/nodes/test_sum.rb", "test/nodes/test_table_alias.rb", "test/nodes/test_true.rb", "test/nodes/test_update_statement.rb", "test/nodes/test_window.rb", "test/support/fake_record.rb", "test/test_activerecord_compat.rb", "test/test_attributes.rb", "test/test_crud.rb", "test/test_delete_manager.rb", "test/test_factory_methods.rb", "test/test_insert_manager.rb", "test/test_select_manager.rb", "test/test_table.rb", "test/test_update_manager.rb", "test/visitors/test_bind_visitor.rb", "test/visitors/test_depth_first.rb", "test/visitors/test_dispatch_contamination.rb", "test/visitors/test_dot.rb", "test/visitors/test_ibm_db.rb", "test/visitors/test_informix.rb", "test/visitors/test_join_sql.rb", "test/visitors/test_mssql.rb", "test/visitors/test_mysql.rb", "test/visitors/test_oracle.rb", "test/visitors/test_postgres.rb", "test/visitors/test_sqlite.rb", "test/visitors/test_to_sql.rb"]
  s.homepage = "http://github.com/rails/arel"
  s.licenses = ["MIT"]
  s.rdoc_options = ["--main", "README.markdown"]
  s.rubyforge_project = "arel"
  s.rubygems_version = "2.2.0"
  s.summary = "Arel is a SQL AST manager for Ruby"
  s.test_files = ["test/attributes/test_attribute.rb", "test/nodes/test_and.rb", "test/nodes/test_as.rb", "test/nodes/test_ascending.rb", "test/nodes/test_bin.rb", "test/nodes/test_count.rb", "test/nodes/test_delete_statement.rb", "test/nodes/test_descending.rb", "test/nodes/test_distinct.rb", "test/nodes/test_equality.rb", "test/nodes/test_extract.rb", "test/nodes/test_false.rb", "test/nodes/test_grouping.rb", "test/nodes/test_infix_operation.rb", "test/nodes/test_insert_statement.rb", "test/nodes/test_named_function.rb", "test/nodes/test_node.rb", "test/nodes/test_not.rb", "test/nodes/test_or.rb", "test/nodes/test_over.rb", "test/nodes/test_select_core.rb", "test/nodes/test_select_statement.rb", "test/nodes/test_sql_literal.rb", "test/nodes/test_sum.rb", "test/nodes/test_table_alias.rb", "test/nodes/test_true.rb", "test/nodes/test_update_statement.rb", "test/nodes/test_window.rb", "test/test_activerecord_compat.rb", "test/test_attributes.rb", "test/test_crud.rb", "test/test_delete_manager.rb", "test/test_factory_methods.rb", "test/test_insert_manager.rb", "test/test_select_manager.rb", "test/test_table.rb", "test/test_update_manager.rb", "test/visitors/test_bind_visitor.rb", "test/visitors/test_depth_first.rb", "test/visitors/test_dispatch_contamination.rb", "test/visitors/test_dot.rb", "test/visitors/test_ibm_db.rb", "test/visitors/test_informix.rb", "test/visitors/test_join_sql.rb", "test/visitors/test_mssql.rb", "test/visitors/test_mysql.rb", "test/visitors/test_oracle.rb", "test/visitors/test_postgres.rb", "test/visitors/test_sqlite.rb", "test/visitors/test_to_sql.rb"]

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<minitest>, ["~> 5.2"])
      s.add_development_dependency(%q<rdoc>, ["~> 4.0"])
      s.add_development_dependency(%q<hoe>, ["~> 3.8"])
    else
      s.add_dependency(%q<minitest>, ["~> 5.2"])
      s.add_dependency(%q<rdoc>, ["~> 4.0"])
      s.add_dependency(%q<hoe>, ["~> 3.8"])
    end
  else
    s.add_dependency(%q<minitest>, ["~> 5.2"])
    s.add_dependency(%q<rdoc>, ["~> 4.0"])
    s.add_dependency(%q<hoe>, ["~> 3.8"])
  end
end
