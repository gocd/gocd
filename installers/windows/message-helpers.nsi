!define ShowMessageAndExit "!insertmacro ShowMessageAndExitMacro"
!macro ShowMessageAndExitMacro INPUT_TEXT
  ${LogText} "${INPUT_TEXT}"
  ${IfNot} ${Silent}
    MessageBox MB_ICONSTOP "${INPUT_TEXT}"
  ${EndIf}
  Quit
!macroend
