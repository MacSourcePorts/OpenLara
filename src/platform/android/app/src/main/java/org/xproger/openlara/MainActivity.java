package org.xproger.openlara;

import java.util.ArrayList;
import javax.microedition.khronos.egl.EGLConfig;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Environment;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnGenericMotionListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;

import com.google.vr.sdk.base.AndroidCompat;
import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrActivity;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;

public class MainActivity extends GvrActivity implements OnTouchListener, OnKeyListener, OnGenericMotionListener {
    static GvrView gvrView;

    private Wrapper wrapper;
    private ArrayList joyList = new ArrayList();

    public static void toggleVR(boolean enable) {
        gvrView.setStereoModeEnabled(enable);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        super.onCreate(savedInstanceState);

        //GLSurfaceView view = new GLSurfaceView(this);
        final GvrView view = new GvrView(this);
        view.setEGLContextClientVersion(2);
        view.setEGLConfigChooser(8, 8, 8, 8, 16, 8);
        //view.setPreserveEGLContextOnPause(true);
        view.setRenderer(wrapper = new Wrapper());

        view.setFocusable(true);
        view.setFocusableInTouchMode(true);

        view.setOnTouchListener(this);
        view.setOnGenericMotionListener(this);
        view.setOnKeyListener(this);
        view.setTransitionViewEnabled(false);

        if (view.setAsyncReprojectionEnabled(true)) {
            AndroidCompat.setSustainedPerformanceMode(this, true);
        }

        //AndroidCompat.setVrModeEnabled(this, false);
        view.setStereoModeEnabled(false);
        view.setDistortionCorrectionEnabled(true);

        view.setOnCloseButtonListener(new Runnable() {
            @Override
            public void run() {
                view.setStereoModeEnabled(false);
                wrapper.toggleVR = true;
            }
        });

        setGvrView(view);

        setContentView(view);

        gvrView = view;

        try {
            String content = Environment.getExternalStorageDirectory().getAbsolutePath();
            wrapper.onCreate(content + "/OpenLara/", getCacheDir().getAbsolutePath() + "/");
        } catch (Exception e) {
            e.printStackTrace();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        wrapper.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        wrapper.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        wrapper.onResume();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction();
        int type = action & MotionEvent.ACTION_MASK;
        int state;

        switch (type) {
            case MotionEvent.ACTION_DOWN :
            case MotionEvent.ACTION_UP :
            case MotionEvent.ACTION_MOVE :
                state = type == MotionEvent.ACTION_MOVE ? 3 : (type == MotionEvent.ACTION_DOWN ? 2 : 1);
                for (int i = 0; i < event.getPointerCount(); i++)
                    wrapper.onTouch(event.getPointerId(i), state, event.getX(i), event.getY(i));
                break;
            case MotionEvent.ACTION_POINTER_DOWN :
            case MotionEvent.ACTION_POINTER_UP :
                int i = (action & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                state = type == MotionEvent.ACTION_POINTER_DOWN ? 2 : 1;
                wrapper.onTouch(event.getPointerId(i), state, event.getX(i), event.getY(i));
                break;
        }
        return true;
    }

    boolean isGamepad(int src) {
        return (src & (InputDevice.SOURCE_GAMEPAD | InputDevice.SOURCE_JOYSTICK)) != 0;
    }

    int getJoyIndex(int joyId) {
        int joyIndex = joyList.indexOf(joyId);
        if (joyIndex == -1) {
            joyIndex = joyList.size();
            joyList.add(joyId);
        }
        return joyIndex;
    }

    @Override
    public boolean onGenericMotion(View v, MotionEvent event) {
        int src = event.getDevice().getSources();

        if (isGamepad(event.getDevice().getSources())) {
            int index = getJoyIndex(event.getDeviceId());
        // axis
            wrapper.onTouch(index, -3, event.getAxisValue(MotionEvent.AXIS_X), event.getAxisValue(MotionEvent.AXIS_Y));
            wrapper.onTouch(index, -4, event.getAxisValue(MotionEvent.AXIS_Z), event.getAxisValue(MotionEvent.AXIS_RZ));

        // d-pad
            if ((src & InputDevice.SOURCE_DPAD) != InputDevice.SOURCE_DPAD) {
                float dx = event.getAxisValue(MotionEvent.AXIS_HAT_X);
                float dy = event.getAxisValue(MotionEvent.AXIS_HAT_Y);
                wrapper.onTouch(index, dx >  0.9 ? -2 : -1, -14, 0);
                wrapper.onTouch(index, dy < -0.9 ? -2 : -1, -15, 0);
                wrapper.onTouch(index, dy >  0.9 ? -2 : -1, -16, 0);
                wrapper.onTouch(index, dx < -0.9 ? -2 : -1, -13, 0);
            }
        }

        return true;
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        int btn;

        switch (keyCode) {
            case KeyEvent.KEYCODE_BUTTON_A      : btn = -1;  break;
            case KeyEvent.KEYCODE_BUTTON_B      : btn = -2;  break;
            case KeyEvent.KEYCODE_BUTTON_X      : btn = -3;  break;
            case KeyEvent.KEYCODE_BUTTON_Y      : btn = -4;  break;
            case KeyEvent.KEYCODE_BUTTON_L1     : btn = -5;  break;
            case KeyEvent.KEYCODE_BUTTON_R1     : btn = -6;  break;
            case KeyEvent.KEYCODE_BUTTON_SELECT : btn = -7;  break;
            case KeyEvent.KEYCODE_BUTTON_START  : btn = -8;  break;
            case KeyEvent.KEYCODE_BUTTON_THUMBL : btn = -9;  break;
            case KeyEvent.KEYCODE_BUTTON_THUMBR : btn = -10; break;
            case KeyEvent.KEYCODE_BUTTON_L2     : btn = -11; break;
            case KeyEvent.KEYCODE_BUTTON_R2     : btn = -12; break;
            case KeyEvent.KEYCODE_DPAD_LEFT     : btn = -13; break;
            case KeyEvent.KEYCODE_DPAD_RIGHT    : btn = -14; break;
            case KeyEvent.KEYCODE_DPAD_UP       : btn = -15; break;
            case KeyEvent.KEYCODE_DPAD_DOWN     : btn = -16; break;
            case KeyEvent.KEYCODE_BACK          : btn = KeyEvent.KEYCODE_ESCAPE; break;
            case KeyEvent.KEYCODE_VOLUME_UP     :
            case KeyEvent.KEYCODE_VOLUME_DOWN   :
            case KeyEvent.KEYCODE_VOLUME_MUTE   : return false;
            default                             : btn = keyCode;
        }

        boolean isDown = event.getAction() == KeyEvent.ACTION_DOWN;
        int index = btn < 0 ? getJoyIndex(event.getDevice().getId()) : 0;
        wrapper.onTouch(index, isDown ? -2 : -1, btn, 0);
        return true;
    }

    static {
        System.loadLibrary("game");
//      System.loadLibrary("gvr");
//        System.load("/storage/emulated/0/libMGD.so");
    }
}

// @TODO: use native OpenSL ES
class Sound {
    private short buffer[];
    private static AudioTrack audioTrack;

    void start(final Wrapper wrapper) {
        int rate = 44100;
        int size = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        //System.out.println(String.format("sound buffer size: %d", bufSize));
        buffer = new short[size / 2];

        try {
            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT, size, AudioTrack.MODE_STREAM);
        }catch (IllegalArgumentException e){
            System.out.println("Error: buffer size is zero");
            return;
        }

        try {
            audioTrack.play();
        }catch (NullPointerException e){
            System.out.println("Error: audioTrack null pointer on start()");
            return;
        }

        new Thread( new Runnable() {
            public void run() {
                while ( audioTrack.getPlayState() != AudioTrack.PLAYSTATE_STOPPED ) {
                    if (audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING && wrapper.ready) {
                        Wrapper.nativeSoundFill(buffer);
                        audioTrack.write(buffer, 0, buffer.length);
                        audioTrack.flush();
                    } else
                        try {
                            Thread.sleep(10);
                        } catch(Exception e) {
                            //
                        }
                }
            }
        } ).start();
    }

    void stop() {
        try {
            audioTrack.flush();
            audioTrack.stop();
            audioTrack.release();
        }catch (NullPointerException e){
            System.out.println("Error: audioTrack null pointer on stop()");
        }
    }

    void play() {
        try {
            audioTrack.play();
        }catch (NullPointerException e){
            System.out.println("Error: audioTrack null pointer on play()");
        }
    }

    void pause() {
        try {
            audioTrack.pause();
        }catch (NullPointerException e){
            System.out.println("Error: audioTrack null pointer on pause()");
        };
    }
}

class Touch {
    int id, state;
    float x, y;
    Touch(int _id, int _state, float _x, float _y) {
        id = _id;
        state = _state;
        x = _x;
        y = _y;
    }
}

class Wrapper implements GvrView.StereoRenderer {
    public static native void nativeInit(String contentDir, String cacheDir);
    public static native void nativeFree();
    public static native void nativeReset();
    public static native void nativeResize(int x, int y, int w, int h);
    public static native void nativeUpdate();
    public static native void nativeSetVR(boolean enabled);
    public static native void nativeSetHead(float head[]);
    public static native void nativeSetEye(int eye, float proj[], float view[]);
    public static native void nativeFrameBegin();
    public static native void nativeFrameEnd();
    public static native void nativeFrameRender();
    public static native void nativeTouch(int id, int state, float x, float y);
    public static native void nativeSoundFill(short buffer[]);

    Boolean ready = false;
    Boolean toggleVR = false;
    private String contentDir;
    private String cacheDir;
    private ArrayList<Touch> touch = new ArrayList<>();
    private Sound sound;

    void onCreate(String contentDir, String cacheDir) {
        this.contentDir  = contentDir;
        this.cacheDir    = cacheDir;

        sound = new Sound();
        sound.start(this);
    }

    void onDestroy() {
        sound.stop();
        nativeFree();
    }

    void onPause() {
        sound.pause();
    }

    void onResume() {
        sound.play();
        if (ready) nativeReset();
    }

    void onTouch(int id, int state, float x, float y) {
        synchronized (this) {
            touch.add(new Touch(id, state, x, y));
        }
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        nativeResize(0, 0, width, height);
    }

    @Override
    public void onSurfaceCreated(EGLConfig config) {
        if (!ready) {
            nativeInit(contentDir, cacheDir);
            sound.play();
            ready = true;
        }
    }

    @Override
    public void onRendererShutdown() {
        //
    }

    @Override
    public void onNewFrame(HeadTransform headTransform) {
        synchronized (this) {
            for (int i = 0; i < touch.size(); i++) {
                Touch t = touch.get(i);
                nativeTouch(t.id, t.state, t.x, t.y);
            }
            touch.clear();
        }

        if (toggleVR) {
            nativeSetVR(false);
            toggleVR = false;
        }

        float view[] = headTransform.getHeadView();
        nativeSetHead(view);

        nativeUpdate();
        nativeFrameBegin();
    }

    @Override
    public void onDrawEye(Eye eye) {
        float proj[] = eye.getPerspective(8.0f, 32.0f * 1024.0f);
        float view[] = eye.getEyeView();

        int index = 0;
        if (eye.getType() == Eye.Type.LEFT)  index = -1;
        if (eye.getType() == Eye.Type.RIGHT) index = +1;

        nativeSetEye(index, proj, view);

        nativeResize(eye.getViewport().x, eye.getViewport().y, eye.getViewport().width, eye.getViewport().height);
        nativeFrameRender();
    }

    @Override
    public void onFinishFrame(Viewport viewport) {
        nativeResize(viewport.x, viewport.y, viewport.width, viewport.height);
        nativeFrameEnd();
    }
}
