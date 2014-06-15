/*****************************************************************************
 * MediaInfoActivity.java
 *****************************************************************************
 * Copyright © 2011-2012 VLC authors and VideoLAN
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 *****************************************************************************/

package org.videolan.vlc.gui.video;

import java.nio.ByteBuffer;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.LibVlcException;
import org.videolan.libvlc.TrackInfo;
import org.videolan.vlc.Media;
import org.videolan.vlc.MediaLibrary;
import com.mine.videoplayer.R;
import org.videolan.vlc.Util;
import org.videolan.vlc.WeakHandler;

import android.app.ListActivity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

public class MediaInfoActivity extends ListActivity {

    public final static String TAG = "VLC/MediaInfoActivity";
    public static final String KEY = "MediaInfoActivity.image";
    private Media mItem;
    private Bitmap mImage;
    private ImageButton mPlayButton;
    private TrackInfo[] mTracks;
    private MediaInfoAdapter mAdapter;
    private final static int NEW_IMAGE = 0;
    private final static int NEW_TEXT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.media_info);
        if (savedInstanceState != null)
            mImage = savedInstanceState.getParcelable(KEY);

        String MRL = getIntent().getExtras().getString("itemLocation");
        if (MRL == null)
            return;
        mItem = MediaLibrary.getInstance(this).getMediaItem(MRL);
        if(mItem == null) {
            // Shouldn't happen, maybe user opened it faster than Media Library could index it
            return;
        }

        // set title
        TextView titleView = (TextView) findViewById(R.id.title);
        titleView.setText(mItem.getTitle());

        // set length
        TextView lengthView = (TextView) findViewById(R.id.length);
        lengthView.setText(Util.millisToString(mItem.getLength()));

        mPlayButton = (ImageButton) findViewById(R.id.play);

        mAdapter = new MediaInfoAdapter(MediaInfoActivity.this);
        setListAdapter(mAdapter);

        new Thread(mLoadImage).start();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY, mImage);
    }

    public void onPlayClick(View v) {
        VideoPlayerActivity.start(this, mItem.getLocation());
    }

    Runnable mLoadImage = new Runnable() {
        @Override
        public void run() {
            LibVLC mLibVlc = null;
            try {
                mLibVlc = Util.getLibVlcInstance();
            } catch (LibVlcException e) {
                return;
            }

            mTracks = mLibVlc.readTracksInfo(mItem.getLocation());
            mHandler.sendEmptyMessage(NEW_TEXT);

            if (mImage == null) {
                DisplayMetrics screen = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(screen);
                int width = Math.min(screen.widthPixels, screen.heightPixels);
                int height = width * 9 / 16;

                // Get the thumbnail.
                mImage = Bitmap.createBitmap(width, height, Config.ARGB_8888);

                byte[] b = mLibVlc.getThumbnail(mItem.getLocation(), width, height);

                if (b == null) // We were not able to create a thumbnail for this item.
                    return;

                mImage.copyPixelsFromBuffer(ByteBuffer.wrap(b));
                mImage = Util.cropBorders(mImage, width, height);
            }

            mHandler.sendEmptyMessage(NEW_IMAGE);
        }
    };

    private void updateImage() {
        ImageView imageView = (ImageView) MediaInfoActivity.this.findViewById(R.id.image);
        imageView.setImageBitmap(mImage);
        mPlayButton.setVisibility(View.VISIBLE);
    }

    private void updateText() {
        for (TrackInfo track : mTracks) {
            if (track.Type != TrackInfo.TYPE_META)
                mAdapter.add(track);
        }
    }

    private Handler mHandler = new MediaInfoHandler(this);

    private static class MediaInfoHandler extends WeakHandler<MediaInfoActivity> {
        public MediaInfoHandler(MediaInfoActivity owner) {
            super(owner);
        }

        @Override
        public void handleMessage(Message msg) {
            MediaInfoActivity activity = getOwner();
            if(activity == null) return;

            switch (msg.what) {
                case NEW_IMAGE:
                    activity.updateImage();
                    break;
                case NEW_TEXT:
                    activity.updateText();
                    break;
            }
        };

    };

}
