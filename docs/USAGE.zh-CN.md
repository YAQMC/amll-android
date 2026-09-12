# AMLL Android 调用与接入指南

本文面向 YAQMC Android 端以及其他 Jetpack Compose 宿主，说明如何把播放状态和现有歌词 DTO 接入 `amll-android`。

> 当前库是纯 Android/Compose 实现，不使用 WebView、DOM 或 PixiJS。最低 Android 版本为 API 26。

## 1. 引入模块

### 方式 A：同一 Gradle 工程直接引用

如果 YAQMC 与本仓库源码放在同一个 Gradle 工程中：

```kotlin
// settings.gradle.kts
include(":amll")
```

应用模块：

```kotlin
// app/build.gradle.kts
dependencies {
    implementation(project(":amll"))
}
```

### 方式 B：本地 Maven

仓库当前已经配置 Maven publication，但这里不假设远程仓库已经发布。开发阶段可先发布到本机：

```bash
./gradlew :amll:publishToMavenLocal
```

宿主工程：

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

## 2. 最小可运行示例

核心只有三步：构造歌词、维护 `AMLLPlayerState`、渲染 `AMLLPlayer`。

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
        onLineClick = { line ->
            seekTo(line.startTimeMs)
            // 让 UI 立即响应，下一次宿主播放器 position 回调会继续校准。
            state.seekTo(line.startTimeMs)
        },
    )
}
```

如果播放时钟本身就是 Flow/回调流，推荐直接在收集位置更新状态，而不是额外做 16 ms 的 UI 定时器：

```kotlin
LaunchedEffect(player) {
    player.playbackState.collect { snapshot ->
        state.update(
            positionMs = snapshot.positionMs,
            isPlaying = snapshot.isPlaying,
        )
    }
}
```

播放器能多频繁提供位置就多频繁推送。AMLL 的 seek/stall 推导会消费每次 position sample，包括数值相同的重复 sample。

## 3. 歌词数据模型

### `LyricWord`

```kotlin
LyricWord(
    startTimeMs = 12_000,
    endTimeMs = 12_450,
    text = "你",
    romanText = "ni",          // 可选：逐词音译，显示在词下方
    obscene = false,
    ruby = emptyList(),         // 可选：逐段 ruby，显示在词上方
)
```

字段含义：

- `startTimeMs` / `endTimeMs`：逐词时间，毫秒。
- `text`：词本体；空格、标点可直接保留在文本中。
- `romanText`：这个词自己的 romanization，不同于整行 `romanLyric`。
- `ruby`：带独立时间的上标注音段。
- `obscene`：预留给歌词优化/屏蔽逻辑；当前渲染接口允许模型携带该信息。

### `LyricRuby`

```kotlin
LyricRuby(
    startTimeMs = 12_000,
    endTimeMs = 12_450,
    text = "ㄋㄧˇ",
)
```

同一个词可以有多个 ruby segment；每段有自己的高亮时间。

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

如果不显式传 `startTimeMs/endTimeMs`，会从 `words` 的最早开始时间和最晚结束时间推导。

`translatedLyric` 和 `romanLyric` 是**整行副歌词**；`LyricWord.romanText` 是跟随单词排版和运动的**逐词音译**，二者可以同时存在。

## 4. 背景人声与对唱

### 背景人声

背景人声使用单独一条 `LyricLine`：

```kotlin
val main = LyricLine(
    words = mainWords,
    isBackground = false,
)

val background = LyricLine(
    words = backgroundWords,
    isBackground = true,
)

val lines = listOf(main, background)
```

**重要：背景行在输入顺序上必须紧跟对应主行。** 当前 renderer 会把一条 `isBackground = true` 的行绑定到前一个还没有背景行的主行，并把二者作为同一个滚动目标。

AMLL 判断背景人声显示在主行上方还是下方时比较的是两行的**第一个真实 word 时间**。因此如果背景人声实际上先唱，可以保留更早的 `background.words.first().startTimeMs`；为了保证 group 顺序，建议 parser/adapter 仍然输出 `main -> background` 的行顺序。对于原始数据中 line start 被同步过的情况尤其适合这种方式。

如需强制背景人声永远后置：

```kotlin
AMLLStyle(
    alwaysPostpositionBackground = true,
)
```

### 对唱

```kotlin
LyricLine(
    words = words,
    isDuet = true,
)
```

`isDuet = true` 会使用右侧 speaker 布局；歌曲只要包含 duet 行，renderer 会为两侧 speaker 保留对应的可用宽度。

## 5. 播放状态同步

推荐使用：

```kotlin
state.update(
    positionMs = currentPositionMs,
    isPlaying = playerIsPlaying,
)
```

也可以分别调用：

```kotlin
state.updatePlaybackState(isPlaying)
state.update(positionMs)
```

或在明确 seek 时：

```kotlin
state.seekTo(newPositionMs)
```

### 暂停

必须同步 `isPlaying = false`。AMLL 的暂停视觉语义不只是停止时间：暂停时 inactive 主行会回到 100% scale，背景人声也会保持展开，因此不要只停止 position 更新而仍让 `isPlaying` 保持 `true`。

### Seek

实际 seek 建议同时做两件事：

```kotlin
player.seekTo(targetMs)
state.seekTo(targetMs)
```

第一步修改真实播放器，第二步让歌词 UI 立刻跳到目标时间。之后继续用真实播放器的位置回调驱动 `state.update(...)`。

## 6. YAQMC DTO 适配

库刻意不依赖 YAQMC、QQ Music 或某一种歌词 parser。推荐在 YAQMC Android 层放一个很薄的 adapter。

下面字段名是示例，请替换成 YAQMC 当前 DTO 的真实字段：

```kotlin
fun YaqmcLineDto.toAMLL(): LyricLine {
    val mappedWords = words.map { word ->
        LyricWord(
            startTimeMs = word.startMs,
            endTimeMs = word.endMs,
            text = word.text,
            romanText = word.romanization,
            ruby = word.rubySegments.map { ruby ->
                LyricRuby(
                    startTimeMs = ruby.startMs,
                    endTimeMs = ruby.endMs,
                    text = ruby.text,
                )
            },
        )
    }

    return LyricLine(
        words = mappedWords,
        translatedLyric = translation.orEmpty(),
        romanLyric = romanization.orEmpty(),
        startTimeMs = startMs,
        endTimeMs = endMs,
        isBackground = isBackground,
        isDuet = isDuet,
    )
}

fun List<YaqmcLineDto>.toAMLLLyrics(): List<LyricLine> =
    map(YaqmcLineDto::toAMLL)
```

建议 adapter 只负责数据转换，不把播放器实例、MediaSession、网络请求或 parser 逻辑放进 `amll` module。

## 7. 样式配置

默认样式已经尽量对齐 AMLL，可以只覆盖宿主真正需要改变的项：

```kotlin
val lyricStyle = AMLLStyle(
    lineFontSize = 34.sp,
    secondaryFontSize = 17.sp,
    activeColor = Color.White,
    lyricFontWeight = FontWeight.SemiBold,
    enableBlur = true,
    wordFadeWidthEm = 1f,
    alignPosition = 0.35f,
    alignAnchor = AMLLAlignAnchor.Center,
)

AMLLPlayer(
    state = state,
    style = lyricStyle,
)
```

常用项：

| 参数 | 含义 | 默认语义 |
| --- | --- | --- |
| `lineFontSize` | 主歌词字号 | `34.sp` |
| `secondaryFontSize` | 翻译/整行音译字号 | `17.sp`，即主字号约 0.5em |
| `lyricFontWeight` | 歌词统一字重 | `600 / SemiBold` |
| `activeColor` | 已唱/亮色 | 白色 |
| `inactiveColor` | active gradient 的暗端基色 | AMLL gradient dark 约 0.4 alpha |
| `backgroundLineScale` | 背景歌词字号比例 | `0.70` |
| `alwaysPostpositionBackground` | 背景人声是否强制后置 | `false` |
| `enableBlur` | 距离模糊 | `true` |
| `wordFadeWidthEm` | 逐词高亮软边宽度 | `1em`，Android-like |
| `alignPosition` | 焦点位于 viewport 高度的比例 | `0.35` |
| `alignAnchor` | 焦点对齐目标行的 Top/Center/Bottom | `Center` |
| `autoAlignResumeDelayMs` | 手动滚动结束后恢复跟随的延迟 | `5000 ms` |

`horizontalPadding`、`verticalPadding`、`lineSpacing` 等也可以覆盖，但如果目标是 AMLL parity，优先保留默认值。

## 8. 完整 Compose 接入模板

```kotlin
@Composable
fun NativeFullScreenLyrics(
    lyricLines: List<LyricLine>,
    player: YaqmcPlayer,
) {
    val state = remember { AMLLPlayerState(initialIsPlaying = player.isPlaying) }

    LaunchedEffect(lyricLines) {
        state.setLyricLines(lyricLines)
    }

    LaunchedEffect(player) {
        player.positionFlow.collect { position ->
            state.update(
                positionMs = position,
                isPlaying = player.isPlaying,
            )
        }
    }

    AMLLPlayer(
        state = state,
        modifier = Modifier.fillMaxSize(),
        style = AMLLStyle(),
        onLineClick = { line ->
            player.seekTo(line.startTimeMs)
            state.seekTo(line.startTimeMs)
        },
    )
}
```

`YaqmcPlayer`、`positionFlow` 只是宿主接口示意；核心要求只有：**把真实 media position 和 isPlaying 持续喂给 `AMLLPlayerState`。**

## 9. 生命周期与性能建议

- 一个歌词页面保持一个 `AMLLPlayerState`，不要每个 position tick 重建 state。
- 换歌时调用 `setLyricLines(newLines)`，并立即 `seekTo()` 或等待真实 position sample 校准。
- 不需要为了 renderer 自己再启动高频 timer；优先使用真实 audio engine / MediaSession 的位置时钟。
- 如果宿主只能低频提供 position，renderer 仍可工作，但逐词运动和 seek 判定精度会随采样精度下降。
- API 31+ 使用原生 blur effect；Android 8-11 会保留其它视觉层级而不强行使用不可用的 Gaussian RenderEffect。
- 手动触摸/滚轮滚动会暂停 auto-align；滚动与惯性停止后默认再等待 5 秒恢复。

## 10. 当前边界

当前重点是 AMLL 动态逐词歌词的 Android-native parity。仍在继续收敛的部分包括：

- soft word-mask leading edge 从分段近似升级到与 upstream 完全一致的连续 gradient；
- CSS text-shadow/glow 的像素级一致性；
- bottom-line / end-of-song focus，需要宿主提供可靠 duration/end signal；
- 更多针对实际 YAQMC 大型歌词数据的性能压测。

这些不会改变上面的核心接入方式；后续 parity 更新应尽量保持 `LyricLine -> AMLLPlayerState -> AMLLPlayer` 这一层 API 稳定。
