SetCompress off

; SetCompressor /SOLID lzma
; SetCompress auto

!ifndef COMPONENT_NAME
  !error "COMPONENT_NAME not defined (Agent or Server)!"
!endif

!ifndef UNINSTALL_MANIFEST_FILE
  !error "UNINSTALL_MANIFEST_FILE not defined"
!endif

!ifndef COMPONENT_FULL_VERSION
  !error "COMPONENT_FULL_VERSION not defined (19.5.0-9280)!"
!endif

!ifndef WINDOWS_PRODUCT_VERSION
  !error "WINDOWS_PRODUCT_VERSION not defined (19.5.0.9280)!"
!endif

!ifndef COMPONENT_VERSION
  !error "COMPONENT_VERSION not defined (19.5.0)!"
!endif

!ifndef COMPONENT_REGISTRY_VERSION
  !error "COMPONENT_REGISTRY_VERSION not defined (19050092800)!"
!endif

!ifndef GOCD_ICON
  !error "GOCD_ICON not defined!"
!endif

!ifndef GOCD_LICENSE_FILE
  !error "GOCD_LICENSE_FILE not defined!"
!endif

!ifndef CUSTOM_PAGES
  !error "CUSTOM_PAGES not defined!"
!endif

!ifndef INSTALLER_CONTENTS
  !error "INSTALLER_CONTENTS not defined!"
!endif

!ifndef ADDITIONAL_PLUGINS_DIR
  !error "ADDITIONAL_PLUGINS_DIR not defined!"
!endif

!ifndef OUTPUT_FILE
  !error "OUTPUT_FILE not defined!"
!endif

!addplugindir "${ADDITIONAL_PLUGINS_DIR}"

!include "MUI.nsh"
!include "LogicLib.nsh"
!include "debug.nsi"
!include "message-helpers.nsi"
!include "service-helpers.nsi"
!include "upgrade-helpers.nsi"
!include "uninstall-helpers.nsi"
!include "cli-helpers.nsi"

OutFile ${OUTPUT_FILE}
Name "GoCD ${COMPONENT_NAME} ${COMPONENT_FULL_VERSION}"
Icon ${GOCD_ICON}
UninstallIcon ${GOCD_ICON}

; Use new visual styles from Windows XP and later
XPStyle on

; User must be admin to run this installer
RequestExecutionLevel admin

; Use these labels for all buttons
MiscButtonText "< &Back" "&Next >" "&Cancel" "&Finish"

; The default installation directory
InstallDir "$PROGRAMFILES\Go ${COMPONENT_NAME}"

; Registry key to check for directory (so if you install again, it will overwrite the old one automatically)
InstallDirRegKey HKLM "Software\ThoughtWorks Studios\Go ${COMPONENT_NAME}" "Install_Dir"

; Set the file to use for the license page
LicenseData ${GOCD_LICENSE_FILE}

; The metadata visible in "File properties" tab
VIAddVersionKey /LANG=0 "ProductName" "GoCD ${COMPONENT_NAME}"
VIAddVersionKey /LANG=0 "ProductVersion" "${COMPONENT_FULL_VERSION}"
VIAddVersionKey /LANG=0 "Comments" "GoCD ${COMPONENT_NAME}"
VIAddVersionKey /LANG=0 "CompanyName" "ThoughtWorks, Inc."
VIAddVersionKey /LANG=0 "LegalCopyright" "Copyright ThoughtWorks, Inc."
VIAddVersionKey /LANG=0 "FileDescription" "GoCD ${COMPONENT_NAME} installer"
VIAddVersionKey /LANG=0 "FileVersion" "1.2.3"

VIProductVersion "${WINDOWS_PRODUCT_VERSION}"
VIFileVersion    "${WINDOWS_PRODUCT_VERSION}"

; show these pages, these are executed in the order they are defined, custom pages, if any come after all the usual installer pages
Page license ; the license page
Page directory skipDirectoryOnUpgrade ; the installation directory

!include "${CUSTOM_PAGES}"

Page instfiles ; show the files being installed

; the uninstaller pages
UninstPage uninstConfirm
UninstPage instfiles

Function "skipDirectoryOnUpgrade"
  ClearErrors
  ReadRegStr $0 HKLM "Software\ThoughtWorks Studios\Go ${COMPONENT_NAME}" "Install_Dir"

  ; If we get an error then the key does not exist and we're doing a clean install
  ; If not we simply hard code the directory (from previous version) and skip the directory selection page

  ${IfNot} ${Errors}
    StrCpy $INSTDIR $0
    Abort ; abort, unlike what the name suggests will just skip the "select install dir" page
  ${EndIf}
FunctionEnd

; this callback function is the `main()` program, is invoked to initialize the installer
Function ".onInit"
  ; clear any previous errors since before `.onInit` was invoked
  InitPluginsDir
  Call OnInitCallback

  StrCpy $GoCDServiceName "Go ${COMPONENT_NAME}"
  ClearErrors

  ; must set output path before enabling logging
  SetOutPath $INSTDIR
  ${LogSet} on

  Call ParseCLI

  ; load install dir from the registry, from a previous install, if any
  ClearErrors
  ReadRegStr $0 HKLM "Software\ThoughtWorks Studios\Go ${COMPONENT_NAME}" "Install_Dir"

  ; If we get an error while reading install dir from registry, then the key does not exist and we're doing a clean install
  ${IfNot} ${Errors}
    ${LogText} "Previous Install dir is $0, checking if upgrade is possible..."
    Call MaybePerformUpgrade
  ${Else}
    ${LogText} "Performing install (not upgrade)"
  ${EndIf}
FunctionEnd

; This section (being the first one, not because it's called Install) will be executed after the `.onInit` callback
Section "Install"
  SectionIn RO

  SetOverWrite on
  SetOutPath $INSTDIR

  ${If} $IsUpgrading == "true"
    ${LogText} "Performing an upgrade"
    Call BeforeUpgrade
    File /r ${GOCD_ICON}
    File /r ${INSTALLER_CONTENTS}\*.*
    Call AfterUpgrade
  ${Else}
    ${LogText} "Performing a fresh install"
    File /r ${GOCD_ICON}
    File /r ${INSTALLER_CONTENTS}\*.*
    Call PostInstall
  ${EndIf}

  Call SetupRegistryKeys
  Call SetupUninstaller
SectionEnd

Function "SetupUninstaller"
  WriteUninstaller "uninstall.exe"

  ; Do the start menu bits
  CreateDirectory "$SMPROGRAMS\Go ${COMPONENT_NAME}"
  CreateShortCut  "$SMPROGRAMS\Go ${COMPONENT_NAME}\Uninstall Go ${COMPONENT_NAME}.lnk" "$INSTDIR\uninstall.exe"
FunctionEnd

Function "SetupRegistryKeys"
  ; Write the installation path into the registry
  WriteRegStr HKLM "SOFTWARE\ThoughtWorks Studios\Go ${COMPONENT_NAME}" "Install_Dir" "$INSTDIR"
  WriteRegStr HKLM "SOFTWARE\ThoughtWorks Studios\Go ${COMPONENT_NAME}" "Version"     "${COMPONENT_FULL_VERSION}"
  WriteRegStr HKLM "SOFTWARE\ThoughtWorks Studios\Go ${COMPONENT_NAME}" "Ver"         "${COMPONENT_REGISTRY_VERSION}"

  ; Write the uninstall keys for Windows
  WriteRegStr   HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Go ${COMPONENT_NAME}" "DisplayName"           "GoCD ${COMPONENT_NAME}"
  WriteRegStr   HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Go ${COMPONENT_NAME}" "UninstallString"       "$\"$INSTDIR\uninstall.exe$\""
  WriteRegStr   HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Go ${COMPONENT_NAME}" "QuietUninstallString"  "$\"$INSTDIR\uninstall.exe$\" /S"
  WriteRegStr   HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Go ${COMPONENT_NAME}" "InstallLocation"       "$\"$INSTDIR$\""
  WriteRegStr   HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Go ${COMPONENT_NAME}" "DisplayIcon"           "$\"$INSTDIR\gocd.ico$\""
  WriteRegStr   HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Go ${COMPONENT_NAME}" "Publisher"             "ThoughtWorks, Inc."
  WriteRegStr   HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Go ${COMPONENT_NAME}" "HelpLink"              "https://docs.gocd.org/${COMPONENT_VERSION}/"
  WriteRegStr   HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Go ${COMPONENT_NAME}" "URLUpdateInfo"          "https://www.gocd.org/"
  WriteRegStr   HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Go ${COMPONENT_NAME}" "URLInfoAbout"          "https://www.gocd.org/"

  WriteRegStr   HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Go ${COMPONENT_NAME}" "DisplayVersion"        "${COMPONENT_FULL_VERSION}"
  WriteRegStr   HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Go ${COMPONENT_NAME}" "VersionMajor"          "${COMPONENT_VERSION}"
  WriteRegStr   HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Go ${COMPONENT_NAME}" "VersionMinor"          "0"

  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Go ${COMPONENT_NAME}" "NoModify"              1
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Go ${COMPONENT_NAME}" "NoRepair"              1
FunctionEnd
