/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3;

import java.util.Random;

import android.content.Context;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.TextView;
//add by zhouerlong
//add by zhouerlong
//add by zhouerlong
//add by zhouerlong
//add by zhouerlong
//A by zel
//add by zhouerlong
//add by zhouerlong

/**
 * An icon on a PagedView, specifically for items in the launcher's paged view (with compound
 * drawables on the top).
 */
public class PagedViewIcon extends TextView {
//add by zhouerlong
    /** A simple callback interface to allow a PagedViewIcon to notify when it has been pressed */
    public static interface PressedCallback {
        void iconPressed(PagedViewIcon icon);
    }

    @SuppressWarnings("unused")
    private static final String TAG = "PagedViewIcon";
    private static final float PRESS_ALPHA = 0.4f;

    private PagedViewIcon.PressedCallback mPressedCallback;
    private boolean mLockDrawableState = false;

    /// M: for OP09 DeleteButton.
    private boolean mSupportEditAndHideApps = false;
    private boolean mDeleteButtonVisiable = false;
    private Drawable mDeleteButtonDrawable = null;
    private int mDeleteMarginleft;

    private Bitmap mIcon;

    /// M: enable shadow
    private static final boolean ENABLE_SHADOW = false;

    public PagedViewIcon(Context context) {
        this(context, null);
    }

    public PagedViewIcon(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PagedViewIcon(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        /// M: Add for edit and hide apps for op09.
        mSupportEditAndHideApps = false;//LauncherExtPlugin.getInstance().getOperatorCheckerExt(context).supportEditAndHideApps();
        if (mSupportEditAndHideApps) {
            mDeleteButtonDrawable = context.getResources().getDrawable(R.drawable.ic_launcher_delete_holo);
            mDeleteMarginleft = (int) context.getResources().getDimension(R.dimen.apps_customize_delete_margin_left);
        }
    }

    public void onFinishInflate() {
        super.onFinishInflate();

        // Ensure we are using the right text size
        LauncherAppState app = LauncherAppState.getInstance();
        DeviceProfile grid = app.getDynamicGrid().getDeviceProfile();
        /// M: Whether is tablet or use tablet solution, need keep textsize.
        if (grid.isTablet() || getResources().getBoolean(R.bool.allow_rotation)) {
            float fontSize = getContext().getResources().getDimensionPixelSize(
                    R.dimen.normal_text_size);
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSize);
        } else {
//            setTextSize(TypedValue.COMPLEX_UNIT_PX, Launcher.textSize);
        }
    }
    
   //add by zel 这里是添加随机游动 动画

	private static final int ICON_WIDTH = 80;
	private static final int ICON_HEIGHT = 94;
	private static final float DEGREE_0 = 10.8f;
	private static final float DEGREE_1 = -20.0f;
	private static final float DEGREE_2 = 20.0f;
	private static final float DEGREE_3 = -10.8f;
	private static final float DEGREE_4 = 10.8f;
	private static final int ANIMATION_DURATION = 800;

	private int mCount = 0;
	
    public void applyFromApplicationInfo(AppInfo info, boolean scaleUp,
            PagedViewIcon.PressedCallback cb) {

		//m by zhouerlong
        final ResolveInfo resolveInfo = this.mContext.getPackageManager().resolveActivity(info.intent, 0);
        if (resolveInfo ==null) {
        	return ;
        }

    	Launcher launcher = null;
    	if (mContext instanceof Launcher) {
    		 launcher = (Launcher) mContext;
    	}
        IconCache ic = launcher.getIconCache();
        	  mIcon = info.iconBitmap;
              mPressedCallback = cb;

          	        setCompoundDrawables(null, Utilities.createIconDrawable(mIcon),
          	                null, null);
        setText(info.title);
        setTag(info);
    }
    
    
    
    
    Handler mHandle = new Handler();
    public void lockDrawableState() {
        mLockDrawableState = true;
    }
			
    
    
    
    
   
			//A by zel
    public void resetDrawableState() {
        mLockDrawableState = false;
        post(new Runnable() {
            @Override
            public void run() {
                refreshDrawableState();
            }
        });
    }

    protected void drawableStateChanged() {
        super.drawableStateChanged();

        // We keep in the pressed state until resetDrawableState() is called to reset the press
        // feedback
        if (isPressed()) {
            setAlpha(PRESS_ALPHA);
            if (mPressedCallback != null) {
                mPressedCallback.iconPressed(this);
            }
        } else if (!mLockDrawableState) {
            setAlpha(1f);
        }
    }

   
			//A by zel
	public Bitmap  getExtractDrawable(Bitmap  src) {
		
		 Paint p = new Paint();
	        p.setColor(Color.WHITE);
		Bitmap b = src;
		Bitmap bitmap = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		canvas.drawBitmap(b.extractAlpha(), 0, 0, p);
		return  bitmap;
	}
			//A by zel
    @Override
    public void draw(Canvas canvas) {
        // If text is transparent, don't draw any shadow
        if (getCurrentTextColor() == getResources().getColor(android.R.color.transparent)) {
            getPaint().clearShadowLayer();
            super.draw(canvas);
            return;
        }

        /// M: it may drop performance a lot
        if(ENABLE_SHADOW == true) {
            // We enhance the shadow by drawing the shadow twice
//            getPaint().setShadowLayer(BubbleTextView.SHADOW_LARGE_RADIUS, 0.0f,
//                    BubbleTextView.SHADOW_Y_OFFSET, BubbleTextView.SHADOW_LARGE_COLOUR);
            super.draw(canvas);
            canvas.save(Canvas.CLIP_SAVE_FLAG);
            canvas.clipRect(getScrollX(), getScrollY() + getExtendedPaddingTop(),
                    getScrollX() + getWidth(),
                    getScrollY() + getHeight(), Region.Op.INTERSECT);
//            getPaint().setShadowLayer(BubbleTextView.SHADOW_SMALL_RADIUS, 0.0f, 0.0f,
//                    BubbleTextView.SHADOW_SMALL_COLOUR);
            super.draw(canvas);
            canvas.restore();
        } else {
            super.draw(canvas);
        }

        /// M: For op09 need draw delete Button.
        if (mSupportEditAndHideApps && mDeleteButtonVisiable) {
            int deleteButtonWidth = mDeleteButtonDrawable.getIntrinsicWidth();
            int deleteButtonHeight = mDeleteButtonDrawable.getIntrinsicHeight();
            int deleteButtonPosX = getScrollX() + mDeleteMarginleft;
            int deleteButtonPosY = getScrollY();

            Rect deleteButtonBounds = new Rect(0, 0, deleteButtonWidth, deleteButtonHeight);
            mDeleteButtonDrawable.setBounds(deleteButtonBounds);

            canvas.save();
            canvas.translate(deleteButtonPosX, deleteButtonPosY);

            mDeleteButtonDrawable.draw(canvas);

            canvas.restore();
        }
//add by zhouerlong
    }

    /// M: for OP09 DeleteButton.@{
    public void setDeleteButtonVisibility(boolean visiable) {
        mDeleteButtonVisiable = visiable;
    }

    public boolean getDeleteButtonVisibility() {
        return mDeleteButtonVisiable;
    }
    /// M: for OP09 DeleteButton.}@

//add by zhouerlong
    private Drawable mDrawable=null;
			

	
			
	
}
 