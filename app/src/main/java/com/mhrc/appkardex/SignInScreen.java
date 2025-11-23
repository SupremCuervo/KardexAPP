package com.mhrc.appkardex;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;
import java.util.Map;

public class SignInScreen extends AppCompatActivity {

	private static final int RC_SIGN_IN = 9001;
	private GoogleSignInClient mGoogleSignInClient;
	private FirebaseAuth mAuth;
	private FirebaseFirestore db;
	private String userType;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sign_in);

		mAuth = FirebaseAuth.getInstance();
		db = FirebaseFirestore.getInstance();

		// Obtener tipo de usuario desde la activity anterior
		userType = getIntent().getStringExtra("userType");

		TextView tvTitle = findViewById(R.id.tvSignInTitle);
		if (userType != null && !userType.equals("login")) {
			tvTitle.setText("Login con Google requerido");
		}

		// Configure Google Sign In - forzar selección de cuenta
		GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
				.requestIdToken(getString(R.string.default_web_client_id))
				.requestEmail()
				.requestProfile()
				.build();

		mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

		// Cerrar sesión previa de Google para forzar selección de cuenta
		mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
			android.util.Log.d("SignInScreen", "Sesión de Google cerrada");
		});

		// Configurar botón de Google Sign In
		SignInButton signInButton = findViewById(R.id.btnGoogleSignIn);
		if (signInButton != null) {
			signInButton.setOnClickListener(v -> signIn());
		} else {
			android.util.Log.e("SignInScreen", "btnGoogleSignIn no encontrado en el layout");
			Toast.makeText(this, "Error: Botón de login no encontrado", Toast.LENGTH_SHORT).show();
		}

		android.util.Log.d("SignInScreen", "onCreate completado - userType: " + userType);
	}

	@Override
	public void onBackPressed() {
		android.util.Log.d("SignInScreen", "Botón atrás presionado");
		
		// Si viene de registro, confirmar antes de salir
		if (userType != null && !userType.equals("login")) {
			new androidx.appcompat.app.AlertDialog.Builder(this)
				.setTitle("¿Cancelar registro?")
				.setMessage("Si regresas, tendrás que iniciar el proceso de registro nuevamente.")
				.setPositiveButton("Continuar registro", null)
				.setNegativeButton("Cancelar", (dialog, which) -> {
					android.util.Log.d("SignInScreen", "Usuario canceló registro, regresando");
					super.onBackPressed();
				})
				.show();
		} else {
			// Si es login normal, permitir regresar
			super.onBackPressed();
		}
	}

	private void signIn() {
		try {
			android.util.Log.d("SignInScreen", "Iniciando login con Google");
			Intent signInIntent = mGoogleSignInClient.getSignInIntent();
			if (signInIntent != null) {
				android.util.Log.d("SignInScreen", "Intent creado, iniciando activity");
				startActivityForResult(signInIntent, RC_SIGN_IN);
			} else {
				Toast.makeText(this, "Error: No se pudo crear el intent de login", Toast.LENGTH_SHORT).show();
				android.util.Log.e("SignInScreen", "signInIntent es null");
			}
		} catch (Exception e) {
			Toast.makeText(this, "Error al iniciar login: " + e.getMessage(), Toast.LENGTH_LONG).show();
			android.util.Log.e("SignInScreen", "Error en signIn()", e);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == RC_SIGN_IN) {
			// Verificar si el usuario canceló el login
			if (resultCode == RESULT_CANCELED) {
				android.util.Log.d("SignInScreen", "Login cancelado por el usuario");
				Toast.makeText(this, "Login cancelado. Puedes intentar de nuevo.", Toast.LENGTH_SHORT).show();
				// NO hacer finish() - dejar que el usuario intente de nuevo
				return;
			}

			// Verificar que el Intent no sea null
			if (data == null) {
				android.util.Log.e("SignInScreen", "Intent data es null");
				Toast.makeText(this, "Error: No se recibieron datos del login", Toast.LENGTH_SHORT).show();
				return;
			}

			Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
			task.addOnCompleteListener(this, new OnCompleteListener<GoogleSignInAccount>() {
				@Override
				public void onComplete(@NonNull Task<GoogleSignInAccount> completedTask) {
					if (completedTask.isSuccessful()) {
						try {
							GoogleSignInAccount account = completedTask.getResult(ApiException.class);
							if (account != null && account.getIdToken() != null) {
								android.util.Log.d("SignInScreen", "Cuenta de Google obtenida exitosamente");
								firebaseAuthWithGoogle(account.getIdToken());
							} else {
								Toast.makeText(SignInScreen.this, "Error: No se pudo obtener la cuenta de Google", Toast.LENGTH_SHORT).show();
								android.util.Log.e("SignInScreen", "Account o idToken es null");
							}
						} catch (ApiException e) {
							String error = "Error al iniciar sesión: " + e.getMessage();
							if (e.getStatusCode() == 12501) {
								error = "Login cancelado. Intenta de nuevo.";
							} else if (e.getStatusCode() == 10) {
								error = "Error de desarrollo. Verifica la configuración de Firebase";
							}
							Toast.makeText(SignInScreen.this, error, Toast.LENGTH_LONG).show();
							android.util.Log.e("SignInScreen", "Error en onActivityResult - ApiException", e);
						}
					} else {
						// La tarea falló
						Exception exception = completedTask.getException();
						if (exception instanceof ApiException) {
							ApiException apiException = (ApiException) exception;
							String error = "Error al iniciar sesión (Código: " + apiException.getStatusCode() + ")";
							if (apiException.getStatusCode() == 12501) {
								error = "Login cancelado. Intenta de nuevo.";
							}
							Toast.makeText(SignInScreen.this, error, Toast.LENGTH_LONG).show();
							android.util.Log.e("SignInScreen", "Error en onActivityResult - ApiException", apiException);
						} else {
							Toast.makeText(SignInScreen.this, "Error al iniciar sesión: " + (exception != null ? exception.getMessage() : "Error desconocido"), Toast.LENGTH_LONG).show();
							android.util.Log.e("SignInScreen", "Error en onActivityResult", exception);
						}
					}
				}
			});
		}
	}

	private void firebaseAuthWithGoogle(String idToken) {
		AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
		mAuth.signInWithCredential(credential)
				.addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
					@Override
					public void onComplete(@NonNull Task<AuthResult> task) {
						if (task.isSuccessful()) {
							FirebaseUser user = mAuth.getCurrentUser();
							if (user != null) {
								handleUserAfterLogin(user);
							}
						} else {
							Toast.makeText(SignInScreen.this, "Error de autenticación", Toast.LENGTH_SHORT).show();
						}
					}
				});
	}

	private void handleUserAfterLogin(FirebaseUser user) {
		// Si viene de registro, verificar tipo de usuario
		if (userType != null && !userType.equals("login")) {
			handleRegistration(user);
		} else {
			// Login normal, primero verificar si tiene datos faciales guardados
			checkFaceDataAndProceed(user);
		}
	}
	
	private void checkFaceDataAndProceed(FirebaseUser user) {
		// Verificar si el usuario tiene datos faciales guardados
		android.util.Log.d("SignInScreen", "=== INICIANDO VERIFICACIÓN DE DATOS FACIALES ===");
		android.util.Log.d("SignInScreen", "Usuario UID: " + user.getUid());
		
		db.collection("usuarios").document(user.getUid()).get()
				.addOnCompleteListener(task -> {
					if (task.isSuccessful()) {
						if (task.getResult().exists()) {
							DocumentSnapshot doc = task.getResult();
							String estado = doc.getString("estado");
							String rol = doc.getString("rol");
							
							android.util.Log.d("SignInScreen", "Documento existe en Firebase");
							android.util.Log.d("SignInScreen", "Estado: " + estado);
							android.util.Log.d("SignInScreen", "Rol: " + rol);
							
							// ADMIN NO NECESITA RECONOCIMIENTO FACIAL - Acceso directo
							if ("admin".equals(rol)) {
								android.util.Log.d("SignInScreen", "Usuario ADMIN - Acceso directo sin reconocimiento facial");
								checkUserRole(user);
								return;
							}
							
							// Si el usuario tiene estado "registro_incompleto", redirigir al registro facial
							if ("registro_incompleto".equals(estado)) {
								android.util.Log.d("SignInScreen", "Usuario con registro incompleto - REDIRIGIENDO a FaceRegistrationActivity");
								Toast.makeText(this, "Debes completar tu registro facial para continuar", Toast.LENGTH_LONG).show();
								Intent intent = new Intent(this, FaceRegistrationActivity.class);
								intent.putExtra("userId", user.getUid());
								intent.putExtra("userType", rol != null ? rol : "alumno");
								intent.putExtra("fromRegistration", true);
								startActivity(intent);
								finish();
								return;
							}
							
							Map<String, Object> faceData = (Map<String, Object>) doc.get("faceData");
							android.util.Log.d("SignInScreen", "faceData es null: " + (faceData == null));
							
							// VERIFICACIÓN DETALLADA DE DATOS FACIALES
							if (faceData != null) {
								android.util.Log.d("SignInScreen", "=== VERIFICACIÓN DETALLADA DE faceData ===");
								android.util.Log.d("SignInScreen", "faceData keys: " + faceData.keySet());
								android.util.Log.d("SignInScreen", "faceData completo: " + faceData.toString());
								
								// Verificar cada campo individualmente
								Object samples = faceData.get("samples");
								Object landmarks = faceData.get("landmarks");
								Object phase1 = faceData.get("phase1_front");
								Object sampleCount = faceData.get("sampleCount");
								Object userId = faceData.get("userId");
								Object timestamp = faceData.get("timestamp");
								
								android.util.Log.d("SignInScreen", "samples: " + (samples != null));
								android.util.Log.d("SignInScreen", "landmarks: " + (landmarks != null));
								android.util.Log.d("SignInScreen", "phase1_front: " + (phase1 != null));
								android.util.Log.d("SignInScreen", "sampleCount: " + sampleCount);
								android.util.Log.d("SignInScreen", "userId en faceData: " + userId);
								android.util.Log.d("SignInScreen", "timestamp: " + timestamp);
								
								if (samples != null && samples instanceof List) {
									android.util.Log.d("SignInScreen", "samples es List con " + ((List<?>) samples).size() + " elementos");
									if (!((List<?>) samples).isEmpty()) {
										Object firstSample = ((List<?>) samples).get(0);
										android.util.Log.d("SignInScreen", "Primera muestra tipo: " + (firstSample != null ? firstSample.getClass().getSimpleName() : "null"));
										if (firstSample instanceof List) {
											android.util.Log.d("SignInScreen", "Primera muestra es List con " + ((List<?>) firstSample).size() + " elementos");
										}
									}
								}
							}
							
							// LÓGICA SIMPLIFICADA Y ROBUSTA: Si tiene faceData (aunque sea un objeto), intentar reconocimiento
							// FaceRecognitionActivity se encargará de validar si los datos son correctos
							if (faceData != null && !faceData.isEmpty()) {
								android.util.Log.d("SignInScreen", "✓✓✓ Usuario tiene faceData - REDIRIGIENDO a FaceRecognitionActivity");
								android.util.Log.d("SignInScreen", "faceData keys: " + faceData.keySet());
								
								// SIEMPRE redirigir a reconocimiento si tiene faceData
								// FaceRecognitionActivity validará si los datos son correctos
								Intent intent = new Intent(this, FaceRecognitionActivity.class);
								intent.putExtra("userId", user.getUid());
								startActivity(intent);
								finish();
								return; // Importante: salir aquí para no ejecutar checkUserRole
							} else {
								// No tiene datos faciales - Verificar si el estado es "aprobado" o "pendiente"
								// Si está aprobado pero no tiene faceData, puede ser un error
								if ("aprobado".equals(estado) || "pendiente".equals(estado)) {
									android.util.Log.e("SignInScreen", "⚠ ERROR: Usuario con estado '" + estado + "' pero sin faceData");
									Toast.makeText(this, "Error: Tu cuenta está " + estado + " pero no tienes registro facial. Contacta al administrador.", Toast.LENGTH_LONG).show();
									mAuth.signOut();
									finish();
									return;
								} else {
									// No tiene datos faciales y no está aprobado - NO PERMITIR ACCESO
									android.util.Log.d("SignInScreen", "✗ faceData es null o está vacío - RECONOCIMIENTO FACIAL OBLIGATORIO");
									Toast.makeText(this, "Error: No tienes registro facial. Debes completar tu registro facial primero.", Toast.LENGTH_LONG).show();
									mAuth.signOut();
									finish();
									return;
								}
							}
						} else {
							// Usuario no encontrado en Firestore - NO PERMITIR ACCESO
							android.util.Log.d("SignInScreen", "✗ Usuario no existe en Firestore - RECONOCIMIENTO FACIAL OBLIGATORIO");
							Toast.makeText(this, "Error: Usuario no encontrado. Debes completar tu registro primero.", Toast.LENGTH_LONG).show();
							// Cerrar sesión y volver a la pantalla de selección
							mAuth.signOut();
							finish();
							return;
						}
					} else {
						// Error al obtener documento
						android.util.Log.e("SignInScreen", "✗ Error al obtener documento de Firestore: " + task.getException());
						Toast.makeText(this, "Error al verificar datos faciales. Procediendo con login normal.", Toast.LENGTH_SHORT).show();
						checkUserRole(user);
					}
					android.util.Log.d("SignInScreen", "=== FIN VERIFICACIÓN DE DATOS FACIALES ===");
				});
	}

	private void handleRegistration(FirebaseUser user) {
		// Validar que tenemos un userType válido
		if (userType == null || userType.isEmpty()) {
			android.util.Log.e("SignInScreen", "userType es null o vacío, redirigiendo a login normal");
			checkUserRole(user);
			return;
		}

		// Validar datos del usuario
		if (user == null || user.getUid() == null) {
			Toast.makeText(this, "Error: No se pudo obtener la información del usuario", Toast.LENGTH_SHORT).show();
			android.util.Log.e("SignInScreen", "Usuario o UID es null");
			return;
		}

		String uid = user.getUid();
		String email = user.getEmail();
		String nombre = user.getDisplayName();

		android.util.Log.d("SignInScreen", "handleRegistration - userType: " + userType + ", UID: " + uid);

		// Redirigir según el tipo de usuario
		switch (userType) {
			case "alumno":
				android.util.Log.d("SignInScreen", "Redirigiendo a RegisterAlumnoActivity");
				redirectToRegisterAlumno(user);
				break;
			case "maestro":
				redirectToRegisterMaestro(user);
				break;
			case "admin":
				registerAdmin(user);
				break;
			default:
				android.util.Log.w("SignInScreen", "Tipo de usuario desconocido: " + userType + ", verificando rol");
				checkUserRole(user);
				break;
		}
	}

	private void registerAdmin(FirebaseUser user) {
		FirebaseFirestore db = FirebaseFirestore.getInstance();
		db.collection("usuarios").document(user.getUid()).set(new java.util.HashMap<String, Object>() {{
			put("id", user.getUid());
			put("nombre", user.getDisplayName());
			put("email", user.getEmail());
			put("rol", "admin");
			put("estado", "aprobado");
			put("fechaCreacion", com.google.firebase.Timestamp.now());
		}}).addOnCompleteListener(task -> {
			Intent intent = new Intent(SignInScreen.this, AdminPanelActivity.class);
			startActivity(intent);
			finish();
		});
	}

	private void redirectToRegisterMaestro(FirebaseUser user) {
		Intent intent = new Intent(SignInScreen.this, RegisterMaestroActivity.class);
		intent.putExtra("nombre", user.getDisplayName());
		intent.putExtra("email", user.getEmail());
		intent.putExtra("uid", user.getUid());
		startActivity(intent);
		finish();
	}

	private void redirectToRegisterAlumno(FirebaseUser user) {
		android.util.Log.d("SignInScreen", "=== INICIANDO REDIRECCIÓN A REGISTRO ALUMNO ===");
		
		// Validaciones estrictas
		if (user == null) {
			android.util.Log.e("SignInScreen", "ERROR: FirebaseUser es null");
			Toast.makeText(this, "Error: Usuario no válido", Toast.LENGTH_SHORT).show();
			return;
		}

		String uid = user.getUid();
		String email = user.getEmail();
		String nombre = user.getDisplayName();

		if (uid == null || uid.isEmpty()) {
			android.util.Log.e("SignInScreen", "ERROR: UID es null o vacío");
			Toast.makeText(this, "Error: No se pudo obtener el ID de usuario", Toast.LENGTH_SHORT).show();
			return;
		}

		android.util.Log.d("SignInScreen", "Datos del usuario - UID: " + uid + ", Email: " + email + ", Nombre: " + nombre);

		try {
			Intent intent = new Intent(this, RegisterAlumnoActivity.class);
			intent.putExtra("uid", uid);
			intent.putExtra("email", email != null ? email : "");
			intent.putExtra("nombre", nombre != null ? nombre : "");
			
			android.util.Log.d("SignInScreen", "Intent creado, iniciando RegisterAlumnoActivity");
			startActivity(intent);
			android.util.Log.d("SignInScreen", "RegisterAlumnoActivity iniciada, cerrando SignInScreen");
			finish();
		} catch (Exception e) {
			android.util.Log.e("SignInScreen", "EXCEPCIÓN al abrir RegisterAlumnoActivity", e);
			Toast.makeText(this, "Error al abrir formulario: " + e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}

	private void checkUserRole(FirebaseUser user) {
		db.collection("usuarios").document(user.getUid()).get()
				.addOnCompleteListener(task -> {
					if (task.isSuccessful() && task.getResult().exists()) {
						DocumentSnapshot doc = task.getResult();
						String rol = doc.getString("rol");
						String estado = doc.getString("estado");

						if (estado == null || estado.equals("aprobado")) {
							redirectToPanel(rol);
						} else if (estado.equals("pendiente")) {
							Toast.makeText(this, "Tu solicitud está pendiente de aprobación", Toast.LENGTH_LONG).show();
							finish();
						} else if (estado.equals("rechazado")) {
							Toast.makeText(this, "Tu solicitud fue rechazada", Toast.LENGTH_LONG).show();
							finish();
						}
					} else {
						Toast.makeText(this, "Usuario no encontrado", Toast.LENGTH_SHORT).show();
						finish();
					}
				});
	}

	private void redirectToPanel(String rol) {
		Intent intent;
		if ("admin".equals(rol)) {
			intent = new Intent(this, AdminPanelActivity.class);
		} else if ("maestro".equals(rol)) {
			intent = new Intent(this, MaestroPanelActivity.class);
		} else if ("alumno".equals(rol)) {
			intent = new Intent(this, AlumnoPanelActivity.class);
		} else {
			Toast.makeText(this, "Rol no reconocido", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		startActivity(intent);
		finish();
	}
}
