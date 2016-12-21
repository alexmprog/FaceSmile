package com.renovavision.facesmile.face;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.NonNull;

import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.Landmark;
import com.renovavision.facesmile.R;
import com.renovavision.facesmile.camera.GraphicOverlay;

/**
 * Created by Alexandr Golovach on 19.09.16.
 */
public class FaceGraphic extends GraphicOverlay.Graphic {
	private static final float ID_TEXT_SIZE = 60.0f;
	private static final float LABEL_Y_OFFSET = 50.0f;
	private static final float BOX_STROKE_WIDTH = 5.0f;

	private static final int VALID_COLOR = Color.GREEN;
	private static final int INVALID_COLOR = Color.RED;

	private Paint mPaint;

	private volatile Face mFace;
	private int mFaceId;
	private boolean mIsReady = false;
	private final String mNotReadyMessage;
	private final String mReadyMessage;

	private Bitmap mEyePatchBitmap;

	private Paint rectPaint;

	private void createRectanglePaint() {
		rectPaint = new Paint();
		rectPaint.setStrokeWidth(5);
		rectPaint.setColor(Color.CYAN);
		rectPaint.setStyle(Paint.Style.STROKE);
	}

	public FaceGraphic(@NonNull GraphicOverlay overlay, @NonNull Context context) {
		super(overlay);
		mNotReadyMessage = overlay.getContext().getResources().getString(R.string.not_ready_message);
		mReadyMessage = overlay.getContext().getResources().getString(R.string.ready_message);

		mPaint = new Paint();
		mPaint.setColor(INVALID_COLOR);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setStrokeWidth(BOX_STROKE_WIDTH);
		mPaint.setTextSize(ID_TEXT_SIZE);

		BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
		bitmapOptions.inMutable = true;
		mEyePatchBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.eye_patch,
				bitmapOptions);

		createRectanglePaint();
	}

	void setId(int id) {
		mFaceId = id;
	}


	void setIsReady(boolean isValid) {
		this.mIsReady = isValid;
		mPaint.setColor(isValid ? VALID_COLOR : INVALID_COLOR);
	}

	void updateFace(Face face) {
		mFace = face;
		postInvalidate();
	}

	@Override
	public void draw(Canvas canvas) {
		Face face = mFace;
		if (face == null) {
			return;
		}

		// Draws a circle at the position of the detected face, with the face's track id below.
		float x = translateX(face.getPosition().x + face.getWidth() / 2);
		float y = translateY(face.getPosition().y + face.getHeight() / 2);

		// Draws a bounding box around the face.
		float xOffset = scaleX(face.getWidth() / 2.0f);
		float yOffset = scaleY(face.getHeight() / 2.0f);
		float left = x - xOffset;
		float top = y - yOffset;
		float right = x + xOffset;
		float bottom = y + yOffset;

		canvas.drawText(mIsReady ? mReadyMessage : mNotReadyMessage, left, top - LABEL_Y_OFFSET, mPaint);

		//canvas.drawRect(left, top, right, bottom, mPaint);

		for (Landmark landmark : face.getLandmarks()) {

			int cx = (int) (landmark.getPosition().x);
			int cy = (int) (landmark.getPosition().y);

			//canvas.drawCircle(cx, cy, 10, rectPaint);

			//drawLandmarkType(canvas, landmark.getType(), cx, cy);

			drawEyePatchBitmap(canvas, landmark.getType(), cx * 2, cy * 2);
		}
	}

	private void drawLandmarkType(@NonNull Canvas canvas, int landmarkType, float cx, float cy) {
		String type = String.valueOf(landmarkType);
		rectPaint.setTextSize(50);
		canvas.drawText(type, cx, cy, rectPaint);
	}

	private void drawEyePatchBitmap(@NonNull Canvas canvas, int landmarkType, float cx, float cy) {

		if (landmarkType == 4) {
			// left eye
			int scaledWidth = mEyePatchBitmap.getScaledWidth(canvas);
			int scaledHeight = mEyePatchBitmap.getScaledHeight(canvas);
			canvas.drawBitmap(mEyePatchBitmap, cx - (scaledWidth / 2), cy - (scaledHeight / 2), null);
		}
	}
}