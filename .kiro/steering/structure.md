# 项目结构

```
parking-service/src/main/java/com/parking/
├── ParkingApplication.java          # 入口（@SpringBootApplication, @EnableScheduling）
├── common/                          # 共享工具类与基础类型
│   ├── ApiResponse.java             # 统一响应封装 {code, message, data, requestId}
│   ├── BusinessException.java       # 业务逻辑异常（携带 ErrorCode）
│   ├── ErrorCode.java               # 错误码枚举（PARKING_XXXX 格式）
│   └── RequestContext.java          # 基于 ThreadLocal 的 requestId 访问器
├── config/                          # Spring 配置类
│   ├── MyBatisConfig.java           # @MapperScan("com.parking.mapper")
│   ├── RedisConfig.java             # RedisTemplate JSON 序列化配置
│   └── WebConfig.java               # 注册拦截器，匹配 /api/** 路径
├── exception/                       # 全局异常处理
│   └── GlobalExceptionHandler.java  # @RestControllerAdvice，将异常映射为 ApiResponse
├── interceptor/                     # HTTP 拦截器
│   └── RequestIdInterceptor.java    # 生成 req_{timestamp}_{uuid} 并写入 MDC
├── mapper/                          # MyBatis Mapper 接口（待创建）
├── model/                           # 实体/领域类（待创建）
├── service/                         # 业务逻辑层（待创建）
└── controller/                      # REST 控制器（待创建）

parking-service/src/main/resources/
├── application.yml                  # 应用配置
├── logback-spring.xml               # 日志配置，pattern 中包含 requestId
└── mapper/                          # MyBatis XML 映射文件

parking-service/src/test/java/com/parking/          # 测试类，镜像主源码结构
```

## 架构模式

- **分层架构**：Controller → Service → Mapper (MyBatis)
- **统一响应**：所有接口返回 `ApiResponse<T>`，格式为 `{code, message, data, requestId}`
- **异常流转**：抛出 `BusinessException(ErrorCode)` → 被 `GlobalExceptionHandler` 捕获 → 映射为 `ApiResponse`
- **请求追踪**：`RequestIdInterceptor` 为每个请求生成唯一 ID，存入 Servlet 属性和 SLF4J MDC
- **API 路径前缀**：所有接口以 `/api/` 开头

## 约定

- 基础包名：`com.parking`
- Mapper 接口放在 `com.parking.mapper`，自动扫描
- Model/实体类型别名来自 `com.parking.model`
- 使用 Lombok 注解（`@Data`、`@Slf4j`、`@Getter` 等）减少样板代码
- 测试使用 JUnit 5 断言；属性测试使用 jqwik `@Property`
- Logback pattern 包含 `[%X{requestId}]` 用于请求关联

## 语言规范（强制要求）

- 所有自然语言描述（包括本文件及后续自动生成的说明性文本）必须使用**中文（简体）**。
- 除非用户在当前对话中明确要求英文，否则：
  - 不得使用英文长句描述需求、设计或产品目标；
  - 可以使用英文**仅限于**：类名、方法名、API 路径、JSON 字段名、错误码、环境变量等技术标识。
- 若已经用英文生成了某段说明，需要在后续输出中用等价的中文描述补齐或替换。
- 所有关键业务名词必须与 `requirements.md` 中 Glossary 定义的专有词保持一致（如 Primary_Vehicle、Visitor_Vehicle、Data_Domain、Available_Spaces 等），不得自行翻译或使用其他表述。
