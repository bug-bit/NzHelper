<div align="center">

![NzHelper](https://socialify.git.ci/bug-bit/NzHelper/image?description=1&font=Inter&forks=1&language=1&name=1&owner=1&stargazers=1&theme=Auto)

# NzHelper

[![license](https://img.shields.io/github/license/bug-bit/NzHelper.svg)](https://www.gnu.org/licenses/gpl-3.0.html)
[![Stars](https://img.shields.io/github/stars/bug-bit/NzHelper?label=stars)](https://github.com/bug-bit/NzHelper)
<a href="https://github.com/bug-bit/NzHelper/releases"><img alt="GitHub all releases" src="https://img.shields.io/github/downloads/bug-bit/NzHelper/total?label=Downloads"></a>
[![GitHub Release](https://img.shields.io/github/v/release/bug-bit/NzHelper)](https://github.com/bug-bit/NzHelper/releases)
<a href="https://github.com/bug-bit/NzHelper/issues"><img alt="GitHub issues" src="https://img.shields.io/github/issues/bug-bit/NzHelper"></a>

一个简单、高效、易用的打飞机记录工具，帮助你科学管理✈️生活

</div>

---

## 功能

- 计时记录：开始、暂停、结束一次记录，并保存时长、地点、备注、评分等信息。
- 历史统计：查看历史记录和统计信息。
- OPPO 流体云 / Android 实时活动：计时开始后会发布可提升的持续通知，在支持实时活动的 ColorOS / OPPO 设备上可作为流体云显示；其他 Android 设备会回退为普通前台计时通知。

### 流体云说明

应用同时使用 Android 标准计时通知和 OPPO 流体云私有通知 extras：

- 已声明 `android.permission.POST_PROMOTED_NOTIFICATIONS`。
- 计时通知为 `ongoing`，并使用 `NotificationCompat.Builder#setRequestPromotedOngoing(true)` 请求提升。
- 运行中只向系统提交计时基准时间，不再每秒重发通知；通知栏/流体云中的秒数由系统 Chronometer / SystemUI 实时绘制。
- 参考 OPPO 官方时钟 APK 的计时器实现，通知 extras 中写入 `oplusLiveAlertAppConfig`、`oplus.livealert.capsule`、`oplus.livealert.card` 和 `op_fluid_serviceId`，并使用 `setShowWhen(false)`、`setWhen(...)`、`setUsesChronometer(true)`。
- 通知使用系统标准样式和计时器 chip，不使用自定义 `RemoteViews`，以符合实时活动的系统展示要求。

首次使用请允许通知权限。流体云展示还取决于设备系统版本、ColorOS 实现、用户的实时活动/通知设置，以及第三方应用可用的 OPPO 私有接口范围。OPPO 官方秒表还使用 Pantanal / Seedling 私有接口，本项目无法直接依赖该私有 SDK，因此采用官方计时器通知 extras 的兼容实现。

## 构建

```bash
./gradlew assembleDebug
```

生成的 APK 位于：

```text
app/build/outputs/apk/debug/
```

## 说明
> 在 GitHub 点击 ⭐ Star 以支持我在空余时间继续开发
> 祝愿所有给本项目Star的小伙伴牛子长度翻倍！
  
- 本项目参考了以下开源项目：
- [DickHelper](https://github.com/zzzdajb/DickHelper)


## 贡献
欢迎提交 issue 或 pull request 以改进本项目。

---

## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=bug-bit/NzHelper&type=Timeline)](https://star-history.com/#bug-bit/NzHelper&Timeline)

## 许可协议

本项目基于 GNU 通用公共许可证 第3版（GPLv3）进行授权。  
详情请查阅 [LICENSE](LICENSE) 文件。
