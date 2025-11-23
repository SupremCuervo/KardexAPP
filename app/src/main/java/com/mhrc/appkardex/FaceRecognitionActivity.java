package com.mhrc.appkardex;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FaceRecognitionActivity extends AppCompatActivity {

	private static final int CAMERA_PERMISSION_REQUEST_CODE = 1002;
	private static final float FACE_MATCH_THRESHOLD = 0.65f; // Umbral optimizado para reconocimiento con múltiples muestras (más permisivo pero seguro)

	private PreviewView previewView;
	private FaceLandmarkOverlayView landmarkOverlay;
	private TextView tvInstructions;
	private Button btnRecognize;

	private FaceDetector faceDetector;
	private ExecutorService cameraExecutor;
	private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
	private ImageCapture imageCapture;
	private boolean isFaceDetected = false;
	private Face lastDetectedFace = null; // Último rostro detectado
	private boolean isRecognizing = false; // Flag para evitar múltiples reconocimientos simultáneos
	private int stableFaceFrames = 0; // Contador de frames con rostro estable
	private static final int REQUIRED_STABLE_FRAMES = 6; // Frames estables requeridos antes de validar (más rápido)
	private List<List<Map<String, Float>>> savedFaceSamples = null; // Múltiples muestras guardadas en Firebase (cached)
	private static final int MIN_MATCHES_REQUIRED = 3; // Mínimo de muestras que deben coincidir para acceso

	private String userId;
	private FirebaseFirestore db;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_face_recognition);

		FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
		if (currentUser == null) {
			Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

		userId = currentUser.getUid();
		db = FirebaseFirestore.getInstance();

		// RECONOCIMIENTO FACIAL OBLIGATORIO - Deshabilitar botón de retroceso
		// No permitir salir sin reconocimiento exitoso

		initViews();
		setupFaceDetector();

		// Usar ML Kit para reconocimiento facial (mostrar cámara)
		previewView.setVisibility(android.view.View.VISIBLE);
		tvInstructions.setText("Posiciona tu rostro frente a la cámara. El reconocimiento es automático.");
		btnRecognize.setText("Reintentar");
		btnRecognize.setVisibility(android.view.View.GONE); // Ocultar inicialmente, solo mostrar si falla

		// Cargar datos faciales guardados de Firestore (cachearlos)
		// IMPORTANTE: El reconocimiento facial es OBLIGATORIO
		loadSavedFaceData();

		if (checkCameraPermission()) {
			startCamera();
		} else {
			requestCameraPermission();
		}

		btnRecognize.setOnClickListener(v -> {
			// Solo para reintentar si falló
			isRecognizing = false;
			stableFaceFrames = 0;
			lastDetectedFace = null;
			isFaceDetected = false;
			btnRecognize.setVisibility(android.view.View.GONE);
			tvInstructions.setText("Posiciona tu rostro frente a la cámara. El reconocimiento es automático.");
			tvInstructions.setTextColor(getResources().getColor(R.color.text_secondary));
			landmarkOverlay.clear();
		});
	}

	private void initViews() {
		previewView = findViewById(R.id.previewView);
		landmarkOverlay = findViewById(R.id.landmarkOverlay);
		tvInstructions = findViewById(R.id.tvInstructions);
		btnRecognize = findViewById(R.id.btnRecognize);
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
		cameraExecutor = Executors.newSingleThreadExecutor();
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
				Toast.makeText(this, "El reconocimiento facial es obligatorio. Se necesita permiso de cámara.", Toast.LENGTH_LONG).show();
				// No permitir continuar sin permiso de cámara
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
		// Si ya estamos reconociendo, no procesar más frames
		if (isRecognizing) {
			imageProxy.close();
			return;
		}

		InputImage image = InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());
		faceDetector.process(image)
				.addOnSuccessListener(faces -> {
					if (!faces.isEmpty() && faces.size() == 1) {
						// Guardar el último rostro detectado
						Face face = faces.get(0);
						lastDetectedFace = face;
						isFaceDetected = true;
						
						// Validar que el rostro esté bien posicionado (de frente)
						boolean isValidPose = validateFacePose(face);
						
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
								stableFaceFrames++;
								
								if (stableFaceFrames < REQUIRED_STABLE_FRAMES) {
									// Aún no hay suficientes frames estables
									tvInstructions.setText("Quédate quieto, escaneando... (" + stableFaceFrames + "/" + REQUIRED_STABLE_FRAMES + ")");
								} else if (stableFaceFrames == REQUIRED_STABLE_FRAMES && savedFaceSamples != null && !savedFaceSamples.isEmpty()) {
									// Rostro estable detectado - validar automáticamente
									tvInstructions.setText("Validando identidad...");
									recognizeFaceAutomatically(face);
								} else if (savedFaceSamples == null || savedFaceSamples.isEmpty()) {
									// Esperando cargar datos de Firebase
									tvInstructions.setText("Cargando datos de reconocimiento...");
								}
							} else {
								stableFaceFrames = 0; // Resetear contador si la pose no es válida
								tvInstructions.setText("Mantén tu rostro de frente y quieto.");
							}
						});
					} else if (faces.size() > 1) {
						stableFaceFrames = 0;
						runOnUiThread(() -> {
							tvInstructions.setText("Por favor, asegúrate de que solo tu rostro esté visible.");
							landmarkOverlay.clear();
						});
						isFaceDetected = false;
					} else {
						stableFaceFrames = 0;
						isFaceDetected = false;
						runOnUiThread(() -> {
							tvInstructions.setText("Posiciona tu rostro frente a la cámara.");
							landmarkOverlay.clear();
						});
					}
				})
				.addOnFailureListener(e -> {
					android.util.Log.e("FaceRecognition", "Error al detectar rostro", e);
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
	
	private void loadSavedFaceData() {
		// Cargar datos faciales guardados de Firestore (cachearlos)
		android.util.Log.d("FaceRecognition", "=== CARGANDO DATOS FACIALES ===");
		android.util.Log.d("FaceRecognition", "Usuario ID: " + userId);
		
		db.collection("usuarios").document(userId).get()
				.addOnSuccessListener(documentSnapshot -> {
					if (documentSnapshot.exists()) {
						Map<String, Object> faceDataMap = (Map<String, Object>) documentSnapshot.get("faceData");
						android.util.Log.d("FaceRecognition", "faceDataMap es null: " + (faceDataMap == null));
						
						if (faceDataMap != null) {
							// Intentar obtener múltiples muestras (nuevo formato mejorado - sin arrays anidados)
							@SuppressWarnings("unchecked")
							List<Map<String, Object>> samplesData = (List<Map<String, Object>>) faceDataMap.get("samples");
							
							android.util.Log.d("FaceRecognition", "samplesData es null: " + (samplesData == null));
							if (samplesData != null) {
								android.util.Log.d("FaceRecognition", "samplesData tamaño: " + samplesData.size());
							}
							
							if (samplesData != null && !samplesData.isEmpty()) {
								// Verificar si es el nuevo formato (objetos aplanados) o formato antiguo (arrays anidados)
								Object firstSample = samplesData.get(0);
								if (firstSample instanceof Map) {
									Map<String, Object> firstSampleMap = (Map<String, Object>) firstSample;
									if (firstSampleMap.containsKey("sampleIndex") && firstSampleMap.containsKey("mapCount")) {
										// Nuevo formato: objetos aplanados con mapCount
										android.util.Log.d("FaceRecognition", "Leyendo nuevo formato (objetos aplanados)");
										savedFaceSamples = new ArrayList<>();
										for (Map<String, Object> sampleObj : samplesData) {
											// Reconstruir la estructura original List<Map<String, Float>>
											List<Map<String, Float>> reconstructedSample = reconstructSampleFromFlattened(sampleObj);
											if (!reconstructedSample.isEmpty()) {
												savedFaceSamples.add(reconstructedSample);
											}
										}
										android.util.Log.d("FaceRecognition", "✓ Cargadas " + savedFaceSamples.size() + " muestras (nuevo formato) para reconocimiento");
									} else {
										// Intentar formato antiguo: arrays anidados (compatibilidad)
										android.util.Log.d("FaceRecognition", "Intentando formato antiguo (arrays anidados)");
										try {
											@SuppressWarnings("unchecked")
											List<List<Map<String, Object>>> oldFormatSamples = (List<List<Map<String, Object>>>) faceDataMap.get("samples");
											if (oldFormatSamples != null && !oldFormatSamples.isEmpty()) {
												savedFaceSamples = new ArrayList<>();
												for (List<Map<String, Object>> sample : oldFormatSamples) {
													savedFaceSamples.add(convertLandmarks(sample));
												}
												android.util.Log.d("FaceRecognition", "✓ Cargadas " + savedFaceSamples.size() + " muestras (formato antiguo) para reconocimiento");
											}
										} catch (ClassCastException e) {
											android.util.Log.e("FaceRecognition", "Error al leer formato antiguo: " + e.getMessage());
										}
									}
								}
								
								if (savedFaceSamples != null && !savedFaceSamples.isEmpty()) {
									runOnUiThread(() -> {
										tvInstructions.setText("Datos cargados. Posiciona tu rostro frente a la cámara.");
									});
								} else {
									android.util.Log.e("FaceRecognition", "✗ ERROR: No se pudieron reconstruir las muestras");
								}
							} else {
								// Formato antiguo - intentar obtener landmarks o phase1_front
								@SuppressWarnings("unchecked")
								List<Map<String, Object>> landmarksData = (List<Map<String, Object>>) faceDataMap.get("landmarks");
								
								// Si no hay landmarks, intentar formato antiguo (compatibilidad con fases)
								if (landmarksData == null) {
									@SuppressWarnings("unchecked")
									List<Map<String, Object>> phase1Data = (List<Map<String, Object>>) faceDataMap.get("phase1_front");
									if (phase1Data != null) {
										landmarksData = phase1Data;
									}
								}
								
								if (landmarksData != null && !landmarksData.isEmpty()) {
									// Convertir a formato de muestras (una sola muestra)
									List<Map<String, Float>> singleSample = convertLandmarks(landmarksData);
									savedFaceSamples = new ArrayList<>();
									savedFaceSamples.add(singleSample);
									android.util.Log.d("FaceRecognition", "✓ Cargada 1 muestra (formato antiguo) para reconocimiento");
									runOnUiThread(() -> {
										tvInstructions.setText("Datos cargados. Posiciona tu rostro frente a la cámara.");
									});
								} else {
									android.util.Log.e("FaceRecognition", "✗ ERROR: No se encontraron datos faciales válidos en faceData");
									android.util.Log.e("FaceRecognition", "faceDataMap keys: " + (faceDataMap != null ? faceDataMap.keySet() : "null"));
									
									// Intentar verificar si hay algún dato en faceDataMap
									if (faceDataMap != null) {
										android.util.Log.e("FaceRecognition", "faceDataMap contiene: " + faceDataMap.keySet());
										// Si tiene sampleCount pero no samples, puede ser un error de formato
										Object sampleCount = faceDataMap.get("sampleCount");
										if (sampleCount != null) {
											android.util.Log.e("FaceRecognition", "Tiene sampleCount: " + sampleCount + " pero no samples válidos");
										}
									}
									
									runOnUiThread(() -> {
										tvInstructions.setText("Error: Datos faciales incompletos. Contacta al administrador.");
										tvInstructions.setTextColor(getResources().getColor(R.color.error));
									});
									Toast.makeText(this, "Error: Tus datos faciales están incompletos. Contacta al administrador para re-registrarte.", Toast.LENGTH_LONG).show();
									// Cerrar sesión y volver
									FirebaseAuth.getInstance().signOut();
									new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
										finish();
									}, 3000);
								}
							}
						} else {
							android.util.Log.e("FaceRecognition", "✗ ERROR: faceDataMap es null");
							runOnUiThread(() -> {
								tvInstructions.setText("Error: No se encontraron datos faciales. Debes completar tu registro facial.");
								tvInstructions.setTextColor(getResources().getColor(R.color.error));
							});
							Toast.makeText(this, "Error: No tienes datos faciales registrados. Debes completar tu registro primero.", Toast.LENGTH_LONG).show();
							// Cerrar sesión y volver
							FirebaseAuth.getInstance().signOut();
							new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
								finish();
							}, 3000);
						}
					} else {
						android.util.Log.e("FaceRecognition", "✗ ERROR: Documento no existe");
						runOnUiThread(() -> {
							tvInstructions.setText("Error: Usuario no encontrado.");
							tvInstructions.setTextColor(getResources().getColor(R.color.error));
						});
						Toast.makeText(this, "Error: Usuario no encontrado.", Toast.LENGTH_LONG).show();
						// Cerrar sesión y volver
						FirebaseAuth.getInstance().signOut();
						new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
							finish();
						}, 3000);
					}
				})
				.addOnFailureListener(e -> {
					android.util.Log.e("FaceRecognition", "✗ ERROR al cargar datos faciales", e);
					runOnUiThread(() -> {
						tvInstructions.setText("Error al cargar datos. Intenta de nuevo.");
						tvInstructions.setTextColor(getResources().getColor(R.color.error));
					});
					Toast.makeText(this, "Error al cargar datos faciales: " + e.getMessage(), Toast.LENGTH_LONG).show();
				});
	}
	
	private void recognizeFaceAutomatically(Face face) {
		if (isRecognizing || savedFaceSamples == null || savedFaceSamples.isEmpty()) {
			return;
		}
		
		isRecognizing = true;
		
		// Extraer landmarks del rostro actual
		List<Map<String, Float>> currentLandmarks = extractFaceLandmarks(face);
		
		// Comparar con TODAS las muestras guardadas usando sistema de votación mejorado
		float bestSimilarity = 0.0f;
		float totalSimilarity = 0.0f;
		int matchesAboveThreshold = 0; // Contador de muestras que superan el umbral
		List<Float> allSimilarities = new ArrayList<>(); // Para calcular mediana
		
		for (List<Map<String, Float>> sample : savedFaceSamples) {
			float similarity = calculateSimilarity(currentLandmarks, sample);
			allSimilarities.add(similarity);
			
			if (similarity > bestSimilarity) {
				bestSimilarity = similarity;
			}
			totalSimilarity += similarity;
			
			// Contar cuántas muestras superan el umbral
			if (similarity >= FACE_MATCH_THRESHOLD) {
				matchesAboveThreshold++;
			}
		}
		
		// Calcular estadísticas
		float averageSimilarity = totalSimilarity / savedFaceSamples.size();
		
		// Calcular mediana para mayor robustez (menos afectada por outliers)
		Collections.sort(allSimilarities);
		float medianSimilarity = 0.0f;
		if (!allSimilarities.isEmpty()) {
			int middle = allSimilarities.size() / 2;
			if (allSimilarities.size() % 2 == 0) {
				medianSimilarity = (allSimilarities.get(middle - 1) + allSimilarities.get(middle)) / 2.0f;
			} else {
				medianSimilarity = allSimilarities.get(middle);
			}
		}
		
		// Estrategia de reconocimiento mejorada:
		// 1. Si al menos MIN_MATCHES_REQUIRED muestras superan el umbral, acceso permitido
		// 2. O si la mejor similitud es muy alta (>0.80), acceso permitido
		// 3. O si la mediana es alta (>0.70), acceso permitido
		boolean accessGranted = false;
		
		if (matchesAboveThreshold >= MIN_MATCHES_REQUIRED) {
			// Al menos MIN_MATCHES_REQUIRED muestras coinciden
			accessGranted = true;
			android.util.Log.d("FaceRecognition", "Acceso por votación: " + matchesAboveThreshold + "/" + savedFaceSamples.size() + " muestras coinciden");
		} else if (bestSimilarity > 0.80f) {
			// Mejor coincidencia muy alta
			accessGranted = true;
			android.util.Log.d("FaceRecognition", "Acceso por mejor coincidencia alta: " + bestSimilarity);
		} else if (medianSimilarity > 0.70f && averageSimilarity > 0.68f) {
			// Mediana y promedio altos
			accessGranted = true;
			android.util.Log.d("FaceRecognition", "Acceso por mediana/promedio: mediana=" + medianSimilarity + ", promedio=" + averageSimilarity);
		}
		
		android.util.Log.d("FaceRecognition", "Mejor: " + bestSimilarity + ", Promedio: " + averageSimilarity + ", Mediana: " + medianSimilarity + ", Coincidencias: " + matchesAboveThreshold + "/" + savedFaceSamples.size());
		
		if (accessGranted) {
			// Rostro reconocido - acceso permitido
			runOnUiThread(() -> {
				tvInstructions.setText("¡Acceso permitido!");
				tvInstructions.setTextColor(getResources().getColor(R.color.success));
			});
			
			// Esperar un momento para mostrar el mensaje y luego proceder
			new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
				Toast.makeText(this, "¡Rostro reconocido! Acceso permitido.", Toast.LENGTH_SHORT).show();
				proceedToMainScreen();
			}, 500);
		} else {
			// Rostro no reconocido
			isRecognizing = false;
			stableFaceFrames = 0;
			runOnUiThread(() -> {
				tvInstructions.setText("Rostro no reconocido. Intenta de nuevo.");
				tvInstructions.setTextColor(getResources().getColor(R.color.error));
				btnRecognize.setVisibility(android.view.View.VISIBLE);
				btnRecognize.setText("Reintentar");
			});
		}
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

	@SuppressWarnings("unchecked")
	private List<Map<String, Float>> convertLandmarks(List<Map<String, Object>> savedLandmarksData) {
		List<Map<String, Float>> landmarks = new ArrayList<>();
		for (Map<String, Object> landmarkData : savedLandmarksData) {
			Map<String, Float> landmark = new HashMap<>();
			for (Map.Entry<String, Object> entry : landmarkData.entrySet()) {
				Object value = entry.getValue();
				if (value instanceof Double) {
					landmark.put(entry.getKey(), ((Double) value).floatValue());
				} else if (value instanceof Float) {
					landmark.put(entry.getKey(), (Float) value);
				} else if (value instanceof Integer) {
					landmark.put(entry.getKey(), ((Integer) value).floatValue());
				}
			}
			landmarks.add(landmark);
		}
		return landmarks;
	}
	
	private List<Map<String, Float>> reconstructSampleFromFlattened(Map<String, Object> flattenedData) {
		// Reconstruir la estructura original desde el formato aplanado
		// El formato aplanado es: "map0_key1", "map0_key2", "map1_key1", etc.
		// Necesitamos agrupar por el índice del map (0, 1, 2, ...)
		
		// Obtener el número de maps originales
		Object mapCountObj = flattenedData.get("mapCount");
		int mapCount = 0;
		if (mapCountObj instanceof Integer) {
			mapCount = (Integer) mapCountObj;
		} else if (mapCountObj instanceof Long) {
			mapCount = ((Long) mapCountObj).intValue();
		}
		
		Map<Integer, Map<String, Float>> groupedData = new HashMap<>();
		
		for (Map.Entry<String, Object> entry : flattenedData.entrySet()) {
			String key = entry.getKey();
			// Ignorar campos de metadatos
			if (key.equals("sampleIndex") || key.equals("mapCount")) {
				continue;
			}
			
			if (key.startsWith("map")) {
				// Formato: "map0_keyName" o "map1_keyName"
				String[] parts = key.split("_", 2);
				if (parts.length >= 2 && parts[0].startsWith("map")) {
					try {
						// Extraer el número después de "map"
						String mapIndexStr = parts[0].substring(3); // "map" tiene 3 caracteres
						int mapIndex = Integer.parseInt(mapIndexStr);
						String originalKey = parts[1];
						
						Map<String, Float> map = groupedData.get(mapIndex);
						if (map == null) {
							map = new HashMap<>();
							groupedData.put(mapIndex, map);
						}
						
						Object value = entry.getValue();
						float floatValue = 0.0f;
						if (value instanceof Double) {
							floatValue = ((Double) value).floatValue();
						} else if (value instanceof Float) {
							floatValue = (Float) value;
						} else if (value instanceof Integer) {
							floatValue = ((Integer) value).floatValue();
						} else if (value instanceof Long) {
							floatValue = ((Long) value).floatValue();
						}
						map.put(originalKey, floatValue);
					} catch (NumberFormatException e) {
						android.util.Log.e("FaceRecognition", "Error al parsear índice en key: " + key + " - " + e.getMessage());
					}
				}
			}
		}
		
		// Convertir el mapa agrupado a lista ordenada
		List<Map<String, Float>> reconstructed = new ArrayList<>();
		for (int i = 0; i < mapCount; i++) {
			Map<String, Float> map = groupedData.get(i);
			if (map != null && !map.isEmpty()) {
				reconstructed.add(map);
			} else {
				// Si falta un map, agregar uno vacío para mantener el orden
				reconstructed.add(new HashMap<>());
			}
		}
		
		android.util.Log.d("FaceRecognition", "Reconstruida muestra con " + reconstructed.size() + " maps (esperado: " + mapCount + ")");
		return reconstructed;
	}

	private float calculateSimilarity(List<Map<String, Float>> currentBiometricData, List<Map<String, Float>> savedBiometricData) {
		if (currentBiometricData.isEmpty() || savedBiometricData.isEmpty()) {
			return 0.0f;
		}

		float totalSimilarity = 0.0f;
		float totalWeight = 0.0f;

		// Comparar cada tipo de dato biométrico
		for (int i = 0; i < Math.min(currentBiometricData.size(), savedBiometricData.size()); i++) {
			Map<String, Float> current = currentBiometricData.get(i);
			Map<String, Float> saved = savedBiometricData.get(i);

			// Calcular similitud para este grupo de características
			float similarity = 0.0f;
			int keyCount = 0;

			for (String key : current.keySet()) {
				if (saved.containsKey(key)) {
					float currentValue = current.get(key);
					float savedValue = saved.get(key);
					
					// Calcular diferencia porcentual
					float diff = Math.abs(currentValue - savedValue);
					float avgValue = (Math.abs(currentValue) + Math.abs(savedValue)) / 2.0f;
					
					if (avgValue > 0.0001f) {
						float percentageDiff = diff / avgValue;
						// Convertir a similitud (menor diferencia = mayor similitud)
						float keySimilarity = 1.0f / (1.0f + percentageDiff * 10.0f);
						similarity += keySimilarity;
						keyCount++;
					}
				}
			}

			if (keyCount > 0) {
				float groupSimilarity = similarity / keyCount;
				
				// Dar más peso a landmarks normalizados y distancias (más confiables)
				float weight = 1.0f;
				if (current.containsKey("leftEye_x") || current.containsKey("eyeDistance")) {
					weight = 2.0f; // Peso doble para características faciales clave
				}
				
				totalSimilarity += groupSimilarity * weight;
				totalWeight += weight;
			}
		}

		return totalWeight > 0 ? totalSimilarity / totalWeight : 0.0f;
	}

	// Método eliminado - el reconocimiento facial es obligatorio

	private void proceedToMainScreen() {
		// Obtener el rol del usuario y redirigir al panel correspondiente
		db.collection("usuarios").document(userId).get()
				.addOnSuccessListener(documentSnapshot -> {
					if (documentSnapshot.exists()) {
						String rol = documentSnapshot.getString("rol");
						String estado = documentSnapshot.getString("estado");

						if (estado != null && estado.equals("aprobado")) {
							Intent intent;
							if ("admin".equals(rol)) {
								intent = new Intent(this, AdminPanelActivity.class);
							} else if ("maestro".equals(rol)) {
								intent = new Intent(this, MaestroPanelActivity.class);
							} else if ("alumno".equals(rol)) {
								intent = new Intent(this, AlumnoPanelActivity.class);
							} else {
								intent = new Intent(this, SelectionScreen.class);
							}
							
							intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
							startActivity(intent);
							finish();
						} else {
							Toast.makeText(this, "Tu cuenta está pendiente de aprobación", Toast.LENGTH_LONG).show();
							finish();
						}
					} else {
						Toast.makeText(this, "Usuario no encontrado", Toast.LENGTH_SHORT).show();
						finish();
					}
				})
				.addOnFailureListener(e -> {
					Toast.makeText(this, "Error al obtener información del usuario", Toast.LENGTH_SHORT).show();
					finish();
				});
	}

	@Override
	public void onBackPressed() {
		// RECONOCIMIENTO FACIAL OBLIGATORIO - No permitir salir sin reconocimiento exitoso
		Toast.makeText(this, "El reconocimiento facial es obligatorio. No puedes salir sin completarlo.", Toast.LENGTH_LONG).show();
		// No llamar super.onBackPressed() para bloquear el retroceso
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

