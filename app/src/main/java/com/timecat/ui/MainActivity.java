package com.timecat.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.timecat.component.locale.LocaleInfo;
import com.timecat.component.locale.MLang;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        rebuild();
    }

    private void rebuild() {
        setContentView(buildUi(), new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private ViewGroup buildUi() {
        final LinearLayout containerLayout = new LinearLayout(this);
        containerLayout.setOrientation(LinearLayout.VERTICAL);

        final TextView detail = new TextView(this);
        detail.setPadding(20, 20, 20, 20);
        detail.setTextSize(18);
        detail.setGravity(Gravity.CENTER);
        String detailText = "语言详情"
                + "\n"
                + "当前语言设置：" + MyLang.loadLanguageKeyInLocal()
                + "\n"
                + "当前语言的英语名：" + MyLang.getString("LanguageNameInEnglish", R.string.LanguageNameInEnglish)
                + "\n\n本地缺失，云端存在的字符串：\n"
                + MyLang.getString("remote_string_only", R.string.fallback_string)
                + "\n\n本地云端都存在，云端将覆盖本地的字符串：\n"
                + MyLang.getString("local_string", R.string.local_string);
        detail.setText(detailText);
        containerLayout.addView(detail);
        MLang.USE_CLOUD_STRINGS = true;
        MyLang.loadRemoteLanguages(this, new MLang.FinishLoadCallback() {
            @Override
            public void finishLoad() {
                containerLayout.removeAllViews();
                containerLayout.addView(detail);
                for (final LocaleInfo info : MyLang.getInstance().remoteLanguages) {
                    Button btn = new Button(MainActivity.this);
                    btn.setText(info.getSaveString());
                    btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            MyLang.applyLanguage(MainActivity.this, info);
                            rebuild();
                        }
                    });
                    containerLayout.addView(btn);
                }
            }
        });
        return containerLayout;
    }
}
