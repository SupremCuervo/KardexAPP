package com.mhrc.appkardex;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceLandmark;
import java.util.HashMap;
import java.util.Map;

public class FaceLandmarkOverlayView extends View {

	private Face currentFace = null;
	private Paint landmarkPaint;
	private Paint linePaint;
	private float scaleX = 1.0f;
	private float scaleY = 1.0f;
	private float offsetX = 0.0f;
	private float offsetY = 0.0f;

	public FaceLandmarkOverlayView(Context context) {
		super(context);
		init();
	}

	public FaceLandmarkOverlayView(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public FaceLandmarkOverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}

	private void init() {
		// Paint para los puntos de landmarks
		landmarkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		landmarkPaint.setColor(0xFF00FF00); // Verde para landmarks
		landmarkPaint.setStyle(Paint.Style.FILL);
		landmarkPaint.setStrokeWidth(8f);

		// Paint para líneas conectando puntos clave
		linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		linePaint.setColor(0xFF00FFFF); // Cyan para líneas
		linePaint.setStyle(Paint.Style.STROKE);
		linePaint.setStrokeWidth(3f);
		linePaint.setAlpha(150);
	}

	public void setFace(Face face) {
		this.currentFace = face;
		invalidate(); // Redibujar la vista
	}

	public void setScaleAndOffset(float scaleX, float scaleY, float offsetX, float offsetY) {
		this.scaleX = scaleX;
		this.scaleY = scaleY;
		this.offsetX = offsetX;
		this.offsetY = offsetY;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if (currentFace == null) {
			return;
		}

		// Dibujar landmarks si están disponibles
		drawLandmarks(canvas);
	}

	private void drawLandmarks(Canvas canvas) {
		Map<String, FaceLandmark> landmarks = new HashMap<>();

		// Obtener todos los landmarks disponibles
		if (currentFace.getLandmark(FaceLandmark.LEFT_EYE) != null) {
			landmarks.put("leftEye", currentFace.getLandmark(FaceLandmark.LEFT_EYE));
		}
		if (currentFace.getLandmark(FaceLandmark.RIGHT_EYE) != null) {
			landmarks.put("rightEye", currentFace.getLandmark(FaceLandmark.RIGHT_EYE));
		}
		if (currentFace.getLandmark(FaceLandmark.NOSE_BASE) != null) {
			landmarks.put("noseBase", currentFace.getLandmark(FaceLandmark.NOSE_BASE));
		}
		if (currentFace.getLandmark(FaceLandmark.MOUTH_BOTTOM) != null) {
			landmarks.put("mouthBottom", currentFace.getLandmark(FaceLandmark.MOUTH_BOTTOM));
		}
		if (currentFace.getLandmark(FaceLandmark.LEFT_CHEEK) != null) {
			landmarks.put("leftCheek", currentFace.getLandmark(FaceLandmark.LEFT_CHEEK));
		}
		if (currentFace.getLandmark(FaceLandmark.RIGHT_CHEEK) != null) {
			landmarks.put("rightCheek", currentFace.getLandmark(FaceLandmark.RIGHT_CHEEK));
		}

		// Dibujar puntos de landmarks
		for (FaceLandmark landmark : landmarks.values()) {
			PointF position = landmark.getPosition();
			// Aplicar escala y offset
			float x = position.x * scaleX + offsetX;
			float y = position.y * scaleY + offsetY;
			
			// Dibujar círculo para cada landmark
			canvas.drawCircle(x, y, 12f, landmarkPaint);
		}

		// Dibujar líneas conectando puntos clave para mostrar el escaneo
		if (landmarks.containsKey("leftEye") && landmarks.containsKey("rightEye")) {
			PointF leftEye = landmarks.get("leftEye").getPosition();
			PointF rightEye = landmarks.get("rightEye").getPosition();
			canvas.drawLine(
				leftEye.x * scaleX + offsetX, leftEye.y * scaleY + offsetY,
				rightEye.x * scaleX + offsetX, rightEye.y * scaleY + offsetY,
				linePaint
			);
		}

		if (landmarks.containsKey("leftEye") && landmarks.containsKey("noseBase")) {
			PointF leftEye = landmarks.get("leftEye").getPosition();
			PointF noseBase = landmarks.get("noseBase").getPosition();
			canvas.drawLine(
				leftEye.x * scaleX + offsetX, leftEye.y * scaleY + offsetY,
				noseBase.x * scaleX + offsetX, noseBase.y * scaleY + offsetY,
				linePaint
			);
		}

		if (landmarks.containsKey("rightEye") && landmarks.containsKey("noseBase")) {
			PointF rightEye = landmarks.get("rightEye").getPosition();
			PointF noseBase = landmarks.get("noseBase").getPosition();
			canvas.drawLine(
				rightEye.x * scaleX + offsetX, rightEye.y * scaleY + offsetY,
				noseBase.x * scaleX + offsetX, noseBase.y * scaleY + offsetY,
				linePaint
			);
		}

		if (landmarks.containsKey("noseBase") && landmarks.containsKey("mouthBottom")) {
			PointF noseBase = landmarks.get("noseBase").getPosition();
			PointF mouthBottom = landmarks.get("mouthBottom").getPosition();
			canvas.drawLine(
				noseBase.x * scaleX + offsetX, noseBase.y * scaleY + offsetY,
				mouthBottom.x * scaleX + offsetX, mouthBottom.y * scaleY + offsetY,
				linePaint
			);
		}
	}

	public void clear() {
		this.currentFace = null;
		invalidate();
	}
}

