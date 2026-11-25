$ErrorActionPreference = 'Stop'
$proyecto = $PSScriptRoot
$jdk = 'C:\Program Files\Java\jdk-22\bin'
$nombre = 'Hospedaje Supply'

if (Get-Process java, javaw, "$nombre" -ErrorAction SilentlyContinue | Where-Object MainWindowTitle -like "$nombre*") {
    Write-Host "Cierra primero el programa Hospedaje Supply y vuelve a ejecutar este script." -ForegroundColor Yellow
    exit 1
}

Write-Host "1/3 Compilando el programa..." -ForegroundColor Cyan
& "$proyecto\mvnw.cmd" -q -o package -DskipTests
if ($LASTEXITCODE -ne 0) { throw "No se pudo compilar." }

$entrada = Join-Path $proyecto 'empaquetado\entrada'
$destino = Join-Path $proyecto 'empaquetado\programa'
Remove-Item -Recurse -Force $entrada, (Join-Path $destino $nombre) -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force $entrada, $destino | Out-Null
Copy-Item (Get-ChildItem "$proyecto\target\alquila-facil-*.jar" | Select-Object -First 1).FullName (Join-Path $entrada 'HospedajeSupply.jar')

Write-Host "2/3 Creando Hospedaje Supply.exe (tarda 1 o 2 minutos)..." -ForegroundColor Cyan
$carpetaDatos = $proyecto -replace '\\', '/'
& "$jdk\jpackage.exe" --type app-image --name $nombre `
    --input $entrada --main-jar HospedajeSupply.jar `
    --main-class org.springframework.boot.loader.launch.JarLauncher `
    --dest $destino --app-version 1.0.0 --vendor $nombre `
    --add-modules 'java.se,jdk.unsupported,jdk.localedata,jdk.charsets,jdk.zipfs,jdk.crypto.ec' `
    --java-options "-Dapp.carpeta=$carpetaDatos" `
    --java-options '-Dfile.encoding=UTF-8'
if ($LASTEXITCODE -ne 0) { throw "jpackage fallo." }

Write-Host "3/3 Creando el acceso directo en el escritorio..." -ForegroundColor Cyan
$exe = Join-Path $destino "$nombre\$nombre.exe"
$escritorio = [Environment]::GetFolderPath('Desktop')
$ws = New-Object -ComObject WScript.Shell
$acceso = $ws.CreateShortcut((Join-Path $escritorio "$nombre.lnk"))
$acceso.TargetPath = $exe
$acceso.WorkingDirectory = $proyecto
$acceso.IconLocation = "$exe,0"
$acceso.Description = 'Alquileres y hospedaje'
$acceso.Save()

Write-Host "Listo: $exe" -ForegroundColor Green
Write-Host "Acceso directo: $escritorio\$nombre.lnk" -ForegroundColor Green
