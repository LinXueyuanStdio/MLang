# MLang 动态化多语言框架

`MLang` 是 MultiLanguage 的简写，是一款动态化的多语言框架。

- [x] 动态下发语言包
- [x] 语言包的增加、升级、删除
- [x] 语言包内部任意字符串的增加、升级、删除
- [x] 自定义语言包的存储路径
- [x] 跟随系统语言
- [x] 处理各种语言的时区、时间格式化问题
- [x] 处理各种语言的复数格式化问题
- [x] static 方法 + 单例模式，一处安装，到处使用

## 1. 使用

使用字符串
```java
// 本地和云端都存在的字符串
MyLang.getString("local_string", R.string.local_string)

// 云端存在 remote_string_only
// 但本地没有 R.string.remote_string_only，用 R.string.fallback_string 代替
MyLang.getString("remote_string_only", R.string.fallback_string)
```
使用语言包
```java
//应用一种语言（这里自动处理了语言包的升级、语言包内部字符串的升级）
MyLang.getInstance().applyLanguage(Context, LocaleInfo, force=true, init=false);

//删除一种语言
MyLang.getInstance().deleteLanguage(Context, LocaleInfo);
```
`LocaleInfo` 可以在以下地方找到
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

## 2. 安装

2.1. 引入

```java
//build.gradle
allprojects {
    repositories {
        google()
        jcenter()
        maven { url "https://github.com/LinXueyuanStdio/MLang/raw/main/dist/" }
    }
}

//app/build.gradle
implementation 'com.timecat.component:MLang:2.0.4'
```

2.2. 在 Application 中初始化，并监听系统语言的更改（如果跟随系统语言的话）：
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

其中建议自己新建一个静态类 MyLang 来代理 MLang。
这样有两个好处：
1. 隔绝 MLang 的 api 变化，提高兼容性和稳定性。
2. 使用更简洁。MLang 不持有 context，但每次获取字符串为空时，需要 context 来兜底，获取本地的字符串。在自己的 MyLang 默认提供 application Context，可以不用到处提供 context，更简洁。

```java
public class MyLang {
    private static File filesDir = getFilesDirFixed(getContext());
    private static LangAction action = new MyLangAction();
    public static void init(@NonNull Context applicationContext) {
        getInstance(applicationContext);
    }
    public static void onConfigurationChanged(@NonNull Configuration newConfig) {
        getInstance().onDeviceConfigurationChange(getContext(), newConfig);
    }
    public static Context getContext() {
        return MyApplication.applicationContext;
    }
    public static MLang getInstance() {
        return getInstance(getContext());
    }
    public static MLang getInstance(Context context) {
        return MLang.getInstance(context, filesDir, action);
    }
}
```

## 3. 设计

### 3.1. 单例模式接收 3 个参数，context，fileDir，action
1. context：MLang 内部不持有该 context。该 context 用于注册时区广播（根据时区来格式化字符串中的时间）、 判断系统当前时间是否 24 小时制等等。
2. filesDir：持久化语言包文件的存储地址。语言包文件是 xml 格式，和 res 下的 strings.xml 一样。
3. action：action 包含了应用语言包、切换语言等等需要的所有回调，即 `LangAction` 接口。

```java
MLang.getInstance(context, filesDir, action);
```

### 3.2. `LangAction` 接口定义了 2 个东西
1. 当前语言的设置存储。
   MLang 根据语言 id (string) 来识别当前语言。语言 id 需要持久化。
   所以设计了下面两个方法，可以自行决定持久化的方式（SharedPreferences、MMKV、SQLite等等）。
   ```java
   void saveLanguageKeyInLocal(String language);
   @Nullable String loadLanguageKeyInLocal();
   ```
2. 必要的网络接口。
   ```java
   void langpack_getDifference(String lang_pack, String lang_code, int from_version, @NonNull final LangAction.GetDifferenceCallback callback)
   void langpack_getLanguages(@NonNull final LangAction.GetLanguagesCallback callback)
   void langpack_getLangPack(String lang_code, @NonNull final LangAction.GetLangPackCallback callback)
   ```

`LangAction` 的注释如下：

```java
public interface LangAction {
    /**
     * SharedPreferences preferences = Utilities.getGlobalMainSettings();
     * SharedPreferences.Editor editor = preferences.edit();
     * editor.putString("language", language);
     * editor.commit();
     * @param language localeInfo.getKey() 语言 id
     */
    void saveLanguageKeyInLocal(String language);

    /**
     * SharedPreferences preferences = Utilities.getGlobalMainSettings();
     * String lang = preferences.getString("language", null);
     * @return @Nullable lang 语言 id
     */
    String loadLanguageKeyInLocal();

    /**
     * 在其他线程网络请求，在主线程或UI线程调用callback
     * 这里设计成这样，是因为这个方法里支持异步执行
     * 您需要在合适的时机手动调用 callback，且只能调用一次
     * @param lang_pack 语言包名字
     * @param lang_code 语言包版本名称
     * @param from_version 语言包版本号
     * @param callback @NonNull 在主线程或UI线程调用
     */
    void langpack_getDifference(String lang_pack, String lang_code, int from_version, GetDifferenceCallback callback);

    /**
     * 在其他线程网络请求，在主线程或UI线程调用callback
     * 这里设计成这样，是因为这个方法里支持异步执行
     * 您需要在合适的时机手动调用 callback，且只能调用一次
     * @param callback @NonNull 在主线程或UI线程调用
     */
    void langpack_getLanguages(GetLanguagesCallback callback);

    /**
     * 在其他线程网络请求，在主线程或UI线程调用callback
     * 这里设计成这样，是因为这个方法里支持异步执行
     * 您需要在合适的时机手动调用 callback，且只能调用一次
     * @param lang_code 语言包版本名称
     * @param callback @NonNull 在主线程或UI线程调用
     */
    void langpack_getLangPack(String lang_code, GetLangPackCallback callback);

    interface GetLanguagesCallback {
        /**
         * 必须在UI线程或者主线程调用
         * 所有可用的语言包
         * @param languageList 语言包列表
         */
        void onLoad(List<LangPackLanguage> languageList);
    }

    interface GetDifferenceCallback {
        /**
         * 必须在UI线程或者主线程调用
         * 如果服务端没有实现增量分发的功能，可以用完整的语言包代替
         * @param languageList 增量的语言包
         */
        void onLoad(LangPackDifference languageList);
    }

    interface GetLangPackCallback {
        /**
         * 必须在UI线程或者主线程调用
         * @param languageList 完整的语言包
         */
        void onLoad(LangPackDifference languageList);
    }

}
```

实现`LangAction`的一个示例如下：

```java
public class MyLangAction implements LangAction {
   @Override
   public static void saveLanguageKeyInLocal(String language) {
       SharedPreferences preferences = getContext().getSharedPreferences("language_locale", Context.MODE_PRIVATE);
       SharedPreferences.Editor editor = preferences.edit();
       editor.putString("language", language);
       editor.apply();
   }

   @Override
   @Nullable
   public static String loadLanguageKeyInLocal() {
       SharedPreferences preferences = getContext().getSharedPreferences("language_locale", Context.MODE_PRIVATE);
       return preferences.getString("language", null);
   }
   @Override
   public void langpack_getDifference(String lang_pack, String lang_code, int from_version, @NonNull final LangAction.GetDifferenceCallback callback) {
       Server.request_langpack_getDifference(lang_pack, lang_code, from_version, new Server.GetDifferenceCallback() {
           @Override
           public void onNext(final LangPackDifference difference) {
               callback.onLoad(difference);
           }
       });
   }

   @Override
   public void langpack_getLanguages(@NonNull final LangAction.GetLanguagesCallback callback) {
       Server.request_langpack_getLanguages(new Server.GetLanguagesCallback() {
           @Override
           public void onNext(final List<LangPackLanguage> languageList) {
               callback.onLoad(languageList);
           }
       });
   }

   @Override
   public void langpack_getLangPack(String lang_code, @NonNull final LangAction.GetLangPackCallback callback) {
       Server.request_langpack_getLangPack(lang_code, new Server.GetLangPackCallback() {
           @Override
           public void onNext(final LangPackDifference difference) {
               callback.onLoad(difference);
           }
       });
   }
}
```
### 3.3. 服务器语言包的结构

[模拟的服务器数据](https://github.com/LinXueyuanStdio/MLang/blob/main/app/src/main/java/com/timecat/ui/Server.java)

语言包实体
- `LangPackLanguage(name, version, ...)`

语言包的数据
- `LangPackDifference(name, version, List<LangPackString>, ...)`
- `LangPackString(key: String, value: String)`

```java
public class Server {
    public static LangPackLanguage chineseLanguage() {
        LangPackLanguage langPackLanguage = new LangPackLanguage();
        langPackLanguage.name = "chinese";
        langPackLanguage.native_name = "简体中文";
        langPackLanguage.lang_code = "zh";
        langPackLanguage.base_lang_code = "zh";
        return langPackLanguage;
    }
    public static LangPackDifference chinesePackDifference() {
        LangPackDifference difference = new LangPackDifference();
        difference.lang_code = "zh";
        difference.from_version = 0;
        difference.version = 1;
        difference.strings = chineseStrings();
        return difference;
    }
    public static ArrayList<LangPackString> chineseStrings() {
        ArrayList<LangPackString> list = new ArrayList<>();
        list.add(new LangPackString("LanguageName", "中文简体"));
        list.add(new LangPackString("LanguageNameInEnglish", "Chinese"));
        list.add(new LangPackString("local_string", "中文的云端字符串"));
        list.add(new LangPackString("remote_string_only", "本地缺失，云端存在的字符串"));
        return list;
    }
}
```
