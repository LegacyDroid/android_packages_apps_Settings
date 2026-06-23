package com.android.settings.legacydroid;

import android.content.Context;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.os.HardwarePropertiesManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

public class CpuGpuUtils {
    private static final String TAG = "CpuGpuUtils";

    public static String getCpuName() {
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream("/proc/cpuinfo")));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("model name") || line.startsWith("Hardware")) {
                    reader.close();
                    String[] parts = line.split(":");
                    if (parts.length >= 2) {
                        return parts[1].trim();
                    }
                }
            }
            reader.close();
        } catch (Exception e) {
            Log.e(TAG, "Failed to read /proc/cpuinfo", e);
        }
        return null;
    }

    public static String getGpuRenderer() {
        EGLDisplay dpy = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (dpy == EGL14.EGL_NO_DISPLAY) return null;

        int[] version = new int[2];
        if (!EGL14.eglInitialize(dpy, version, 0, version, 1)) return null;

        int[] configAttribs = {
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_NONE
        };
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        if (!EGL14.eglChooseConfig(dpy, configAttribs, 0, configs, 0, 1, numConfigs, 0)) {
            EGL14.eglTerminate(dpy);
            return null;
        }
        if (numConfigs[0] == 0) { EGL14.eglTerminate(dpy); return null; }

        EGLContext context = EGL14.eglCreateContext(dpy, configs[0], EGL14.EGL_NO_CONTEXT,
                new int[]{EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE}, 0);
        if (context == EGL14.EGL_NO_CONTEXT) { EGL14.eglTerminate(dpy); return null; }

        EGLSurface surface = EGL14.eglCreatePbufferSurface(dpy, configs[0],
                new int[]{EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE}, 0);
        if (surface == EGL14.EGL_NO_SURFACE) {
            EGL14.eglDestroyContext(dpy, context);
            EGL14.eglTerminate(dpy);
            return null;
        }

        EGL14.eglMakeCurrent(dpy, surface, surface, context);
        String renderer = GLES20.glGetString(GLES20.GL_RENDERER);

        EGL14.eglMakeCurrent(dpy, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT);
        EGL14.eglDestroySurface(dpy, surface);
        EGL14.eglDestroyContext(dpy, context);
        EGL14.eglTerminate(dpy);

        return renderer;
    }

    public static String getCpuTemp(Context context) {
        try {
            HardwarePropertiesManager hpm = (HardwarePropertiesManager)
                    context.getSystemService(Context.HARDWARE_PROPERTIES_SERVICE);
            if (hpm != null) {
                float[] temps = hpm.getDeviceTemperatures(
                        HardwarePropertiesManager.DEVICE_TEMPERATURE_CPU,
                        HardwarePropertiesManager.TEMPERATURE_CURRENT);
                if (temps != null && temps.length > 0) {
                    float temp = temps[0];
                    if (temp != HardwarePropertiesManager.UNDEFINED_TEMPERATURE) {
                        return String.format("%.1f", temp);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get CPU temp via HardwarePropertiesManager", e);
        }
        return null;
    }
}
