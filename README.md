# Fabric Loading Framework

**其他加载器版本：** [Forge 版 · Forge Loading Framework](https://github.com/Bantanxiaon/ForgeLoadingFramework)。两个项目使用不同的 Mod 文件，请按实例加载器选择。

Windows 客户端启动视频 Mod。独立的 Fabric 项目，使用自己的 Jar 和 fabric_loading 文件夹。

**当前是 0.1.0-dev 开发版，请先用于测试副本。** 代码、解码和接口检查已完成；动画显示、窗口交接、主菜单进入及低配电脑表现仍待游戏内验证。

新增代码采用 [PolyForm Noncommercial 1.0.0](LICENSE)，未经另行授权不得用于商业用途。[许可范围](COMMERCIAL-USE.md)中说明了保留 MIT 授权的原有部分。

## 安装与换视频

本版先提供手动安装，没有移植 Forge 的作者工具，也不需要运行 Forge 工具。

1. 准备可以正常进入主菜单的 Fabric 客户端，退出游戏。
2. 将 FabricLoadingFramework-0.1.0-dev.jar 放入该实例的 mods 文件夹。
3. 将 examples 里的 fabric_loading 文件夹复制到该实例的游戏目录，与 mods 同级。
4. 把自己的 H.264 MP4 放到 fabric_loading/video/loading.mp4。视频含 AAC 音轨时，默认一并播放。
5. 把 Windows 版 FFmpeg 的 ffmpeg.exe 及该发行包要求的配套文件放到 fabric_loading/bin。程序默认读取 bin/ffmpeg.exe。

目录示例：

~~~text
实例游戏目录/
├─ mods/
│  └─ FabricLoadingFramework-0.1.0-dev.jar
└─ fabric_loading/
   ├─ config.json
   ├─ video/loading.mp4
   ├─ bin/ffmpeg.exe
   ├─ fonts/             可选字体
   └─ images/logo.png    可选标志
~~~

使用 PCL 版本隔离时，应放到该版本实际使用的游戏目录。不要照搬 Forge 的 bootstrap Jar、client Jar 或 fml.toml 设置。Fabric 版只需本项目这一份 Mod Jar，正常 Minecraft 客户端已经提供 JNA；不额外要求 Fabric API。

视频可以直接替换，也可以在 config.json 修改 video 路径，不用重新编译。路径都以 fabric_loading 为起点，使用相对路径。源码包和开发包不附带 FFmpeg、Java、Minecraft 或原整合包视频，使用这些文件时应保留各自许可。

## 标题、文字与布局

title 留空时自动读取整合包名称，顺序是：

1. 游戏目录 modrinth.index.json 的 name；
2. 游戏目录 manifest.json 的 name；
3. 启动器传来的 --version；
4. 游戏目录的文件夹名称。

例如目录叫“涟漪之篇”，又没有前面的名称信息，标题就显示“涟漪之篇”。如果 PCL 传来了版本名称，则优先使用版本名称。需要固定标题时，直接填写 title。

[完整样式教程](docs/文字与视频设置.md)包含字体、字号、颜色、透明度、坐标、标题变量和视频参数示例。

## 版本范围

设计目标为 Minecraft 1.20.1–26.3 的已知正式版本。代码根据游戏接口选择 GLFW 或 SDL 窗口入口，不按每个 Loader 补丁版本单独制作 Jar。

检查过的 Fabric Loader SDK 基线为 0.14.21、0.15.11、0.16.14、0.17.3、0.18.4、0.19.5；运行时仅尝试 0.14.21 起至 0.19.x 的正式 Loader。这些 SDK 检查不等同于每个 Minecraft/Loader 组合已经启动成功。安装时仍需使用彼此兼容的游戏、Loader 和 Java。

未知游戏、预览版 Loader、接口不符合预期时，不接入动画，保留原加载过程。未来接口变化可能需要更新路由，不能承诺尚未出现的版本自动兼容。

## 加载信息

界面显示一个从零到完成的总进度，不在切换任务时归零。进度来自可观察的初始化回调和客户端资源加载，完成交接前不会显示 100%。Fabric 没有提供覆盖整个启动过程的官方统一百分比，因此它不是精确的剩余时间比例。

任务条目显示当前进入的 Mod 初始化回调及 Mod 名称。首版识别默认语言适配器中可直接定位的 preLaunch、main 和 client 回调；自定义语言适配器、继承方法、工厂入口或已经加载的类可能无法列出。回调内部再派发的异步任务没有自动细分。不能把回调识别结果当作所有 Mod 内部工作的完整追踪。

## 播放方式与开销

默认按 640×360、30 FPS 解码，不要求原视频是这个分辨率。FFmpeg 播放时缩放，文件不会被改写。队列默认三帧，播放线程持续运行；UI 内容最多每秒更新十次。H.264 解码和 YUV 转换使用 CPU，没有向游戏的 OpenGL/SDL 上下文提交视频绘制。

加载初期先显示不透明的播放窗口，游戏窗口创建后将播放区域挂到同一进程的游戏窗口内。这个原生子窗口方案仍需实测窗口切换、DPI、焦点、闪烁和渐隐；不能据此声称已经消除了双窗口现象，也不能预先保证低配电脑稳定达到 30 FPS。

## 构建与维护

需要 Python 3、JDK 17 和互联网。Java 17 编译的 Jar 可以使用游戏所需的更高版本 Java 运行，仍需实际兼容性测试。

~~~text
python -B build.py --jdk "D:/Java/jdk-17" --download-sdk
python -B test.py --jdk "D:/Java/jdk-17" --engine
~~~

SDK 和依赖的下载地址、哈希记录在 sdk-lock.json、build-dependencies.json 中。build.py 不读取其他项目的源码，也不启动游戏。test.py 只运行无界面检查；可加 --java 路径检查其他 Java，--game-sdk 检查已有的 26.3 官方客户端文件，--ffmpeg 检查实际 H.264 解码和退出清理。

[模块与维护说明](docs/开发与维护.md)。测试报告留在本地 audit 目录，不放入源码分发包。
