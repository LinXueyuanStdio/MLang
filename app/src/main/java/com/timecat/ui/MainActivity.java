package com.timecat.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        MyLang.getInstance().loadRemoteLanguages(this);

        LinearLayout containerLayout = new LinearLayout(this);
        containerLayout.setOrientation(LinearLayout.VERTICAL);

        TextView detail = new TextView(this);
        detail.setText(MyLang.getString("LanguageNameInEnglish", R.string.LanguageNameInEnglish));

        Button language = new Button(this);
        language.setText(MyLang.getString("LanguageName", R.string.LanguageName));
        language.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //MyLang.getInstance().applyLanguage();
            }
        });

        containerLayout.addView(language);
        containerLayout.addView(detail);
        //获取状态栏高度
        setContentView(containerLayout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

    }


}
