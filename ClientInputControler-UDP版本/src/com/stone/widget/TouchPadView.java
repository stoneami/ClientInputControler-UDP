package com.stone.widget;

import android.R.integer;
import android.content.Context;
import android.nfc.Tag;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class TouchPadView extends View {
	private final static String TAG = "TouchPadView";
	private final static boolean DEBUG = true;

	TouchPadListener mListener;

	public TouchPadView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	public TouchPadView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	public TouchPadView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}

	private int mStartX = -1;
	private int mStartY = -1;

	private int mAccuracy = 15;
	private final static int FACTOR = 5;

	public void setAccuracy(int accuracy) {
		mAccuracy = accuracy;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		int action = event.getAction();

		if (DEBUG)
			Log.i(TAG, "TouchPadView.onTouchEvent(): action = " + action);

		if (action == MotionEvent.ACTION_DOWN) {
			mStartX = (int) event.getX();
			mStartY = (int) event.getY();
		} else if (action == MotionEvent.ACTION_UP) {
			mStartX = -1;
			mStartY = -1;
		} else if (action == MotionEvent.ACTION_MOVE) {
			if (mListener != null) {
				if (DEBUG)
					Log.i(TAG, "TouchPadView.onTouchEvent(): onMove()");				

				if (Math.abs((int) event.getX() - mStartX) - mAccuracy > 0
						|| Math.abs((int) event.getY() - mStartY) - mAccuracy > 0) {
					
					int dx = ((int) event.getX()-mStartX)/FACTOR;
					int dy = ((int) event.getY()-mStartY)/FACTOR;
					
					mListener.onMove(dx, dy);
					
					mStartX = (int) event.getX();
					mStartY = (int) event.getY();
				}
			}
		}

		return true;// super.onTouchEvent(event);
	}

	public void setTouchPadListener(TouchPadListener listener) {
		mListener = listener;
	}

	/*-
	 * For Inner Classes BEGIN>>>
	 */
	public interface TouchPadListener {
		boolean onMove(int dx, int dy);
	}
	/*-
	 * For Inner Classes BEGIN>>>
	 */

}
