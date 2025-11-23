package com.mhrc.appkardex;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegisterAlumnoActivity extends AppCompatActivity {

	private static final String TAG = "RegisterAlumno";

	private FirebaseFirestore db;
	private String uid;
	private String email;
	private String nombre;
	
	// Views
	private EditText etNombre;
	private EditText tvEmail;
	private EditText etMatricula;
	private Spinner spinnerCarrera;
	private Spinner spinnerGrupo;
	private Button btnRegister;
	private TextView tvMateriasLabel;
	private TextView tvMateriasList;

	private int semestreActual = 1; // Semestre que se va a inscribir
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		try {
			android.util.Log.d(TAG, "=== onCreate INICIADO ===");
			super.onCreate(savedInstanceState);
			android.util.Log.d(TAG, "super.onCreate completado");
			
			// Inicializar helper de materias
			MateriasHelper.inicializar(this);
			
			android.util.Log.d(TAG, "Intentando cargar layout...");
			setContentView(R.layout.activity_register_alumno);
			android.util.Log.d(TAG, "Layout cargado exitosamente");
			
			// Inicializar Firestore
			db = FirebaseFirestore.getInstance();
			android.util.Log.d(TAG, "Firestore inicializado");
			
			// Obtener datos del Intent
			android.util.Log.d(TAG, "Obteniendo datos del Intent...");
			if (!obtenerDatosDelIntent()) {
				android.util.Log.e(TAG, "Error al obtener datos del Intent, cerrando activity");
				finish();
				return;
			}
			android.util.Log.d(TAG, "Datos del Intent obtenidos correctamente");
			
			// Inicializar todas las vistas PRIMERO
			android.util.Log.d(TAG, "Inicializando vistas...");
			initializeViews();
			android.util.Log.d(TAG, "Vistas inicializadas");
			
			// Verificar si el alumno ya existe y determinar semestre (después de inicializar vistas)
			verificarAlumnoExistente();
			
			// Configurar los spinners
			android.util.Log.d(TAG, "Configurando spinners...");
			setupSpinners();
			android.util.Log.d(TAG, "Spinners configurados");
			
			// Configurar la matrícula
			android.util.Log.d(TAG, "Configurando matrícula...");
			setupMatricula();
			android.util.Log.d(TAG, "Matrícula configurada");
			
			// Configurar el botón de registro
			android.util.Log.d(TAG, "Configurando botón de registro...");
			setupRegisterButton();
			android.util.Log.d(TAG, "Botón de registro configurado");
			
			android.util.Log.d(TAG, "=== onCreate COMPLETADO EXITOSAMENTE ===");
		} catch (Exception e) {
			android.util.Log.e(TAG, "ERROR CRÍTICO en onCreate", e);
			Toast.makeText(this, "Error al iniciar formulario: " + e.getMessage(), Toast.LENGTH_LONG).show();
			e.printStackTrace();
			finish();
		}
	}
	
	/**
	 * Verifica si el alumno ya existe y determina el semestre correspondiente
	 */
	private void verificarAlumnoExistente() {
		db.collection("alumnos").document(uid).get()
			.addOnCompleteListener(task -> {
				if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
					// El alumno ya existe, obtener su semestre actual
					String semestreStr = task.getResult().getString("semestre");
					String carrera = task.getResult().getString("carrera");
					
					try {
						int semestreAnterior = semestreStr != null ? Integer.parseInt(semestreStr) : 1;
						semestreActual = semestreAnterior + 1; // Siguiente semestre
						
						if (semestreActual > 4) {
							Toast.makeText(this, "Ya completaste todos los semestres disponibles", Toast.LENGTH_LONG).show();
							finish();
							return;
						}
						
						// Obtener calificaciones del semestre anterior
						@SuppressWarnings("unchecked")
						Map<String, Map<String, Long>> calificaciones = 
							(Map<String, Map<String, Long>>) task.getResult().get("calificaciones");
						
						// Validar aprobación del semestre anterior
						if (!MateriasHelper.validarAprobacionSemestreAnterior(carrera, semestreActual, calificaciones)) {
							Toast.makeText(this, 
								"No puedes inscribirte al semestre " + semestreActual + 
								". Debes aprobar todas las materias del semestre anterior con calificación >= 6",
								Toast.LENGTH_LONG).show();
							finish();
							return;
						}
						
						// Pre-llenar campos si el alumno existe
						String nombreExistente = task.getResult().getString("nombre");
						String emailExistente = task.getResult().getString("email");
						if (nombreExistente != null && !nombreExistente.isEmpty()) {
							nombre = nombreExistente;
						}
						if (emailExistente != null && !emailExistente.isEmpty()) {
							email = emailExistente;
						}
						
						// Actualizar TextView del semestre
						android.widget.TextView tvSemestre = findViewById(R.id.tvSemestre);
						if (tvSemestre != null) {
							tvSemestre.setText(String.valueOf(semestreActual));
						}
						
						// Actualizar texto de semestre
						android.widget.TextView tvPrimerParcial = findViewById(R.id.tvPrimerParcial);
						if (tvPrimerParcial != null) {
							tvPrimerParcial.setText("Eres de semestre " + semestreActual);
						}
						
						android.util.Log.d(TAG, "Alumno encontrado, semestre actual: " + semestreActual);
					} catch (NumberFormatException e) {
						android.util.Log.e(TAG, "Error al parsear semestre", e);
						semestreActual = 1;
					}
				} else {
					// Alumno nuevo, semestre 1
					semestreActual = 1;
					android.util.Log.d(TAG, "Alumno nuevo, semestre 1");
				}
			});
	}

	/**
	 * Obtiene los datos del Intent y valida que sean correctos
	 */
	private boolean obtenerDatosDelIntent() {
		android.util.Log.d(TAG, "Obteniendo datos del Intent...");
		
		uid = getIntent().getStringExtra("uid");
		email = getIntent().getStringExtra("email");
		nombre = getIntent().getStringExtra("nombre");
		
		android.util.Log.d(TAG, "UID: " + uid);
		android.util.Log.d(TAG, "Email: " + email);
		android.util.Log.d(TAG, "Nombre: " + nombre);
		
		// Validar UID (obligatorio)
		if (uid == null || uid.isEmpty()) {
			android.util.Log.e(TAG, "ERROR: UID es null o vacío");
			Toast.makeText(this, "Error: No se pudo obtener el ID de usuario", Toast.LENGTH_LONG).show();
			finish();
			return false;
		}
		
		// Email y nombre pueden ser null, se manejarán en initializeViews
		if (email == null) {
			email = "";
			android.util.Log.w(TAG, "Email es null, usando string vacío");
		}
		if (nombre == null) {
			nombre = "";
			android.util.Log.w(TAG, "Nombre es null, usando string vacío");
		}
		
		android.util.Log.d(TAG, "Datos del Intent validados correctamente");
		return true;
	}

	/**
	 * Inicializa todas las vistas y las configura con los datos recibidos
	 */
	private void initializeViews() {
		android.util.Log.d(TAG, "Inicializando vistas...");
		
		etNombre = findViewById(R.id.etNombre);
		tvEmail = findViewById(R.id.tvEmail);
		etMatricula = findViewById(R.id.etMatricula);
		spinnerCarrera = findViewById(R.id.spinnerCarrera);
		spinnerGrupo = findViewById(R.id.spinnerGrupo);
		btnRegister = findViewById(R.id.btnRegister);
		tvMateriasLabel = findViewById(R.id.tvMateriasLabel);
		tvMateriasList = findViewById(R.id.tvMateriasList);
		
		// Validar que todas las vistas se encontraron
		if (etNombre == null || tvEmail == null || etMatricula == null || 
		    spinnerCarrera == null || spinnerGrupo == null || btnRegister == null ||
		    tvMateriasLabel == null || tvMateriasList == null) {
			android.util.Log.e(TAG, "ERROR: Una o más vistas no se encontraron");
			Toast.makeText(this, "Error: Problema con el layout", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		
		// Configurar campos con datos recibidos
		etNombre.setText(nombre);
		tvEmail.setText(email);
		tvEmail.setEnabled(false);
		etMatricula.setEnabled(false);
		
		android.util.Log.d(TAG, "Vistas inicializadas correctamente");
	}

	/**
	 * Configura los spinners de carrera y grupo
	 */
	private void setupSpinners() {
		android.util.Log.d(TAG, "Configurando spinners...");
		
		// Spinner de carreras
		String[] carreras = {"Programación", "Ingeniería Civil", "Arquitectura"};
		ArrayAdapter<String> carreraAdapter = new ArrayAdapter<>(
			this, 
			android.R.layout.simple_spinner_item, 
			carreras
		);
		carreraAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerCarrera.setAdapter(carreraAdapter);
		spinnerCarrera.setSelection(0); // Seleccionar primera opción por defecto
		
		// Listener para mostrar materias cuando se seleccione carrera
		spinnerCarrera.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
				String carrera = parent.getItemAtPosition(position).toString();
				mostrarMaterias(carrera, semestreActual);
			}

			@Override
			public void onNothingSelected(android.widget.AdapterView<?> parent) {}
		});
		
		// Mostrar materias inicialmente para la primera carrera
		mostrarMaterias(carreras[0], semestreActual);
		
		// Spinner de grupos
		String[] grupos = {"1", "2", "3"};
		ArrayAdapter<String> grupoAdapter = new ArrayAdapter<>(
			this,
			android.R.layout.simple_spinner_item,
			grupos
		);
		grupoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerGrupo.setAdapter(grupoAdapter);
		spinnerGrupo.setSelection(0); // Seleccionar primera opción por defecto
		
		android.util.Log.d(TAG, "Spinners configurados correctamente");
	}

	/**
	 * Configura la matrícula obteniendo la última del sistema o usando 100 como inicial
	 */
	private void setupMatricula() {
		android.util.Log.d(TAG, "Configurando matrícula...");
		
		// Primero establecer un valor por defecto
		etMatricula.setText("100");
		
		// Intentar obtener la última matrícula de Firestore
		db.collection("alumnos")
			.get()
			.addOnCompleteListener(task -> {
				if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
					long maxMatricula = 99; // Empezar desde 99 para que la primera sea 100
					
					for (QueryDocumentSnapshot doc : task.getResult()) {
						Long matricula = doc.getLong("matricula");
						if (matricula != null && matricula > maxMatricula) {
							maxMatricula = matricula;
						}
					}
					
					long nuevaMatricula = maxMatricula + 1;
					etMatricula.setText(String.valueOf(nuevaMatricula));
					android.util.Log.d(TAG, "Matrícula obtenida de Firestore: " + nuevaMatricula);
				} else {
					// Primera matrícula o error al obtener
					etMatricula.setText("100");
					android.util.Log.d(TAG, "Usando matrícula inicial: 100");
				}
			})
			.addOnFailureListener(e -> {
				// En caso de error, usar la primera matrícula
				etMatricula.setText("100");
				android.util.Log.w(TAG, "Error al obtener matrícula, usando valor por defecto: 100", e);
			});
	}

	/**
	 * Configura el botón de registro
	 */
	private void setupRegisterButton() {
		android.util.Log.d(TAG, "Configurando botón de registro...");
		
		btnRegister.setOnClickListener(v -> {
			android.util.Log.d(TAG, "Botón de registro presionado");
			registerAlumno();
		});
	}

	/**
	 * Registra el alumno en Firestore
	 */
	private void registerAlumno() {
		android.util.Log.d(TAG, "=== INICIANDO REGISTRO DE ALUMNO ===");
		
		// Validar nombre
		String nombre = etNombre.getText().toString().trim();
		if (nombre.isEmpty()) {
			Toast.makeText(this, "El nombre es requerido", Toast.LENGTH_SHORT).show();
			android.util.Log.w(TAG, "Validación fallida: nombre vacío");
			return;
		}
		
		// Validar carrera seleccionada
		Object carreraObj = spinnerCarrera.getSelectedItem();
		if (carreraObj == null) {
			Toast.makeText(this, "Debes seleccionar una carrera", Toast.LENGTH_SHORT).show();
			android.util.Log.w(TAG, "Validación fallida: carrera no seleccionada");
			return;
		}
		String carrera = carreraObj.toString();
		
		// Validar grupo seleccionado
		Object grupoObj = spinnerGrupo.getSelectedItem();
		if (grupoObj == null) {
			Toast.makeText(this, "Debes seleccionar un grupo", Toast.LENGTH_SHORT).show();
			android.util.Log.w(TAG, "Validación fallida: grupo no seleccionado");
			return;
		}
		String grupoSolicitado = grupoObj.toString();
		
		// Validar matrícula
		String matriculaStr = etMatricula.getText().toString().trim();
		if (matriculaStr.isEmpty()) {
			Toast.makeText(this, "Error: Matrícula no válida", Toast.LENGTH_SHORT).show();
			android.util.Log.w(TAG, "Validación fallida: matrícula vacía");
			return;
		}
		
		long matricula;
		try {
			matricula = Long.parseLong(matriculaStr);
		} catch (NumberFormatException e) {
			Toast.makeText(this, "Error: Matrícula no válida", Toast.LENGTH_SHORT).show();
			android.util.Log.e(TAG, "Error al parsear matrícula: " + matriculaStr, e);
			return;
		}
		
		// Obtener email (debe ser final para usar en lambda)
		String emailDelCampo = tvEmail.getText().toString().trim();
		final String emailFinal = emailDelCampo.isEmpty() ? email : emailDelCampo;
		
		android.util.Log.d(TAG, "Datos validados - Nombre: " + nombre + ", Email: " + emailFinal + 
			", Matrícula: " + matricula + ", Carrera: " + carrera + ", Grupo: " + grupoSolicitado + 
			", Semestre: " + semestreActual);
		
		// Obtener materias según la carrera y semestre actual
		List<String> materias = MateriasHelper.getMateriasPorSemestre(carrera, semestreActual);
		android.util.Log.d(TAG, "Materias obtenidas para semestre " + semestreActual + ": " + materias.size());
		
		if (materias.isEmpty()) {
			Toast.makeText(this, "Error: No hay materias disponibles para el semestre " + semestreActual, Toast.LENGTH_LONG).show();
			return;
		}
		
		// Crear datos del alumno
		Map<String, Object> alumnoData = new HashMap<>();
		alumnoData.put("id", uid);
		alumnoData.put("nombre", nombre);
		alumnoData.put("email", emailFinal);
		alumnoData.put("matricula", matricula);
		alumnoData.put("carrera", carrera);
		alumnoData.put("semestre", String.valueOf(semestreActual));
		alumnoData.put("grupo", null); // Admin asignará el grupo
		alumnoData.put("rol", "alumno");
		alumnoData.put("estado", "pendiente");
		alumnoData.put("parcial", "1"); // Empezar con parcial 1
		alumnoData.put("materias", materias);
		alumnoData.put("grupoSolicitado", grupoSolicitado);
		
		// Inicializar calificaciones con estructura para 3 parciales
		Map<String, Map<String, Long>> calificacionesInit = new HashMap<>();
		for (String materia : materias) {
			Map<String, Long> parciales = new HashMap<>();
			parciales.put("parcial1", null);
			parciales.put("parcial2", null);
			parciales.put("parcial3", null);
			calificacionesInit.put(materia, parciales);
		}
		alumnoData.put("calificaciones", calificacionesInit);
		
		android.util.Log.d(TAG, "Guardando alumno temporalmente en Firestore (pendiente registro facial)...");
		
		// Deshabilitar botón para evitar doble registro
		btnRegister.setEnabled(false);
		btnRegister.setText("Registrando...");
		
		// Guardar temporalmente con flag de registro facial pendiente
		alumnoData.put("faceDataPending", true); // Marca que falta el registro facial
		
		// Guardar en colección "alumnos"
		db.collection("alumnos").document(uid).set(alumnoData)
			.addOnCompleteListener(task -> {
				if (task.isSuccessful()) {
					android.util.Log.d(TAG, "Alumno guardado temporalmente en 'alumnos'");
					
					// También guardar en colección "usuarios" con flag pendiente
					Map<String, Object> usuarioData = new HashMap<>();
					usuarioData.put("id", uid);
					usuarioData.put("nombre", nombre);
					usuarioData.put("email", emailFinal);
					usuarioData.put("rol", "alumno");
					usuarioData.put("estado", "registro_incompleto"); // Estado especial para registro incompleto
					usuarioData.put("faceDataPending", true);
					
					db.collection("usuarios").document(uid).set(usuarioData)
						.addOnCompleteListener(task2 -> {
							if (task2.isSuccessful()) {
								android.util.Log.d(TAG, "Usuario guardado temporalmente en 'usuarios'");
								Toast.makeText(RegisterAlumnoActivity.this, 
									"Registro facial obligatorio para completar el registro", Toast.LENGTH_LONG).show();
								
								// Redirigir a registro facial (OBLIGATORIO)
								android.content.Intent intent = new android.content.Intent(RegisterAlumnoActivity.this, FaceRegistrationActivity.class);
								intent.putExtra("userId", uid);
								intent.putExtra("userType", "alumno");
								intent.putExtra("fromRegistration", true); // Marca que viene del registro
								startActivity(intent);
								finish();
							} else {
								android.util.Log.e(TAG, "Error al guardar en 'usuarios'", task2.getException());
								btnRegister.setEnabled(true);
								btnRegister.setText(getString(R.string.register_button));
								Toast.makeText(RegisterAlumnoActivity.this, 
									"Error al guardar información de usuario", Toast.LENGTH_SHORT).show();
							}
						})
						.addOnFailureListener(e -> {
							android.util.Log.e(TAG, "Excepción al guardar en 'usuarios'", e);
							btnRegister.setEnabled(true);
							btnRegister.setText(getString(R.string.register_button));
							Toast.makeText(RegisterAlumnoActivity.this, 
								"Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
						});
				} else {
					android.util.Log.e(TAG, "Error al guardar en 'alumnos'", task.getException());
					btnRegister.setEnabled(true);
					btnRegister.setText(getString(R.string.register_button));
					Toast.makeText(RegisterAlumnoActivity.this, 
						"Error al registrar: " + (task.getException() != null ? task.getException().getMessage() : "Error desconocido"), 
						Toast.LENGTH_LONG).show();
				}
			})
			.addOnFailureListener(e -> {
				android.util.Log.e(TAG, "Excepción al guardar en 'alumnos'", e);
				btnRegister.setEnabled(true);
				btnRegister.setText(getString(R.string.register_button));
				Toast.makeText(RegisterAlumnoActivity.this, 
					"Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
			});
	}

	/**
	 * Muestra las materias de la carrera seleccionada para el semestre actual
	 */
	private void mostrarMaterias(String carrera, int semestre) {
		List<String> materias = MateriasHelper.getMateriasPorSemestre(carrera, semestre);
		
		if (!materias.isEmpty()) {
			StringBuilder materiasTexto = new StringBuilder();
			for (int i = 0; i < materias.size(); i++) {
				materiasTexto.append("• ").append(materias.get(i));
				if (i < materias.size() - 1) {
					materiasTexto.append("\n");
				}
			}
			
			tvMateriasLabel.setVisibility(android.view.View.VISIBLE);
			tvMateriasList.setText(materiasTexto.toString());
			tvMateriasList.setVisibility(android.view.View.VISIBLE);
		} else {
			tvMateriasLabel.setVisibility(android.view.View.GONE);
			tvMateriasList.setVisibility(android.view.View.GONE);
		}
	}

}
