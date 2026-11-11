# 启动脚本：读取 .env → 注入环境变量 → 运行 Spring Boot
# 用法：右键“使用 PowerShell 运行”，或  .\run.ps1

$envFile = Join-Path $PSScriptRoot ".env"
if (Test-Path $envFile) {
    Get-Content $envFile -Encoding UTF8 | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
            $idx = $line.IndexOf("=")
            $name = $line.Substring(0, $idx).Trim()
            $value = $line.Substring($idx + 1).Trim().Trim('"')
            [Environment]::SetEnvironmentVariable($name, $value, "Process")
        }
    }
    Write-Host "[OK] 已加载 .env" -ForegroundColor Green
} else {
    Write-Host "[提示] 没有 .env，使用系统环境变量（可复制 .env.example 为 .env）" -ForegroundColor Yellow
}

if (-not $env:DEEPSEEK_API_KEY) {
    Write-Host "[警告] DEEPSEEK_API_KEY 未设置，判定接口会返回 502" -ForegroundColor Red
}

Write-Host "[启动] http://localhost:8080" -ForegroundColor Cyan
mvn spring-boot:run
