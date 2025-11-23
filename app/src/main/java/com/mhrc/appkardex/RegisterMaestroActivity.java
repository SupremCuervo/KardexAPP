package com.mhrc.appkardex;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegisterMaestroActivity extends AppCompatActivity {

	private static final String TAG = "RegisterMaestro";

	private FirebaseFirestore db;
	private String uid;
	private EditText etNombre;
	private EditText tvEmail;
	private Spinner spinnerCarrera;
	private LinearLayout llMateriasContainer;
	private TextView tvMateriasTitle;
	private TextView tvMateriasHint;
	private List<CheckBox> checkBoxesMaterias;
	private String areaAcademica;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_register_maestro);

		db = FirebaseFirestore.getInstance();
		uid = getIntent().getStringExtra("uid");
		
		if (uid == null) {
			Toast.makeText(this, "Error: UID no encontrado", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

		checkBoxesMaterias = new ArrayList<>();
		
		initializeViews();
		setupCarreraSpinner();
		setupRegisterButton();
	}

	private void initializeViews() {
		etNombre = findViewById(R.id.etNombre);
		tvEmail = findViewById(R.id.tvEmail);
		spinnerCarrera = findViewById(R.id.spinnerCarrera);
		llMateriasContainer = findViewById(R.id.llMateriasContainer);
		tvMateriasTitle = findViewById(R.id.tvMateriasTitle);
		tvMateriasHint = findViewById(R.id.tvMateriasHint);

		String nombre = getIntent().getStringExtra("nombre");
		String email = getIntent().getStringExtra("email");

		if (nombre != null) {
			etNombre.setText(nombre);
		}
		if (email != null) {
			tvEmail.setText(email);
		}
		tvEmail.setEnabled(false);
	}

	private void setupCarreraSpinner() {
		String[] carreras = {"Programación", "Ingeniería Civil", "Arquitectura"};
		ArrayAdapter<String> adapter = new ArrayAdapter<>(
			this,
			android.R.layout.simple_spinner_item,
			carreras
		);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerCarrera.setAdapter(adapter);
		spinnerCarrera.setSelection(0); // Seleccionar primera opción por defecto
		
		// Listener para cuando se seleccione una carrera
		spinnerCarrera.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
				areaAcademica = parent.getItemAtPosition(position).toString();
				actualizarMaterias();
			}

			@Override
			public void onNothingSelected(android.widget.AdapterView<?> parent) {
				// No hacer nada
			}
		});
		
		// Inicializar con la primera carrera
		areaAcademica = carreras[0];
		actualizarMaterias();
	}

	private void actualizarMaterias() {
		android.util.Log.d(TAG, "Actualizando materias para área: " + areaAcademica);
		
		// Limpiar checkboxes anteriores
		llMateriasContainer.removeAllViews();
		checkBoxesMaterias.clear();
		
		// Obtener materias según el área académica
		List<String> materias = getMateriasByArea(areaAcademica);
		
		// Crear checkboxes para cada materia
		for (String materia : materias) {
			CheckBox checkBox = new CheckBox(this);
			checkBox.setText(materia);
			checkBox.setTextSize(16);
			checkBox.setPadding(0, 16, 0, 16);
			checkBox.setTextColor(getResources().getColor(R.color.text_primary));
			
			llMateriasContainer.addView(checkBox);
			checkBoxesMaterias.add(checkBox);
		}
		
		// Mostrar el contenedor de materias
		tvMateriasTitle.setVisibility(TextView.VISIBLE);
		tvMateriasHint.setVisibility(TextView.VISIBLE);
		llMateriasContainer.setVisibility(LinearLayout.VISIBLE);
		
		android.util.Log.d(TAG, "Se crearon " + checkBoxesMaterias.size() + " checkboxes de materias");
	}

	private List<String> getMateriasByArea(String area) {
		List<String> materias = new ArrayList<>();
		if ("Programación".equals(area)) {
			materias.add(getString(R.string.materia_programacion));
			materias.add(getString(R.string.materia_bases_datos));
			materias.add(getString(R.string.materia_estructuras));
			materias.add(getString(R.string.materia_disenno));
		} else if ("Ingeniería Civil".equals(area)) {
			materias.add(getString(R.string.materia_matematicas));
			materias.add(getString(R.string.materia_estadistica));
			materias.add(getString(R.string.materia_resistencia));
			materias.add(getString(R.string.materia_topografia));
		} else if ("Arquitectura".equals(area)) {
			materias.add(getString(R.string.materia_dibujo));
			materias.add(getString(R.string.materia_historia));
			materias.add(getString(R.string.materia_construccion));
			materias.add(getString(R.string.materia_urbanismo));
		}
		return materias;
	}

	private void setupRegisterButton() {
		Button btnRegister = findViewById(R.id.btnRegister);
		btnRegister.setOnClickListener(v -> registerMaestro());
	}

	private void registerMaestro() {
		String nombre = etNombre.getText().toString().trim();

		if (nombre.isEmpty()) {
			Toast.makeText(this, "El nombre es requerido", Toast.LENGTH_SHORT).show();
			return;
		}

		// Validar que se haya seleccionado una carrera
		if (areaAcademica == null || areaAcademica.isEmpty()) {
			Toast.makeText(this, "Debes seleccionar un área académica", Toast.LENGTH_SHORT).show();
			return;
		}

		// Obtener materias seleccionadas
		List<String> materiasSeleccionadas = new ArrayList<>();
		for (CheckBox checkBox : checkBoxesMaterias) {
			if (checkBox.isChecked()) {
				materiasSeleccionadas.add(checkBox.getText().toString());
			}
		}

		// Validar que se haya seleccionado al menos una materia
		if (materiasSeleccionadas.isEmpty()) {
			Toast.makeText(this, "Debes seleccionar al menos una materia que puedes impartir", Toast.LENGTH_LONG).show();
			return;
		}

		android.util.Log.d(TAG, "Registrando maestro - Nombre: " + nombre + ", Área: " + areaAcademica + 
			", Materias: " + materiasSeleccionadas.size());

		Map<String, Object> maestroData = new HashMap<>();
		maestroData.put("id", uid);
		maestroData.put("nombre", nombre);
		maestroData.put("email", tvEmail.getText().toString());
		maestroData.put("areaAcademica", areaAcademica);
		maestroData.put("rol", "maestro");
		maestroData.put("estado", "pendiente");
		maestroData.put("materias", materiasSeleccionadas);

		// Deshabilitar botón para evitar doble registro
		Button btnRegister = findViewById(R.id.btnRegister);
		btnRegister.setEnabled(false);
		btnRegister.setText("Registrando...");

		// Guardar temporalmente con flag de registro facial pendiente
		maestroData.put("faceDataPending", true); // Marca que falta el registro facial
		
		// Guardar en Firestore
		db.collection("maestros").document(uid).set(maestroData)
			.addOnCompleteListener(task -> {
				if (task.isSuccessful()) {
					// También guardar en usuarios con flag pendiente
					Map<String, Object> usuarioData = new HashMap<>();
					usuarioData.put("id", uid);
					usuarioData.put("nombre", nombre);
					usuarioData.put("email", tvEmail.getText().toString());
					usuarioData.put("rol", "maestro");
					usuarioData.put("estado", "registro_incompleto"); // Estado especial para registro incompleto
					usuarioData.put("faceDataPending", true);

					db.collection("usuarios").document(uid).set(usuarioData)
						.addOnCompleteListener(task2 -> {
							if (task2.isSuccessful()) {
								Toast.makeText(RegisterMaestroActivity.this, 
									"Registro facial obligatorio para completar el registro", Toast.LENGTH_LONG).show();
								
								// Redirigir a registro facial (OBLIGATORIO)
								android.content.Intent intent = new android.content.Intent(RegisterMaestroActivity.this, FaceRegistrationActivity.class);
								intent.putExtra("userId", uid);
								intent.putExtra("userType", "maestro");
								intent.putExtra("fromRegistration", true); // Marca que viene del registro
								startActivity(intent);
								finish();
							} else {
								btnRegister.setEnabled(true);
								btnRegister.setText(getString(R.string.register_button));
								Toast.makeText(RegisterMaestroActivity.this, 
									"Error al guardar información de usuario", Toast.LENGTH_SHORT).show();
							}
						})
						.addOnFailureListener(e -> {
							btnRegister.setEnabled(true);
							btnRegister.setText(getString(R.string.register_button));
							Toast.makeText(RegisterMaestroActivity.this, 
								"Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
						});
				} else {
					btnRegister.setEnabled(true);
					btnRegister.setText(getString(R.string.register_button));
					Toast.makeText(RegisterMaestroActivity.this, 
						"Error al registrar: " + (task.getException() != null ? task.getException().getMessage() : "Error desconocido"), 
						Toast.LENGTH_LONG).show();
				}
			})
			.addOnFailureListener(e -> {
				btnRegister.setEnabled(true);
				btnRegister.setText(getString(R.string.register_button));
				Toast.makeText(RegisterMaestroActivity.this, 
					"Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
			});
	}
}
