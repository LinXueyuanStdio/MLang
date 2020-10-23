package com.locale.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.util.Log;
import android.view.ActionMode;
import android.view.ViewGroup;
import android.view.Window;

import com.locale.lib.core.ActionBarLayout;
import com.locale.lib.core.BaseSegment;
import com.locale.lib.util.AndroidUtilities;
import com.locale.ui.segment.SplashSegment;

import java.util.ArrayList;

public class MainActivity extends Activity implements ActionBarLayout.ActionBarLayoutDelegate{
    private ActionBarLayout actionBarLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        //获取状态栏高度
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            AndroidUtilities.statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }

        actionBarLayout = new ActionBarLayout(this);
        actionBarLayout.init(new ArrayList<BaseSegment>());
        actionBarLayout.setDelegate(this);

//        actionBarLayout.setBottomBar(createBottomBar());
//        actionBarLayout.setBottomBarHeight(240);

        actionBarLayout.presentFragment(new SplashSegment());
        setContentView(actionBarLayout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        BaseSegment lastSegment = null;
        if (!actionBarLayout.fragmentsStack.isEmpty()) {
            lastSegment = actionBarLayout.fragmentsStack.get(actionBarLayout.fragmentsStack.size() - 1);

            if (lastSegment != null) {
                Bundle args = lastSegment.getArguments();
                lastSegment.saveSelfArgs(args);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        actionBarLayout.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        actionBarLayout.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (actionBarLayout.fragmentsStack.size() != 0) {
            BaseSegment segment = actionBarLayout.fragmentsStack.get(actionBarLayout.fragmentsStack.size() - 1);
            segment.onActivityResultFragment(requestCode, resultCode, data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (actionBarLayout.fragmentsStack.size() != 0) {
            BaseSegment segment = actionBarLayout.fragmentsStack.get(actionBarLayout.fragmentsStack.size() - 1);
            segment.onRequestPermissionsResultFragment(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        actionBarLayout.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        actionBarLayout.onBackPressed();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        actionBarLayout.onLowMemory();
    }

    @Override
    public void onActionModeStarted(ActionMode mode) {
        super.onActionModeStarted(mode);
        actionBarLayout.onActionModeStarted(mode);
    }

    @Override
    public void onActionModeFinished(ActionMode mode) {
        super.onActionModeFinished(mode);
        actionBarLayout.onActionModeFinished(mode);
    }

    @Override
    public boolean onPreIme() {
        Log.d("which2","onPreIme...");
        return false;
    }

    @Override
    public boolean needPresentFragment(BaseSegment fragment, boolean removeLast, boolean forceWithoutAnimation, ActionBarLayout layout) {
        Log.d("which2","needPresentFragment...");

        //这个很重要，必须要返回true，才可以到下个界面
        return true;
    }

    @Override
    public boolean needAddFragmentToStack(BaseSegment fragment, ActionBarLayout layout) {
        Log.d("which2","needAddFragmentToStack...");



        return true;
    }

    @Override
    public boolean needCloseLastFragment(ActionBarLayout layout) {
        Log.d("which2","needCloseLastFragment...");
        if(actionBarLayout.fragmentsStack.size() <= 1){
            finish();
            return false;
        }

        return true;
    }

    @Override
    public void onRebuildAllFragments(ActionBarLayout layout) {
        Log.d("which2","onRebuildAllFragments...");
    }
}
