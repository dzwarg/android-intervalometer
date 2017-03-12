/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.example.android.skeletonapp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.example.android.skeletonapp.R;
import com.example.android.skeletonapp.IIntervalService;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.StatFs;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.RemoteViews.ActionException;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView.OnEditorActionListener;
import android.widget.TextView;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;

/**
 * This class provides a basic demonstration of how to write an Android
 * activity. Inside of its window, it places a single view: an EditText that
 * displays and edits some internal text.
 */
public class SkeletonActivity extends Activity implements SurfaceHolder.Callback {
    private static String TAG = "Intervalometer";
    
    private Button mStartStopButton;
    private EditText mIntervalText;
    private Camera mCamera;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mHolder;
    private int mTicks, mTickSetting;
    private MediaScannerConnection mConnection;
    private MediaScannerConnectionClient mClient;
    private SeekBar mSeekBar;
    private float mOrigBright;
    private Location mLocation;
    private LocationManager mLocationManager;
    
    public SkeletonActivity() {
    }

    /** Called with the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate our UI from its XML layout description.
        setContentView(R.layout.skeleton_activity);

        // Hook up button presses to the appropriate event handler.
        mStartStopButton = ((Button) findViewById(R.id.start));
        mStartStopButton.setOnClickListener(mStartListener);
        
        mSeekBar = ((SeekBar) findViewById(R.id.seekBar1));
        mSeekBar.setOnSeekBarChangeListener(mSeekListener);
        mIntervalText = (EditText) findViewById(R.id.editText1);
        mIntervalText.setOnEditorActionListener(mTextListener);
        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView1);
        
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        
        mTicks = 0;
        mTickSetting = getResources().getInteger(R.integer.defaultinterval);
        
        mClient = new MediaScannerConnectionClient() {
			
			@Override
			public void onScanCompleted(String path, Uri uri) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onMediaScannerConnected() {
				// TODO Auto-generated method stub
				
			}
		};
        mConnection = new MediaScannerConnection(this, mClient);
        
        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
    }

    /**
     * Called when the activity is about to start interacting with the user.
     */
    @Override
    protected void onResume() {
        super.onResume();
        mConnection.connect();
    }
    
    @Override
    protected void onPause() {
    	mConnection.disconnect();
    	super.onPause();
    }

    OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
		
		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
//			Log.d(TAG, "yup");
			mTickSetting = seekBar.getProgress();
			
			if (mTicks > mTickSetting) {
				mTicks = mTickSetting;
			}
		}
		
		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
//			Log.d(TAG, "yup");
		}
		
		@Override
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			if (mIntervalText != null) {
				mIntervalText.setText("" + progress);
			}
		}
	};
	
	Camera.AutoFocusCallback mCameraFocused = new Camera.AutoFocusCallback() {
		@Override
		public void onAutoFocus(boolean success, Camera camera) {
			if (success) {
				mCamera.takePicture(null, null, jpegCallback);
			}
		}
	};
		
	PictureCallback jpegCallback = new PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
			StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
			long bytesAvail = (long)stat.getBlockSize() * (long)stat.getAvailableBlocks();
			
			if (bytesAvail < data.length) {
				Log.w(TAG, "Not enough space on device.");
				return;
			}
			
			FileOutputStream sdStream = null;
			String path = String.format("%s/%d.jpg", 
					Environment.getExternalStorageDirectory().getPath(), System.currentTimeMillis());
			try {
				sdStream = new FileOutputStream(path);
				if (mLocation != null) {
					//
				} else {
					sdStream.write(data);
				}
				sdStream.close();
				Log.d(TAG, "saved photo!");
				
				mConnection.scanFile(path, "image/jpeg");
				Log.d(TAG, "scanned photo into gallery!");
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
	};
	
	//
	// SurfaceHolder.Callback functions
	//
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
    	if (mCamera != null) {
    		mCamera.stopPreview();
    		mCamera.release();
    		mCamera = null;
    	}		
	}
	
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
        mCamera = Camera.open();
        try {
			mCamera.setPreviewDisplay(holder);
		} catch (IOException e) {
			mCamera.release();
			mCamera = null;
		}
	}
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
        mCamera.setParameters(mCamera.getParameters());
	}

	private OnEditorActionListener mTextListener = new OnEditorActionListener() {
		
		@Override
		public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
			// TODO Auto-generated method stub
			int setting = Integer.parseInt(v.getText().toString());
			
			if (setting > mSeekBar.getMax()) {
				v.setText("" + getResources().getInteger(R.integer.maxinterval));
				return true;
			} else if (setting < 0) {
				v.setText("0");
				return true;
			}
			
			mTickSetting = setting;
			mSeekBar.setProgress(mTickSetting);

			if (mTicks > mTickSetting) {
				mTicks = mTickSetting;
			}
			
			return false;
		}
	};
	
	/* Service-related code below */
	/** The primary interface we will be calling on the service. */
    IIntervalService mService = null;
    
    private boolean mIsBound;

    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                IBinder service) {
        	Log.d(TAG, "service connected");
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
            mService = IIntervalService.Stub.asInterface(service);
            mStartStopButton.setText(getResources().getText(R.string.stop));

            // We want to monitor the service for as long as we are
            // connected to it.
            try {
                mService.registerCallback(mCallback);
            } catch (RemoteException e) {
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything here.
            }
            
            // As part of the sample, tell the user what happened.
            Toast.makeText(SkeletonActivity.this, R.string.interval_service_connected,
                    Toast.LENGTH_SHORT).show();
        }

        public void onServiceDisconnected(ComponentName className) {
        	Log.d(TAG, "service disconnected");
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
            mStartStopButton.setText(getResources().getText(R.string.start));

            // As part of the sample, tell the user what happened.
            Toast.makeText(SkeletonActivity.this, R.string.interval_service_disconnected,
                    Toast.LENGTH_SHORT).show();
        }
    };
    

    /**
     * A call-back for when the user presses the start button.
     */
    OnClickListener mStartListener = new OnClickListener() {
        public void onClick(View v) {
        	WindowManager.LayoutParams params = getWindow().getAttributes();
        	
        	if (mStartStopButton.getText() == getResources().getText(R.string.start)) {
        		// set the brightness to minimal
        		mOrigBright = params.screenBrightness;
        		params.screenBrightness = 0.01f;
        		getWindow().setAttributes(params);
        		
        		// bind to the service to take the photos
        		bindService(new Intent("com.example.android.skeletonapp.INTERVAL_SERVICE").putExtra("interval", mSeekBar.getProgress()),
        				mServiceConnection, Context.BIND_AUTO_CREATE);
        		
        		// update the label
        		mSeekBar.setEnabled(false);
        		mIntervalText.setEnabled(false);
        		mStartStopButton.setText(getResources().getText(R.string.starting));
        		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
        	} else if (mStartStopButton.getText() == getResources().getText(R.string.stop)) {
        		mLocationManager.removeUpdates(mLocationListener);
        		// restore the screen brightness
        		params.screenBrightness = mOrigBright;
        		getWindow().setAttributes(params);
        		
        		unbindService(mServiceConnection);
        		
        		// stop the picture taking service
        		stopService(new Intent("com.example.android.skeletonapp.INTERVAL_SERVICE"));
        		
        		// update the label
        		mSeekBar.setEnabled(true);
        		mIntervalText.setEnabled(true);
        		mStartStopButton.setText(getResources().getText(R.string.start));
        	}
        }
    };
    
    LocationListener mLocationListener = new LocationListener() {

		@Override
		public void onLocationChanged(Location arg0) {
			// TODO Auto-generated method stub
			mLocation = arg0;
			
			Log.d(TAG, "Location: " + mLocation);
		}

		@Override
		public void onProviderDisabled(String arg0) {
			// TODO Auto-generated method stub
			mLocation = null;
		}

		@Override
		public void onProviderEnabled(String arg0) {
			// TODO Auto-generated method stub
			mLocation = null;
		}

		@Override
		public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
			// TODO Auto-generated method stub
			mLocation = null;
		}
    	
    };

    
    // ----------------------------------------------------------------------
    // Code showing how to deal with callbacks.
    // ----------------------------------------------------------------------
    
    /**
     * This implementation is used to receive callbacks from the remote
     * service.
     */
    private IIntervalServiceCallback mCallback = new IIntervalServiceCallback.Stub() {
        /**
         * This is called by the remote service regularly to tell us about
         * new values.  Note that IPC calls are dispatched through a thread
         * pool running in each process, so the code executing here will
         * NOT be running in our main thread like most other things -- so,
         * to update the UI, we need to use a Handler to hop over there.
         */
        public void valueChanged(int value) {
        	Log.d(TAG, "heard \"valueChanged\" event");
            mHandler.sendMessage(mHandler.obtainMessage(SNAP_MSG, value, 0));
        }
    };
    
    private static final int SNAP_MSG = 1;
    
    private Handler mHandler = new Handler() {
        @Override public void handleMessage(Message msg) {
        	Log.d(TAG, "message received");
            switch (msg.what) {
                case SNAP_MSG:
    	    		mCamera.startPreview();
    	            mCamera.autoFocus(mCameraFocused);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
        
    };
}


