package com.locale.ui.segment;

import android.content.Intent;
import android.view.View;
import android.widget.Toast;

import com.locale.lib.core.BaseSegment;
import com.locale.ui.R;
import com.locale.ui.TestActivity;

public class MainSegment extends BaseSegment{
    @Override
    protected int getLayoutId() {
        return R.layout.segment_main;
    }

    @Override
    protected void initView(View root) {
        super.initView(root);
        root.findViewById(R.id.btn_main).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getParentActivity(),"我是主界面",Toast.LENGTH_SHORT).show();
                presentFragment(new DetailSegment());
            }
        });

        root.findViewById(R.id.btn_test).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getParentActivity(),TestActivity.class);
                parentLayout.startActivityForResult(intent,100);
            }
        });


    }

    @Override
    public boolean onBackPressed() {
        return true;
    }
}
