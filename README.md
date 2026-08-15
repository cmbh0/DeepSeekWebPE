# DeepSeekWebPE

> ### 💬 QQ 交流群：**1045531031**
>
> 一个由 **GPT（DeepSeek PE / Harness）** + **AIDE** 编写制作、面向**安卓移动端**的 **DeepSeek Hermes** 专用适配壳。内置快捷的 **悬浮窗 UI** 与**内置教程**，让 DeepSeek 原生 Web 界面在手机上也能流畅、顺手地使用。

---

## 📸 效果图

![](https://i.imgs.ovh/2026/08/15/e7a35add8538de80026f3a8971d894b2.jpg)

---

## ✨ 特性

- 📱 **安卓移动端适配** —— 专为手机优化的 WebView 外壳，自动注入 iPad 平板视口与横屏沉浸式全屏，让桌面版界面在手机上完整呈现。
- 🪟 **快捷悬浮窗 UI** —— 通过系统悬浮窗（Overlay）将 DeepSeek Web 界面弹出为可自由拖动、缩放的小窗，边看边聊、多任务不打断。
- 🔘 **浮动操作菜单** —— 主界面内置可拖拽的悬浮按钮，一键唤出：连接重试 / 后退 / 前进 等功能。
- 🎓 **内置教程** —— 应用内自带使用引导（AndroidManifest 与源码内已内置相关说明），开箱即上手。
- ⚡ **本地直连** —— 默认指向本机运行的服务（`http://127.0.0.1:3080`），内置连接超时与错误页重试机制。
- 🌀 **液态玻璃（Liquid Glass）UI** —— 自定义无边框毛玻璃图标与面板，视觉更现代。
- 🔧 **纯原生 / 轻量** —— 无第三方依赖、源码精简，由 AIDE 一键编译构建。

---

## 🚀 使用说明

1. 在本机启动 DeepSeek **PE / Harness** Web 服务（默认地址 `http://127.0.0.1:3080`）。
2. 用 AIDE（或任意可用的 Gradle 构建工具）打开并构建本工程，生成 APK。
3. 安装到安卓设备，首次启动授予 **悬浮窗** 权限（SYSTEM_ALERT_WINDOW）。
4. 打开应用即可看到完整的 DeepSeek Web 界面，点按悬浮菜单可随时唤出悬浮窗，在多任务下继续对话。

> 需要权限：`INTERNET`、`SYSTEM_ALERT_WINDOW`（悬浮窗）、`FOREGROUND_SERVICE`（后台悬浮服务）。

---

## 🗂️ 项目结构

```
app/src/main/
├── AndroidManifest.xml
├── assets/error.html          # 连接失败时的内置错误提示页
├── java/com/mycompany/application/
│   ├── MainActivity.java      # 主界面：WebView + 悬浮操作菜单（横屏沉浸式）
│   ├── OverlayLauncherActivity.java  # 悬浮窗启动引导（权限申请）
│   ├── OverlayService.java    # 系统悬浮窗服务（可缩放 / 拖动小窗）
│   ├── LiquidGlassDrawable.java      # 液态玻璃 UI 绘制
│   ├── WindowIconDrawable.java       # 悬浮窗图标绘制
│   └── App.java               # Application 入口
└── res/                       # 布局 / 颜色 / 图标 / 资源
```

---

## 🛠️ 构建

```bash
# 使用 AIDE 打开本工程直接编译，或命令行：
./gradlew assembleDebug
```

- minSdk 19 · targetSdk 29 · compileSdk 30
- 构建工具：AGP 3.6.1（含阿里云镜像仓库配置）

---

## 📄 声明与说明

- 本项目由 GPT（DeepSeek PE / Harness）辅助编写，用于 **DeepSeek Hermes** 在安卓移动端的适配与使用。
- 默认连接地址为本机服务 `127.0.0.1:3080`，请按需在源码中调整。
- **注意**：`app/.backup/` 为运行时聊天数据，默认被 `.gitignore` 排除，不会上传到仓库。

---

## 🤝 友情链接

- **[Linux DO](https://linux.do)** —— 开放、自由的 Linux 与开源开发者社区

[![Linux DO](https://img.shields.io/badge/Linux_DO-%E5%BC%80%E5%8F%91%E8%80%85%E7%A4%BE%E5%8C%BA-4169E1)](https://linux.do)

## 📬 交流与反馈

QQ 交流群：**1045531031**

## 📃 许可证

本项目基于 **[MIT License](./LICENSE)** 开源。
