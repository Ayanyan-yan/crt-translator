/**
 * oled.h -- 0.96 寸 OLED（SSD1306，128x64，I2C）STM32 HAL 驱动
 * PlatformIO / stm32cube 工程版本（本工程没有 main.h，所以直接包含 stm32f1xx_hal.h）
 *
 * 特点：RAM 显存（1024 字节）+ 整屏刷新，绘制时不碰总线，画面不会闪。
 * 用法：初始化后调用绘制函数修改显存，最后统一调用 OLED_Refresh() 刷到屏幕。
 */
#ifndef __OLED_H
#define __OLED_H

#include "stm32f1xx_hal.h"

/* ============================ 按你的工程修改 ============================ */
#define OLED_I2C_HANDLE   hi2c1   /* I2C 句柄名（本工程在 LED_light.c 里定义） */
#define OLED_I2C_ADDR     0x78    /* 8 位写地址 = 7 位 0x3C << 1；个别模块是 0x7A（0x3D） */
#define OLED_I2C_TIMEOUT  20      /* 单次 I2C 传输超时（ms） */
/* ======================================================================= */

#define OLED_WIDTH   128
#define OLED_HEIGHT  64
#define OLED_PAGES   (OLED_HEIGHT / 8)

/* 初始化（会先清屏） */
void OLED_Init(void);

/* 显存操作：只改 RAM，不刷屏 */
void OLED_Clear(void);                              /* 全部熄灭（需 Refresh 生效） */
void OLED_ClearPages(uint8_t page, uint8_t count);  /* 只清若干页（滚动字幕用） */
void OLED_Fill(uint8_t color);                      /* color=1 全亮，0 全灭 */
void OLED_DrawPoint(uint8_t x, uint8_t y, uint8_t color);

/* 文本：x 为像素列（0~120），y 为像素行，必须是 8 的倍数且 <= 48（字高 16） */
void OLED_ShowChar(uint8_t x, uint8_t y, char ch);
void OLED_ShowString(uint8_t x, uint8_t y, const char *str);
/* 同 OLED_ShowString，但 x 可以是负数/超出屏宽，越界部分自动裁剪（滚动字幕用） */
void OLED_ShowStringAt(int16_t x, uint8_t y, const char *str);
void OLED_ShowNum(uint8_t x, uint8_t y, int32_t num, uint8_t len);

/* 把显存刷到 OLED：整屏约 24ms @400kHz；OLED_RefreshPages 可只刷若干页（约 3ms/页） */
void OLED_Refresh(void);
void OLED_RefreshPages(uint8_t page, uint8_t count);

#endif /* __OLED_H */
