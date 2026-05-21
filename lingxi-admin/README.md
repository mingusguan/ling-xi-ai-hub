# LingXi Admin

单体多模块启动服务。`lingxi-admin` 负责启动 Web 应用，认证、系统、代码生成、任务调度、文件、知识库、OA、AI 都作为普通 Maven 模块依赖加载。

## 构建

```bash
mvn -pl lingxi-admin -am package -DskipTests
```

构建产物：

```text
lingxi-admin/target/lingxi-admin.jar
```

## 启动

```bash
java -jar lingxi-admin/target/lingxi-admin.jar
```

默认端口是 `8080`，可以覆盖：

```bash
java -jar lingxi-admin/target/lingxi-admin.jar --server.port=8080
```

## 配置

本地配置在：

```text
lingxi-admin/src/main/resources/application.yml
```

常用部署项：

- `spring.datasource.dynamic.datasource.master`: MySQL 连接
- `spring.data.redis`: Redis 连接
- `file.path`: 本地上传文件目录
- `dashscope.api-key`: AI 模型 API Key，默认读取环境变量 `DASHSCOPE_API_KEY`
- `milvus`: 知识库向量库配置

单体部署不再需要启动 `lingxi-gateway`、Nacos、Sentinel。
