package com.mhrc.appkardex;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MaestroPanelActivity extends AppCompatActivity {

	private static final String TAG = "MaestroPanel";

	private FirebaseFirestore db;
	private FirebaseUser user;
	private String carreraAsignada;
	private List<String> gruposAsignados;
	private List<String> materiasAsignadas;
	private String materiaSeleccionada;
	private Spinner spinnerMaterias;
	private Spinner spinnerParcial;
	private String parcialSeleccionado = "parcial1"; // Por defecto parcial 1
	private String carreraSeleccionada;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_maestro_panel);

		db = FirebaseFirestore.getInstance();
		user = FirebaseAuth.getInstance().getCurrentUser();

		// Validar que el usuario esté autenticado
		if (user == null) {
			Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

		Button btnLogout = findViewById(R.id.btnLogout);
		if (btnLogout != null) {
			btnLogout.setOnClickListener(v -> logout());
		}

		loadMaestroData();
	}

	private void loadMaestroData() {
		if (user == null || user.getUid() == null) {
			Toast.makeText(this, "Error: Usuario no válido", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

		db.collection("maestros").document(user.getUid()).get()
			.addOnCompleteListener(task -> {
				if (task.isSuccessful()) {
					DocumentSnapshot doc = task.getResult();
					if (doc != null && doc.exists()) {
						try {
							android.util.Log.d(TAG, "=== INICIANDO PROCESAMIENTO DE DATOS DEL MAESTRO ===");
							
							// Verificar si tiene asignaciones nuevas (múltiples grupos/materias)
							List<String> gruposAsign = null;
							List<String> materiasAsign = null;
							
							// Obtener grupos asignados con manejo seguro de tipos
							android.util.Log.d(TAG, "Obteniendo gruposAsignados...");
							Object gruposObj = doc.get("gruposAsignados");
							android.util.Log.d(TAG, "gruposObj tipo: " + (gruposObj != null ? gruposObj.getClass().getName() : "null"));
							
							if (gruposObj != null) {
								if (gruposObj instanceof List) {
									try {
										@SuppressWarnings("unchecked")
										List<Object> gruposList = (List<Object>) gruposObj;
										gruposAsign = new ArrayList<>();
										for (Object item : gruposList) {
											if (item != null) {
												android.util.Log.d(TAG, "Grupo item tipo: " + item.getClass().getName() + ", valor: " + item.toString());
												gruposAsign.add(item.toString());
											}
										}
										android.util.Log.d(TAG, "gruposAsign convertido: " + gruposAsign);
									} catch (Exception e) {
										android.util.Log.e(TAG, "Error al convertir gruposAsignados", e);
										e.printStackTrace();
									}
								} else {
									android.util.Log.w(TAG, "gruposAsignados no es una List, es: " + gruposObj.getClass().getName());
								}
							} else {
								android.util.Log.d(TAG, "gruposAsignados es null");
							}
							
							// Obtener materias asignadas con manejo seguro de tipos
							android.util.Log.d(TAG, "Obteniendo materiasAsignadas...");
							Object materiasObj = doc.get("materiasAsignadas");
							android.util.Log.d(TAG, "materiasObj tipo: " + (materiasObj != null ? materiasObj.getClass().getName() : "null"));
							
							if (materiasObj != null) {
								if (materiasObj instanceof List) {
									try {
										@SuppressWarnings("unchecked")
										List<Object> materiasList = (List<Object>) materiasObj;
										materiasAsign = new ArrayList<>();
										for (Object item : materiasList) {
											if (item != null) {
												android.util.Log.d(TAG, "Materia item tipo: " + item.getClass().getName() + ", valor: " + item.toString());
												materiasAsign.add(item.toString());
											}
										}
										android.util.Log.d(TAG, "materiasAsign convertido: " + materiasAsign);
									} catch (Exception e) {
										android.util.Log.e(TAG, "Error al convertir materiasAsignadas", e);
										e.printStackTrace();
									}
								} else {
									android.util.Log.w(TAG, "materiasAsignadas no es una List, es: " + materiasObj.getClass().getName());
								}
							} else {
								android.util.Log.d(TAG, "materiasAsignadas es null");
							}
							
							String carreraAsign = null;
							android.util.Log.d(TAG, "Obteniendo carreraAsignada...");
							Object carreraObj = doc.get("carreraAsignada");
							android.util.Log.d(TAG, "carreraObj tipo: " + (carreraObj != null ? carreraObj.getClass().getName() : "null"));
							if (carreraObj != null) {
								carreraAsign = carreraObj.toString();
								android.util.Log.d(TAG, "carreraAsign: " + carreraAsign);
							}

							if (gruposAsign != null && !gruposAsign.isEmpty() && 
								materiasAsign != null && !materiasAsign.isEmpty() && 
								carreraAsign != null && !carreraAsign.isEmpty()) {
								// Usar asignaciones nuevas
								gruposAsignados = gruposAsign;
								materiasAsignadas = materiasAsign;
								carreraAsignada = carreraAsign;
							} else {
								android.util.Log.d(TAG, "No se encontraron asignaciones nuevas, buscando asignaciones antiguas...");
								// Compatibilidad con asignaciones antiguas (un solo grupo/materia)
								// Manejo seguro de tipos para campos antiguos
								String grupoAsignado = null;
								Object grupoObj = doc.get("grupoAsignado");
								android.util.Log.d(TAG, "grupoAsignado tipo: " + (grupoObj != null ? grupoObj.getClass().getName() : "null"));
								if (grupoObj != null) {
									grupoAsignado = grupoObj.toString();
									android.util.Log.d(TAG, "grupoAsignado: " + grupoAsignado);
								}
								
								String materiaAsignada = null;
								Object materiaObj = doc.get("materiaAsignada");
								android.util.Log.d(TAG, "materiaAsignada tipo: " + (materiaObj != null ? materiaObj.getClass().getName() : "null"));
								if (materiaObj != null) {
									materiaAsignada = materiaObj.toString();
									android.util.Log.d(TAG, "materiaAsignada: " + materiaAsignada);
								}
								
								String areaAcademica = null;
								Object areaObj = doc.get("areaAcademica");
								android.util.Log.d(TAG, "areaAcademica tipo: " + (areaObj != null ? areaObj.getClass().getName() : "null"));
								if (areaObj != null) {
									areaAcademica = areaObj.toString();
									android.util.Log.d(TAG, "areaAcademica: " + areaAcademica);
								}

								if (grupoAsignado != null && !grupoAsignado.isEmpty() && 
									materiaAsignada != null && !materiaAsignada.isEmpty()) {
									gruposAsignados = new ArrayList<>();
									gruposAsignados.add(grupoAsignado);
									materiasAsignadas = new ArrayList<>();
									materiasAsignadas.add(materiaAsignada);
									carreraAsignada = areaAcademica != null && !areaAcademica.isEmpty() ? areaAcademica : carreraAsign;
								} else {
									// Si no tiene asignaciones, usar área académica
									carreraAsignada = areaAcademica;
									gruposAsignados = new ArrayList<>();
									materiasAsignadas = new ArrayList<>();
								}
							}

							android.util.Log.d(TAG, "Datos procesados correctamente. Iniciando setupUI...");
							
							// Actualizar nombre del maestro desde Firestore
							actualizarNombreMaestro(doc);
							
							setupUI();
							android.util.Log.d(TAG, "=== PROCESAMIENTO COMPLETADO ===");
						} catch (Exception e) {
							android.util.Log.e(TAG, "EXCEPCIÓN al procesar datos del maestro", e);
							android.util.Log.e(TAG, "Mensaje: " + e.getMessage());
							android.util.Log.e(TAG, "Causa: " + (e.getCause() != null ? e.getCause().getMessage() : "null"));
							e.printStackTrace();
							Toast.makeText(this, "Error al procesar datos: " + e.getMessage(), Toast.LENGTH_LONG).show();
						}
					} else {
						Toast.makeText(this, "No se encontró información del maestro", Toast.LENGTH_SHORT).show();
						android.util.Log.w(TAG, "Documento del maestro no existe");
					}
				} else {
					Exception exception = task.getException();
					String errorMsg = "Error al cargar datos del maestro";
					if (exception != null) {
						errorMsg += ": " + exception.getMessage();
						android.util.Log.e(TAG, "Error al cargar maestro", exception);
					}
					Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
				}
			})
			.addOnFailureListener(e -> {
				android.util.Log.e(TAG, "Fallo al cargar maestro", e);
				Toast.makeText(this, "Error de conexión: " + e.getMessage(), Toast.LENGTH_LONG).show();
			});
	}

	private void actualizarNombreMaestro(DocumentSnapshot doc) {
		TextView tvNombre = findViewById(R.id.tvNombre);
		if (tvNombre != null) {
			// Obtener nombre desde Firestore, no de Google
			String nombreMaestro = doc.getString("nombre");
			if (nombreMaestro == null || nombreMaestro.isEmpty()) {
				// Fallback a nombre de Google si no existe en Firestore
				nombreMaestro = user.getDisplayName();
				if (nombreMaestro == null || nombreMaestro.isEmpty()) {
					nombreMaestro = user.getEmail();
				}
				if (nombreMaestro == null || nombreMaestro.isEmpty()) {
					nombreMaestro = "Usuario";
				}
			}
			tvNombre.setText(nombreMaestro);
		}
	}

	private void setupUI() {
		// Mostrar carrera asignada (sin selector)
		TextView tvCarreraAsignada = findViewById(R.id.tvCarreraAsignada);
		if (carreraAsignada != null && !carreraAsignada.isEmpty()) {
			carreraSeleccionada = carreraAsignada;
			if (tvCarreraAsignada != null) {
				tvCarreraAsignada.setText("Carrera: " + carreraAsignada);
			}
		} else {
			carreraSeleccionada = null;
			if (tvCarreraAsignada != null) {
				tvCarreraAsignada.setText("Carrera: Sin asignar");
			}
		}
		
		// Configurar spinner de parcial
		spinnerParcial = findViewById(R.id.spinnerParcial);
		String[] parciales = {"Parcial 1", "Parcial 2", "Parcial 3"};
		ArrayAdapter<String> parcialAdapter = new ArrayAdapter<>(this,
			android.R.layout.simple_spinner_item, parciales);
		parcialAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerParcial.setAdapter(parcialAdapter);
		spinnerParcial.setSelection(0); // Parcial 1 por defecto
		
		spinnerParcial.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				parcialSeleccionado = "parcial" + (position + 1);
				loadGruposAsignados();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {}
		});
		
		TextView tvMateriaLabel = findViewById(R.id.tvMateriaLabel);
		spinnerMaterias = findViewById(R.id.spinnerMaterias);

		// Si hay múltiples materias, mostrar spinner de selección
		if (materiasAsignadas != null && materiasAsignadas.size() > 1) {
			if (tvMateriaLabel != null && spinnerMaterias != null) {
				tvMateriaLabel.setVisibility(TextView.VISIBLE);
				spinnerMaterias.setVisibility(Spinner.VISIBLE);

				ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
					android.R.layout.simple_spinner_item, materiasAsignadas);
				adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				spinnerMaterias.setAdapter(adapter);

				spinnerMaterias.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
						materiaSeleccionada = materiasAsignadas.get(position);
						loadGruposAsignados();
					}

					@Override
					public void onNothingSelected(AdapterView<?> parent) {}
				});

				// Seleccionar primera materia por defecto
				spinnerMaterias.setSelection(0);
				materiaSeleccionada = materiasAsignadas.get(0);
			}
		} else if (materiasAsignadas != null && !materiasAsignadas.isEmpty()) {
			// Solo una materia - ocultar spinner
			if (tvMateriaLabel != null) {
				tvMateriaLabel.setVisibility(TextView.GONE);
			}
			if (spinnerMaterias != null) {
				spinnerMaterias.setVisibility(Spinner.GONE);
			}
			materiaSeleccionada = materiasAsignadas.get(0);
		} else {
			// No hay materias asignadas
			if (tvMateriaLabel != null) {
				tvMateriaLabel.setVisibility(TextView.GONE);
			}
			if (spinnerMaterias != null) {
				spinnerMaterias.setVisibility(Spinner.GONE);
			}
		}

		loadGruposAsignados();
	}

	private void loadGruposAsignados() {
		ViewGroup container = findViewById(R.id.containerGrupos);
		if (container == null) {
			android.util.Log.e(TAG, "containerGrupos no encontrado en el layout");
			return;
		}
		
		try {
			container.removeAllViews();

			if (gruposAsignados == null || gruposAsignados.isEmpty()) {
				TextView tvEmpty = new TextView(this);
				tvEmpty.setText("No tienes grupos asignados aún");
				tvEmpty.setTextSize(16);
				tvEmpty.setTextColor(getResources().getColor(R.color.text_secondary));
				tvEmpty.setPadding(16, 16, 16, 16);
				container.addView(tvEmpty);
				return;
			}
		} catch (Exception e) {
			android.util.Log.e(TAG, "Error en loadGruposAsignados", e);
			return;
		}

		// Mostrar solo los grupos asignados al maestro
		for (String grupo : gruposAsignados) {
			View itemView = LayoutInflater.from(this)
				.inflate(R.layout.item_grupo, container, false);

			TextView tvGrupo = itemView.findViewById(R.id.tvGrupo);
			if (tvGrupo != null) {
				String grupoText = getString(R.string.grupo, grupo);
				if (materiaSeleccionada != null) {
					grupoText += " - " + materiaSeleccionada;
				}
				tvGrupo.setText(grupoText);
			}

			itemView.setOnClickListener(v -> mostrarAlumnosGrupo(grupo, container));
			container.addView(itemView);
		}
	}

	private void mostrarAlumnosGrupo(String grupo, ViewGroup container) {
		String carreraAFiltrar = carreraSeleccionada != null ? carreraSeleccionada : carreraAsignada;
		if (carreraAFiltrar == null || grupo == null) {
			Toast.makeText(this, "Error: Falta información de carrera o grupo", Toast.LENGTH_SHORT).show();
			return;
		}

		container.removeAllViews();

		// Crear botón de volver
		TextView tvBack = new TextView(this);
		tvBack.setText("← Volver");
		tvBack.setTextSize(16);
		tvBack.setTextColor(getResources().getColor(R.color.primary));
		tvBack.setPadding(16, 16, 16, 16);
		tvBack.setOnClickListener(v -> loadGruposAsignados());
		container.addView(tvBack);

		// Buscar alumnos del grupo, carrera seleccionada y que tengan la materia asignada
		db.collection("alumnos")
			.whereEqualTo("grupo", grupo)
			.whereEqualTo("carrera", carreraAFiltrar)
			.whereEqualTo("estado", "aprobado")
			.get()
			.addOnCompleteListener(task -> {
				if (task.isSuccessful()) {
					int alumnosEncontrados = 0;
					for (QueryDocumentSnapshot doc : task.getResult()) {
						@SuppressWarnings("unchecked")
						List<String> materiasAlumno = (List<String>) doc.get("materias");

						// Verificar que el alumno tenga la materia asignada al maestro
						if (materiasAlumno != null && materiaSeleccionada != null && materiasAlumno.contains(materiaSeleccionada)) {
							View itemView = LayoutInflater.from(this)
								.inflate(R.layout.item_alumno_lista, container, false);

							TextView tvNombre = itemView.findViewById(R.id.tvNombre);
							TextView tvMatricula = itemView.findViewById(R.id.tvMatricula);

							String nombre = doc.getString("nombre");
							Long matricula = doc.getLong("matricula");

							// Obtener calificaciones del alumno para mostrar (con compatibilidad hacia atrás)
							@SuppressWarnings("unchecked")
							Object califObj = doc.get("calificaciones");
							Map<String, Map<String, Long>> calificacionesAlumno = new HashMap<>();
							Long calificacionExistente = null;
							
							if (califObj != null && califObj instanceof Map) {
								@SuppressWarnings("unchecked")
								Map<String, Object> califMap = (Map<String, Object>) califObj;
								
								for (Map.Entry<String, Object> entry : califMap.entrySet()) {
									Object value = entry.getValue();
									if (value instanceof Map) {
										// Estructura nueva
										@SuppressWarnings("unchecked")
										Map<String, Long> parciales = (Map<String, Long>) value;
										calificacionesAlumno.put(entry.getKey(), parciales);
									} else if (value instanceof Number) {
										// Estructura antigua - convertir
										Map<String, Long> parciales = new HashMap<>();
										parciales.put("parcial1", ((Number) value).longValue());
										parciales.put("parcial2", null);
										parciales.put("parcial3", null);
										calificacionesAlumno.put(entry.getKey(), parciales);
									}
								}
							}
							
							if (calificacionesAlumno != null && materiaSeleccionada != null) {
								Map<String, Long> parciales = calificacionesAlumno.get(materiaSeleccionada);
								if (parciales != null) {
									calificacionExistente = parciales.get(parcialSeleccionado);
								}
							}

							if (tvNombre != null) {
								String nombreTexto = "Nombre: " + nombre;
								if (calificacionExistente != null) {
									String parcialNombre = parcialSeleccionado.equals("parcial1") ? "P1" : 
										parcialSeleccionado.equals("parcial2") ? "P2" : "P3";
									nombreTexto += " - " + parcialNombre + ": " + calificacionExistente;
								} else {
									String parcialNombre = parcialSeleccionado.equals("parcial1") ? "P1" : 
										parcialSeleccionado.equals("parcial2") ? "P2" : "P3";
									nombreTexto += " - " + parcialNombre + ": Sin calificación";
								}
								tvNombre.setText(nombreTexto);
							}
							if (tvMatricula != null) {
								tvMatricula.setText("Matrícula: " + (matricula != null ? matricula : "N/A"));
							}

							String uid = doc.getId();
							// Pasar el grupo como referencia para poder recargar la lista después
							final String grupoFinal = grupo;
							itemView.setOnClickListener(v -> mostrarDialogCalificaciones(uid, doc, grupoFinal));
							container.addView(itemView);
							alumnosEncontrados++;
						}
					}

					if (alumnosEncontrados == 0) {
						TextView tvEmpty = new TextView(this);
						tvEmpty.setText("No hay alumnos en este grupo con la materia seleccionada");
						tvEmpty.setTextSize(16);
						tvEmpty.setTextColor(getResources().getColor(R.color.text_secondary));
						tvEmpty.setPadding(16, 16, 16, 16);
						container.addView(tvEmpty);
					}
				} else {
					Toast.makeText(this, "Error al cargar alumnos", Toast.LENGTH_SHORT).show();
					android.util.Log.e(TAG, "Error al cargar alumnos", task.getException());
				}
			});
	}

	private void mostrarDialogCalificaciones(String uid, DocumentSnapshot doc, String grupo) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		
		// Obtener calificación existente para el parcial seleccionado (con compatibilidad hacia atrás)
		@SuppressWarnings("unchecked")
		Object califObj = doc.get("calificaciones");
		Map<String, Map<String, Long>> calificaciones = new HashMap<>();
		Long calificacionExistente = null;
		
		if (califObj != null && califObj instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<String, Object> califMap = (Map<String, Object>) califObj;
			
			for (Map.Entry<String, Object> entry : califMap.entrySet()) {
				Object value = entry.getValue();
				if (value instanceof Map) {
					// Estructura nueva
					@SuppressWarnings("unchecked")
					Map<String, Long> parciales = (Map<String, Long>) value;
					calificaciones.put(entry.getKey(), parciales);
				} else if (value instanceof Number) {
					// Estructura antigua - convertir
					Map<String, Long> parciales = new HashMap<>();
					parciales.put("parcial1", ((Number) value).longValue());
					parciales.put("parcial2", null);
					parciales.put("parcial3", null);
					calificaciones.put(entry.getKey(), parciales);
				}
			}
		}
		
		if (calificaciones != null && materiaSeleccionada != null) {
			Map<String, Long> parciales = calificaciones.get(materiaSeleccionada);
			if (parciales != null) {
				calificacionExistente = parciales.get(parcialSeleccionado);
			}
		}
		
		String parcialNombre = parcialSeleccionado.equals("parcial1") ? "Parcial 1" : 
			parcialSeleccionado.equals("parcial2") ? "Parcial 2" : "Parcial 3";
		
		String titulo = calificacionExistente != null ? 
			"Modificar " + parcialNombre + " (Actual: " + calificacionExistente + ")" : 
			"Poner Calificación - " + parcialNombre;
		builder.setTitle(titulo);

		View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_calificaciones, null);
		ViewGroup containerMaterias = dialogView.findViewById(R.id.containerMaterias);

		if (containerMaterias == null) {
			Toast.makeText(this, "Error al cargar el diálogo", Toast.LENGTH_SHORT).show();
			return;
		}

		// Solo mostrar la materia asignada al maestro
		if (materiaSeleccionada != null) {
			View itemView = LayoutInflater.from(this)
				.inflate(R.layout.item_materia_input, containerMaterias, false);

			TextView tvMateria = itemView.findViewById(R.id.tvMateria);
			EditText etCalificacion = itemView.findViewById(R.id.etCalificacion);

			if (tvMateria != null) {
				String materiaTexto = materiaSeleccionada + "\n" + parcialNombre;
				if (calificacionExistente != null) {
					materiaTexto += " - Calificación actual: " + calificacionExistente;
				}
				tvMateria.setText(materiaTexto);
			}

			// Poblar campo con calificación existente
			if (etCalificacion != null && calificacionExistente != null) {
				etCalificacion.setText(String.valueOf(calificacionExistente));
				// Seleccionar todo el texto para facilitar la edición
				etCalificacion.selectAll();
			} else if (etCalificacion != null) {
				etCalificacion.setHint("Ingresa calificación (0-100)");
			}

			containerMaterias.addView(itemView);

			builder.setView(dialogView);
			builder.setPositiveButton("Guardar", (dialog, which) -> {
				if (etCalificacion != null) {
					String text = etCalificacion.getText().toString().trim();
					if (!text.isEmpty()) {
						try {
							long calificacion = Long.parseLong(text);
							if (calificacion >= 0 && calificacion <= 100) {
								// Crear o actualizar estructura de calificaciones con parciales
								Map<String, Map<String, Long>> calificacionesUpdate = new HashMap<>();
								if (calificaciones != null) {
									calificacionesUpdate.putAll(calificaciones);
								}
								
								// Obtener o crear el mapa de parciales para esta materia
								Map<String, Long> parciales = calificacionesUpdate.get(materiaSeleccionada);
								if (parciales == null) {
									parciales = new HashMap<>();
								}
								
								// Actualizar el parcial seleccionado
								parciales.put(parcialSeleccionado, calificacion);
								calificacionesUpdate.put(materiaSeleccionada, parciales);
								
								guardarCalificaciones(uid, calificacionesUpdate, grupo);
							} else {
								Toast.makeText(this, "Las calificaciones deben estar entre 0 y 100", Toast.LENGTH_SHORT).show();
							}
						} catch (NumberFormatException e) {
							Toast.makeText(this, "Calificación inválida", Toast.LENGTH_SHORT).show();
						}
					} else {
						Toast.makeText(this, "Debes ingresar una calificación", Toast.LENGTH_SHORT).show();
					}
				}
			});
			builder.setNegativeButton("Cancelar", null);
			builder.show();
		} else {
			Toast.makeText(this, "No hay materia seleccionada", Toast.LENGTH_SHORT).show();
		}
	}

	private void guardarCalificaciones(String uid, Map<String, Map<String, Long>> calificaciones, String grupo) {
		db.collection("alumnos").document(uid).update("calificaciones", calificaciones)
			.addOnCompleteListener(task -> {
				if (task.isSuccessful()) {
					Toast.makeText(this, "Calificación guardada", Toast.LENGTH_SHORT).show();
					// Recargar la lista de alumnos para mostrar la calificación actualizada
					ViewGroup container = findViewById(R.id.containerGrupos);
					if (container != null && grupo != null) {
						mostrarAlumnosGrupo(grupo, container);
					}
				} else {
					Toast.makeText(this, "Error al guardar calificación", Toast.LENGTH_SHORT).show();
					android.util.Log.e(TAG, "Error al guardar calificación", task.getException());
				}
			});
	}

	private void logout() {
		FirebaseAuth.getInstance().signOut();
		
		GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
			.requestIdToken(getString(R.string.default_web_client_id))
			.build();
		GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);
		googleSignInClient.signOut().addOnCompleteListener(this, task -> {
			Intent intent = new Intent(this, SplashScreen.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
			startActivity(intent);
			finish();
		});
	}
}
