package com.locale.ui.segment;

import android.view.View;

import com.locale.lib.core.BaseSegment;
import com.locale.ui.R;

public class DetailSegment extends BaseSegment implements View.OnClickListener{
    @Override
    protected int getLayoutId() {
        return R.layout.segment_detail_layout;
    }

    @Override
    protected void initView(View root) {
        super.initView(root);
        root.findViewById(R.id.btn_back).setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_back:
                presentFragment(new SplashSegment(),true);
                break;
        }
    }
}
