try {
    $cim = Get-CimInstance Win32_Process -ErrorAction Stop
} catch {
    $cim = $null
}

if ($cim) {
    $candidates = $cim | Where-Object { $_.Name -match 'java' -and ($_.CommandLine -match 'h2|H2|H2Console|nbcode|jdt.ls|Code|vsc|language') }
} else {
    $candidates = Get-Process -Name java,javaw -ErrorAction SilentlyContinue
}

if (-not $candidates -or $candidates.Count -eq 0) {
    Write-Host "No se detectaron procesos Java sospechosos."
} else {
    Write-Host "Procesos Java detectados que podrían bloquear archivos (lista):"
    $candidates | Format-Table ProcessId, Name, CommandLine -AutoSize
    Write-Host "\n¿Detener estos procesos? (y/N)"
    $answer = Read-Host
    if ($answer -match '^[yY]') {
        foreach ($p in $candidates) {
            try {
                Stop-Process -Id $p.ProcessId -Force -ErrorAction SilentlyContinue
                Write-Host "Detenido PID $($p.ProcessId)"
            } catch {
                Write-Host "No se pudo detener PID $($p.ProcessId): $_"
            }
        }
    } else {
        Write-Host "No se detuvieron procesos. Continúo con la limpieza de target."
    }
}

if (Test-Path .\target) {
    Remove-Item -Recurse -Force .\target -ErrorAction SilentlyContinue
    Write-Host "Directorio target eliminado (si existía)."
} else {
    Write-Host "No existe target."
}

Write-Host "Pre-limpieza completada. Ahora puedes ejecutar la tarea 'Maven: clean test'."
