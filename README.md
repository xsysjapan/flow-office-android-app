# flow-office-android-app

譌｢蟄倥・蜍､諤邂｡逅・す繧ｹ繝・Β`flow-office`縺ｨ騾｣謳ｺ縺吶ｋAndroid謇灘綾繝ｪ繝ｼ繝繝ｼ繧｢繝励Μ縺ｧ縺吶ょ・譛臥ｫｯ譛ｫ縺ｧ
NFC繧ｫ繝ｼ繝峨ｒ隱ｭ縺ｿ蜿悶ｊ縲∝・蜍､繝ｻ騾蜍､繝ｻ莨第・髢句ｧ九・莨第・邨ゆｺ・ｒ險倬鹸縺励∪縺吶・
## 迴ｾ蝨ｨ縺ｮ螳溯｣・
- Android CLI縺ｮCompose `empty-activity`繝・Φ繝励Ξ繝ｼ繝医ｒ蝓ｺ縺ｫ縺励◆蜊倅ｸ`app`繝｢繧ｸ繝･繝ｼ繝ｫ
- package / application ID: `jp.co.xsys.flowoffice`
- compile / target SDK 36縲［in SDK 26
- Material 3縺ｮ繝ｩ繧､繝茨ｼ上ム繝ｼ繧ｯTheme
- 蜈ｱ譛臥ｫｯ譛ｫ縺ｮ繝壹い繝ｪ繝ｳ繧ｰ逕ｻ髱｢・・hone / tablet Preview縲√お繝ｩ繝ｼ迥ｶ諷九ｒ蜷ｫ繧・・- NFC UID縺ｮ豁｣隕丞喧縺ｨ蜊倅ｽ薙ユ繧ｹ繝・- 繝壹い繝ｪ繝ｳ繧ｰ逕ｻ髱｢縺ｮCompose UI繝・せ繝・
繝壹い繝ｪ繝ｳ繧ｰAPI騾壻ｿ｡縲》oken菫晏ｭ倥ヽoom縲仝orkManager縲¨FC Reader Mode縺ｯ谺｡縺ｮ螳溯｣・ヵ繧ｧ繝ｼ繧ｺ縺ｧ縺吶・
## 蠢・ｦ∫腸蠅・
- JDK 21・・radle縺ｮtoolchain縺ｯJava 17繧剃ｽｿ逕ｨ・・- Android SDK Platform 36
- Android SDK Build-Tools
- Android CLI: `C:\Users\yuto.nagano\AppData\AndroidCLI\android.exe`

SDK菴咲ｽｮ縺ｯ蜷・幕逋ｺ迺ｰ蠅・・`local.properties`縺ｸ險ｭ螳壹＠縺ｾ縺吶ゅ％縺ｮ繝輔ぃ繧､繝ｫ縺ｯGit邂｡逅・＠縺ｾ縺帙ｓ縲・
```properties
sdk.dir=C\:\\Users\\<user>\\AppData\\Local\\Android\\Sdk
```

## 繝薙Ν繝峨→繝・せ繝・
PowerShell縺九ｉ螳溯｡後＠縺ｾ縺吶・
```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
.\gradlew.bat lintDebug assembleDebugAndroidTest
```

debug APK縺ｯ谺｡縺ｸ逕滓・縺輔ｌ縺ｾ縺吶・
```text
app/build/outputs/apk/debug/app-debug.apk
```

`assembleDebugAndroidTest`縺ｯCompose UI繝・せ繝・PK縺ｮ繧ｳ繝ｳ繝代う繝ｫ縺ｾ縺ｧ繧堤｢ｺ隱阪＠縺ｾ縺吶６I繝・せ繝医・螳溯｡後↓縺ｯ
螳滓ｩ溘∪縺溘・襍ｷ蜍墓ｸ医∩繧ｨ繝溘Η繝ｬ繝ｼ繧ｿ縺悟ｿ・ｦ√〒縺吶・
## 繝峨く繝･繝｡繝ｳ繝・
- [蝓ｺ譛ｬ繝ｻ隧ｳ邏ｰ險ｭ險・(docs/android-punch-reader-design.md)
- [Claude Code髢狗匱隕冗ｴЬ(CLAUDE.md)
- [Android UI繝・じ繧､繝ｳSkill](.claude/skills/android-ui-design-system/SKILL.md)
- [Compose逕ｻ髱｢霑ｽ蜉Skill](.claude/skills/add-compose-screen/SKILL.md)
