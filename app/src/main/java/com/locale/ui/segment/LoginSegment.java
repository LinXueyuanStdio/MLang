package com.locale.ui.segment;

import android.content.Context;
import android.util.Log;
import android.view.View;

import com.locale.lib.core.BaseSegment;
import com.locale.ui.R;

public class LoginSegment extends BaseSegment{
    @Override
    protected int getLayoutId() {
        return R.layout.segment_login;
    }

    @Override
    protected void initView(View root) {
        super.initView(root);
        root.findViewById(R.id.btn_login_in_login).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presentFragment(new MainSegment());
            }
        });

    }

    @Override
    public boolean onFragmentCreate() {
        Log.d(TAG,"onFragmentCreate...");
        return super.onFragmentCreate();
    }

    @Override
    public View createView(Context context) {
        Log.d(TAG,"createView...");
        return super.createView(context);
    }

    @Override
    protected void onRemoveFromParent() {
        super.onRemoveFromParent();
        Log.d(TAG,"onRemoveFromParent...");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG,"onPause...");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG,"onResume...");
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        Log.d(TAG,"onFragmentDestroy...");
    }
}
