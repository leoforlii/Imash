# Compila la release di Imash firmata con la chiave vera.
# La password viene chiesta qui (non si vede mentre la scrivi), passata a Gradle solo per questa
# compilazione e poi cancellata: non resta su disco né nella cronologia dei comandi.
# Uso:  powershell -ExecutionPolicy Bypass -File firma-release.ps1
$sicura = Read-Host "Password della chiave di Imash" -AsSecureString
$bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($sicura)
try {
    $env:IMASH_FIRMA_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    if (-not $env:JAVA_HOME) { $env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr' }
    # Senza cache di configurazione, così la password non viene scritta nei file di Gradle.
    & "$PSScriptRoot\gradlew.bat" -p "$PSScriptRoot" assembleRelease --no-configuration-cache
    $esito = $LASTEXITCODE
    # Ferma il demone di Gradle, che altrimenti terrebbe la password in memoria.
    & "$PSScriptRoot\gradlew.bat" -p "$PSScriptRoot" --stop | Out-Null
    if ($esito -eq 0) {
        $apk = "$PSScriptRoot\app\build\outputs\apk\release\app-release.apk"
        # Copia comoda sul Desktop (anche se il Desktop è su OneDrive).
        $desktop = Join-Path ([Environment]::GetFolderPath('Desktop')) 'Imash.apk'
        Copy-Item $apk $desktop -Force
        Write-Host "`nFatto: APK firmato in $apk"
        Write-Host "Copia sul Desktop: $desktop"
    } else {
        Write-Host "`nCompilazione non riuscita (password sbagliata?)."
    }
} finally {
    Remove-Item Env:IMASH_FIRMA_PASSWORD -ErrorAction SilentlyContinue
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
}
