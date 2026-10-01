# Xposed 通过反射加载 HookEntry，禁止混淆其类名和方法名
-keep class com.example.lspadskip.HookEntry { *; }
-keep class de.robv.android.xposed.** { *; }
