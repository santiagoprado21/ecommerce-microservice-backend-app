# Script para construir y subir una imagen específica a Docker Hub
param(
    [Parameter(Mandatory=$true)]
    [string]$ServiceName,
    [string]$DockerUsername = "selimhorri",
    [string]$Version = "0.1.0"
)

Write-Host "🚀 Construyendo y subiendo $ServiceName..." -ForegroundColor Green

# Verificar si existe el directorio
if (-not (Test-Path $ServiceName)) {
    Write-Host "❌ El directorio '$ServiceName' no existe" -ForegroundColor Red
    exit 1
}

# Cambiar al directorio del servicio
Set-Location $ServiceName

# Nombre de la imagen
$imageName = "$DockerUsername/$ServiceName-ecommerce-boot:$Version"

# Construir la imagen
Write-Host "📦 Construyendo imagen: $imageName" -ForegroundColor Blue
docker build -t $imageName .

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Construcción exitosa" -ForegroundColor Green
    
    # Subir la imagen
    Write-Host "🔄 Subiendo imagen a Docker Hub..." -ForegroundColor Blue
    docker push $imageName
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ ¡Imagen subida exitosamente!" -ForegroundColor Green
        Write-Host "🔗 Imagen disponible en: https://hub.docker.com/r/$DockerUsername/$ServiceName-ecommerce-boot" -ForegroundColor Cyan
    } else {
        Write-Host "❌ Error subiendo la imagen" -ForegroundColor Red
    }
} else {
    Write-Host "❌ Error construyendo la imagen" -ForegroundColor Red
}

# Volver al directorio raíz
Set-Location .. 