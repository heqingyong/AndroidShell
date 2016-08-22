jarsigner -verbose -keystore debug.keystore -storepass 123456 -signedjar AndroidShell_signed.apk -digestalg SHA1 -sigalg MD5withRSA AndroidProtectionShell.apk debug
pause