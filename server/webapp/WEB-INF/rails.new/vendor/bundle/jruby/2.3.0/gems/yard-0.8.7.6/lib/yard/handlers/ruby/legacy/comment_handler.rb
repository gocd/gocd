# (see Ruby::CommentHandler)
class YARD::Handlers::Ruby::Legacy::CommentHandler < YARD::Handlers::Ruby::Legacy::Base
  handles TkCOMMENT
  namespace_only

  process do
    register_docstring(nil)
  end
end
