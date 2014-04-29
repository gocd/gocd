# Generator logic is always loaded because we need it for Enumerator#next.
# However, we namespace it under JRuby normally so it does not show up at
# toplevel until actually required. See JRUBY-5675.
Generator = JRuby::Generator