// CRT 下位机：UART1(9600) 接收 'O'/'E'，驱动 OLED、LED、蜂鸣器
// 绿灯 PC13，红灯 PA3（均低电平亮）；蜂鸣器 PB3；OLED PB6/PB7；串口 PA9/PA10
#include "stm32f1xx_hal.h"
#include "oled.h"
#include "crt_font.h"

/* ======================== 引脚 ======================== */
#define LED_GREEN_PORT   GPIOC
#define LED_GREEN_PIN    GPIO_PIN_13
#define LED_GREEN_ON     GPIO_PIN_RESET   /* 低电平亮（若接反就改成 GPIO_PIN_SET） */
#define LED_GREEN_OFF    GPIO_PIN_SET

#define LED_RED_PORT     GPIOA
#define LED_RED_PIN      GPIO_PIN_3
#define LED_RED_ON       GPIO_PIN_RESET   /* 实测低电平亮 */
#define LED_RED_OFF      GPIO_PIN_SET

#define BUZZER_PORT      GPIOB
#define BUZZER_PIN       GPIO_PIN_3
#define BUZZER_ON        GPIO_PIN_RESET
#define BUZZER_OFF       GPIO_PIN_SET

// 串口波特率（上位机 CRT_SERIAL_BAUD 需一致）
#define CRT_BAUD_RATE    9600u

/* ======================== 串口 ======================== */
I2C_HandleTypeDef hi2c1;

static volatile char    g_cmd = 0;      /* 收到的指令字符 */
static volatile uint8_t g_cmd_ready = 0;

/* ======================== 16x16 中文绘制 ======================== */
static void draw_glyph16(uint8_t x, uint8_t y, const uint8_t *glyph)
{
    uint8_t col, row;

    if (x > (OLED_WIDTH - 16) || y > (OLED_HEIGHT - 16)) {
        return;
    }
    for (col = 0; col < 16; col++) {
        uint8_t top = glyph[col];
        uint8_t bottom = glyph[col + 16];
        for (row = 0; row < 8; row++) {
            if (top & (1u << row)) {
                OLED_DrawPoint((uint8_t)(x + col), (uint8_t)(y + row), 1);
            }
            if (bottom & (1u << row)) {
                OLED_DrawPoint((uint8_t)(x + col), (uint8_t)(y + row + 8), 1);
            }
        }
    }
}

static void draw_line16(uint8_t x, uint8_t y, const uint8_t *const *glyphs, uint8_t count)
{
    uint8_t i;
    for (i = 0; i < count; i++) {
        draw_glyph16((uint8_t)(x + i * 16u), y, glyphs[i]);
    }
}

/* 每行 4 个汉字，宽 64 像素 → 居中 x = 32 */
static const uint8_t *const LINE_OK[4]    = {CRT_XU, CRT_QIU, CRT_TONG, CRT_GUO};
static const uint8_t *const LINE_ERR_1[4] = {CRT_JI, CRT_DU, CRT_LI, CRT_PU};
static const uint8_t *const LINE_ERR_2[4] = {CRT_JU, CRT_JUE, CRT_JIE, CRT_RU};

/* ======================== 屏幕 ======================== */
static void screen_idle(void)
{
    OLED_Clear();
    OLED_ShowString(52, 0, "CRT");
    OLED_ShowString(36, 48, "WAITING");
    OLED_Refresh();
}

static void screen_ok(void)
{
    OLED_Clear();
    OLED_ShowString(52, 0, "CRT");
    draw_line16(32, 20, LINE_OK, 4);      /* 需求通过 */
    OLED_ShowString(48, 48, "PASS");
    OLED_Refresh();
}

static void screen_error(void)
{
    OLED_Clear();
    OLED_ShowString(52, 0, "CRT");
    draw_line16(32, 16, LINE_ERR_1, 4);   /* 极度离谱 */
    draw_line16(32, 34, LINE_ERR_2, 4);   /* 拒绝接入 */
    OLED_Refresh();
}

/* ======================== 报警 ======================== */
/* 红灯（PA4）为主；同时让板载绿灯（PC13）一起闪，作为红灯故障时的保底提示 */
static void alarm_sequence(void)
{
    uint8_t i;
    for (i = 0; i < 6u; i++) {
        HAL_GPIO_WritePin(LED_RED_PORT, LED_RED_PIN, LED_RED_ON);
        HAL_GPIO_WritePin(LED_GREEN_PORT, LED_GREEN_PIN, LED_GREEN_ON);
        HAL_GPIO_WritePin(BUZZER_PORT, BUZZER_PIN, BUZZER_ON);
        HAL_Delay(120);
        HAL_GPIO_WritePin(LED_RED_PORT, LED_RED_PIN, LED_RED_OFF);
        HAL_GPIO_WritePin(LED_GREEN_PORT, LED_GREEN_PIN, LED_GREEN_OFF);
        HAL_GPIO_WritePin(BUZZER_PORT, BUZZER_PIN, BUZZER_OFF);
        HAL_Delay(120);
    }
    /* 报警结束：红灯常亮表示"拒绝"，绿灯熄灭 */
    HAL_GPIO_WritePin(LED_RED_PORT, LED_RED_PIN, LED_RED_ON);
    HAL_GPIO_WritePin(LED_GREEN_PORT, LED_GREEN_PIN, LED_GREEN_OFF);
}

/* ======================== 指令处理 ======================== */
static void handle_command(char cmd)
{
    if (cmd == 'O' || cmd == 'o') {
        HAL_GPIO_WritePin(LED_RED_PORT, LED_RED_PIN, LED_RED_OFF);
        HAL_GPIO_WritePin(BUZZER_PORT, BUZZER_PIN, BUZZER_OFF);
        HAL_GPIO_WritePin(LED_GREEN_PORT, LED_GREEN_PIN, LED_GREEN_ON);
        screen_ok();
    } else if (cmd == 'E' || cmd == 'e') {
        HAL_GPIO_WritePin(LED_GREEN_PORT, LED_GREEN_PIN, LED_GREEN_OFF);
        screen_error();
        alarm_sequence();
    }
}

/* ======================== 初始化 ======================== */
static void MX_GPIO_Init(void)
{
    GPIO_InitTypeDef gpio = {0};

    __HAL_RCC_GPIOA_CLK_ENABLE();
    __HAL_RCC_GPIOB_CLK_ENABLE();
    __HAL_RCC_GPIOC_CLK_ENABLE();

    /* PB3 复位后是 JTDO，关掉 JTAG（保留 SWD）才能当普通 IO */
    __HAL_RCC_AFIO_CLK_ENABLE();
    __HAL_AFIO_REMAP_SWJ_NOJTAG();

    /* 蜂鸣器 PB3 */
    HAL_GPIO_WritePin(BUZZER_PORT, BUZZER_PIN, BUZZER_OFF);
    gpio.Pin = BUZZER_PIN;
    gpio.Mode = GPIO_MODE_OUTPUT_PP;
    gpio.Pull = GPIO_NOPULL;
    HAL_GPIO_Init(BUZZER_PORT, &gpio);

    /* 红灯 PA3 */
    HAL_GPIO_WritePin(LED_RED_PORT, LED_RED_PIN, LED_RED_OFF);
    gpio.Pin = LED_RED_PIN;
    HAL_GPIO_Init(LED_RED_PORT, &gpio);

    /* 绿灯 PC13 */
    HAL_GPIO_WritePin(LED_GREEN_PORT, LED_GREEN_PIN, LED_GREEN_OFF);
    gpio.Pin = LED_GREEN_PIN;
    HAL_GPIO_Init(LED_GREEN_PORT, &gpio);
}

static void MX_USART1_Init(void)
{
    GPIO_InitTypeDef gpio = {0};

    __HAL_RCC_USART1_CLK_ENABLE();
    __HAL_RCC_GPIOA_CLK_ENABLE();

    /* PA9 = TX */
    gpio.Pin = GPIO_PIN_9;
    gpio.Mode = GPIO_MODE_AF_PP;
    gpio.Speed = GPIO_SPEED_FREQ_HIGH;
    HAL_GPIO_Init(GPIOA, &gpio);

    /* PA10 = RX */
    gpio.Pin = GPIO_PIN_10;
    gpio.Mode = GPIO_MODE_INPUT;
    gpio.Pull = GPIO_PULLUP;
    HAL_GPIO_Init(GPIOA, &gpio);

    USART1->BRR = (uint16_t)((SystemCoreClock + (CRT_BAUD_RATE / 2u)) / CRT_BAUD_RATE);
    USART1->CR1 = USART_CR1_UE | USART_CR1_TE | USART_CR1_RE | USART_CR1_RXNEIE;

    HAL_NVIC_SetPriority(USART1_IRQn, 2, 0);
    HAL_NVIC_EnableIRQ(USART1_IRQn);
}

static void MX_I2C1_Init(void)
{
    GPIO_InitTypeDef gpio = {0};

    __HAL_RCC_GPIOB_CLK_ENABLE();
    __HAL_RCC_I2C1_CLK_ENABLE();

    gpio.Pin = GPIO_PIN_6 | GPIO_PIN_7;
    gpio.Mode = GPIO_MODE_AF_OD;
    gpio.Pull = GPIO_PULLUP;
    gpio.Speed = GPIO_SPEED_FREQ_HIGH;
    HAL_GPIO_Init(GPIOB, &gpio);

    hi2c1.Instance = I2C1;
    hi2c1.Init.ClockSpeed = 400000;
    hi2c1.Init.DutyCycle = I2C_DUTYCYCLE_2;
    hi2c1.Init.OwnAddress1 = 0;
    hi2c1.Init.AddressingMode = I2C_ADDRESSINGMODE_7BIT;
    hi2c1.Init.DualAddressMode = I2C_DUALADDRESS_DISABLE;
    hi2c1.Init.OwnAddress2 = 0;
    hi2c1.Init.GeneralCallMode = I2C_GENERALCALL_DISABLE;
    hi2c1.Init.NoStretchMode = I2C_NOSTRETCH_DISABLE;
    (void)HAL_I2C_Init(&hi2c1);
}

/* ======================== 中断 ======================== */
void USART1_IRQHandler(void)
{
    if ((USART1->SR & USART_SR_RXNE) != 0u) {
        char c = (char)(USART1->DR & 0xFFu);   /* 读 DR 同时清 RXNE */

        /* 回显：收到什么就发回什么，用于确认 PC ↔ 板子 的串口是通的 */
        while ((USART1->SR & USART_SR_TXE) == 0u) {
        }
        USART1->DR = (uint8_t)c;

        g_cmd = c;
        g_cmd_ready = 1;
    }
}

/* ======================== 主程序 ======================== */
int main(void)
{
    HAL_Init();

    MX_GPIO_Init();
    MX_USART1_Init();
    MX_I2C1_Init();

    /* 开机自检：绿灯、红灯各亮 300ms（确认 LED 接线/极性） */
    HAL_GPIO_WritePin(LED_GREEN_PORT, LED_GREEN_PIN, LED_GREEN_ON);
    HAL_Delay(300);
    HAL_GPIO_WritePin(LED_GREEN_PORT, LED_GREEN_PIN, LED_GREEN_OFF);
    HAL_GPIO_WritePin(LED_RED_PORT, LED_RED_PIN, LED_RED_ON);
    HAL_Delay(300);
    HAL_GPIO_WritePin(LED_RED_PORT, LED_RED_PIN, LED_RED_OFF);

    OLED_Init();
    screen_idle();

    while (1) {
        if (g_cmd_ready) {
            char cmd = g_cmd;
            g_cmd_ready = 0;
            handle_command(cmd);
        }
    }
}

/* HAL 需要 SysTick 自增 */
void SysTick_Handler(void)
{
    HAL_IncTick();
}

void SystemClock_Config(void)
{
}
