# mall-platform

商城平台（monorepo），包含后端和前端两个子项目。

## 项目结构

```
mall-platform/
├── backend/          # 后端：Spring Cloud 微服务商城 (D:\JavaProject\mall)
│   ├── mall-common/
│   ├── mall-coupon/
│   ├── mall-gateway/
│   ├── mall-member/
│   ├── mall-order/
│   ├── mall-product/
│   ├── mall-search/
│   ├── mall-third-party-service/
│   ├── mall-ware/
│   ├── renren-fast/
│   └── renren-generator/
│
└── frontend/         # 前端：Vue 管理后台 (D:\vue_project\renren-fast-vue)
    ├── src/
    ├── build/
    ├── config/
    └── ...
```

## 快速开始

### 后端
```bash
cd backend
mvn clean install
```

### 前端
```bash
cd frontend
npm install
npm run dev
```

## 技术栈

| 层     | 技术                          |
| ------ | ----------------------------- |
| 后端   | Spring Cloud, Spring Boot, MyBatis-Plus, Redis |
| 前端   | Vue 2, Element UI, renren-fast-vue |
