package com.lvonasek.openconstructor;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.WindowManager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public abstract class AbstractActivity extends Activity
{
  protected static final int BUFFER_SIZE = 65536;
  protected static final String CARDBOARD_APP = "com.lvonasek.daydreamOBJ";
  protected static final String FILE_KEY = "FILE2OPEN";
  protected static final String MODEL_DIRECTORY = "/Models/";
  protected static final String RESOLUTION_KEY = "RESOLUTION";
  protected static final String TEMP_DIRECTORY = "dataset";
  protected static final String URL_KEY = "URL2OPEN";
  protected static final String USER_AGENT = "Mozilla/5.0 Google";
  public static final String[] FILE_EXT = {".obj"};
  public static final String TAG = "tango_app";

  public static boolean isAirplaneModeOn(Context context)
  {
    return Settings.Global.getInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
  }

  public static void installCardboardApp(Context context)
  {
    Intent i = new Intent(Intent.ACTION_VIEW);
    i.setData(Uri.parse("https://play.google.com/store/apps/details?id=" + CARDBOARD_APP));
    context.startActivity(i);
  }

  public static boolean isCardboardAppInstalled(Context context)
  {
    try {
      context.getPackageManager().getPackageInfo(CARDBOARD_APP, 0);
      return true;
    } catch (PackageManager.NameNotFoundException e) {
      return false;
    }
  }

  public static boolean isCardboardEnabled(Context context)
  {
    if (!isCardboardAppInstalled(context))
      return false;
    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
    String key = context.getString(R.string.pref_cardboard);
    return pref.getBoolean(key, false);
  }

  public static boolean isPortrait(Context context) {
    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
    String key = context.getString(R.string.pref_landscape);
    return !pref.getBoolean(key, false);
  }

  public boolean isNoiseFilterOn()
  {
    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
    String key = getString(R.string.pref_noisefilter);
    return pref.getBoolean(key, false);
  }

  public boolean isTexturingOn()
  {
    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
    String key = getString(R.string.pref_texture);
    return pref.getBoolean(key, true);
  }

  public static int getModelType(String filename) {
    for(int i = 0; i < FILE_EXT.length; i++) {
      int begin = filename.length() - FILE_EXT[i].length();
      if (begin >= 0)
        if (filename.substring(begin).contains(FILE_EXT[i]))
          return i;
    }
    return -1;
  }

  public static ArrayList<String> getObjResources(File file)
  {
    ArrayList<String> output = new ArrayList<>();
    String mtlLib = null;
    try
    {
      Scanner sc = new Scanner(new FileInputStream(file.getAbsolutePath()));
      while(sc.hasNext()) {
        String line = sc.nextLine();
        if (line.startsWith("mtllib")) {
          mtlLib = line.substring(7);
          output.add(mtlLib);
          break;
        }
      }
      sc.close();
    } catch (FileNotFoundException e)
    {
      e.printStackTrace();
    }
    if (mtlLib != null) {
      mtlLib = file.getParent() + "/" + mtlLib;
      try
      {
        Scanner sc = new Scanner(new FileInputStream(mtlLib));
        while(sc.hasNext()) {
          String line = sc.nextLine();
          if (line.startsWith("map_Kd")) {
            output.add(line.substring(7));
          }
        }
        sc.close();
      } catch (FileNotFoundException e)
      {
        e.printStackTrace();
      }
    }
    return output;
  }

  public static void setOrientation(boolean portrait, Activity activity) {
    int value = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
    if (!portrait)
      value = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
    activity.setRequestedOrientation(value);
  }

  @Override
  protected void onResume() {
    super.onResume();
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    setOrientation(isPortrait(this), this);
  }

  public void deleteRecursive(File fileOrDirectory) {
    if (fileOrDirectory.isDirectory())
      for (File child : fileOrDirectory.listFiles())
        deleteRecursive(child);

    if (fileOrDirectory.delete())
      Log.d(TAG, fileOrDirectory + " deleted");
  }

  public Uri filename2Uri(String filename) {
    if(filename == null)
      return null;
    return Uri.fromFile(new File(getPath(), filename));
  }

  public static String getPath() {
    String dir = Environment.getExternalStorageDirectory().getPath() + MODEL_DIRECTORY;
    if (new File(dir).mkdir())
      Log.d(TAG, "Directory " + dir + " created");
    return dir;
  }

  public static File getTempPath() {
    File dir = new File(getPath(), TEMP_DIRECTORY);
    if (dir.mkdir())
      Log.d(TAG, "Directory " + dir + " created");
    return dir;
  }

  protected void zip(String[] files, String zip) throws Exception {
    try (ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zip))))
    {
      byte data[] = new byte[BUFFER_SIZE];
      for (String file : files)
      {
        FileInputStream fi = new FileInputStream(file);
        try (BufferedInputStream origin = new BufferedInputStream(fi, BUFFER_SIZE))
        {
          ZipEntry entry = new ZipEntry(file.substring(file.lastIndexOf("/") + 1));
          out.putNextEntry(entry);
          int count;
          while ((count = origin.read(data, 0, BUFFER_SIZE)) != -1)
          {
            out.write(data, 0, count);
          }
        }
      }
    }
  }
}
