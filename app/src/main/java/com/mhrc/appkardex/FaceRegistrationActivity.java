package com.mhrc.appkardex;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FaceRegistrationActivity extends AppCompatActivity {

	private static final int CAMERA_PERMISSION_REQUEST_CODE = 1001;
	private PreviewView previewView;
	private FaceLandmarkOverlayView landmarkOverlay;
	private TextView tvInstructions;
	private TextView tvPoseFeedback;
	private Button btnStart;
	private Button btnCapture;
	private boolean scanningStarted = false; // Flag para saber si el escaneo ha comenzado

	private FaceDetector faceDetector;
	private ExecutorService cameraExecutor;
	private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
	private ImageCapture imageCapture;
	private boolean isFaceDetected = false;
	
	// Sistema mejorado - captura múltiples muestras para mayor precisión
	private List<List<Map<String, Float>>> faceDataSamples = new ArrayList<>(); // Múltiples muestras biométricas del rostro
	private boolean isFaceDetectedAndValid = false; // Si el rostro está detectado y es válido
	private Face lastValidFace = null; // Último rostro válido detectado
	private boolean isCapturing = false; // Flag para evitar múltiples capturas simultáneas
	private int stableFaceFrames = 0; // Contador de frames con rostro estable
	private static final int REQUIRED_STABLE_FRAMES = 8; // Frames estables requeridos antes de empezar a capturar
	private static final int REQUIRED_SAMPLES = 15; // Número de muestras a capturar para máxima precisión
	private int capturedSamples = 0; // Contador de muestras capturadas
	private int framesSinceLastCapture = 0; // Frames desde la última captura
	private static final int FRAMES_BETWEEN_SAMPLES = 2; // Frames entre cada captura de muestra (más rápido)

	private String userId;
	private String userType;
	private boolean fromRegistration = false; // Indica si viene del registro

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_face_registration);

		userId = getIntent().getStringExtra("userId");
		userType = getIntent().getStringExtra("userType");
		fromRegistration = getIntent().getBooleanExtra("fromRegistration", false);

		if (userId == null || userId.isEmpty()) {
			Toast.makeText(this, "Error: ID de usuario no válido", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

		initViews();
		setupFaceDetector();

		// Usar ML Kit para registro facial (mostrar cámara y guardar datos biométricos)
		previewView.setVisibility(android.view.View.VISIBLE);
		scanningStarted = false;
		btnStart.setVisibility(android.view.View.VISIBLE);
		btnCapture.setVisibility(android.view.View.GONE);
		
		// Inicializar cámara pero no empezar a escanear hasta que el usuario presione "Empezar"
		if (checkCameraPermission()) {
			startCamera();
		} else {
			requestCameraPermission();
		}

		btnStart.setOnClickListener(v -> {
			// Iniciar el escaneo cuando el usuario presione "Empezar"
			scanningStarted = true;
			btnStart.setVisibility(android.view.View.GONE);
			stableFaceFrames = 0;
			capturedSamples = 0;
			framesSinceLastCapture = 0;
			faceDataSamples.clear();
			tvPoseFeedback.setText("Posiciona tu rostro de frente frente a la cámara.");
			tvPoseFeedback.setTextColor(getResources().getColor(R.color.text_secondary));
			tvInstructions.setText("El sistema escaneará automáticamente tu rostro. Mantén tu cara quieta y de frente.");
		});

		btnCapture.setOnClickListener(v -> {
			// Solo para reiniciar si es necesario
			isCapturing = false;
			scanningStarted = true;
			stableFaceFrames = 0;
			capturedSamples = 0;
			framesSinceLastCapture = 0;
			faceDataSamples.clear();
			lastValidFace = null;
			isFaceDetectedAndValid = false;
			btnCapture.setVisibility(android.view.View.GONE);
			btnStart.setVisibility(android.view.View.GONE);
			tvPoseFeedback.setText("Posiciona tu rostro de frente frente a la cámara.");
			tvPoseFeedback.setTextColor(getResources().getColor(R.color.text_secondary));
			landmarkOverlay.clear();
		});
		
		// Si viene del registro, mostrar mensaje de que es obligatorio
		if (fromRegistration) {
			tvInstructions.setText("El registro facial es OBLIGATORIO para completar tu registro. Por favor, posiciona tu rostro frente a la cámara.");
		}
	}
	
	@Override
	public void onBackPressed() {
		if (fromRegistration) {
			// Si viene del registro, mostrar diálogo de advertencia
			new androidx.appcompat.app.AlertDialog.Builder(this)
				.setTitle("⚠️ Registro Incompleto")
				.setMessage("El registro facial es OBLIGATORIO. Si cancelas, tu registro se eliminará y tendrás que empezar de nuevo.")
				.setPositiveButton("Continuar registro", null)
				.setNegativeButton("Cancelar registro", (dialog, which) -> {
					// Eliminar usuario de Firestore si cancela
					deleteIncompleteRegistration();
				})
				.show();
		} else {
			super.onBackPressed();
		}
	}
	
	private void deleteIncompleteRegistration() {
		FirebaseFirestore db = FirebaseFirestore.getInstance();
		
		// Eliminar de ambas colecciones
		db.collection("usuarios").document(userId).delete()
			.addOnSuccessListener(aVoid -> {
				if ("alumno".equals(userType)) {
					db.collection("alumnos").document(userId).delete()
						.addOnSuccessListener(aVoid2 -> {
							Toast.makeText(this, "Registro cancelado", Toast.LENGTH_SHORT).show();
							finish();
							startActivity(new android.content.Intent(this, SelectionScreen.class));
						});
				} else if ("maestro".equals(userType)) {
					db.collection("maestros").document(userId).delete()
						.addOnSuccessListener(aVoid2 -> {
							Toast.makeText(this, "Registro cancelado", Toast.LENGTH_SHORT).show();
							finish();
							startActivity(new android.content.Intent(this, SelectionScreen.class));
						});
				} else {
					finish();
				}
			});
	}

	private void initViews() {
		previewView = findViewById(R.id.previewView);
		landmarkOverlay = findViewById(R.id.landmarkOverlay);
		tvInstructions = findViewById(R.id.tvInstructions);
		tvPoseFeedback = findViewById(R.id.tvPoseFeedback);
		btnStart = findViewById(R.id.btnStart);
		btnCapture = findViewById(R.id.btnCapture);
	}
	
	private void setupFaceDetector() {
		FaceDetectorOptions options = new FaceDetectorOptions.Builder()
				.setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
				.setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
				.setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
				.setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
				.setMinFaceSize(0.15f)
				.enableTracking()
				.build();

		faceDetector = FaceDetection.getClient(options);
		if (cameraExecutor == null) {
			cameraExecutor = Executors.newSingleThreadExecutor();
		}
	}

	private boolean checkCameraPermission() {
		return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
	}

	private void requestCameraPermission() {
		ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				startCamera();
			} else {
				Toast.makeText(this, "Se necesita permiso de cámara para registrar el rostro", Toast.LENGTH_LONG).show();
				finish();
			}
		}
	}

	private void startCamera() {
		cameraProviderFuture = ProcessCameraProvider.getInstance(this);

		cameraProviderFuture.addListener(() -> {
			try {
				ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
				bindCameraUseCases(cameraProvider);
			} catch (ExecutionException | InterruptedException e) {
				Toast.makeText(this, "Error al iniciar la cámara: " + e.getMessage(), Toast.LENGTH_SHORT).show();
			}
		}, ContextCompat.getMainExecutor(this));
	}

	private void bindCameraUseCases(ProcessCameraProvider cameraProvider) {
		Preview preview = new Preview.Builder()
				.setTargetAspectRatio(AspectRatio.RATIO_4_3)
				.build();

		preview.setSurfaceProvider(previewView.getSurfaceProvider());

		imageCapture = new ImageCapture.Builder()
				.setTargetAspectRatio(AspectRatio.RATIO_4_3)
				.setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
				.build();

		ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
				.setTargetAspectRatio(AspectRatio.RATIO_4_3)
				.setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
				.build();

		imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

		CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

		try {
			cameraProvider.unbindAll();
			Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview, imageAnalysis, imageCapture);
		} catch (Exception e) {
			Toast.makeText(this, "Error al conectar la cámara: " + e.getMessage(), Toast.LENGTH_SHORT).show();
		}
	}

	private void analyzeImage(ImageProxy imageProxy) {
		// Si el escaneo no ha comenzado o ya estamos capturando, no procesar más frames
		if (!scanningStarted || isCapturing) {
			imageProxy.close();
			return;
		}

		InputImage image = InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());
		faceDetector.process(image)
				.addOnSuccessListener(faces -> {
					if (!faces.isEmpty() && faces.size() == 1) {
						Face face = faces.get(0);
						
						// Validar que el rostro esté bien posicionado (frente, no girado)
						boolean isValidPose = validateFacePose(face);
						
						// Guardar el último rostro válido
						if (isValidPose) {
							lastValidFace = face;
							isFaceDetectedAndValid = true;
							stableFaceFrames++;
						} else {
							isFaceDetectedAndValid = false;
							stableFaceFrames = 0; // Resetear contador si la pose no es válida
						}
						
						// Actualizar overlay view con el rostro detectado
						runOnUiThread(() -> {
							// Calcular escala y offset para el overlay
							float previewWidth = previewView.getWidth();
							float previewHeight = previewView.getHeight();
							float imageWidth = imageProxy.getWidth();
							float imageHeight = imageProxy.getHeight();
							
							if (previewWidth > 0 && previewHeight > 0 && imageWidth > 0 && imageHeight > 0) {
								float scaleX = previewWidth / imageWidth;
								float scaleY = previewHeight / imageHeight;
								landmarkOverlay.setScaleAndOffset(scaleX, scaleY, 0, 0);
								landmarkOverlay.setFace(face);
							}
							
							if (isValidPose) {
								isFaceDetected = true;
								
								if (stableFaceFrames < REQUIRED_STABLE_FRAMES) {
									// Aún no hay suficientes frames estables para empezar
									tvPoseFeedback.setText("Quédate quieto, preparando escaneo... (" + stableFaceFrames + "/" + REQUIRED_STABLE_FRAMES + ")");
									tvPoseFeedback.setTextColor(getResources().getColor(R.color.accent));
									framesSinceLastCapture = 0;
								} else if (capturedSamples < REQUIRED_SAMPLES) {
									// Ya tenemos frames estables, ahora capturamos múltiples muestras
									framesSinceLastCapture++;
									
									if (framesSinceLastCapture >= FRAMES_BETWEEN_SAMPLES) {
										// Capturar una muestra
										captureSample(face);
										framesSinceLastCapture = 0;
									}
									
									// Mostrar progreso de captura
									tvPoseFeedback.setText("Escaneando... (" + capturedSamples + "/" + REQUIRED_SAMPLES + " muestras)");
									tvPoseFeedback.setTextColor(getResources().getColor(R.color.primary));
								} else if (capturedSamples == REQUIRED_SAMPLES && !isCapturing) {
									// Todas las muestras capturadas - guardar datos
									isCapturing = true; // Prevenir múltiples guardados
									tvPoseFeedback.setText("✓ Escaneo completo! Guardando datos...");
									tvPoseFeedback.setTextColor(getResources().getColor(R.color.success));
									saveFaceData();
								}
							} else {
								stableFaceFrames = 0;
								capturedSamples = 0;
								framesSinceLastCapture = 0;
								isFaceDetected = false;
								tvPoseFeedback.setText("⏳ Ajusta tu posición. Mantén tu rostro de frente.");
								tvPoseFeedback.setTextColor(getResources().getColor(R.color.warning));
							}
							
							if (faces.size() > 1) {
								stableFaceFrames = 0;
								tvPoseFeedback.setText("⚠ Solo un rostro debe estar visible");
								tvPoseFeedback.setTextColor(getResources().getColor(R.color.error));
								landmarkOverlay.clear();
							}
						});
					} else if (faces.size() > 1) {
						stableFaceFrames = 0;
						runOnUiThread(() -> {
							tvPoseFeedback.setText("⚠ Asegúrate de que solo tu rostro esté visible");
							tvPoseFeedback.setTextColor(getResources().getColor(R.color.error));
							isFaceDetected = false;
							landmarkOverlay.clear();
						});
					} else {
						stableFaceFrames = 0;
						runOnUiThread(() -> {
							tvPoseFeedback.setText("⏳ Buscando rostro...");
							tvPoseFeedback.setTextColor(getResources().getColor(R.color.text_secondary));
							isFaceDetected = false;
							landmarkOverlay.clear();
						});
					}
				})
				.addOnFailureListener(e -> {
					android.util.Log.e("FaceRegistration", "Error al detectar rostro", e);
					runOnUiThread(() -> {
						landmarkOverlay.clear();
					});
				})
				.addOnCompleteListener(task -> {
					imageProxy.close();
				});
	}
	
	private boolean validateFacePose(Face face) {
		// Validar que el rostro esté de frente (no girado)
		// Verificar que tenga los landmarks principales
		boolean hasLeftEye = face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.LEFT_EYE) != null;
		boolean hasRightEye = face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.RIGHT_EYE) != null;
		boolean hasNoseBase = face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.NOSE_BASE) != null;
		boolean hasMouthBottom = face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.MOUTH_BOTTOM) != null;
		
		// Todos los landmarks principales deben estar presentes
		if (!hasLeftEye || !hasRightEye || !hasNoseBase || !hasMouthBottom) {
			return false;
		}
		
		// Verificar que el rostro no esté muy girado (usando head euler angles)
		float headEulerAngleY = face.getHeadEulerAngleY(); // Rotación horizontal
		float headEulerAngleX = face.getHeadEulerAngleX(); // Rotación vertical
		
		// Debe estar de frente: Y cerca de 0, X cerca de 0
		return Math.abs(headEulerAngleY) < 15 && Math.abs(headEulerAngleX) < 15;
	}
	
	private void captureSample(Face face) {
		// Extraer datos biométricos del rostro para esta muestra
		List<Map<String, Float>> sampleData = extractFaceLandmarks(face);
		faceDataSamples.add(sampleData);
		capturedSamples++;
		
		android.util.Log.d("FaceRegistration", "Muestra " + capturedSamples + "/" + REQUIRED_SAMPLES + " capturada");
	}
	


	private Bitmap rotateBitmap(Bitmap bitmap, int degrees) {
		android.graphics.Matrix matrix = new android.graphics.Matrix();
		matrix.postRotate(degrees);
		return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
	}

	private List<Map<String, Float>> extractFaceLandmarks(Face face) {
		List<Map<String, Float>> biometricData = new ArrayList<>();
		Map<String, com.google.mlkit.vision.face.FaceLandmark> landmarks = new HashMap<>();
		
		// Extraer todos los landmarks disponibles
		if (face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.LEFT_EYE) != null) {
			landmarks.put("leftEye", face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.LEFT_EYE));
		}
		if (face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.RIGHT_EYE) != null) {
			landmarks.put("rightEye", face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.RIGHT_EYE));
		}
		if (face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.NOSE_BASE) != null) {
			landmarks.put("noseBase", face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.NOSE_BASE));
		}
		if (face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.MOUTH_BOTTOM) != null) {
			landmarks.put("mouthBottom", face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.MOUTH_BOTTOM));
		}
		if (face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.LEFT_CHEEK) != null) {
			landmarks.put("leftCheek", face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.LEFT_CHEEK));
		}
		if (face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.RIGHT_CHEEK) != null) {
			landmarks.put("rightCheek", face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.RIGHT_CHEEK));
		}
		
		// Guardar coordenadas normalizadas de landmarks
		float faceWidth = (float) face.getBoundingBox().width();
		float faceHeight = (float) face.getBoundingBox().height();
		float faceCenterX = face.getBoundingBox().centerX();
		float faceCenterY = face.getBoundingBox().centerY();
		
		Map<String, Float> normalizedLandmarks = new HashMap<>();
		for (Map.Entry<String, com.google.mlkit.vision.face.FaceLandmark> entry : landmarks.entrySet()) {
			float x = entry.getValue().getPosition().x;
			float y = entry.getValue().getPosition().y;
			// Normalizar respecto al centro del rostro
			normalizedLandmarks.put(entry.getKey() + "_x", (x - faceCenterX) / faceWidth);
			normalizedLandmarks.put(entry.getKey() + "_y", (y - faceCenterY) / faceHeight);
		}
		biometricData.add(normalizedLandmarks);
		
		// Calcular distancias biométricas (normalizadas)
		Map<String, Float> distances = new HashMap<>();
		
		if (landmarks.containsKey("leftEye") && landmarks.containsKey("rightEye")) {
			float eyeDistance = calculateDistance(
				landmarks.get("leftEye").getPosition(),
				landmarks.get("rightEye").getPosition()
			);
			distances.put("eyeDistance", eyeDistance / faceWidth);
		}
		
		if (landmarks.containsKey("noseBase") && landmarks.containsKey("mouthBottom")) {
			float noseMouthDistance = calculateDistance(
				landmarks.get("noseBase").getPosition(),
				landmarks.get("mouthBottom").getPosition()
			);
			distances.put("noseMouthDistance", noseMouthDistance / faceHeight);
		}
		
		if (landmarks.containsKey("leftEye") && landmarks.containsKey("noseBase")) {
			float leftEyeNoseDistance = calculateDistance(
				landmarks.get("leftEye").getPosition(),
				landmarks.get("noseBase").getPosition()
			);
			distances.put("leftEyeNoseDistance", leftEyeNoseDistance / faceWidth);
		}
		
		if (landmarks.containsKey("rightEye") && landmarks.containsKey("noseBase")) {
			float rightEyeNoseDistance = calculateDistance(
				landmarks.get("rightEye").getPosition(),
				landmarks.get("noseBase").getPosition()
			);
			distances.put("rightEyeNoseDistance", rightEyeNoseDistance / faceWidth);
		}
		
		// Calcular ancho de boca usando MOUTH_BOTTOM y una estimación basada en la posición
		if (landmarks.containsKey("mouthBottom") && landmarks.containsKey("leftEye") && landmarks.containsKey("rightEye")) {
			// Estimar ancho de boca basado en proporciones faciales
			float eyeDistance = calculateDistance(
				landmarks.get("leftEye").getPosition(),
				landmarks.get("rightEye").getPosition()
			);
			// El ancho de la boca suele ser aproximadamente 0.6-0.7 veces la distancia entre ojos
			distances.put("estimatedMouthWidth", (eyeDistance * 0.65f) / faceWidth);
		}
		
		if (landmarks.containsKey("leftCheek") && landmarks.containsKey("rightCheek")) {
			float cheekWidth = calculateDistance(
				landmarks.get("leftCheek").getPosition(),
				landmarks.get("rightCheek").getPosition()
			);
			distances.put("cheekWidth", cheekWidth / faceWidth);
		}
		
		if (!distances.isEmpty()) {
			biometricData.add(distances);
		}
		
		// Calcular proporciones faciales
		Map<String, Float> proportions = new HashMap<>();
		if (faceWidth > 0 && faceHeight > 0) {
			proportions.put("faceAspectRatio", faceWidth / faceHeight);
		}
		
		if (landmarks.containsKey("leftEye") && landmarks.containsKey("rightEye") && 
			landmarks.containsKey("mouthBottom")) {
			float eyeLevel = (landmarks.get("leftEye").getPosition().y + 
							  landmarks.get("rightEye").getPosition().y) / 2.0f;
			float mouthLevel = landmarks.get("mouthBottom").getPosition().y;
			float eyeMouthRatio = (mouthLevel - eyeLevel) / faceHeight;
			proportions.put("eyeMouthRatio", eyeMouthRatio);
		}
		
		if (landmarks.containsKey("leftEye") && landmarks.containsKey("rightEye") &&
			landmarks.containsKey("noseBase")) {
			float eyeLevel = (landmarks.get("leftEye").getPosition().y + 
							  landmarks.get("rightEye").getPosition().y) / 2.0f;
			float noseLevel = landmarks.get("noseBase").getPosition().y;
			float eyeNoseRatio = (noseLevel - eyeLevel) / faceHeight;
			proportions.put("eyeNoseRatio", eyeNoseRatio);
		}
		
		if (!proportions.isEmpty()) {
			biometricData.add(proportions);
		}
		
		// Agregar información del bounding box normalizado
		Map<String, Float> faceInfo = new HashMap<>();
		faceInfo.put("width", faceWidth);
		faceInfo.put("height", faceHeight);
		faceInfo.put("area", faceWidth * faceHeight);
		biometricData.add(faceInfo);
		
		return biometricData;
	}
	
	private float calculateDistance(android.graphics.PointF point1, android.graphics.PointF point2) {
		float dx = point1.x - point2.x;
		float dy = point1.y - point2.y;
		return (float) Math.sqrt(dx * dx + dy * dy);
	}

	// Método eliminado - retryCapture ya no se necesita con el nuevo sistema

	private void saveFaceData() {
		// Verificar que se hayan capturado suficientes muestras
		if (faceDataSamples == null || faceDataSamples.isEmpty() || faceDataSamples.size() < REQUIRED_SAMPLES) {
			Toast.makeText(this, "Error: No se capturaron suficientes muestras. Intenta de nuevo.", Toast.LENGTH_LONG).show();
			// Resetear para permitir nuevo intento
			capturedSamples = 0;
			faceDataSamples.clear();
			stableFaceFrames = 0;
			isCapturing = false;
			return;
		}

		FirebaseFirestore db = FirebaseFirestore.getInstance();
		Map<String, Object> faceDataMap = new HashMap<>();
		
		// Convertir las muestras a formato compatible con Firestore
		// Firestore NO soporta arrays anidados (List<List<...>>), así que aplanamos la estructura
		// Convertimos List<Map<String, Float>> a un solo Map<String, Object> aplanado
		List<Map<String, Object>> samplesForFirestore = new ArrayList<>();
		for (int i = 0; i < faceDataSamples.size(); i++) {
			List<Map<String, Float>> sample = faceDataSamples.get(i);
			
			// Aplanar todos los maps de la muestra en un solo map
			Map<String, Object> flattenedSample = new HashMap<>();
			flattenedSample.put("sampleIndex", i);
			
			// Combinar todos los maps en uno solo con prefijos para evitar colisiones
			int mapIndex = 0;
			for (Map<String, Float> map : sample) {
				for (Map.Entry<String, Float> entry : map.entrySet()) {
					// Usar formato: "map0_key", "map1_key", etc.
					flattenedSample.put("map" + mapIndex + "_" + entry.getKey(), entry.getValue());
				}
				mapIndex++;
			}
			flattenedSample.put("mapCount", mapIndex); // Guardar cuántos maps había originalmente
			
			samplesForFirestore.add(flattenedSample);
		}
		
		// Guardar múltiples muestras biométricas del rostro para mayor precisión
		faceDataMap.put("samples", samplesForFirestore); // Lista de objetos aplanados (compatible con Firestore)
		faceDataMap.put("sampleCount", faceDataSamples.size()); // Número de muestras capturadas
		faceDataMap.put("userId", userId);
		faceDataMap.put("timestamp", com.google.firebase.Timestamp.now());

		android.util.Log.d("FaceRegistration", "=== GUARDANDO DATOS FACIALES ===");
		android.util.Log.d("FaceRegistration", "Usuario ID: " + userId);
		android.util.Log.d("FaceRegistration", "Número de muestras: " + faceDataSamples.size());
		android.util.Log.d("FaceRegistration", "Tamaño de samplesForFirestore: " + samplesForFirestore.size());
		
		Map<String, Object> updates = new HashMap<>();
		updates.put("faceData", faceDataMap);
		updates.put("faceDataPending", false); // Ya no está pendiente
		
		// Si viene del registro, cambiar el estado a "pendiente" (esperando aprobación)
		if (fromRegistration) {
			updates.put("estado", "pendiente"); // Cambiar de "registro_incompleto" a "pendiente"
			android.util.Log.d("FaceRegistration", "Actualizando estado a 'pendiente'");
			
			// IMPORTANTE: Copiar faceData a la colección específica (alumnos/maestros) también
			// para que esté disponible cuando el admin apruebe
			if ("alumno".equals(userType)) {
				Map<String, Object> alumnoFaceData = new HashMap<>();
				alumnoFaceData.put("faceData", faceDataMap);
				alumnoFaceData.put("faceDataPending", false);
				db.collection("alumnos").document(userId)
					.set(alumnoFaceData, com.google.firebase.firestore.SetOptions.merge())
					.addOnSuccessListener(aVoid -> {
						android.util.Log.d("FaceRegistration", "✓ faceData copiado a alumnos y flag actualizado");
					})
					.addOnFailureListener(e -> {
						android.util.Log.e("FaceRegistration", "✗ Error al copiar faceData a alumnos: " + e.getMessage());
					});
			} else if ("maestro".equals(userType)) {
				Map<String, Object> maestroFaceData = new HashMap<>();
				maestroFaceData.put("faceData", faceDataMap);
				maestroFaceData.put("faceDataPending", false);
				db.collection("maestros").document(userId)
					.set(maestroFaceData, com.google.firebase.firestore.SetOptions.merge())
					.addOnSuccessListener(aVoid -> {
						android.util.Log.d("FaceRegistration", "✓ faceData copiado a maestros y flag actualizado");
					})
					.addOnFailureListener(e -> {
						android.util.Log.e("FaceRegistration", "✗ Error al copiar faceData a maestros: " + e.getMessage());
					});
			}
		}

		// VERIFICACIÓN ANTES DE GUARDAR
		android.util.Log.d("FaceRegistration", "=== VERIFICACIÓN ANTES DE GUARDAR ===");
		android.util.Log.d("FaceRegistration", "faceDataMap keys: " + faceDataMap.keySet());
		android.util.Log.d("FaceRegistration", "faceDataMap completo: " + faceDataMap.toString());
		android.util.Log.d("FaceRegistration", "samplesForFirestore tamaño: " + samplesForFirestore.size());
		if (!samplesForFirestore.isEmpty()) {
			android.util.Log.d("FaceRegistration", "Primera muestra tamaño: " + samplesForFirestore.get(0).size());
		}
		android.util.Log.d("FaceRegistration", "updates keys: " + updates.keySet());
		
		// IMPORTANTE: Guardar faceData en la colección usuarios
		// Usar set con merge para no sobrescribir otros campos del documento
		android.util.Log.d("FaceRegistration", "Guardando en usuarios/" + userId);
		db.collection("usuarios").document(userId)
				.set(updates, com.google.firebase.firestore.SetOptions.merge())
				.addOnSuccessListener(aVoid -> {
					android.util.Log.d("FaceRegistration", "✓✓✓ Datos faciales guardados exitosamente en Firebase");
					android.util.Log.d("FaceRegistration", "faceData guardado con " + samplesForFirestore.size() + " muestras");
					
					// Verificar que los datos se guardaron correctamente leyendo el documento INMEDIATAMENTE
					android.util.Log.d("FaceRegistration", "=== VERIFICACIÓN POST-GUARDADO ===");
					db.collection("usuarios").document(userId).get()
						.addOnSuccessListener(documentSnapshot -> {
							if (documentSnapshot.exists()) {
								android.util.Log.d("FaceRegistration", "✓ Documento existe después de guardar");
								Map<String, Object> savedFaceData = (Map<String, Object>) documentSnapshot.get("faceData");
								if (savedFaceData != null) {
									android.util.Log.d("FaceRegistration", "✓ VERIFICACIÓN: faceData existe en Firebase");
									android.util.Log.d("FaceRegistration", "✓ VERIFICACIÓN: faceData keys: " + savedFaceData.keySet());
									
									Object savedSamples = savedFaceData.get("samples");
									Object savedSampleCount = savedFaceData.get("sampleCount");
									Object savedUserId = savedFaceData.get("userId");
									
									android.util.Log.d("FaceRegistration", "✓ VERIFICACIÓN: samples existe: " + (savedSamples != null));
									android.util.Log.d("FaceRegistration", "✓ VERIFICACIÓN: sampleCount: " + savedSampleCount);
									android.util.Log.d("FaceRegistration", "✓ VERIFICACIÓN: userId en faceData: " + savedUserId);
									
									if (savedSamples != null) {
										android.util.Log.d("FaceRegistration", "✓ VERIFICACIÓN: Tipo de samples: " + savedSamples.getClass().getSimpleName());
										if (savedSamples instanceof List) {
											int savedSamplesSize = ((List<?>) savedSamples).size();
											android.util.Log.d("FaceRegistration", "✓ VERIFICACIÓN: Número de muestras guardadas: " + savedSamplesSize);
											
											if (savedSamplesSize > 0) {
												Object firstSample = ((List<?>) savedSamples).get(0);
												android.util.Log.d("FaceRegistration", "✓ VERIFICACIÓN: Primera muestra tipo: " + (firstSample != null ? firstSample.getClass().getSimpleName() : "null"));
												if (firstSample instanceof List) {
													android.util.Log.d("FaceRegistration", "✓ VERIFICACIÓN: Primera muestra tiene " + ((List<?>) firstSample).size() + " elementos");
												}
											}
											
											if (savedSamplesSize == samplesForFirestore.size()) {
												android.util.Log.d("FaceRegistration", "✓✓✓ VERIFICACIÓN EXITOSA: Todas las muestras se guardaron correctamente");
											} else {
												android.util.Log.e("FaceRegistration", "✗ ERROR: Número de muestras no coincide. Esperado: " + samplesForFirestore.size() + ", Guardado: " + savedSamplesSize);
											}
										} else {
											android.util.Log.e("FaceRegistration", "✗ ERROR: samples no es una List, es: " + savedSamples.getClass().getSimpleName());
										}
									} else {
										android.util.Log.e("FaceRegistration", "✗ ERROR: samples es null después de guardar");
									}
								} else {
									android.util.Log.e("FaceRegistration", "✗ ERROR: faceData es null después de guardar");
								}
							} else {
								android.util.Log.e("FaceRegistration", "✗ ERROR: Documento no existe después de guardar");
							}
						})
						.addOnFailureListener(e -> {
							android.util.Log.e("FaceRegistration", "✗ ERROR al verificar datos guardados: " + e.getMessage());
							e.printStackTrace();
						});
					
					Toast.makeText(this, "Registro facial completado. Tu registro está pendiente de aprobación.", Toast.LENGTH_LONG).show();
					
					// Redirigir según el tipo de usuario
					android.content.Intent intent;
					if (fromRegistration) {
						// Si viene del registro, mostrar mensaje y volver a la pantalla de selección
						intent = new android.content.Intent(this, SelectionScreen.class);
						Toast.makeText(this, "Tu solicitud está pendiente de aprobación del administrador", Toast.LENGTH_LONG).show();
					} else if ("alumno".equals(userType)) {
						intent = new android.content.Intent(this, AlumnoPanelActivity.class);
					} else if ("maestro".equals(userType)) {
						intent = new android.content.Intent(this, MaestroPanelActivity.class);
					} else {
						intent = new android.content.Intent(this, SelectionScreen.class);
					}
					
					intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
					startActivity(intent);
					finish();
				})
				.addOnFailureListener(e -> {
					android.util.Log.e("FaceRegistration", "✗ ERROR al guardar datos faciales: " + e.getMessage());
					android.util.Log.e("FaceRegistration", "Stack trace: ", e);
					Toast.makeText(this, "Error al guardar datos faciales: " + e.getMessage(), Toast.LENGTH_LONG).show();
					// Resetear flags para permitir nuevo intento
					isCapturing = false;
					capturedSamples = 0;
					faceDataSamples.clear();
					stableFaceFrames = 0;
					Toast.makeText(this, "Error al guardar datos faciales: " + e.getMessage() + ". Intenta de nuevo.", Toast.LENGTH_LONG).show();
					android.util.Log.e("FaceRegistration", "Error al guardar en Firestore", e);
					runOnUiThread(() -> {
						tvPoseFeedback.setText("Error al guardar. Intenta de nuevo.");
						tvPoseFeedback.setTextColor(getResources().getColor(R.color.error));
					});
				});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (cameraExecutor != null) {
			cameraExecutor.shutdown();
		}
		if (faceDetector != null) {
			faceDetector.close();
		}
	}
}

