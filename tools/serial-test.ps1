# 串口快速测试：不启动 Spring Boot，直接往下位机发一个字符，验证接线和屏幕
# 用法：
#   .\serial-test.ps1 -Port COM3 -Cmd O     # 屏幕显示「需求通过」
#   .\serial-test.ps1 -Port COM3 -Cmd E     # 屏幕显示「极度离谱 / 拒绝接入」
#   .\serial-test.ps1                        # 自动选择第一个串口，发 O

param(
    [string]$Port = "",
    [ValidateSet('O', 'E', 'o', 'e')]
    [string]$Cmd = 'O',
    [int]$Baud = 9600
)

if (-not $Port) {
    $ports = [System.IO.Ports.SerialPort]::GetPortNames()
    if (-not $ports -or $ports.Count -eq 0) {
        Write-Host "[错误] 没有检测到串口：请插好 USB-TTL（CH340），确认设备管理器里有 COM 口" -ForegroundColor Red
        exit 1
    }
    $Port = $ports[0]
    Write-Host "[提示] 自动选择串口：$Port" -ForegroundColor Yellow
}

Write-Host "[发送] $Port @ $Baud bps → '$Cmd'" -ForegroundColor Cyan

$sp = New-Object System.IO.Ports.SerialPort($Port, $Baud, [System.IO.Ports.Parity]::None, 8, [System.IO.Ports.StopBits]::One)
try {
    $sp.Open()
    $sp.Write($Cmd)
    Start-Sleep -Milliseconds 300
    Write-Host "[OK] 已发送，看下 OLED 屏幕有没有变化" -ForegroundColor Green
}
catch {
    Write-Host "[失败] $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "        常见原因：串口被 STC-ISP/串口助手占用、COM 口不对、板子没供电" -ForegroundColor Yellow
}
finally {
    if ($sp.IsOpen) { $sp.Close() }
}
