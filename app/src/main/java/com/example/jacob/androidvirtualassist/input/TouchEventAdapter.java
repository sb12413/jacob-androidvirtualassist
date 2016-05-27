
package com.example.jacob.androidvirtualassist.input;

import android.app.Activity;
import android.support.v4.view.GestureDetectorCompat;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import com.realvnc.vncsdk.Viewer;

import java.util.EnumSet;

/**
 * Detects touch gestures and sends the corresponding touch events. A pinch gesture will scale the
 * FrameBufferView and a drag gesture will scroll the FrameBufferView. The scaling and positioning
 * of the framebuffer is very basic in this sample. A pinch gesture will just adjust the scale
 * factor, in a more complete sample the pinch gesture should be implemented in such a way as to
 * translate the framebuffer at the same time so that view is scaled from the centre of the pinch
 * gesture.
 *
 * Single tap sends pointer events to create a left mouse button click. Double tap sends a double
 * left click.
 */
public class TouchEventAdapter extends GestureDetector.SimpleOnGestureListener implements ScaleGestureDetector.OnScaleGestureListener {
    private static final String TAG = "TouchEventAdapter";
    private final Callback mCallback;
    ScaleGestureDetector mScaleGestureDetector;
    GestureDetectorCompat mGestureDetector;

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        mCallback.onScaleChanged(detector.getScaleFactor());
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        mCallback.onScaleChanged(detector.getScaleFactor());
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        mCallback.onScaleChanged(detector.getScaleFactor());
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                            float distanceY) {
        mCallback.onScroll(distanceX, distanceY);
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent event) {
        final float x = event.getX(), y = event.getY();
        mCallback.onPointerEvent((int) x, (int) y, EnumSet.of(Viewer.MouseButton.MOUSE_BUTTON_LEFT));
        mCallback.onPointerEvent((int) x, (int) y, EnumSet.noneOf(Viewer.MouseButton.class));
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent event) {
        final float x = event.getX(), y = event.getY();
        mCallback.onPointerEvent((int) x, (int) y, EnumSet.of(Viewer.MouseButton.MOUSE_BUTTON_LEFT));
        mCallback.onPointerEvent((int) x, (int) y, EnumSet.noneOf(Viewer.MouseButton.class));
        mCallback.onPointerEvent((int) x, (int) y, EnumSet.of(Viewer.MouseButton.MOUSE_BUTTON_LEFT));
        mCallback.onPointerEvent((int) x, (int) y, EnumSet.noneOf(Viewer.MouseButton.class));
        return true;
    }

    /**
     * Callback interface for responding to scroll, scale and pointer events
     */
    public interface Callback {
        void onPointerEvent(int x, int y, EnumSet<Viewer.MouseButton> mouseButtons);
        void onScaleChanged(float scale);
        void onScroll(float x, float y);
    }

    public TouchEventAdapter(Activity context, Callback callback) {
        mCallback = callback;
        mGestureDetector = new GestureDetectorCompat(context, this);
        mScaleGestureDetector = new ScaleGestureDetector(context, this);
    }

    public boolean onTouchEvent(MotionEvent event) {
        boolean handled = false;
        handled |= this.mGestureDetector.onTouchEvent(event);
        handled |= this.mScaleGestureDetector.onTouchEvent(event);
        return handled;
    }
}
