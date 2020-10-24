package com.timecat.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.timecat.component.locale.MLang;

public class MainActivity extends Activity {
    LinearLayout containerLayout;
    TextView detail;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        containerLayout = new LinearLayout(this);
        containerLayout.setOrientation(LinearLayout.VERTICAL);
        detail = new TextView(this);
        detail.setText("语言详情\n"
                + MyLang.getString("LanguageNameInEnglish", R.string.LanguageNameInEnglish)
                + "\n"
                + MyLang.getString("test", R.string.test)
        );
        containerLayout.addView(detail);

        MyLang.getInstance().loadRemoteLanguages(this, new MLang.FinishLoadCallback() {
            @Override
            public void finishLoad() {
                containerLayout.removeAllViews();
                containerLayout.addView(detail);
                for (final MLang.LocaleInfo info : MyLang.getInstance().remoteLanguages) {
                    Button language = new Button(MainActivity.this);
                    language.setText(info.getSaveString());
                    language.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            MyLang.getInstance().applyLanguage(MainActivity.this, info, false, false);
                            recreate();
                        }
                    });
                    containerLayout.addView(language);
                }
            }
        });

        //获取状态栏高度
        setContentView(containerLayout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }


}
