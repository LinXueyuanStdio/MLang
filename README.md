# MLang

多语言框架

## 安装

引入

在 Application 中初始化，并监听系统语言的更改（如果跟随系统语言的话）：
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
2. 使用

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
