/*
 * Copyright 2013
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU  General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */

package org.kiwix.kiwixmobile;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.os.ParcelFileDescriptor.AutoCloseOutputStream;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.File;  
import java.io.FileNotFoundException;
import java.io.FileOutputStream;   
import java.io.IOException;
import java.io.InputStream; 
import java.io.OutputStream;

public class ZimContentProvider extends ContentProvider {

    public static final Uri CONTENT_URI = Uri.parse("content://org.kiwix.zim/");

    public static final Uri UI_URI = Uri.parse("content://org.kiwix.ui/");

    private static String zimFileName;

    private static JNIKiwix jniKiwix;

    public synchronized static String setZimFile(String fileName) {
        if (!jniKiwix.loadZIM(fileName)) {
            Log.e("kiwix", "Unable to open the file " + fileName);
            zimFileName = null;
        } else {
            zimFileName = fileName;
        }
        return zimFileName;
    }

    public static String getZimFile() {
        return zimFileName;
    }

    public static String getZimFileTitle() {
        if (jniKiwix == null || zimFileName == null) {
            return null;
        } else {
            JNIKiwixString title = new JNIKiwixString();
            if (jniKiwix.getTitle(title)) {
                return title.value;
            } else {
                return null;
            }
        }
    }

    public static String getMainPage() {
        if (jniKiwix == null || zimFileName == null) {
            return null;
        } else {
            return jniKiwix.getMainPage();
        }
    }

    public static String getId() {
        if (jniKiwix == null || zimFileName == null) {
            return null;
        } else {
            return jniKiwix.getId();
        }
    }

    public static boolean searchSuggestions(String prefix, int count) {
        if (jniKiwix == null || zimFileName == null) {
            return false;
        } else {
            return jniKiwix.searchSuggestions(prefix, count);
        }
    }

    public static String getNextSuggestion() {
        if (jniKiwix == null || zimFileName == null) {
            return null;
        } else {
            JNIKiwixString title = new JNIKiwixString();
            if (jniKiwix.getNextSuggestion(title)) {
                return title.value;
            } else {
                return null;
            }
        }
    }

    public static String getPageUrlFromTitle(String title) {
        if (jniKiwix == null || zimFileName == null) {
            return null;
        } else {
            JNIKiwixString url = new JNIKiwixString();
            if (jniKiwix.getPageUrlFromTitle(title, url)) {
                return url.value;
            } else {
                return null;
            }
        }
    }

    public static String getRandomArticleUrl() {
        if (jniKiwix == null || zimFileName == null) {
            return null;
        } else {
            JNIKiwixString url = new JNIKiwixString();
            if (jniKiwix.getRandomPage(url)) {
                return url.value;
            } else {
                return null;
            }
        }
    }

    @Override
    public boolean onCreate() {
        jniKiwix = new JNIKiwix();
	setIcuDataDirectory();
        return (true);
    }

    @Override
    public String getType(Uri uri) {
        Log.w("kiwix", "ZimContentProvider.getType() (not implemented) called");
        return null;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode)
            throws FileNotFoundException {
        ParcelFileDescriptor[] pipe = null;

        try {
            pipe = ParcelFileDescriptor.createPipe();
            new TransferThread(jniKiwix, uri, new AutoCloseOutputStream(
                    pipe[1])).start();
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(), "Exception opening pipe", e);
            throw new FileNotFoundException("Could not open pipe for: "
                    + uri.toString());
        }

        return (pipe[0]);
    }

    @Override
    public Cursor query(Uri url, String[] projection, String selection,
            String[] selectionArgs, String sort) {
        throw new RuntimeException("Operation not supported");
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        throw new RuntimeException("Operation not supported");
    }

    @Override
    public int update(Uri uri, ContentValues values, String where,
            String[] whereArgs) {
        throw new RuntimeException("Operation not supported");
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        throw new RuntimeException("Operation not supported");
    }

    static class TransferThread extends Thread {

        Uri articleUri;

        String articleZimUrl;

        OutputStream out;

        JNIKiwix jniKiwix;

        TransferThread(JNIKiwix jniKiwix, Uri articleUri, OutputStream out) throws IOException {
            this.articleUri = articleUri;
            this.jniKiwix = jniKiwix;
            Log.d("kiwix",
                    "Retrieving :"
                            + articleUri.toString());

            String t = articleUri.toString();
            int pos = articleUri.toString().indexOf(CONTENT_URI.toString());
            if (pos != -1) {
                t = articleUri.toString().substring(
                        CONTENT_URI.toString().length());
            }
            // Remove fragment (#...) as not supported by zimlib
            pos = t.indexOf("#");
            if (pos != -1) {
                t = t.substring(0, pos);
            }

            this.out = out;
            this.articleZimUrl = t;
        }

        @Override
        public void run() {
            byte[] buf = new byte[1024];
            int len;

            try {
                JNIKiwixString mime = new JNIKiwixString();
                JNIKiwixInt size = new JNIKiwixInt();
                byte[] data = jniKiwix.getContent(articleZimUrl, mime, size);
                // Log.d("kiwix","articleDataByteArray:"+articleDataByteArray.toString());
                // ByteArrayInputStream articleDataInputStream = new
                // ByteArrayInputStream(articleDataByteArray.toByteArray());
                // Log.d("kiwix","article data loaded from zime file");

                //ByteArrayInputStream articleDataInputStream = new ByteArrayInputStream(
                //	articleDataByteArray.toByteArray());
                ByteArrayInputStream articleDataInputStream = new ByteArrayInputStream(data);
                while ((len = articleDataInputStream.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }

                articleDataInputStream.close();
                out.flush();

                Log.d("kiwix", "reading  " + articleZimUrl
                        + "(mime " + mime.value + ", size: " + size.value + ") finished.");
            } catch (IOException e) {
                Log.e(getClass().getSimpleName(), "Exception reading article "
                        + articleZimUrl + " from zim file", e);
            } catch (NullPointerException e) {
                Log.e(getClass().getSimpleName(), "Exception reading article "
                        + articleZimUrl + " from zim file", e);

            } finally {
                try {
                    out.close();
                } catch (IOException e) {

                }

            }
        }
    }

    private void setIcuDataDirectory() {
        File workingDir = this.getContext().getFilesDir();
        String icuDirPath = loadICUData(this.getContext(), workingDir);

        if(icuDirPath != null) {
            Log.d("kiwix", "Setting the ICU directory path to " + icuDirPath);
            jniKiwix.setDataDirectory(icuDirPath);
        }
    }

    private static String loadICUData(Context context, File workingDir) {
        String icuFileName = "icudt49l.dat";
        try {
            File icuDir = new File(workingDir, "icu");
            if(!icuDir.exists()) icuDir.mkdirs();
            File icuDataFile = new File(icuDir, icuFileName);
            if(!icuDataFile.exists()) {
                InputStream in = context.getAssets().open(icuFileName);
                OutputStream out =  new FileOutputStream(icuDataFile);
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.flush();
                out.close();
            }
            return icuDir.getAbsolutePath();
        }
        catch (Exception e) {
            Log.e("kiwix", "Error copying icu data file", e);
            return null;
        }
    }

}