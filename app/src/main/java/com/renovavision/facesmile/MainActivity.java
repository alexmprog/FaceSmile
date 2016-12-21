package com.renovavision.facesmile;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.renovavision.facesmile.camera.CameraSourcePreview;
import com.renovavision.facesmile.camera.GraphicOverlay;
import com.renovavision.facesmile.face.GraphicFaceTracker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Alexandr Golovach on 19.09.16.
 */
public class MainActivity extends AppCompatActivity implements GraphicFaceTracker.CaptureListener {

	private static final String TAG = "FaceTracker";

	private CameraSource mCameraSource = null;

	private CameraSourcePreview mPreview;
	private GraphicOverlay mGraphicOverlay;

	private static final int RC_HANDLE_GMS = 9001;

	private static final int RC_HANDLE_CAMERA_PERM = 2;


	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.activity_main);

		mPreview = (CameraSourcePreview) findViewById(R.id.preview);
		mGraphicOverlay = (GraphicOverlay) findViewById(R.id.faceOverlay);

		// Check for the camera permission before accessing the camera.  If the
		// permission is not granted yet, request permission.
		int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
		if (rc == PackageManager.PERMISSION_GRANTED) {
			createCameraSource();
		} else {
			requestCameraPermission();
		}
	}

	private void requestCameraPermission() {
		Log.w(TAG, "Camera permission is not granted. Requesting permission");

		final String[] permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};

		if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
				Manifest.permission.CAMERA)) {
			ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
			return;
		}

		final Activity thisActivity = this;

		View.OnClickListener listener = new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				ActivityCompat.requestPermissions(thisActivity, permissions,
						RC_HANDLE_CAMERA_PERM);
			}
		};

		Snackbar.make(mGraphicOverlay, R.string.permission_camera_rationale,
				Snackbar.LENGTH_INDEFINITE)
				.setAction(R.string.ok, listener)
				.show();
	}

	private void createCameraSource() {

		Context context = getApplicationContext();
		FaceDetector detector = new FaceDetector.Builder(context)
				.setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
				.setTrackingEnabled(false)
				.build();

		detector.setProcessor(new MultiProcessor.Builder<>(new GraphicFaceTrackerFactory())
				.build());

		if (!detector.isOperational()) {
			Log.w(TAG, "Face detector dependencies are not yet available.");
		}

		mCameraSource = new CameraSource.Builder(context, detector)
				.setRequestedPreviewSize(640, 480)
				.setFacing(CameraSource.CAMERA_FACING_FRONT)
				.setRequestedFps(30.0f)
				.build();
	}

	@Override
	protected void onResume() {
		super.onResume();
		startCameraSource();
	}

	@Override
	protected void onPause() {
		super.onPause();
		mPreview.stop();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mCameraSource != null) {
			mCameraSource.release();
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		if (requestCode != RC_HANDLE_CAMERA_PERM) {
			Log.d(TAG, "Got unexpected permission result: " + requestCode);
			super.onRequestPermissionsResult(requestCode, permissions, grantResults);
			return;
		}

		if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
			Log.d(TAG, "Camera permission granted - initialize the camera source");
			// we have permission, so create the camerasource
			createCameraSource();
			return;
		}

		Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
				" Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

		DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				finish();
			}
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Face Smile")
				.setMessage(R.string.no_camera_permission)
				.setPositiveButton(R.string.ok, listener)
				.show();
	}

	private void startCameraSource() {

		// check that the device has play services available.
		int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
				getApplicationContext());
		if (code != ConnectionResult.SUCCESS) {
			Dialog dlg =
					GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
			dlg.show();
		}

		if (mCameraSource != null) {
			try {
				mPreview.start(mCameraSource, mGraphicOverlay);
			} catch (IOException e) {
				Log.e(TAG, "Unable to start camera source.", e);
				mCameraSource.release();
				mCameraSource = null;
			}
		}
	}


	@Override
	public void takeCapture() {
		mCameraSource.takePicture(null, new CameraSource.PictureCallback() {
			@Override
			public void onPictureTaken(byte[] bytes) {
				mPreview.stop();
				File shot = null;
				try {
					shot = saveImage(bytes, "jpg");
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
				Intent shareIntent = new Intent(Intent.ACTION_SEND);
				shareIntent.setType("image/jpeg");
				shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(shot));
				startActivity(Intent.createChooser(shareIntent, ""));
			}
		});
	}

	private class GraphicFaceTrackerFactory implements MultiProcessor.Factory<Face> {
		@Override
		public Tracker<Face> create(Face face) {
			return new GraphicFaceTracker(mGraphicOverlay, MainActivity.this, MainActivity.this);
		}
	}

	private File saveImage(byte[] content, String extension) throws IOException {
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		File storageDir = Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_PICTURES);
		File image = new File(storageDir, timeStamp + "." + extension);
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(image);
			out.write(content);
			out.close();
		} finally {
			if (out != null) {
				out.close();
			}
		}
		return image;
	}

}
