package com.renovavision.facesmile.face;

import android.content.Context;
import android.support.annotation.NonNull;

import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.renovavision.facesmile.camera.GraphicOverlay;

/**
 * Created by Alexandr Golovach on 19.09.16.
 */

public class GraphicFaceTracker extends Tracker<Face> {

	private static final double SMILING_THRESHOLD = 0.4;
	private static final double WINK_THRESHOLD = 0.5;
	private GraphicOverlay mOverlay;
	private FaceGraphic mFaceGraphic;
	private CaptureListener mCaptureListener;

	public GraphicFaceTracker(@NonNull GraphicOverlay overlay, @NonNull CaptureListener captureListener, @NonNull Context context) {
		mOverlay = overlay;
		mFaceGraphic = new FaceGraphic(overlay, context);
		mCaptureListener = captureListener;
	}

	@Override
	public void onNewItem(int faceId, Face item) {
		mFaceGraphic.setId(faceId);
	}

	@Override
	public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {
		mOverlay.add(mFaceGraphic);
		boolean isSmiling = face.getIsSmilingProbability() > SMILING_THRESHOLD;
		if (isSmiling) {
			float leftEye = face.getIsLeftEyeOpenProbability();
			float rightEye = face.getIsRightEyeOpenProbability();
			if (Math.abs(leftEye - rightEye) >= WINK_THRESHOLD) {
				if (mCaptureListener != null) {
					mCaptureListener.takeCapture();
				}
			}
		}

		mFaceGraphic.setIsReady(isSmiling);
		mFaceGraphic.updateFace(face);
	}

	@Override
	public void onMissing(FaceDetector.Detections<Face> detectionResults) {
		mOverlay.remove(mFaceGraphic);
	}

	@Override
	public void onDone() {
		mOverlay.remove(mFaceGraphic);
	}

	public interface CaptureListener {
		void takeCapture();
	}

}