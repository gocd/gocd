;
; Copyright 2016 ThoughtWorks, Inc.
;
; Licensed under the Apache License, Version 2.0 (the "License");
; you may not use this file except in compliance with the License.
; You may obtain a copy of the License at
;
;    http://www.apache.org/licenses/LICENSE-2.0
;
; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.
;

; Compressor issues with prompts, so leave it off...
; SetCompressor /SOLID lzma

MiscButtonText "< &Back" "&Next >" "&Cancel" "&Finish"

; The name of the installer
Name "Go $%NAME% $%VERSION%"

; The file to write
OutFile "$%OUTDIR%\go-$%MODULE%-$%VERSION%-setup.exe"

; The default installation directory
InstallDir "$PROGRAMFILES\Go $%NAME%"

; Registry key to check for directory (so if you install again, it will overwrite the old one automatically)
InstallDirRegKey HKLM "Software\ThoughtWorks Studios\Go $%NAME%" "Install_Dir"

Var UPGRADING
Var IsUpgrading

; Load our plugins
!addplugindir ..\..\..\..\windows-shared\nsis-plugins

!include FileFunc.nsh
!include LogicLib.nsh

!insertmacro GetParameters
!insertmacro GetOptions

ReserveFile "${NSISDIR}\Plugins\InstallOptions.dll"
ReserveFile "JavaHome.ini"

; Pages
Page directory skipDirectoryOnUpgrade
Page custom AskUserForJavaHome HandleGoJavaHomeSelection
Page instfiles

UninstPage uninstConfirm
UninstPage instfiles

RequestExecutionLevel admin

Icon $%GO_ICON%
UninstallIcon $%GO_ICON%

XPStyle on

;--------------------------------
; Make logging conditional. Inspiration: http://nsis.sourceforge.net/Logging:Enable_Logs_Quickly
; NSIS has to be *compiled* with logging enabled. This flag allows logging to be disabled
; during installer creation, so that the default nsis package in debian can be used to create
; and installer, which does not create an install.log file.
!define LogSet "!insertmacro LogSetMacro"
!macro LogSetMacro SETTING
  !if "$%DISABLE_LOGGING%" != "true"
    LogSet ${SETTING}
  !endif
!macroend

!define LogText "!insertmacro LogTextMacro"
!macro LogTextMacro INPUT_TEXT
  !if "$%DISABLE_LOGGING%" != "true"
    LogText $INPUT_TEXT
  !endif
!macroend
;--------------------------------

;--------------------------------
Function .onInit
    ; Check if go is already installed - if it exists no errors is set
    ClearErrors
    ReadRegStr $0 HKLM "Software\ThoughtWorks Studios\Go $%NAME%" "Install_Dir"
    StrCpy $UPGRADING "TRUE"

    IfSilent TURN_ON_LOGGING TURN_OFF_LOGGING
      TURN_ON_LOGGING:
                  SetOutPath $INSTDIR
                  ${LogSet} on
      TURN_OFF_LOGGING:

   ; If we get an error then the key does not exist and we're doing a clean install
    IfErrors Go_not_found
        ReadRegStr $1 HKLM "Software\ThoughtWorks Studios\Go $%NAME%" "Ver"
        ReadRegStr $2 HKLM "Software\ThoughtWorks Studios\Go $%NAME%" "Version"
        IntCmp $1 $%REGVER% issame isnewer isolder

        issame:
            IfSilent IsSameSilentLabel IsSameNonSilentLabel
	        IsSameSilentLabel:
	            ${LogText} "Go $%NAME% $%VERSION% is already installed."
                Goto IsSameDone
            IsSameNonSilentLabel:
                MessageBox MB_OK "Go $%NAME% $%VERSION% is already installed."
		        Goto IsSameDone
	        IsSameDone:
                Goto dontupgrade

        isnewer:
            IfSilent IsNewerSilentLabel IsNewerNonSilentLabel
            IsNewerSilentLabel:
                ${LogText} "Go $%NAME% upgraded from $2 to $%VERSION%"
                Goto upgrade
            IsNewerNonSilentLabel:
                MessageBox MB_YESNO "This will upgrade Go $%NAME% from $2 to $%VERSION%.$\r$\nMake sure you have backups before doing this!$\r$\nDo you want to continue?" IDYES upgrade IDNO dontupgrade

        isolder:
            IfSilent IsOlderSilentLabel IsOlderNonSilentLabel
            IsOlderSilentLabel:
                ${LogText} "Go $%NAME% $2 is installed, and you are trying to install an older version ($%VERSION%).This is not supported."
                Goto IsOlderDone
            IsOlderNonSilentLabel:
                MessageBox MB_OK "Go $%NAME% $2 is installed, and you are trying to install an older version ($%VERSION%).$\r$\nThis is not supported."
		        Goto IsOlderDone
	        IsOlderDone:
                Goto dontupgrade

        dontupgrade:
            Quit

        upgrade:
            StrCpy $IsUpgrading $UPGRADING
            ExecWait 'net stop "Go $%NAME%"'
    Goto done

    Go_not_found:
    ; Check if cruise exists - if it exists no errors
    ClearErrors
    ReadRegStr $0 HKLM "Software\ThoughtWorks Studios\Cruise $%NAME%" "Install_Dir"

    ; If we get an error then the key does not exist and we're doing a clean install
    IfErrors done
        ReadRegStr $1 HKLM "Software\ThoughtWorks Studios\Cruise $%NAME%" "Ver"
        ReadRegStr $2 HKLM "Software\ThoughtWorks Studios\Cruise $%NAME%" "Version"
        IntCmp $1 $%REGVER% isCruise

        isCruise:
            IfSilent IsCruiseSilentLabel IsCruiseNonSilentLabel
            IsCruiseSilentLabel:
                ${LogText} "Go $%NAME% upgrade from $2 to $%VERSION%"
                Goto upgradeToGo
            IsCruiseNonSilentLabel:
                MessageBox MB_YESNO "This will upgrade Go $%NAME% from $2 to $%VERSION%.$\r$\nMake sure you have backups before doing this!$\r$\nDo you want to continue?" IDYES upgradeToGo IDNO dontupgradeToGo

        dontupgradeToGo:
            Quit

        upgradeToGo:
            StrCpy $IsUpgrading $UPGRADING
            ExecWait 'net stop "Cruise $%NAME%"'

    done:
        Call CustomOnInit
FunctionEnd

; If we are upgrading, we don't allow people to change the directory
Function skipDirectoryOnUpgrade
    ; check if go is already installed
    ClearErrors
    ReadRegStr $0 HKLM "Software\ThoughtWorks Studios\Go $%NAME%" "Install_Dir"

    ; If we get an error then the key does not exist and we're doing a clean install
    IfErrors Go_not_found
        ; If not we simply hard code the directory and skip the directory selection page
        StrCpy $INSTDIR $0
        Abort
    Goto done

    Go_not_found:
        ; check if cruise is already installed
        ClearErrors
        ReadRegStr $0 HKLM "Software\ThoughtWorks Studios\Cruise $%NAME%" "Install_Dir"

        ; If we get an error then the key does not exist and we're doing a clean install
        IfErrors done
            ; If not we simply hard code the directory and skip the directory selection page
            StrCpy $INSTDIR $0
            ; Uninstall the old version
            ; Stop and remove
            ExecWait '"$INSTDIR\cruisewrapper.exe" --remove "$INSTDIR\config\wrapper-$%NAME%.conf"'

            ; Remove registry keys
            DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Cruise $%NAME%"
            DeleteRegKey HKLM "SOFTWARE\ThoughtWorks Studios\Cruise $%NAME%"

            ; Remove Env Vars
            DeleteRegValue HKLM "SYSTEM\CurrentControlSet\Control\Session Manager\Environment" "CRUISE_SERVER_DIR"

            SimpleFC::RemovePort 8153 6
            SimpleFC::RemovePort 8154 6

            Sleep 10000

            ; Remove files and uninstaller
            Delete $INSTDIR\uninstall.exe

            ; Remove directories used
            RMDir /r $INSTDIR\apache-ant-1.7.0
            RMDir /r $INSTDIR\jre
            RMDir /r $INSTDIR\lib
            Delete $INSTDIR\*.*

            RMDir /r "$SMPROGRAMS\Cruise $%NAME%"
            Delete "$DESKTOP\Cruise $%NAME%.url"
            Abort
    done:

FunctionEnd

Var USE_BUNDLED_JAVA
Var JAVA_HOME_SET_BY_USER
Var GO_$%NAME%_JAVA_HOME

Function AskUserForJavaHome
    ReadRegStr $GO_$%NAME%_JAVA_HOME HKLM "SYSTEM\CurrentControlSet\Control\Session Manager\Environment" "GO_$%NAME%_JAVA_HOME"
    StrCmp $GO_$%NAME%_JAVA_HOME "" 0 done
    Push $R0
    InstallOptions::dialog $PLUGINSDIR\JavaHome.ini
    Pop $R0
    done:
FunctionEnd

Function HandleGoJavaHomeSelection
    ReadINIStr $0 "$PLUGINSDIR\JavaHome.ini" "Settings" "State"
    StrCmp $0 0 SetGoJavaHomeBasedOnUserOption
    StrCmp $0 1 DisableDirectoryInput
    StrCmp $0 2 EnableDirectoryInput
    Abort

DisableDirectoryInput:
    StrCpy $1 0
    StrCpy $2 "DISABLED"
    Call toggleDirectoryInput
    Abort

EnableDirectoryInput:
    StrCpy $1 1
    StrCpy $2 ""
    Call toggleDirectoryInput
    Abort

SetGoJavaHomeBasedOnUserOption:
    ReadINIStr $USE_BUNDLED_JAVA "$PLUGINSDIR\JavaHome.ini" "Field 1" "State"
    ReadINIStr $JAVA_HOME_SET_BY_USER "$PLUGINSDIR\JavaHome.ini" "Field 3" "State"
    StrCmp $GO_$%NAME%_JAVA_HOME "" 0 done
    ${If} $USE_BUNDLED_JAVA == 1
        StrCpy $GO_$%NAME%_JAVA_HOME "$INSTDIR\jre"
    ${Else}
        StrCpy $GO_$%NAME%_JAVA_HOME "$JAVA_HOME_SET_BY_USER"
    ${EndIf}
    done:
FunctionEnd

Function toggleDirectoryInput
    ReadINIStr $3 "$PLUGINSDIR\JavaHome.ini" "Field 3" "HWND"
    EnableWindow $3 $1
    ReadINIStr $3 "$PLUGINSDIR\JavaHome.ini" "Field 3" "HWND2"
    EnableWindow $3 $1
    WriteINIStr "$PLUGINSDIR\JavaHome.ini" "Field 3" "Flags" "$2"
FunctionEnd

; The stuff to install
Section "Install"
    SectionIn RO

    StrCmp $IsUpgrading $UPGRADING upgrade install

    upgrade:
        ; Don't do anything special anymore - upgrade logic is now in code. Do the same as we would for an install.

    install:
        SetOverWrite on

        ; Set output path to the installation directory.
        SetOutPath $INSTDIR

        ; Put file there
        ; This is where all your data files come from
        File /r $%BINARY_SOURCE_DIR%\*.*

    done:
    SetOverWrite on
    ; Uncomment line below to enable NSIS logging
    ${LogSet} on

    SetOutPath $INSTDIR\$%JAVA%
    File /r $%JAVASRC%\*.*

    ; Write the installation path into the registry
    WriteRegStr HKLM "SOFTWARE\ThoughtWorks Studios\Go $%NAME%" "Install_Dir" "$INSTDIR"
    WriteRegStr HKLM "SOFTWARE\ThoughtWorks Studios\Go $%NAME%" "Version" "$%VERSION%"
    WriteRegStr HKLM "SOFTWARE\ThoughtWorks Studios\Go $%NAME%" "Ver" "$%REGVER%"

    ; Write the uninstall keys for Windows
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Go $%NAME%" "DisplayName" "Go $%NAME%"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Go $%NAME%" "DisplayVersion" "$%VERSION%"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Go $%NAME%" "HelpLink" "http://www.thoughtworks.com/products/support"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Go $%NAME%" "InstallLocation" "$INSTDIR"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Go $%NAME%" "URLInfoAbout" "http://www.thoughtworks.com/products/go-continuous-delivery/"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Go $%NAME%" "UninstallString" '"$INSTDIR\uninstall.exe"'
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Go $%NAME%" "DisplayIcon" '"$INSTDIR\go.ico"'
    WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Go $%NAME%" "NoModify" 1
    WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Go $%NAME%" "NoRepair" 1
    WriteUninstaller "uninstall.exe"

    ; Do the start menu bits
    CreateDirectory "$SMPROGRAMS\Go $%NAME%"
    CreateShortCut "$SMPROGRAMS\Go $%NAME%\Uninstall Go $%NAME%.lnk" "$INSTDIR\uninstall.exe"
SectionEnd
