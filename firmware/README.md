# CRT 下位机固件（STM32F103C8 / Black Pill）

接收上位机通过 UART 发来的单字符指令，驱动 OLED、LED 和蜂鸣器。

## 协议

| 指令 | 含义 | 硬件反应 |
| --- | --- | --- |
| `'O'` / `'o'` | 需求合理 | 绿灯常亮，OLED 显示「需求通过」+ `PASS` |
| `'E'` / `'e'` | 需求离谱 | 红灯闪 6 下 + 蜂鸣器报警，OLED 显示「极度离谱 / 拒绝接入」，结束后红灯常亮 |

串口参数：**9600 bps，8 数据位，无校验，1 停止位**。

## 接线

| 外设 | STM32 引脚 | 说明 |
| --- | --- | --- |
| CH340 RXD | **PA9**（USART1_TX） | 交叉连接 |
| CH340 TXD | **PA10**（USART1_RX） | 交叉连接 |
| CH340 GND | GND | 必须共地 |
| OLED SCL | **PB6** | I2C1，地址 0x78 |
| OLED SDA | **PB7** | |
| 绿灯 | **PB12** | 低电平亮 |
| 红灯 | **PA3** | 低电平亮（LED 接 3.3V 侧） |
| 蜂鸣器 | **PB3** | 低电平触发；无源 TMB12A05 需要方波，本固件用通断报警 |

> PB3 复位后是 JTDO，固件里已调用 `__HAL_AFIO_REMAP_SWJ_NOJTAG()` 关闭 JTAG（保留 SWD），所以烧录/调试不受影响。

## 编译与烧录

```powershell
cd D:\develop\Translator\firmware
pio run                # 编译
pio run -t upload      # 通过 ST-Link 烧录
```

## 不带硬件时测试

用串口助手（或 `pio device monitor`）打开对应 COM 口，波特率 9600：

- 发送 `O` → 绿灯亮 + OLED「需求通过」
- 发送 `E` → 红灯闪 + OLED「极度离谱 / 拒绝接入」
