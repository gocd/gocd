module AbstractController
  module Translation
    # Delegates to <tt>I18n.translate</tt>. Also aliased as <tt>t</tt>.
    #
    # When the given key starts with a period, it will be scoped by the current
    # controller and action. So if you call <tt>translate(".foo")</tt> from
    # <tt>PeopleController#index</tt>, it will convert the call to
    # <tt>I18n.translate("people.index.foo")</tt>. This makes it less repetitive
    # to translate many keys within the same controller / action and gives you a
    # simple framework for scoping them consistently.
    def translate(*args)
      key = args.first
      if key.is_a?(String) && (key[0] == '.')
        key = "#{ controller_path.tr('/', '.') }.#{ action_name }#{ key }"
        args[0] = key
      end

      I18n.translate(*args)
    end
    alias :t :translate

    # Delegates to <tt>I18n.localize</tt>. Also aliased as <tt>l</tt>.
    def localize(*args)
      I18n.localize(*args)
    end
    alias :l :localize
  end
end
