/*
 * Copyright 2015 Tinbytes Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tinbytes.simplestandardhttpapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.net.URL;

public class SimpleStandardHttpActivity extends AppCompatActivity {
  private static final String TAG = SimpleStandardHttpActivity.class.getSimpleName();
  private static final String FLICKR_PHOTOS_URL = "http://api.flickr.com/services/feeds/photos_public.gne?id=37348700@N00&lang=en-us&format=atom";

  private Handler handler;
  private TextView tvStatus;
  private LinearLayout llImages;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_simple_standard_http);

    handler = new Handler();
    tvStatus = (TextView) findViewById(R.id.tvStatus);
    llImages = (LinearLayout) findViewById(R.id.llImages);
    findViewById(R.id.bLoadOnThread).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        llImages.removeAllViews();
        loadOnThread();
      }
    });
    findViewById(R.id.bLoadOnAsyncTask).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        llImages.removeAllViews();
        loadOnAsyncTask();
      }
    });
  }

  private void loadOnThread() {
    new Thread(new Runnable() {
      public void run() {
        handler.post(new Runnable() {
          @Override
          public void run() {
            tvStatus.setText("Parsing...");
          }
        });

        try {
          URL text = new URL(FLICKR_PHOTOS_URL);
          XmlPullParserFactory parserCreator = XmlPullParserFactory.newInstance();
          XmlPullParser parser = parserCreator.newPullParser();
          parser.setInput(text.openStream(), null);
          int parserEvent = parser.getEventType();
          while (parserEvent != XmlPullParser.END_DOCUMENT) {
            switch (parserEvent) {
              case XmlPullParser.START_TAG:
                String tag = parser.getName();
                if (tag.compareTo("link") == 0) {
                  String relType = parser.getAttributeValue(null, "rel");
                  if (relType.compareTo("enclosure") == 0) {
                    String encType = parser.getAttributeValue(null, "type");
                    if (encType.startsWith("image/")) {
                      final String imageSrc = parser.getAttributeValue(null, "href");
                      final Bitmap bitmap = BitmapFactory.decodeStream(new URL(imageSrc).openStream());
                      Log.i(TAG, "image source = " + imageSrc);
                      handler.post(new Runnable() {
                        @Override
                        public void run() {
                          tvStatus.setText("Loading...");
                          ImageView im = new ImageView(SimpleStandardHttpActivity.this);
                          im.setImageBitmap(bitmap);
                          llImages.addView(im);
                        }
                      });
                    }
                  }
                }
                break;
            }
            parserEvent = parser.next();
          }
        } catch (IOException | XmlPullParserException e) {
          Log.e(TAG, e.getMessage(), e);
        }

        handler.post(new Runnable() {
          @Override
          public void run() {
            tvStatus.setText("Done");
          }
        });
      }
    }).start();
  }

  private void loadOnAsyncTask() {
    new ImageLoaderTask().execute();
  }

  private class ImageLoaderTask extends AsyncTask<Void, String, Void> {
    @Override
    protected Void doInBackground(Void... params) {
      publishProgress("Parsing...");

      try {
        URL text = new URL(FLICKR_PHOTOS_URL);
        XmlPullParserFactory parserCreator = XmlPullParserFactory.newInstance();
        XmlPullParser parser = parserCreator.newPullParser();
        parser.setInput(text.openStream(), null);
        int parserEvent = parser.getEventType();
        while (parserEvent != XmlPullParser.END_DOCUMENT) {
          switch (parserEvent) {
            case XmlPullParser.START_TAG:
              String tag = parser.getName();
              if (tag.compareTo("link") == 0) {
                String relType = parser.getAttributeValue(null, "rel");
                if (relType.compareTo("enclosure") == 0) {
                  String encType = parser.getAttributeValue(null, "type");
                  if (encType.startsWith("image/")) {
                    final String imageSrc = parser.getAttributeValue(null, "href");
                    final Bitmap bitmap = BitmapFactory.decodeStream(new URL(imageSrc).openStream());
                    Log.i(TAG, "image source = " + imageSrc);
                    handler.post(new Runnable() {
                      @Override
                      public void run() {
                        publishProgress("Loading...");
                        ImageView im = new ImageView(SimpleStandardHttpActivity.this);
                        im.setImageBitmap(bitmap);
                        llImages.addView(im);
                      }
                    });
                  }
                }
              }
              break;
          }
          parserEvent = parser.next();
        }
      } catch (IOException | XmlPullParserException e) {
        Log.e(TAG, e.getMessage(), e);
      }

      publishProgress("Done");
      return null;
    }

    @Override
    protected void onProgressUpdate(String... values) {
      tvStatus.setText(values[0]);
    }
  }
}