# AMLL Android 调用与接入指南

本文面向 YAQMC Android 以及其它 Android 宿主，说明如何接入 `amll-android`。

> 渲染器本身使用 Jetpack Compose，但不依赖 WebView、DOM 或 PixiJS。Compose 宿主可以直接使用 `AMLLPlayer`；传统 Activity / Fragment / Capacitor / View 宿主可以使用 `AMLLPlayerView`，无需为了歌词组件把整个宿主改成 Compose。

最低 Android 版本为 API 26。

## 1. 当前构建基线

- compile SDK 36
- demo target SDK 36
- minSdk 26
- AGP 8.13.0
- Kotlin / Compose compiler plugin 2.2.20
- Compose BOM 2026.06.00（Compose 1.11 generation）
- Gradle 8.14.3
- Java / Kotlin JVM target 21

这套基线与当前 YAQMC Android 宿主对齐，避免独立 AAR 在 Kotlin metadata、AGP 或 compileSdk 上高于宿主。

## 2. 引入依赖

### 2.1 同一 Gradle 工程

```kotlin
// settings.gradle.kts
include(":amll")
```

```kotlin
// app/build.gradle.kts
dependencies {
    implementation(project(":amll"))
}
```

### 2.2 Maven Local

先在 `amll-android` 仓库发布：

```bash
./gradlew :amll:publishReleasePublicationToMavenLocal
```

宿主：

```kotlin
repositories {
    mavenLocal()
    google()
    mavenCentral()
}

dependencies {
    implementation("dev.yaqmc:amll-android:0.1.0-SNAPSHOT")
}
```

CI 会把 release AAR 发布到 Maven Local，然后使用两个**独立 Gradle consumer**重新编译：一个验证 Compose API，一个验证不启用 Compose compiler plugin 的普通 Android View 宿主。

### 2.3 GitHub Packages

仓库已经支持 repository-scoped GitHub Packages publication。发布 workflow 可以由 GitHub Release 或手动 dispatch 触发。

发布版本来自：

1. 环境变量 `AMLL_VERSION`；
2. Gradle property `-PamllVersion=...`；
3. 都不存在时回退 `0.1.0-SNAPSHOT`。

例如 tag `v0.1.0-alpha.1` 会发布成 Maven version `0.1.0-alpha.1`。

Gradle 发布仓库只会在 `GITHUB_ACTOR` 和 `GITHUB_TOKEN` 都存在时注册，因此普通本地/PR 构建不需要 GitHub 凭据。

跨私有仓库消费时仍需认证。对于 `YAQMC/YAQMC`：

- 如果它的 Actions token 被授予该 package 的读取权限，可以使用仓库 `GITHUB_TOKEN`；
- 否则需要 PAT classic，并包含 `read:packages`；
- 不要把 token 写进 `settings.gradle.kts` 或提交到仓库。

## 3. 歌词模型

### `LyricRuby`

```kotlin
LyricRuby(
    startTimeMs = 12_000,
    endTimeMs = 12_450,
    text = "ㄋㄧˇ",
)
```

一个 `LyricWord` 可以包含多个 ruby segment。renderer 会保持 segment 独立 shaping/flex geometry；存在 ruby 时，segment 时间会驱动整个 visual word box 的 mask 扫描。

### `LyricWord`

```kotlin
LyricWord(
    startTimeMs = 12_000,
    endTimeMs = 12_450,
    text = "你",
    romanText = "ni",
    obscene = false,
    ruby = emptyList(),
)
```

字段：

- `startTimeMs` / `endTimeMs`：逐词时间；
- `text`：基础歌词文本；
- `romanText`：逐词 romanization，显示在主字下方；
- `ruby`：逐段带时间的上方注音；
- `obscene`：标记为需要按上游规则进行屏蔽的词。

Ruby、主字、逐词 romanization 共用一个 parent word-level bright→dark mask。强调动画发生在子 grapheme 上时，mask 仍停留在 parent word coordinate space，不会跟着字符 transform 一起移动。

### `LyricLine`

```kotlin
LyricLine(
    words = words,
    translatedLyric = "I still remember you",
    romanLyric = "wo hai ji de ni",
    startTimeMs = 12_000,
    endTimeMs = 15_200,
    isBackground = false,
    isDuet = false,
)
```

`translatedLyric` / `romanLyric` 是整行副歌词；`LyricWord.romanText` 是逐词 annotation，两者可以同时存在。

## 4. Compose 宿主

```kotlin
@Composable
fun LyricsScreen(
    lines: List<LyricLine>,
    playbackPositionMs: Long,
    isPlaying: Boolean,
    seekTo: (Long) -> Unit,
) {
    val state = remember { AMLLPlayerState() }

    LaunchedEffect(lines) {
        state.setLyricLines(lines)
    }

    LaunchedEffect(playbackPositionMs, isPlaying) {
        state.update(
            positionMs = playbackPositionMs,
            isPlaying = isPlaying,
        )
    }

    AMLLPlayer(
        state = state,
        modifier = Modifier.fillMaxSize(),
        style = AMLLStyle(),
        onLineClick = { line ->
            seekTo(line.startTimeMs)
            state.seekTo(line.startTimeMs)
        },
        bottomLine = {
            Text("Lyrics by Example Artist")
        },
    )
}
```

## 5. 传统 Android View / Capacitor 宿主

`AMLLPlayerView` 是 `AbstractComposeView` wrapper。宿主代码本身不需要出现 `@Composable`，也不需要启用 Compose compiler plugin。

```kotlin
val lyricsView = AMLLPlayerView(context).apply {
    setLyricLines(lines)
    update(
        positionMs = currentPositionMs,
        isPlaying = isPlaying,
    )
    onLineClick = { line ->
        player.seekTo(line.startTimeMs)
        seekTo(line.startTimeMs)
    }
}

container.addView(
    lyricsView,
    ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT,
    ),
)
```

可直接调用：

```kotlin
lyricsView.setLyricLines(lines)
lyricsView.update(positionMs, isPlaying)
lyricsView.updatePosition(positionMs)
lyricsView.updatePlaybackState(isPlaying)
lyricsView.seekTo(positionMs)
lyricsView.setAutoSeekDetectionEnabled(enabled)
lyricsView.setMaskObsceneWordsMode(mode)
lyricsView.setMaskObsceneWordChar('*')
lyricsView.style = AMLLStyle(...)
```

也可以通过 `lyricsView.state` 获取底层 `AMLLPlayerState` 做高级配置。

`AMLLPlayerView` 使用 `ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed`，应挂在正常 Activity/Fragment lifecycle View tree 下。和普通 View 修改一样，状态更新建议在 Android main thread 调用。

## 6. 播放状态与 Seek

推荐入口：

```kotlin
state.update(
    positionMs = currentPositionMs,
    isPlaying = playerIsPlaying,
)
```

明确 seek 时：

```kotlin
player.seekTo(targetMs)
state.seekTo(targetMs)
```

不要只停止 position 更新而继续让 `isPlaying = true`。暂停状态会改变 inactive line scale、背景人声展开等视觉语义。

### 自动 Seek 推导

默认开启：

```kotlin
state.updateAutoSeekDetectionEnabled(true)
```

关闭后，普通 position sample 不再进入自动 drift detector；明确调用 `seekTo()` 仍始终使用 seek-motion 语义。

renderer 的运动由 media time 重建，不维护一个与播放器脱离的“自由运行歌词时钟”。因此 seek/pause/resume 后可以确定性恢复视觉状态。

## 7. Obscene word masking

v33 起 `LyricWord.obscene` 会被实际处理，而不是预留字段。

```kotlin
state.updateMaskObsceneWordsMode(MaskObsceneWordsMode.Disabled)
state.updateMaskObsceneWordsMode(MaskObsceneWordsMode.FullMask)
state.updateMaskObsceneWordsMode(MaskObsceneWordsMode.PartialMask)
state.updateMaskObsceneWordChar('*')
```

语义与 upstream 对齐：

- `Disabled`：保留原文本；
- `FullMask`：所有非空白字符替换为 mask char；
- `PartialMask`：短词全屏蔽；较长词保留首尾可见字符，中间非空白字符屏蔽。

`AMLLPlayerState` 内部保留原始 lyric lines。运行时切换 mode/char 会从**原始文本**重新处理，因此不会发生“已经打星后再继续打星”的不可逆问题。

屏蔽只改基础 lyric text；word timing、ruby、romanization 等元数据保持不变。

## 8. 背景人声、对唱与注音

### 背景人声

背景行必须紧跟对应主行：

```kotlin
val lines = listOf(
    LyricLine(words = mainWords),
    LyricLine(words = backgroundWords, isBackground = true),
)
```

renderer 会把它们组合成一个滚动目标。背景先唱/后唱的布局判断依据两行第一个真实 word 的时间，而不是简单依赖同步过的 line start。

强制背景人声后置：

```kotlin
AMLLStyle(alwaysPostpositionBackground = true)
```

### 对唱

```kotlin
LyricLine(
    words = words,
    isDuet = true,
)
```

存在 duet 时，两侧 speaker 会按 AMLL 几何使用约 85% 可用内容宽度并保留 opposite-side inset。

### Ruby / Roman

Ruby segment 独立 shape，然后以 flex-row-like 方式居中。Mask sweep 使用 ruby segment UTF-16 code-unit count，保留 segment timing gap；逐词 romanization 保留 upstream `0.3em` inline-end padding 语义。

## 9. 曲末与 Bottom Line

renderer 从所有主歌词 group 的最大 `endTimeMs` 推导 end-of-song，不要求宿主再提供 media duration/ended signal。

```kotlin
AMLLPlayer(
    state = state,
    bottomLine = {
        Text("Lyrics by Example Artist")
    },
)
```

到达歌词时间线末尾后，active lyric 状态清空；有 `bottomLine` 时自动聚焦它，否则聚焦最后一个 lyric group。

## 10. 常用样式

```kotlin
AMLLStyle(
    lineFontSize = 34.sp,
    secondaryFontSize = 17.sp,
    lyricFontWeight = FontWeight.SemiBold,
    enableBlur = true,
    enableScale = true,
    enableSpring = true,
    hidePassedLines = false,
    alwaysPostpositionBackground = false,
    wordFadeWidthEm = 1f,
    alignPosition = 0.35f,
    alignAnchor = AMLLAlignAnchor.Center,
)
```

重点默认行为：

- 主歌词 inactive scale `0.97`；
- 背景歌词 inactive scale `0.75`；
- `enableSpring = false` 时使用约 500ms CSS-ease 风格 transform fallback，不瞬移；
- `hidePassedLines = false`；
- focus 默认 `Center @ 0.35`；
- `horizontalPadding = Dp.Unspecified` 时，容器 `<=500dp` 用 `20dp`，更宽时用约 `1em`；
- 字重默认 `600 / SemiBold`；
- word fade width 默认 `1em`。

## 11. YAQMC 推荐接入结构

当前 YAQMC Android 是 Capacitor `BridgeActivity + WebView`，因此推荐先采用 native overlay，而不是一次重写整个页面：

1. 保留现有歌词 provider / fetch / parser；
2. 歌词变化时，把当前 `LyricDocument` 转成 `List<LyricLine>`；
3. 在 Android `MainActivity` 的 View hierarchy 中把 `AMLLPlayerView` 挂成 WebView sibling/overlay；
4. JavaScript 只负责页面状态和“显示/隐藏 native lyrics”等低频事件，不做逐帧 position bridge；
5. position/play/pause/seek 优先直接读取 YAQMC Android/Rust Core 的 native playback clock；
6. `onLineClick` 直接调用 native seek，再同步 `AMLLPlayerView.seekTo()`；
7. overlay 路径稳定后，再决定是否把 Android full-screen lyrics 整页迁移到 native UI。

这样既能保留现有 YAQMC Web 前端，又能把歌词这种高频、动画密集的渲染链路移出 WebView。

## 12. 生命周期与性能

- 一个歌词页面维持一个 state/view，不要每个 position tick 重建；
- 换歌时调用 `setLyricLines()`；
- 不要为了 renderer 额外启动独立 16ms JS timer；优先使用 native audio clock；
- 主线程只提交必要的状态更新，歌词解析/网络请求继续留在原有层；
- API 31+ 距离 blur 可以使用原生 blur effect；旧版本保持其它视觉层级而不强制软件 Gaussian blur；
- emphasized text shadow 在 API 26+ 使用 native text shadow，不依赖 API 31 `RenderEffect`；
- CI 已覆盖普通 Compose consumer 和不启用 Compose compiler plugin 的 View consumer。

## 13. 当前边界

仍需继续验证的主要是：

- 浏览器 CSS 与 Android/Skia shadow blur kernel 的少量像素差异；
- DOM 与 Compose/Skia 在字体度量、shaping、亚像素排版上的残余差异；
- 较少使用的 upstream 配置；
- YAQMC 真实大型逐词歌词、低端设备和长时间播放的性能/内存表现；
- YAQMC 主仓 package 权限和真正的 native overlay 集成。

这些边界不改变稳定宿主接口：Compose 使用 `LyricLine -> AMLLPlayerState -> AMLLPlayer`，传统 Android 使用 `LyricLine -> AMLLPlayerView`。
