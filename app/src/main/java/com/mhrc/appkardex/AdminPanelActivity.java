package com.mhrc.appkardex;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminPanelActivity extends AppCompatActivity {

	private static final String TAG = "AdminPanel";

	private FirebaseFirestore db;
	private TabLayout tabLayout;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_admin_panel);

		// Inicializar helper de materias
		MateriasHelper.inicializar(this);

		db = FirebaseFirestore.getInstance();
		tabLayout = findViewById(R.id.tabLayout);

		Button btnLogout = findViewById(R.id.btnLogout);
		btnLogout.setOnClickListener(v -> logout());

		tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
			@Override
			public void onTabSelected(TabLayout.Tab tab) {
				if (tab.getPosition() == 0) {
					loadPendingAlumnos();
				} else if (tab.getPosition() == 1) {
					loadPendingMaestros();
				} else if (tab.getPosition() == 2) {
					loadReinscripciones();
				}
			}

			@Override
			public void onTabUnselected(TabLayout.Tab tab) {}

			@Override
			public void onTabReselected(TabLayout.Tab tab) {}
		});
		
		// Cargar datos iniciales de la primera pestaña
		TabLayout.Tab firstTab = tabLayout.getTabAt(0);
		if (firstTab != null) {
			firstTab.select();
		}
	}

	private void loadPendingAlumnos() {
		ViewGroup container = findViewById(R.id.containerPending);
		container.removeAllViews();

		db.collection("alumnos").whereEqualTo("estado", "pendiente").get()
			.addOnCompleteListener(task -> {
				if (task.isSuccessful()) {
					for (DocumentSnapshot doc : task.getResult().getDocuments()) {
						View itemView = LayoutInflater.from(this)
							.inflate(R.layout.item_pending_alumno, container, false);

						String uid = doc.getId();
						String nombre = doc.getString("nombre");
						String email = doc.getString("email");
						Long matricula = doc.getLong("matricula");
						String carrera = doc.getString("carrera");
						String grupoSolicitado = doc.getString("grupoSolicitado");
						@SuppressWarnings("unchecked")
						List<String> materias = (List<String>) doc.get("materias");

						TextView tvNombre = itemView.findViewById(R.id.tvNombre);
						TextView tvEmail = itemView.findViewById(R.id.tvEmail);
						TextView tvMatricula = itemView.findViewById(R.id.tvMatricula);
						TextView tvCarrera = itemView.findViewById(R.id.tvCarrera);
						TextView tvGrupo = itemView.findViewById(R.id.tvGrupo);
						TextView tvMaterias = itemView.findViewById(R.id.tvMaterias);
						Button btnAceptar = itemView.findViewById(R.id.btnAceptar);
						Button btnRechazar = itemView.findViewById(R.id.btnRechazar);

						tvNombre.setText("Nombre: " + nombre);
						tvEmail.setText("Email: " + email);
						tvMatricula.setText("Matrícula: " + (matricula != null ? matricula : "N/A"));
						tvCarrera.setText("Carrera: " + carrera);
						tvGrupo.setText("Grupo solicitado: " + grupoSolicitado);
						
						// Mostrar solo las materias
						if (materias != null && !materias.isEmpty()) {
							StringBuilder materiasText = new StringBuilder("Materias:\n");
							for (String materia : materias) {
								materiasText.append("• ").append(materia).append("\n");
							}
							tvMaterias.setText(materiasText.toString());
							tvMaterias.setVisibility(TextView.VISIBLE);
						} else {
							tvMaterias.setVisibility(TextView.GONE);
						}

						btnAceptar.setOnClickListener(v -> showAceptarAlumnoDialog(uid, carrera, grupoSolicitado));
						btnRechazar.setOnClickListener(v -> rechazarAlumno(uid));

						container.addView(itemView);
					}
				}
			});
	}

	private void loadPendingMaestros() {
		ViewGroup container = findViewById(R.id.containerPending);
		container.removeAllViews();

		db.collection("maestros").whereEqualTo("estado", "pendiente")
			.get()
			.addOnCompleteListener(task -> {
				if (task.isSuccessful()) {
					for (DocumentSnapshot doc : task.getResult().getDocuments()) {
						View itemView = LayoutInflater.from(this)
							.inflate(R.layout.item_pending_maestro, container, false);

						String uid = doc.getId();
						String nombre = doc.getString("nombre");
						String email = doc.getString("email");
						String area = doc.getString("areaAcademica");
						@SuppressWarnings("unchecked")
						List<String> materias = (List<String>) doc.get("materias");

						TextView tvNombre = itemView.findViewById(R.id.tvNombre);
						TextView tvEmail = itemView.findViewById(R.id.tvEmail);
						TextView tvArea = itemView.findViewById(R.id.tvArea);
						TextView tvMaterias = itemView.findViewById(R.id.tvMaterias);
						Button btnAceptar = itemView.findViewById(R.id.btnAceptar);
						Button btnRechazar = itemView.findViewById(R.id.btnRechazar);

						tvNombre.setText("Nombre: " + nombre);
						tvEmail.setText("Email: " + email);
						tvArea.setText("Área: " + area);
						
						// Mostrar materias seleccionadas por el maestro
						if (materias != null && !materias.isEmpty()) {
							tvMaterias.setText("Materias seleccionadas: " + String.join(", ", materias));
							tvMaterias.setVisibility(TextView.VISIBLE);
						} else {
							tvMaterias.setVisibility(TextView.GONE);
						}

						// Hacer el nombre clickable para mostrar detalles y asignar
						tvNombre.setOnClickListener(v -> showMaestroDetailsDialog(uid, doc));

						btnAceptar.setOnClickListener(v -> aceptarMaestro(uid));
						btnRechazar.setOnClickListener(v -> rechazarMaestro(uid));

						container.addView(itemView);
					}
				}
			});
	}

	private void showMaestroDetailsDialog(String uid, DocumentSnapshot doc) {
		String nombre = doc.getString("nombre");
		String email = doc.getString("email");
		String area = doc.getString("areaAcademica");
		@SuppressWarnings("unchecked")
		List<String> materias = (List<String>) doc.get("materias");

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Información del Maestro");

		StringBuilder info = new StringBuilder();
		info.append("Nombre: ").append(nombre).append("\n");
		info.append("Email: ").append(email).append("\n");
		info.append("Área Académica: ").append(area).append("\n");
		info.append("\nMaterias seleccionadas:\n");
		if (materias != null && !materias.isEmpty()) {
			for (String materia : materias) {
				info.append("• ").append(materia).append("\n");
			}
		} else {
			info.append("No hay materias seleccionadas\n");
		}

		builder.setMessage(info.toString());
		builder.setPositiveButton("Asignar Grupo y Materia", (dialog, which) -> {
			showAsignarGrupoMateriaDialog(uid, area, materias);
		});
		builder.setNegativeButton("Cerrar", null);
		builder.show();
	}

	private void showAsignarGrupoMateriaDialog(String uid, String area, List<String> materiasDisponibles) {
		if (materiasDisponibles == null || materiasDisponibles.isEmpty()) {
			Toast.makeText(this, "Este maestro no tiene materias seleccionadas", Toast.LENGTH_SHORT).show();
			return;
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Asignar Grupos y Materias");

		View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_asignar_grupo_materia, null);

		LinearLayout llGruposContainer = dialogView.findViewById(R.id.llGruposContainer);
		LinearLayout llMateriasContainer = dialogView.findViewById(R.id.llMateriasContainer);
		TextView tvMateriaOcupada = dialogView.findViewById(R.id.tvMateriaOcupada);

		// Crear checkboxes para grupos
		final List<android.widget.CheckBox> checkBoxesGrupos = new ArrayList<>();
		String[] grupos = {"1", "2", "3"};
		for (String grupo : grupos) {
			android.widget.CheckBox checkBox = new android.widget.CheckBox(this);
			checkBox.setText("Grupo " + grupo);
			checkBox.setTextSize(16);
			checkBox.setPadding(0, 12, 0, 12);
			checkBox.setTextColor(getResources().getColor(R.color.text_primary));
			llGruposContainer.addView(checkBox);
			checkBoxesGrupos.add(checkBox);
			
			// Cuando se seleccione un grupo, actualizar disponibilidad de materias
			checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
				actualizarDisponibilidadMaterias(area, materiasDisponibles, llMateriasContainer, 
					checkBoxesGrupos, tvMateriaOcupada);
			});
		}

		// Inicializar materias (se actualizarán cuando se seleccionen grupos)
		actualizarDisponibilidadMaterias(area, materiasDisponibles, llMateriasContainer, 
			checkBoxesGrupos, tvMateriaOcupada);

		builder.setView(dialogView);
		builder.setPositiveButton("Asignar", (dialog, which) -> {
			// Obtener grupos seleccionados
			List<String> gruposSeleccionados = new ArrayList<>();
			for (android.widget.CheckBox cb : checkBoxesGrupos) {
				if (cb.isChecked()) {
					String grupoText = cb.getText().toString().replace("Grupo ", "");
					gruposSeleccionados.add(grupoText);
				}
			}

			// Obtener materias seleccionadas (solo las habilitadas pueden estar seleccionadas)
			List<String> materiasSeleccionadas = new ArrayList<>();
			int childCount = llMateriasContainer.getChildCount();
			for (int i = 0; i < childCount; i++) {
				View child = llMateriasContainer.getChildAt(i);
				if (child instanceof android.widget.CheckBox) {
					android.widget.CheckBox cb = (android.widget.CheckBox) child;
					if (cb.isChecked() && cb.isEnabled()) {
						String materiaText = cb.getText().toString();
						// Si tiene " (Ocupada", limpiar esa parte
						if (materiaText.contains(" (Ocupada")) {
							materiaText = materiaText.substring(0, materiaText.indexOf(" (Ocupada"));
						}
						materiasSeleccionadas.add(materiaText);
					}
				}
			}

			if (gruposSeleccionados.isEmpty()) {
				Toast.makeText(this, "Debes seleccionar al menos un grupo", Toast.LENGTH_SHORT).show();
				return;
			}

			if (materiasSeleccionadas.isEmpty()) {
				Toast.makeText(this, "Debes seleccionar al menos una materia", Toast.LENGTH_SHORT).show();
				return;
			}

			// Validar que no haya conflictos antes de asignar
			validarYAsignarMaestro(uid, area, gruposSeleccionados, materiasSeleccionadas);
		});
		builder.setNegativeButton("Cancelar", null);
		builder.show();
	}

	private void actualizarDisponibilidadMaterias(String area, List<String> materiasDisponibles, 
		LinearLayout container, List<android.widget.CheckBox> checkBoxesGrupos, TextView tvMateriaOcupada) {
		
		// Obtener grupos seleccionados
		List<String> gruposSeleccionados = new ArrayList<>();
		for (android.widget.CheckBox cb : checkBoxesGrupos) {
			if (cb.isChecked()) {
				String grupoText = cb.getText().toString().replace("Grupo ", "");
				gruposSeleccionados.add(grupoText);
			}
		}
		
		// Limpiar contenedor
		container.removeAllViews();
		
		// Si no hay grupos seleccionados, mostrar todas las materias deshabilitadas
		if (gruposSeleccionados.isEmpty()) {
			for (String materia : materiasDisponibles) {
				android.widget.CheckBox checkBox = new android.widget.CheckBox(this);
				checkBox.setText(materia);
				checkBox.setEnabled(false);
				checkBox.setTextColor(getResources().getColor(R.color.text_secondary));
				checkBox.setTextSize(16);
				checkBox.setPadding(0, 12, 0, 12);
				container.addView(checkBox);
			}
			tvMateriaOcupada.setVisibility(TextView.GONE);
			return;
		}
		
		// Obtener todas las asignaciones de maestros para esta área
		db.collection("maestros")
			.whereEqualTo("carreraAsignada", area)
			.whereEqualTo("estado", "aprobado")
			.get()
			.addOnCompleteListener(task -> {
				// Mapa: materia -> [grupos donde está ocupada]
				Map<String, List<String>> materiasOcupadasPorGrupo = new HashMap<>();
				
				if (task.isSuccessful()) {
					for (QueryDocumentSnapshot doc : task.getResult()) {
						// Verificar campos antiguos (compatibilidad)
						String grupoAsignado = doc.getString("grupoAsignado");
						String materiaAsignada = doc.getString("materiaAsignada");
						
						if (grupoAsignado != null && materiaAsignada != null) {
							List<String> grupos = materiasOcupadasPorGrupo.get(materiaAsignada);
							if (grupos == null) {
								grupos = new ArrayList<>();
								materiasOcupadasPorGrupo.put(materiaAsignada, grupos);
							}
							if (!grupos.contains(grupoAsignado)) {
								grupos.add(grupoAsignado);
							}
						}
						
						// Verificar campos nuevos (múltiples grupos y materias)
						@SuppressWarnings("unchecked")
						List<String> gruposAsignados = (List<String>) doc.get("gruposAsignados");
						@SuppressWarnings("unchecked")
						List<String> materiasAsignadas = (List<String>) doc.get("materiasAsignadas");
						
						if (gruposAsignados != null && materiasAsignadas != null) {
							for (String materia : materiasAsignadas) {
								List<String> grupos = materiasOcupadasPorGrupo.get(materia);
								if (grupos == null) {
									grupos = new ArrayList<>();
									materiasOcupadasPorGrupo.put(materia, grupos);
								}
								for (String grupo : gruposAsignados) {
									if (!grupos.contains(grupo)) {
										grupos.add(grupo);
									}
								}
							}
						}
					}
				}

				// Crear checkboxes verificando ocupación por grupo
				boolean hayOcupadas = false;
				StringBuilder mensajeOcupadas = new StringBuilder("Materias ocupadas en grupos seleccionados: ");
				
				for (String materia : materiasDisponibles) {
					android.widget.CheckBox checkBox = new android.widget.CheckBox(this);
					List<String> gruposOcupados = materiasOcupadasPorGrupo.get(materia);
					
					// Verificar si está ocupada en ALGUNO de los grupos seleccionados
					boolean ocupadaEnGruposSeleccionados = false;
					List<String> gruposOcupadosEnSeleccionados = new ArrayList<>();
					
					if (gruposOcupados != null) {
						for (String grupoOcupado : gruposOcupados) {
							if (gruposSeleccionados.contains(grupoOcupado)) {
								ocupadaEnGruposSeleccionados = true;
								gruposOcupadosEnSeleccionados.add("Grupo " + grupoOcupado);
							}
						}
					}
					
					if (ocupadaEnGruposSeleccionados) {
						String gruposText = String.join(", ", gruposOcupadosEnSeleccionados);
						checkBox.setText(materia + " (Ocupada en " + gruposText + ")");
						checkBox.setEnabled(false);
						checkBox.setTextColor(getResources().getColor(R.color.text_secondary));
						hayOcupadas = true;
						if (mensajeOcupadas.length() > "Materias ocupadas en grupos seleccionados: ".length()) {
							mensajeOcupadas.append(", ");
						}
						mensajeOcupadas.append(materia);
					} else {
						checkBox.setText(materia);
						checkBox.setTextColor(getResources().getColor(R.color.text_primary));
						checkBox.setEnabled(true);
					}
					
					checkBox.setTextSize(16);
					checkBox.setPadding(0, 12, 0, 12);
					container.addView(checkBox);
				}

				if (hayOcupadas) {
					tvMateriaOcupada.setText(mensajeOcupadas.toString());
					tvMateriaOcupada.setVisibility(TextView.VISIBLE);
				} else {
					tvMateriaOcupada.setVisibility(TextView.GONE);
				}
			});
	}

	private void validarYAsignarMaestro(String uidMaestro, String carrera, 
		List<String> gruposSeleccionados, List<String> materiasSeleccionadas) {
		
		// Validar que no haya conflictos (mismo grupo + misma materia ya asignada)
		db.collection("maestros")
			.whereEqualTo("carreraAsignada", carrera)
			.whereEqualTo("estado", "aprobado")
			.get()
			.addOnCompleteListener(task -> {
				List<String> conflictos = new ArrayList<>();
				
				if (task.isSuccessful()) {
					for (QueryDocumentSnapshot doc : task.getResult()) {
						if (doc.getId().equals(uidMaestro)) continue; // Ignorar el maestro actual
						
						// Verificar campos antiguos (compatibilidad)
						String grupoExistente = doc.getString("grupoAsignado");
						String materiaExistente = doc.getString("materiaAsignada");
						
						if (grupoExistente != null && materiaExistente != null) {
							if (gruposSeleccionados.contains(grupoExistente) && 
								materiasSeleccionadas.contains(materiaExistente)) {
								conflictos.add("Grupo " + grupoExistente + " - " + materiaExistente);
							}
						}
						
						// Verificar campos nuevos (múltiples grupos y materias)
						@SuppressWarnings("unchecked")
						List<String> gruposExistentes = (List<String>) doc.get("gruposAsignados");
						@SuppressWarnings("unchecked")
						List<String> materiasExistentes = (List<String>) doc.get("materiasAsignadas");
						
						if (gruposExistentes != null && materiasExistentes != null) {
							for (String grupo : gruposSeleccionados) {
								for (String materia : materiasSeleccionadas) {
									if (gruposExistentes.contains(grupo) && materiasExistentes.contains(materia)) {
										conflictos.add("Grupo " + grupo + " - " + materia);
									}
								}
							}
						}
					}
				}

				if (!conflictos.isEmpty()) {
					Toast.makeText(this, 
						"Conflicto: Ya existe un maestro asignado a:\n" + String.join("\n", conflictos), 
						Toast.LENGTH_LONG).show();
					return;
				}

				// Si no hay conflictos, proceder con la asignación
				asignarGruposMateriasMaestro(uidMaestro, carrera, gruposSeleccionados, materiasSeleccionadas);
			});
	}

	private void asignarGruposMateriasMaestro(String uidMaestro, String carrera, 
		List<String> gruposSeleccionados, List<String> materiasSeleccionadas) {
		
		android.util.Log.d(TAG, "Asignando maestro " + uidMaestro + " a grupos " + gruposSeleccionados + 
			" para materias " + materiasSeleccionadas);

		// Actualizar al maestro con las asignaciones
		// Guardar como lista de asignaciones en lugar de campos simples
		List<Map<String, String>> asignaciones = new ArrayList<>();
		for (String grupo : gruposSeleccionados) {
			for (String materia : materiasSeleccionadas) {
				Map<String, String> asignacion = new HashMap<>();
				asignacion.put("grupo", grupo);
				asignacion.put("materia", materia);
				asignaciones.add(asignacion);
			}
		}

		// IMPORTANTE: Copiar faceData de "usuarios" a "maestros" antes de aprobar
		db.collection("usuarios").document(uidMaestro).get()
			.addOnSuccessListener(usuarioDoc -> {
				if (usuarioDoc.exists()) {
					Map<String, Object> faceData = (Map<String, Object>) usuarioDoc.get("faceData");
					
					Map<String, Object> maestroUpdate = new HashMap<>();
					maestroUpdate.put("gruposAsignados", gruposSeleccionados);
					maestroUpdate.put("materiasAsignadas", materiasSeleccionadas);
					maestroUpdate.put("carreraAsignada", carrera);
					maestroUpdate.put("asignaciones", asignaciones);
					maestroUpdate.put("estado", "aprobado");
					
					// Copiar faceData a la colección maestros si existe
					if (faceData != null && !faceData.isEmpty()) {
						maestroUpdate.put("faceData", faceData);
						android.util.Log.d("AdminPanel", "✓ faceData copiado de usuarios a maestros para: " + uidMaestro);
					} else {
						android.util.Log.w("AdminPanel", "⚠ No se encontró faceData en usuarios para: " + uidMaestro);
					}

					// Actualizar maestro y usuario
					db.collection("maestros").document(uidMaestro).update(maestroUpdate)
						.addOnCompleteListener(task -> {
							if (task.isSuccessful()) {
								// Actualizar también en la colección de usuarios
								db.collection("usuarios").document(uidMaestro).update("estado", "aprobado")
									.addOnCompleteListener(task2 -> {
							// Actualizar a todos los alumnos de esa carrera y grupos con el maestro para esas materias
							final int[] totalActualizados = {0};
							final int totalAsignaciones = gruposSeleccionados.size() * materiasSeleccionadas.size();
							
							if (totalAsignaciones == 0) {
								Toast.makeText(this, 
									"Maestro aceptado y asignado a " + gruposSeleccionados.size() + " grupo(s) y " + 
									materiasSeleccionadas.size() + " materia(s)", 
									Toast.LENGTH_LONG).show();
								loadPendingMaestros();
								return;
							}
							
							for (String grupo : gruposSeleccionados) {
								for (String materia : materiasSeleccionadas) {
									actualizarAlumnosConMaestro(carrera, grupo, materia, uidMaestro, () -> {
										totalActualizados[0]++;
										if (totalActualizados[0] == totalAsignaciones) {
											Toast.makeText(this, 
												"Maestro aceptado y asignado a " + gruposSeleccionados.size() + " grupo(s) y " + 
												materiasSeleccionadas.size() + " materia(s)", 
												Toast.LENGTH_LONG).show();
											loadPendingMaestros();
										}
									});
								}
							}
									});
							} else {
								Toast.makeText(this, "Error al asignar maestro", Toast.LENGTH_SHORT).show();
							}
						});
				} else {
					Toast.makeText(this, "Error: Usuario no encontrado", Toast.LENGTH_SHORT).show();
				}
			})
			.addOnFailureListener(e -> {
				android.util.Log.e("AdminPanel", "Error al obtener datos de usuario: " + e.getMessage());
				Toast.makeText(this, "Error al obtener datos del usuario", Toast.LENGTH_SHORT).show();
			});
	}

	private void actualizarAlumnosConMaestro(String carrera, String grupo, String materia, String uidMaestro, Runnable callback) {
		// Obtener el nombre del maestro
		db.collection("maestros").document(uidMaestro).get()
			.addOnCompleteListener(task -> {
				if (task.isSuccessful() && task.getResult().exists()) {
					String nombreMaestro = task.getResult().getString("nombre");

					// Buscar todos los alumnos de esa carrera y grupo
					db.collection("alumnos")
						.whereEqualTo("carrera", carrera)
						.whereEqualTo("grupo", grupo)
						.whereEqualTo("estado", "aprobado")
						.get()
						.addOnCompleteListener(task2 -> {
							if (task2.isSuccessful()) {
								int actualizados = 0;
								for (QueryDocumentSnapshot doc : task2.getResult()) {
									@SuppressWarnings("unchecked")
									List<String> materiasAlumno = (List<String>) doc.get("materias");
									
									if (materiasAlumno != null && materiasAlumno.contains(materia)) {
										Map<String, Object> alumnoUpdate = new HashMap<>();
										
										// Obtener maestros actuales o inicializar
										Map<String, String> maestrosAsignados = new HashMap<>();
										@SuppressWarnings("unchecked")
										Map<String, String> maestrosActuales = (Map<String, String>) doc.get("maestrosAsignados");
										
										if (maestrosActuales != null) {
											maestrosAsignados.putAll(maestrosActuales);
										} else {
											// Si no existe el mapa, inicializar todas las materias
											for (String m : materiasAlumno) {
												maestrosAsignados.put(m, "No hay maestro aún");
											}
										}
										
										// Asignar el maestro para esta materia
										maestrosAsignados.put(materia, nombreMaestro + " (" + uidMaestro + ")");
										
										alumnoUpdate.put("maestrosAsignados", maestrosAsignados);
										db.collection("alumnos").document(doc.getId()).update(alumnoUpdate);
										actualizados++;
									}
								}
								if (callback != null) {
									callback.run();
								}
							}
						});
				}
			});
	}

	private void showAceptarAlumnoDialog(String uid, String carrera, String grupoSolicitado) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Asignar Grupo Final");

		View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_asignar_grupo, null);
		Spinner spinnerGrupo = dialogView.findViewById(R.id.spinnerGrupo);
		ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
			android.R.layout.simple_spinner_item, new String[]{"1", "2", "3"});
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerGrupo.setAdapter(adapter);

		// Preseleccionar el grupo solicitado si existe
		if (grupoSolicitado != null) {
			for (int i = 0; i < adapter.getCount(); i++) {
				if (adapter.getItem(i).equals(grupoSolicitado)) {
					spinnerGrupo.setSelection(i);
					break;
				}
			}
		}

		builder.setView(dialogView);
		builder.setPositiveButton("Aceptar", (dialog, which) -> {
			String grupoFinal = spinnerGrupo.getSelectedItem().toString();
			aceptarAlumno(uid, grupoFinal, carrera);
		});
		builder.setNegativeButton("Cancelar", null);
		builder.show();
	}

	private void aceptarAlumno(String uid, String grupoFinal, String carrera) {
		// Verificar que el grupo no tenga más de 30 alumnos
		db.collection("alumnos")
			.whereEqualTo("carrera", carrera)
			.whereEqualTo("grupo", grupoFinal)
			.whereEqualTo("estado", "aprobado")
			.get()
			.addOnCompleteListener(task -> {
				if (task.isSuccessful() && task.getResult().size() >= 30) {
					Toast.makeText(this, "Este grupo ya tiene 30 alumnos", Toast.LENGTH_SHORT).show();
				} else {
					// IMPORTANTE: Copiar faceData de "usuarios" a "alumnos" antes de aprobar
					db.collection("usuarios").document(uid).get()
						.addOnSuccessListener(usuarioDoc -> {
							if (usuarioDoc.exists()) {
								Map<String, Object> faceData = (Map<String, Object>) usuarioDoc.get("faceData");
								
								Map<String, Object> updates = new HashMap<>();
								updates.put("estado", "aprobado");
								updates.put("grupo", grupoFinal);
								
								// Copiar faceData a la colección alumnos si existe
								if (faceData != null && !faceData.isEmpty()) {
									updates.put("faceData", faceData);
									android.util.Log.d("AdminPanel", "✓ faceData copiado de usuarios a alumnos para: " + uid);
								} else {
									android.util.Log.w("AdminPanel", "⚠ No se encontró faceData en usuarios para: " + uid);
								}

								db.collection("alumnos").document(uid).update(updates)
									.addOnSuccessListener(aVoid -> {
										android.util.Log.d("AdminPanel", "✓ Alumno actualizado en colección alumnos");
									});
								db.collection("usuarios").document(uid).update("estado", "aprobado");

								// Asignar maestros a las materias del alumno si ya existen asignaciones
								asignarMaestrosAAlumno(uid, carrera, grupoFinal);

								Toast.makeText(this, "Alumno aceptado", Toast.LENGTH_SHORT).show();
								loadPendingAlumnos();
							} else {
								Toast.makeText(this, "Error: Usuario no encontrado", Toast.LENGTH_SHORT).show();
							}
						})
						.addOnFailureListener(e -> {
							android.util.Log.e("AdminPanel", "Error al obtener datos de usuario: " + e.getMessage());
							Toast.makeText(this, "Error al obtener datos del usuario", Toast.LENGTH_SHORT).show();
						});
				}
			});
	}

	private void asignarMaestrosAAlumno(String uidAlumno, String carrera, String grupo) {
		// Buscar maestros asignados a esta carrera y grupo
		db.collection("maestros")
			.whereEqualTo("carreraAsignada", carrera)
			.whereEqualTo("grupoAsignado", grupo)
			.whereEqualTo("estado", "aprobado")
			.get()
			.addOnCompleteListener(task -> {
				// Obtener datos del alumno
				db.collection("alumnos").document(uidAlumno).get()
					.addOnCompleteListener(task2 -> {
						if (task2.isSuccessful() && task2.getResult().exists()) {
							DocumentSnapshot alumnoDoc = task2.getResult();
							@SuppressWarnings("unchecked")
							List<String> materiasAlumno = (List<String>) alumnoDoc.get("materias");

							if (materiasAlumno == null || materiasAlumno.isEmpty()) {
								return;
							}

							Map<String, String> maestrosAsignados = new HashMap<>();
							
							// Inicializar todas las materias con "no hay maestro aún"
							for (String materia : materiasAlumno) {
								maestrosAsignados.put(materia, "No hay maestro aún");
							}

							// Si hay maestros asignados, actualizar las materias correspondientes
							if (task.isSuccessful()) {
								for (QueryDocumentSnapshot maestroDoc : task.getResult()) {
									String materiaAsignada = maestroDoc.getString("materiaAsignada");
									String nombreMaestro = maestroDoc.getString("nombre");
									String uidMaestro = maestroDoc.getId();

									if (materiaAsignada != null && materiasAlumno.contains(materiaAsignada)) {
										maestrosAsignados.put(materiaAsignada, nombreMaestro + " (" + uidMaestro + ")");
									}
								}
							}

							// Actualizar el alumno con los maestros asignados
							Map<String, Object> update = new HashMap<>();
							update.put("maestrosAsignados", maestrosAsignados);
							db.collection("alumnos").document(uidAlumno).update(update);
						}
					});
			});
	}

	private void rechazarAlumno(String uid) {
		Map<String, Object> updates = new HashMap<>();
		updates.put("estado", "rechazado");

		db.collection("alumnos").document(uid).update(updates);
		db.collection("usuarios").document(uid).update("estado", "rechazado");

		Toast.makeText(this, "Alumno rechazado", Toast.LENGTH_SHORT).show();
		loadPendingAlumnos();
	}

	private void aceptarMaestro(String uid) {
		// Mostrar diálogo de asignación primero
		db.collection("maestros").document(uid).get()
			.addOnCompleteListener(task -> {
				if (task.isSuccessful() && task.getResult().exists()) {
					DocumentSnapshot doc = task.getResult();
					String area = doc.getString("areaAcademica");
					@SuppressWarnings("unchecked")
					List<String> materias = (List<String>) doc.get("materias");

					// Mostrar diálogo de asignación (aceptar después de asignar)
					showAsignarGrupoMateriaDialog(uid, area, materias);
				}
			});
	}

	private void rechazarMaestro(String uid) {
		db.collection("maestros").document(uid).update("estado", "rechazado");
		db.collection("usuarios").document(uid).update("estado", "rechazado");

		Toast.makeText(this, "Maestro rechazado", Toast.LENGTH_SHORT).show();
		loadPendingMaestros();
	}

	private void loadReinscripciones() {
		ViewGroup container = findViewById(R.id.containerPending);
		container.removeAllViews();

		db.collection("alumnos").whereEqualTo("solicitudReinscripcion", "pendiente").get()
			.addOnCompleteListener(task -> {
				if (task.isSuccessful()) {
					for (DocumentSnapshot doc : task.getResult().getDocuments()) {
						View itemView = LayoutInflater.from(this)
							.inflate(R.layout.item_pending_alumno, container, false);

						String uid = doc.getId();
						String nombre = doc.getString("nombre");
						String email = doc.getString("email");
						Long matricula = doc.getLong("matricula");
						String carrera = doc.getString("carrera");
						String grupo = doc.getString("grupo");
						String semestre = doc.getString("semestre");

						TextView tvNombre = itemView.findViewById(R.id.tvNombre);
						TextView tvEmail = itemView.findViewById(R.id.tvEmail);
						TextView tvMatricula = itemView.findViewById(R.id.tvMatricula);
						TextView tvCarrera = itemView.findViewById(R.id.tvCarrera);
						TextView tvGrupo = itemView.findViewById(R.id.tvGrupo);
						TextView tvMaterias = itemView.findViewById(R.id.tvMaterias);
						Button btnAceptar = itemView.findViewById(R.id.btnAceptar);
						Button btnRechazar = itemView.findViewById(R.id.btnRechazar);

						tvNombre.setText("Nombre: " + nombre);
						tvEmail.setText("Email: " + email);
						tvMatricula.setText("Matrícula: " + (matricula != null ? matricula : "N/A"));
						tvCarrera.setText("Carrera: " + carrera);
						tvGrupo.setText("Semestre actual: " + semestre + " - Grupo: " + grupo);
						
						// Obtener materias del siguiente semestre
						int semestreInt = Integer.parseInt(semestre);
						int semestreSiguiente = semestreInt + 1;
						List<String> materiasSiguiente = MateriasHelper.getMateriasPorSemestre(carrera, semestreSiguiente);
						
						if (materiasSiguiente != null && !materiasSiguiente.isEmpty()) {
							StringBuilder materiasText = new StringBuilder("Materias del siguiente semestre (" + semestreSiguiente + "):\n");
							for (String materia : materiasSiguiente) {
								materiasText.append("• ").append(materia).append("\n");
							}
							tvMaterias.setText(materiasText.toString());
							tvMaterias.setVisibility(TextView.VISIBLE);
						} else {
							tvMaterias.setVisibility(TextView.GONE);
						}

						btnAceptar.setOnClickListener(v -> aceptarReinscripcion(uid, carrera, semestre));
						btnRechazar.setOnClickListener(v -> rechazarReinscripcion(uid));

						container.addView(itemView);
					}
				}
			});
	}

	private void aceptarReinscripcion(String uid, String carrera, String semestreActual) {
		// Calcular siguiente semestre
		int semestreInt = Integer.parseInt(semestreActual);
		int semestreSiguiente = semestreInt + 1;
		
		// Obtener materias del siguiente semestre
		List<String> materiasSiguienteSemestre = MateriasHelper.getMateriasPorSemestre(carrera, semestreSiguiente);
		
		if (materiasSiguienteSemestre == null || materiasSiguienteSemestre.isEmpty()) {
			Toast.makeText(this, "No hay materias disponibles para el siguiente semestre", Toast.LENGTH_SHORT).show();
			return;
		}
		
		// Actualizar alumno: nuevo semestre, nuevas materias, limpiar calificaciones del semestre anterior
		Map<String, Object> updates = new HashMap<>();
		updates.put("semestre", String.valueOf(semestreSiguiente));
		updates.put("materias", materiasSiguienteSemestre);
		updates.put("solicitudReinscripcion", "aprobada");
		updates.put("parcial", "1"); // Reiniciar parcial a 1
		
		// Inicializar calificaciones vacías para las nuevas materias
		Map<String, Map<String, Long>> nuevasCalificaciones = new HashMap<>();
		Map<String, String> nuevosMaestros = new HashMap<>();
		for (String materia : materiasSiguienteSemestre) {
			Map<String, Long> parciales = new HashMap<>();
			parciales.put("parcial1", null);
			parciales.put("parcial2", null);
			parciales.put("parcial3", null);
			nuevasCalificaciones.put(materia, parciales);
			nuevosMaestros.put(materia, "No hay maestro aún");
		}
		updates.put("calificaciones", nuevasCalificaciones);
		updates.put("maestrosAsignados", nuevosMaestros);

		db.collection("alumnos").document(uid).update(updates)
			.addOnCompleteListener(task -> {
				if (task.isSuccessful()) {
					Toast.makeText(this, "Reinscripción aprobada", Toast.LENGTH_SHORT).show();
					loadReinscripciones();
				} else {
					Toast.makeText(this, "Error al aprobar reinscripción", Toast.LENGTH_SHORT).show();
				}
			});
	}

	private void rechazarReinscripcion(String uid) {
		db.collection("alumnos").document(uid).update("solicitudReinscripcion", "rechazada")
			.addOnCompleteListener(task -> {
				if (task.isSuccessful()) {
					Toast.makeText(this, "Reinscripción rechazada", Toast.LENGTH_SHORT).show();
					loadReinscripciones();
				} else {
					Toast.makeText(this, "Error al rechazar reinscripción", Toast.LENGTH_SHORT).show();
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
