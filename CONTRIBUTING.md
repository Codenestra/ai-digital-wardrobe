# 参与贡献

感谢你愿意改进衣见。项目欢迎缺陷修复、测试、文档、无障碍体验、设备兼容性和经过讨论的新功能。

## 开始之前

1. 搜索现有 Issue，避免重复提交。
2. 较大的交互、数据结构或 AI 供应商改动，请先创建功能建议 Issue。
3. 不要在 Issue、PR、提交记录、测试数据或截图中放入真实 API Key、Workspace ID、签名材料或个人照片。
4. 保持本地优先原则：核心衣橱操作不能因为云端失败而无法保存。

## 本地开发

环境要求为 JDK 17、Android SDK 36 和 Android 8.0（API 26）以上设备。

~~~bash
git clone https://github.com/wutao2022/ai-digital-wardrobe.git
cd ai-digital-wardrobe
./gradlew testDebugUnitTest lintDebug assembleDebug
~~~

连接设备或模拟器后：

~~~bash
./gradlew connectedDebugAndroidTest
~~~

## 分支与提交

- 从最新 `main` 创建短生命周期分支。
- 使用清晰的提交信息，例如 `fix: handle denied camera permission`。
- 一次 PR 解决一个明确问题，避免夹带无关格式化或重构。
- 更新行为时同步更新测试和相关文档。

## Pull Request 检查清单

- [ ] 没有真实凭据、个人数据、签名文件、APK 或构建缓存。
- [ ] 单元测试、Lint 和 Debug 构建通过。
- [ ] 涉及 UI 时已在真实模拟器或设备上点击验证。
- [ ] 涉及本地数据时验证了重启后的持久化结果。
- [ ] 涉及 AI 时说明了实际供应商验证与固定响应注入的区别。
- [ ] 涉及图片发送时保留调用前确认和最小必要数据原则。
- [ ] 用户可见变化已更新 README 或操作说明。

## 代码约定

- Kotlin 代码遵循项目现有风格和官方 Kotlin 编码约定。
- UI 使用 Jetpack Compose，不引入 WebView 业务壳。
- 网络错误必须映射成用户可理解且不泄露供应商敏感信息的提示。
- 日志中禁止出现 API Key、临时上传凭证、完整请求头或用户图片内容。

提交贡献即表示你同意按 Apache License 2.0 发布贡献内容。
