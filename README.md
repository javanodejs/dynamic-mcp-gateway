# Dynamic MCP Gateway

<p align="center">
  <a href="#english">English</a> | <a href="#中文">中文</a>
</p>

---

<a name="english"></a>

## 🇬🇧 English

A dynamic, multi-tenant MCP (Model Context Protocol) gateway with runtime tool management capabilities. This project allows you to dynamically configure and manage multiple MCP services and their tools through REST APIs, with automatic runtime reloading.

### Features

- **Multi-tenant MCP Endpoints**: Each service is exposed as an independent MCP endpoint (e.g., `/mcp/shop`, `/mcp/logistics`)
- **Dynamic Tool Management**: Add, update, delete, and toggle tools at runtime without restart
- **Tool Reusability**: Tools can be shared across multiple services through binding relationships
- **Request Header Passthrough**: Configure static headers per service/tool with automatic merging
- **Auto-refresh**: Tool callbacks automatically refresh based on catalog changes
- **REST Admin API**: Full CRUD operations for services and tools via REST endpoints
- **Health Monitoring**: Built-in health check and runtime status endpoints

### Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     Dynamic MCP Gateway                          │
├─────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐    ┌──────────────────────────────────────┐  │
│  │ MCP Client   │───▶│  /mcp/{serviceCode}                 │  │
│  │ (Claude/AI)  │    │  McpMultiEndpointRegistry            │  │
│  └──────────────┘    │  ├─ Service: shop                    │  │
│                      │  └─ Service: logistics               │  │
│                      └──────────────────────────────────────┘  │
│                                      │                           │
│                                      ▼                           │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │               DynamicMcpToolCallbackProvider              │  │
│  │  - Auto-refresh (10s interval)  - Signature detection    │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                      │                           │
│                                      ▼                           │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  McpCatalogService  ←→  ConfiguredApiExecutor            │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### Data Model

| Table | Description |
|-------|-------------|
| `mcp_service` | MCP service configuration with endpoint settings |
| `mcp_tool` | Reusable tool definitions with API parameters |
| `mcp_service_tool_binding` | Many-to-many relationship between services and tools |

### Quick Start

**Prerequisites**: Java 17+, Maven 3.6+, MySQL 8.0+

```bash
# 1. Create database
mysql -u root -p -e "CREATE DATABASE mcp_gateway DEFAULT CHARACTER SET utf8mb4;"

# 2. Run application
cd dynamic-mcp-gateway
mvn spring-boot:run
```

Application starts at `http://127.0.0.1:8080`.

### MCP Endpoints

| Service | MCP URL |
|---------|---------|
| shop | `http://127.0.0.1:8080/mcp/shop` |
| logistics | `http://127.0.0.1:8080/mcp/logistics` |

### Admin API Reference

#### Service Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/admin/mcp/services` | List all services |
| POST | `/admin/mcp/services` | Create a new service |
| GET | `/admin/mcp/services/{id}` | Get service details |
| PUT | `/admin/mcp/services/{id}` | Update service |
| PATCH | `/admin/mcp/services/{id}/status?enabled=true/false` | Toggle service |
| DELETE | `/admin/mcp/services/{id}` | Delete service |
| GET | `/admin/mcp/services/connections` | List connections |
| POST | `/admin/mcp/services/reload` | Reload all tools |

#### Tool Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/admin/mcp/services/{serviceId}/tools` | List tools |
| POST | `/admin/mcp/services/{serviceId}/tools` | Create tool |
| GET | `/admin/mcp/tools/{toolId}` | Get tool details |
| PUT | `/admin/mcp/tools/{toolId}` | Update tool |
| PATCH | `/admin/mcp/tools/{toolId}/status?enabled=true/false` | Toggle tool |
| DELETE | `/admin/mcp/tools/{toolId}` | Delete tool |

### Example: Create Service & Tool

```bash
# Create service
curl -X POST http://127.0.0.1:8080/admin/mcp/services \
  -H "Content-Type: application/json" \
  -d '{
    "serviceCode": "shop",
    "serviceName": "E-commerce Service",
    "protocol": "STATELESS",
    "enabled": true
  }'

# Create tool
curl -X POST http://127.0.0.1:8080/admin/mcp/services/1/tools \
  -H "Content-Type: application/json" \
  -d '{
    "toolCode": "goods.list",
    "toolName": "Query Products",
    "httpMethod": "GET",
    "apiUrl": "https://api.example.com/products",
    "enabled": true,
    "parameters": [
      {"name": "page", "required": false, "type": "integer", "description": "Page number", "defaultValue": 1}
    ]
  }'
```

### Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | 8080 | Server port |
| `spring.datasource.url` | - | MySQL connection URL |
| `app.public-base-url` | http://127.0.0.1:8080 | Public URL for MCP |

### Tech Stack

- Java 17 / Spring Boot 3.2.3
- Spring AI 1.1.2 (MCP Server)
- MyBatis Plus 3.5.7 / WebFlux / MySQL 8.0

### License

[MIT License](LICENSE)

---

<a name="中文"></a>

## 🇨🇳 中文

一个支持运行时工具管理的动态多租户 MCP（Model Context Protocol）网关。通过 REST API 动态配置和管理多个 MCP 服务及其工具，支持自动运行时重载。

### 功能特性

- **多租户 MCP 端点**：每个服务暴露为独立的 MCP 端点（如 `/mcp/shop`、`/mcp/logistics`）
- **动态工具管理**：运行时添加、更新、删除、启停工具，无需重启
- **工具复用**：通过绑定关系，工具可在多个服务间共享
- **请求头透传**：配置服务/工具级别的静态请求头，自动合并
- **自动刷新**：工具回调基于目录变更自动刷新
- **REST 管理 API**：通过 REST 端点完整管理服务和工具
- **健康监控**：内置健康检查和运行状态端点

### 架构设计

```
┌─────────────────────────────────────────────────────────────────┐
│                     Dynamic MCP Gateway                          │
├─────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐    ┌──────────────────────────────────────┐  │
│  │ MCP 客户端   │───▶│  /mcp/{serviceCode}                 │  │
│  │ (Claude/AI)  │    │  McpMultiEndpointRegistry            │  │
│  └──────────────┘    │  ├─ Service: shop                    │  │
│                      │  └─ Service: logistics               │  │
│                      └──────────────────────────────────────┘  │
│                                      │                           │
│                                      ▼                           │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │               DynamicMcpToolCallbackProvider              │  │
│  │  - 自动刷新 (10秒间隔)  - 签名变更检测                     │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                      │                           │
│                                      ▼                           │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  McpCatalogService  ←→  ConfiguredApiExecutor            │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### 数据模型

| 表名 | 说明 |
|------|------|
| `mcp_service` | MCP 服务配置，包含端点设置 |
| `mcp_tool` | 可复用的工具定义，包含 API 参数 |
| `mcp_service_tool_binding` | 服务与工具的多对多绑定关系 |

### 快速开始

**环境要求**：Java 17+、Maven 3.6+、MySQL 8.0+

```bash
# 1. 创建数据库
mysql -u root -p -e "CREATE DATABASE mcp_gateway DEFAULT CHARACTER SET utf8mb4;"

# 2. 启动应用
cd dynamic-mcp-gateway
mvn spring-boot:run
```

应用启动地址：`http://127.0.0.1:8080`

### MCP 端点

| 服务 | MCP 地址 |
|------|----------|
| shop | `http://127.0.0.1:8080/mcp/shop` |
| logistics | `http://127.0.0.1:8080/mcp/logistics` |

### 管理 API 参考

#### 服务管理

| 方法 | 端点 | 说明 |
|------|------|------|
| GET | `/admin/mcp/services` | 查询服务列表 |
| POST | `/admin/mcp/services` | 新增服务 |
| GET | `/admin/mcp/services/{id}` | 查询服务详情 |
| PUT | `/admin/mcp/services/{id}` | 更新服务 |
| PATCH | `/admin/mcp/services/{id}/status?enabled=true/false` | 启停服务 |
| DELETE | `/admin/mcp/services/{id}` | 删除服务 |
| GET | `/admin/mcp/services/connections` | 查询连接信息 |
| POST | `/admin/mcp/services/reload` | 重载所有工具 |

#### 工具管理

| 方法 | 端点 | 说明 |
|------|------|------|
| GET | `/admin/mcp/services/{serviceId}/tools` | 查询工具列表 |
| POST | `/admin/mcp/services/{serviceId}/tools` | 新增工具 |
| GET | `/admin/mcp/tools/{toolId}` | 查询工具详情 |
| PUT | `/admin/mcp/tools/{toolId}` | 更新工具 |
| PATCH | `/admin/mcp/tools/{toolId}/status?enabled=true/false` | 启停工具 |
| DELETE | `/admin/mcp/tools/{toolId}` | 删除工具 |

### 示例：创建服务和工具

```bash
# 创建服务
curl -X POST http://127.0.0.1:8080/admin/mcp/services \
  -H "Content-Type: application/json" \
  -d '{
    "serviceCode": "shop",
    "serviceName": "电商服务",
    "protocol": "STATELESS",
    "enabled": true
  }'

# 创建工具
curl -X POST http://127.0.0.1:8080/admin/mcp/services/1/tools \
  -H "Content-Type: application/json" \
  -d '{
    "toolCode": "goods.list",
    "toolName": "查询商品列表",
    "httpMethod": "GET",
    "apiUrl": "https://api.example.com/products",
    "enabled": true,
    "parameters": [
      {"name": "page", "required": false, "type": "integer", "description": "页码", "defaultValue": 1}
    ]
  }'
```

### 配置说明

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `server.port` | 8080 | 服务端口 |
| `spring.datasource.url` | - | MySQL 连接地址 |
| `app.public-base-url` | http://127.0.0.1:8080 | MCP 公开访问地址 |

### 技术栈

- Java 17 / Spring Boot 3.2.3
- Spring AI 1.1.2 (MCP Server)
- MyBatis Plus 3.5.7 / WebFlux / MySQL 8.0

### 许可证

[MIT License](LICENSE)

---

<p align="center">
  <a href="#english">English</a> | <a href="#中文">中文</a>
</p>