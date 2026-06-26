# libxposed API 102 混淆规则
-dontwarn io.github.libxposed.annotation.**
-adaptresourcefilecontents META-INF/xposed/java_init.list
-keep,allowoptimization,allowobfuscation public class * extends io.github.libxposed.api.XposedModule {
    public <init>();
}

# 保留模块入口类
-keep class com.fanqie.auto.hook.MainHook { *; }

# 保留Xposed相关类
-keep class io.github.libxposed.api.** { *; }
-keep @io.github.libxposed.api.annotations.XposedHooker class * { *; }
