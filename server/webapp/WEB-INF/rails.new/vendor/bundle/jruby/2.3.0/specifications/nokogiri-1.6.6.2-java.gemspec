# -*- encoding: utf-8 -*-
# stub: nokogiri 1.6.6.2 java lib

Gem::Specification.new do |s|
  s.name = "nokogiri".freeze
  s.version = "1.6.6.2"
  s.platform = "java".freeze

  s.required_rubygems_version = Gem::Requirement.new(">= 0".freeze) if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib".freeze]
  s.authors = ["Aaron Patterson".freeze, "Mike Dalessio".freeze, "Yoko Harada".freeze, "Tim Elliott".freeze, "Akinori MUSHA".freeze]
  s.date = "2015-01-23"
  s.description = "Nokogiri (\u{92f8}) is an HTML, XML, SAX, and Reader parser.  Among Nokogiri's\nmany features is the ability to search documents via XPath or CSS3 selectors.\n\nXML is like violence - if it doesn\u{2019}t solve your problems, you are not using\nenough of it.".freeze
  s.email = ["aaronp@rubyforge.org".freeze, "mike.dalessio@gmail.com".freeze, "yokolet@gmail.com".freeze, "tle@holymonkey.com".freeze, "knu@idaemons.org".freeze]
  s.executables = ["nokogiri".freeze]
  s.extra_rdoc_files = ["CHANGELOG.ja.rdoc".freeze, "CHANGELOG.rdoc".freeze, "C_CODING_STYLE.rdoc".freeze, "Manifest.txt".freeze, "README.ja.rdoc".freeze, "README.rdoc".freeze, "ROADMAP.md".freeze, "STANDARD_RESPONSES.md".freeze, "Y_U_NO_GEMSPEC.md".freeze, "suppressions/README.txt".freeze, "ext/nokogiri/html_document.c".freeze, "ext/nokogiri/html_element_description.c".freeze, "ext/nokogiri/html_entity_lookup.c".freeze, "ext/nokogiri/html_sax_parser_context.c".freeze, "ext/nokogiri/html_sax_push_parser.c".freeze, "ext/nokogiri/nokogiri.c".freeze, "ext/nokogiri/xml_attr.c".freeze, "ext/nokogiri/xml_attribute_decl.c".freeze, "ext/nokogiri/xml_cdata.c".freeze, "ext/nokogiri/xml_comment.c".freeze, "ext/nokogiri/xml_document.c".freeze, "ext/nokogiri/xml_document_fragment.c".freeze, "ext/nokogiri/xml_dtd.c".freeze, "ext/nokogiri/xml_element_content.c".freeze, "ext/nokogiri/xml_element_decl.c".freeze, "ext/nokogiri/xml_encoding_handler.c".freeze, "ext/nokogiri/xml_entity_decl.c".freeze, "ext/nokogiri/xml_entity_reference.c".freeze, "ext/nokogiri/xml_io.c".freeze, "ext/nokogiri/xml_libxml2_hacks.c".freeze, "ext/nokogiri/xml_namespace.c".freeze, "ext/nokogiri/xml_node.c".freeze, "ext/nokogiri/xml_node_set.c".freeze, "ext/nokogiri/xml_processing_instruction.c".freeze, "ext/nokogiri/xml_reader.c".freeze, "ext/nokogiri/xml_relax_ng.c".freeze, "ext/nokogiri/xml_sax_parser.c".freeze, "ext/nokogiri/xml_sax_parser_context.c".freeze, "ext/nokogiri/xml_sax_push_parser.c".freeze, "ext/nokogiri/xml_schema.c".freeze, "ext/nokogiri/xml_syntax_error.c".freeze, "ext/nokogiri/xml_text.c".freeze, "ext/nokogiri/xml_xpath_context.c".freeze, "ext/nokogiri/xslt_stylesheet.c".freeze]
  s.files = ["CHANGELOG.ja.rdoc".freeze, "CHANGELOG.rdoc".freeze, "C_CODING_STYLE.rdoc".freeze, "Manifest.txt".freeze, "README.ja.rdoc".freeze, "README.rdoc".freeze, "ROADMAP.md".freeze, "STANDARD_RESPONSES.md".freeze, "Y_U_NO_GEMSPEC.md".freeze, "bin/nokogiri".freeze, "ext/nokogiri/html_document.c".freeze, "ext/nokogiri/html_element_description.c".freeze, "ext/nokogiri/html_entity_lookup.c".freeze, "ext/nokogiri/html_sax_parser_context.c".freeze, "ext/nokogiri/html_sax_push_parser.c".freeze, "ext/nokogiri/nokogiri.c".freeze, "ext/nokogiri/xml_attr.c".freeze, "ext/nokogiri/xml_attribute_decl.c".freeze, "ext/nokogiri/xml_cdata.c".freeze, "ext/nokogiri/xml_comment.c".freeze, "ext/nokogiri/xml_document.c".freeze, "ext/nokogiri/xml_document_fragment.c".freeze, "ext/nokogiri/xml_dtd.c".freeze, "ext/nokogiri/xml_element_content.c".freeze, "ext/nokogiri/xml_element_decl.c".freeze, "ext/nokogiri/xml_encoding_handler.c".freeze, "ext/nokogiri/xml_entity_decl.c".freeze, "ext/nokogiri/xml_entity_reference.c".freeze, "ext/nokogiri/xml_io.c".freeze, "ext/nokogiri/xml_libxml2_hacks.c".freeze, "ext/nokogiri/xml_namespace.c".freeze, "ext/nokogiri/xml_node.c".freeze, "ext/nokogiri/xml_node_set.c".freeze, "ext/nokogiri/xml_processing_instruction.c".freeze, "ext/nokogiri/xml_reader.c".freeze, "ext/nokogiri/xml_relax_ng.c".freeze, "ext/nokogiri/xml_sax_parser.c".freeze, "ext/nokogiri/xml_sax_parser_context.c".freeze, "ext/nokogiri/xml_sax_push_parser.c".freeze, "ext/nokogiri/xml_schema.c".freeze, "ext/nokogiri/xml_syntax_error.c".freeze, "ext/nokogiri/xml_text.c".freeze, "ext/nokogiri/xml_xpath_context.c".freeze, "ext/nokogiri/xslt_stylesheet.c".freeze, "suppressions/README.txt".freeze]
  s.homepage = "http://nokogiri.org".freeze
  s.licenses = ["MIT".freeze]
  s.rdoc_options = ["--main".freeze, "README.rdoc".freeze]
  s.rubygems_version = "2.6.13".freeze
  s.summary = "Nokogiri (\u{92f8}) is an HTML, XML, SAX, and Reader parser".freeze

  s.installed_by_version = "2.6.13" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<rdoc>.freeze, ["~> 4.0"])
      s.add_development_dependency(%q<hoe-bundler>.freeze, [">= 1.1"])
      s.add_development_dependency(%q<hoe-debugging>.freeze, ["~> 1.2.0"])
      s.add_development_dependency(%q<hoe-gemspec>.freeze, [">= 1.0"])
      s.add_development_dependency(%q<hoe-git>.freeze, [">= 1.4"])
      s.add_development_dependency(%q<minitest>.freeze, ["~> 2.2.2"])
      s.add_development_dependency(%q<rake>.freeze, [">= 0.9"])
      s.add_development_dependency(%q<rake-compiler>.freeze, ["~> 0.9.2"])
      s.add_development_dependency(%q<racc>.freeze, [">= 1.4.6"])
      s.add_development_dependency(%q<rexical>.freeze, [">= 1.0.5"])
      s.add_development_dependency(%q<hoe>.freeze, ["~> 3.13"])
    else
      s.add_dependency(%q<rdoc>.freeze, ["~> 4.0"])
      s.add_dependency(%q<hoe-bundler>.freeze, [">= 1.1"])
      s.add_dependency(%q<hoe-debugging>.freeze, ["~> 1.2.0"])
      s.add_dependency(%q<hoe-gemspec>.freeze, [">= 1.0"])
      s.add_dependency(%q<hoe-git>.freeze, [">= 1.4"])
      s.add_dependency(%q<minitest>.freeze, ["~> 2.2.2"])
      s.add_dependency(%q<rake>.freeze, [">= 0.9"])
      s.add_dependency(%q<rake-compiler>.freeze, ["~> 0.9.2"])
      s.add_dependency(%q<racc>.freeze, [">= 1.4.6"])
      s.add_dependency(%q<rexical>.freeze, [">= 1.0.5"])
      s.add_dependency(%q<hoe>.freeze, ["~> 3.13"])
    end
  else
    s.add_dependency(%q<rdoc>.freeze, ["~> 4.0"])
    s.add_dependency(%q<hoe-bundler>.freeze, [">= 1.1"])
    s.add_dependency(%q<hoe-debugging>.freeze, ["~> 1.2.0"])
    s.add_dependency(%q<hoe-gemspec>.freeze, [">= 1.0"])
    s.add_dependency(%q<hoe-git>.freeze, [">= 1.4"])
    s.add_dependency(%q<minitest>.freeze, ["~> 2.2.2"])
    s.add_dependency(%q<rake>.freeze, [">= 0.9"])
    s.add_dependency(%q<rake-compiler>.freeze, ["~> 0.9.2"])
    s.add_dependency(%q<racc>.freeze, [">= 1.4.6"])
    s.add_dependency(%q<rexical>.freeze, [">= 1.0.5"])
    s.add_dependency(%q<hoe>.freeze, ["~> 3.13"])
  end
end
