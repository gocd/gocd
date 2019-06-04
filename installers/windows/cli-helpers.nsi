; macros to read and parse CLI args (for slient installation)
!include FileFunc.nsh
!insertmacro GetParameters
!insertmacro GetOptions

Var ARGV

Function "ParseCLI"
  ${GetParameters} $ARGV
  Call DoParseCLI
FunctionEnd
