package com.team341.daisycv;

import android.app.Activity;
import android.content.Context;
import android.hardware.camera2.CaptureRequest;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.team341.daisycv.vision.ImageProcessor;
import com.team341.daisycv.vision.ImageProcessor.PROCESSING_MODE;
import java.util.HashMap;
import org.opencv.android.BetterCamera2Renderer;
import org.opencv.android.BetterCameraGLSurfaceView;

/**
 * This is the main view for providing images to the display. Thanks to 254 for providing a
 * "Better" implementation of CameraGLSurfaceView that takes Camera Settings as an input, and
 * provides a relevant timestamp for when the capture was started. This allows you to set the
 * camera exposure, capture size, width, ect. They also do the work of calculating the Camera's
 * field of view.
 */

public class CameraView extends BetterCameraGLSurfaceView implements
    BetterCameraGLSurfaceView.CameraTextureListener {

  public static String LOGTAG = "CameraView";

  private TextView mFpsText;
  private int mFrameCounter;
  private long mLastNanoTime;

  static final int kHeight = 480;
  static final int kWidth = 640;

  PROCESSING_MODE procMode = PROCESSING_MODE.NO_PROCESSING;

  static BetterCamera2Renderer.Settings getCameraSettings() {
    BetterCamera2Renderer.Settings settings = new BetterCamera2Renderer.Settings();
    settings.height = kHeight;
    settings.width = kWidth;
    settings.camera_settings = new HashMap<>();
    settings.camera_settings.put(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF);
    settings.camera_settings.put(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
        CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF);
    settings.camera_settings.put(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
        CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_OFF);
    settings.camera_settings.put(CaptureRequest.SENSOR_EXPOSURE_TIME, 30000000L);
    settings.camera_settings.put(CaptureRequest.LENS_FOCUS_DISTANCE, .2f);

    return settings;
  }

  public CameraView(Context context, AttributeSet attrs) {
    super(context, attrs, getCameraSettings());
    this.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        if (procMode == PROCESSING_MODE.NO_PROCESSING) {
          procMode = PROCESSING_MODE.BINARY;
        } else {
          procMode = PROCESSING_MODE.NO_PROCESSING;
        }
      }
    });
  }

  /**
   * A callback for when the CameraView starts to display images
   *
   * @param width -  the width of the frames that will be delivered
   * @param height - the height of the frames that will be delivered
   */
  @Override
  public void onCameraViewStarted(int width, int height) {

  }

  /**
   * A callback for when the CameraView stops receiving display images
   */
  @Override
  public void onCameraViewStopped() {

  }

  /**
   * A callback for when  a new camera frame is available
   *
   * @param texIn -  the OpenGL texture ID that contains frame in RGBA format
   * @param texOut - the OpenGL texture ID that can be used to store modified frame image t display
   * @param width -  the width of the frame
   * @param height - the height of the frame
   * @return True if we have processed the frame and want to draw the processed frame, false
   * otherwise
   */
  @Override
  public boolean onCameraTexture(int texIn, int texOut, int width, int height, long timeStamp) {
    // as soon as we hit 30 frames, let's calculate frames/second
    mFrameCounter++;
    if (mFrameCounter >= 30) {
      final int fps = (int) (mFrameCounter * 1e9 / (System.nanoTime() - mLastNanoTime));

      /* If mFpsText is null, grab the instance from the layout.
       * Otherwise, add the fpsUpdater to the Main looper's runnable queue (all UI must be done
       * on the UI thread, AKA the main thread */
      if (mFpsText != null) {
        Runnable fpsUpdater = new Runnable() {
          public void run() {
            mFpsText.setText("FPS: " + fps);
          }
        };
        new Handler(Looper.getMainLooper()).post(fpsUpdater);
      } else {
        Log.d(LOGTAG, "mFpsText == null");
        mFpsText = (TextView) ((Activity) getContext()).findViewById(R.id.fps_text_view);
      }
      mFrameCounter = 0;
      mLastNanoTime = System.nanoTime();
    }

    // finally, process the image! This calls our native C++ code
    ImageProcessor.processImage(texIn, texOut, width, height, procMode.ordinal());

    return true;
  }

  public void setProcessingMode(PROCESSING_MODE mode) {
    procMode = mode;
  }
}
