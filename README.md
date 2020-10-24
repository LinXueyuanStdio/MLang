# MLang 动态化多语言框架

`MLang` 是 MultiLanguage 的简写，是一款动态化的多语言框架。

- [x] 动态下发语言包
- [x] 语言包的增加、升级、删除
- [x] 语言包内部任意字符串的增加、升级、删除
- [x] 跟随系统语言
- [x] 详尽的中文注释
- [x] 完善的 demo

## 安装

1. 引入

```java
//build.gradle
allprojects {
    repositories {
        google()
        jcenter()
        maven { url "https://raw.githubusercontent.com/LinXueyuanStdio/MLang/main/dist" }
    }
}

//app/build.gradle
implementation 'com.timecat.component:MLang:1.0.1'
```

2. 在 Application 中初始化，并监听系统语言的更改（如果跟随系统语言的话）：
```java
public class MyApplication extends Application {
    @SuppressLint("StaticFieldLeak")
    public static volatile Context applicationContext;
    public static volatile Handler applicationHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        applicationContext = this;
        applicationHandler = new Handler(applicationContext.getMainLooper());
        MyLang.init(applicationContext);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        MyLang.onConfigurationChanged(newConfig);
    }
}

```
其中建议自己新建一个静态类 MyLang 来隔绝 MLang 的 api 变化，提高兼容性和稳定性。
```java
public class MyLang {
    public static void init(@NonNull Context applicationContext) {
        MLang.action = new AbsLangAction() {...};
        try {
            MLang.getInstance(applicationContext);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void onConfigurationChanged(@NonNull Configuration newConfig) {
        MLang.getInstance(getContext()).onDeviceConfigurationChange(getContext(), newConfig);
    }
    public static Context getContext() {
        return MyApplication.applicationContext;
    }
    ...
}
```

`AbsLangAction` 抽象类定义了必要的网络接口，注释和demo给出了详细的说明和实现，只需照抄即可。

## 使用

使用字符串
```java
//本地和云端都存在都字符串
MyLang.getString("local_string", R.string.local_string)
//本地没有，云端存在的字符串
MyLang.getString("remote_string_only", R.string.fallback_string)
```
使用语言包
```java
//应用一种语言
MyLang.getInstance().applyLanguage(Context, MLang.LocaleInfo, force=true, init=false);
//删除一种语言
MyLang.getInstance().deleteLanguage(Context, MLang.LocaleInfo);
```
`MLang.LocaleInfo` 可以在以下地方找到
```java
//1. 所有云端的语言包
MyLang.getInstance().remoteLanguages
//2. 所有下载到本地、可用的语言包
MyLang.getInstance().languages
//3. 所有非官方的语言包
MyLang.getInstance().unofficialLanguages
//4. 除内置支持的语言外，另外安装的云端的语言包
MyLang.getInstance().otherLanguages
```
