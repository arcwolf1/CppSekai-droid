# CppSekai Android

CppSekai 的 Android 移植版本。

基于 [BSOD-MEMZ/CppSekai](https://github.com/BSOD-MEMZ/CppSekai)（AGPL-3.0）修改而来。
上游 CppSekai 又派生自 [sekai-mmw-preview-web](https://github.com/Team-Hanasato/sekai-mmw-preview-web)（AGPL-3.0），其本身基于 MikuMikuWorld（MIT）。

## 支持

| 平台 | ABI | 最低版本 |
|---|---|---|
| 老设备（Mali-400 等） | armeabi-v7a | Android 4.4 (API 19) |
| 现代设备 | arm64-v8a | Android 5.0 (API 21) |

## 已适配

- OpenGL ES 2.0 渲染（替换原桌面 GL 3.3 core）
- GLSL ES 1.00 shader（替换原 GLSL 330）
- Android 系统字体（HarmonyOS / Noto CJK / DroidSansFallback）
- 触摸输入坐标映射
- 强制全屏 + 固定分辨率渲染（默认 1280×720）
- 谱面下载器（原 Win32 chartdl 重写为 Android Activity）
- 返回键退出
- 资源自包含：首次启动自动释放 `assets/` 到 `/sdcard/CppSekai/`

## 构建

### 依赖

- **Android NDK r25c**（必须）
  - r21 缺少 `std::filesystem` 实现
  - r26+ 已放弃 API 19 支持
- Android SDK：`platforms;android-28` + `build-tools;30.0.3`
- JDK 8
- SDL2 2.0.14 源码

### 编译 SDL2

```bash
# 用 SDL2 的 androidbuild.sh 生成工程，然后分别编两个 ABI
cd SDL2/build-scripts
./androidbuild.sh org.libsdl.app ../test/testgles.c
cd ../build/org.libsdl.app/app
$ANDROID_NDK_HOME/ndk-build \
    NDK_PROJECT_PATH=. \
    APP_BUILD_SCRIPT=jni/Android.mk \
    APP_APPLICATION_MK=jni/Application.mk \
    APP_ABI=armeabi-v7a \
    APP_PLATFORM=android-19

$ANDROID_NDK_HOME/ndk-build \
    NDK_PROJECT_PATH=. \
    APP_BUILD_SCRIPT=jni/Android.mk \
    NDK_APPLICATION_MK=jni/Application.mk \
    APP_ABI=arm64-v8a \
    APP_PLATFORM=android-21 \
