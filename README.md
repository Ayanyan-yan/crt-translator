# 赛博需求翻译机 · CRT (Cyber Requirement Translator)

一款专治"产品经理离谱需求"的 AIoT 软硬协同终端：

> 输入需求 → AI 判定离谱程度 → 串口下发 `O` / `E` → STM32 亮绿灯或红灯狂闪报警。

## 架构

```
浏览器（文字 / 语音输入）
        │  POST /api/judge
        ▼
Spring Boot 上位机（本项目）
        │  ① 调 DeepSeek 判定
        │  ② 解析结论 OK / ABSURD
        ▼
      串口（UART 9600 8N1）
        │  'O' = 合理      'E' = 离谱
        ▼
STM32 下位机
        ├─ 绿灯亮 + OLED「需求通过」
        └─ 红灯闪烁 + 蜂鸣器报警 + OLED「极度离谱，拒绝接入」
```

## 快速开始

### 1. 环境

- JDK 21
- Maven 3.9+

### 2. 配置 AI Key

**不要把 Key 写进代码或提交到仓库。** 推荐放到系统环境变量：

```powershell
setx DEEPSEEK_API_KEY "sk-你的key"
```

重开终端 / IDEA 后生效。也可以复制 `.env.example` 为 `.env` 再填（`.env` 已被 `.gitignore` 忽略）。

### 3. 启动

```powershell
cd D:\develop\Translator
.\run.ps1
```

`run.ps1` 会先读取 `.env` 注入环境变量，再执行 `mvn spring-boot:run`。

> 现在 `application.yml` 里也加了 `spring.config.import: optional:file:./.env[.properties]`，
> 所以**在 IDEA 里直接运行 `CrtApplication` 同样会自动读取 `.env`**，不需要再配环境变量。
> 直接 `mvn spring-boot:run` 也可以。

浏览器打开 <http://localhost:8080>，输入需求点「判定需求」。

### 4. 接上 STM32（可选）

在 `.env` 里加两行（或设同名环境变量）：

```ini
CRT_SERIAL_ENABLED=true
CRT_SERIAL_PORT=auto
```

`auto` 会自动挑选 CH340 / USB-SERIAL 这类串口；也可以写死 `COM3`。
串口打开失败会自动重试 3 次，发送前如果端口掉了也会尝试重新打开。

## 接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/judge` | 请求 `{"text":"需求内容"}`，返回判定结果并下发串口 |
| GET | `/api/status` | 返回 `{"serialOpen":true/false}` |

响应示例：

```json
{
  "verdict": "ABSURD",
  "code": "E",
  "message": "极度离谱，拒绝接入",
  "raw": "{\"verdict\":\"ABSURD\",\"reason\":\"一天写完淘宝不现实\"}",
  "serialSent": true
}
```

## 串口协议

| 判定 | 字符 | STM32 行为 |
| --- | --- | --- |
| 合理 | `O` (0x4F) | 绿灯亮，OLED 显示「需求通过」 |
| 离谱 | `E` (0x45) | 红灯闪烁 + 蜂鸣器长鸣，OLED 显示「极度离谱，拒绝接入」 |

参数：**9600 bps，8 数据位，无校验，1 停止位**。

## 测试

```powershell
mvn test
```

覆盖的测试类：

| 测试类 | 验证内容 |
| --- | --- |
| `VerdictParserTest` | JSON / 代码块 / 中文关键词 → OK / ABSURD 的解析，含"不合理"不被"合理"误判 |
| `AiJudgeServiceTest` | Prompt 拼装、用户输入透传、结论解析 |
| `DeepSeekClientTest` | 用本地 HttpServer 模拟接口，验证请求体与响应解析、非 200 异常 |
| `JudgeControllerTest` | MockMvc：`O`/`E` 指令、空白入参 400、状态接口 |
| `LoggingSerialServiceTest` | 模拟串口发送与协议字符映射 |
| `CrtApplicationTests` | Spring 容器正常启动 |

## 目录结构

```
src/main/java/com/crt/
├── CrtApplication.java          # 启动类
├── ai/                          # AI 判定：Verdict / VerdictParser / DeepSeekClient / AiJudgeService
├── serial/                      # 串口：SerialPortService 接口 + 真实/模拟两种实现
├── web/                         # JudgeController + 请求/响应 DTO
└── config/                      # CrtProperties、AiConfig
src/main/resources/
├── application.yml
└── static/index.html            # 前端页面（文字 + 语音输入）
src/test/java/com/crt/           # 单元测试与接口测试
```

## 作者与致谢

- **作者**：铵盐（GitHub [@Ayanyan-yan](https://github.com/Ayanyan-yan)）
- **AI 协作**：[DeepSeek](https://www.deepseek.com) —— 代码生成、调试与文档辅助，所有改动均经人工审查后合入

## 许可证

本项目基于 [MIT License](LICENSE) 开源。

