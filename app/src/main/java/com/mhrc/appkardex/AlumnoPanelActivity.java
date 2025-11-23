package com.mhrc.appkardex;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AlumnoPanelActivity extends AppCompatActivity {

	private FirebaseFirestore db;
	private FirebaseUser user;
	private List<String> materias;
	private DocumentSnapshot alumnoDoc;
	private String nombreAlumno;
	private String grupoAlumno;
	private String carreraAlumno;
	private Long matriculaAlumno;
	private String semestreActual;
	private Map<String, Map<String, Long>> calificacionesAlumno; // Nueva estructura: materia -> { parcial1, parcial2, parcial3 }
	private Map<String, String> maestrosAsignados;
	private Button btnReinscripcion;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_alumno_panel);

		db = FirebaseFirestore.getInstance();
		user = FirebaseAuth.getInstance().getCurrentUser();

		Button btnLogout = findViewById(R.id.btnLogout);
		Button btnImprimirPDF = findViewById(R.id.btnImprimirPDF);
		btnReinscripcion = findViewById(R.id.btnReinscripcion);

		btnLogout.setOnClickListener(v -> logout());
		btnImprimirPDF.setOnClickListener(v -> imprimirPDF());
		btnReinscripcion.setOnClickListener(v -> solicitarReinscripcion());

		loadAlumnoData();
	}


	private void loadAlumnoData() {
		db.collection("alumnos").document(user.getUid()).get()
			.addOnCompleteListener(task -> {
				if (task.isSuccessful()) {
					DocumentSnapshot doc = task.getResult();
					if (doc.exists()) {
						alumnoDoc = doc;
						displayAlumnoInfo(doc);
					}
				}
			});
	}

	private void displayAlumnoInfo(DocumentSnapshot doc) {
		TextView tvNombre = findViewById(R.id.tvNombre);
		TextView tvGrupo = findViewById(R.id.tvGrupo);
		TextView tvCarrera = findViewById(R.id.tvCarrera);
		TextView tvMatricula = findViewById(R.id.tvMatricula);

		nombreAlumno = doc.getString("nombre");
		grupoAlumno = doc.getString("grupo");
		carreraAlumno = doc.getString("carrera");
		matriculaAlumno = doc.getLong("matricula");
		semestreActual = doc.getString("semestre");

		tvNombre.setText(nombreAlumno);
		tvGrupo.setText("Grupo: " + grupoAlumno);
		tvCarrera.setText("Carrera: " + carreraAlumno);
		tvMatricula.setText("Matrícula: " + (matriculaAlumno != null ? matriculaAlumno : "N/A"));

		// Obtener materias
		materias = (List<String>) doc.get("materias");
		if (materias == null) {
			materias = new ArrayList<>();
		}

		// Obtener calificaciones (nueva estructura con 3 parciales, con compatibilidad hacia atrás)
		@SuppressWarnings("unchecked")
		Object califObj = doc.get("calificaciones");
		Map<String, Map<String, Long>> calificaciones = new HashMap<>();
		
		if (califObj != null) {
			if (califObj instanceof Map) {
				@SuppressWarnings("unchecked")
				Map<String, Object> califMap = (Map<String, Object>) califObj;
				
				// Verificar si es la estructura nueva (Map<String, Map<String, Long>>) o antigua (Map<String, Long>)
				for (Map.Entry<String, Object> entry : califMap.entrySet()) {
					Object value = entry.getValue();
					if (value instanceof Map) {
						// Estructura nueva: materia -> { parcial1, parcial2, parcial3 }
						@SuppressWarnings("unchecked")
						Map<String, Long> parciales = (Map<String, Long>) value;
						calificaciones.put(entry.getKey(), parciales);
					} else if (value instanceof Number) {
						// Estructura antigua: materia -> calificacion (Long)
						// Convertir a estructura nueva con solo parcial1
						Map<String, Long> parciales = new HashMap<>();
						parciales.put("parcial1", ((Number) value).longValue());
						parciales.put("parcial2", null);
						parciales.put("parcial3", null);
						calificaciones.put(entry.getKey(), parciales);
					}
				}
			}
		}
		
		calificacionesAlumno = calificaciones;

		// Obtener maestros asignados
		@SuppressWarnings("unchecked")
		Map<String, String> maestros = (Map<String, String>) doc.get("maestrosAsignados");
		if (maestros == null) {
			maestros = new HashMap<>();
		}
		maestrosAsignados = maestros;

		displayCalificaciones(materias, calificaciones);
		
		// Verificar si puede solicitar reinscripción
		verificarHabilitarReinscripcion(doc);
	}

	private void displayCalificaciones(List<String> materias, Map<String, Map<String, Long>> calificaciones) {
		ViewGroup container = findViewById(R.id.containerCalificaciones);
		container.removeAllViews();

		for (String materia : materias) {
			View itemView = LayoutInflater.from(this)
				.inflate(R.layout.item_materia_calificacion, container, false);

			TextView tvMateria = itemView.findViewById(R.id.tvMateria);
			TextView tvCalificacion = itemView.findViewById(R.id.tvCalificacion);

			tvMateria.setText(materia);

			// Obtener parciales para esta materia
			Map<String, Long> parciales = calificaciones != null ? calificaciones.get(materia) : null;
			
			if (parciales != null) {
				Long parcial1 = parciales.get("parcial1");
				Long parcial2 = parciales.get("parcial2");
				Long parcial3 = parciales.get("parcial3");
				
				StringBuilder califText = new StringBuilder();
				califText.append("P1: ").append(parcial1 != null ? parcial1 : "-");
				califText.append(" | P2: ").append(parcial2 != null ? parcial2 : "-");
				califText.append(" | P3: ").append(parcial3 != null ? parcial3 : "-");
				
				// Calcular promedio si hay al menos un parcial
				int count = 0;
				double suma = 0.0;
				if (parcial1 != null) { suma += parcial1; count++; }
				if (parcial2 != null) { suma += parcial2; count++; }
				if (parcial3 != null) { suma += parcial3; count++; }
				
				if (count > 0) {
					double promedio = suma / count;
					califText.append("\nPromedio: ").append(String.format("%.1f", promedio));
				}
				
				tvCalificacion.setText(califText.toString());
			} else {
				tvCalificacion.setText("Sin calificaciones");
			}

			container.addView(itemView);
		}
	}

	private void imprimirPDF() {
		if (alumnoDoc == null || nombreAlumno == null) {
			Toast.makeText(this, "Error: No hay datos del alumno", Toast.LENGTH_SHORT).show();
			return;
		}

		try {
			// Crear documento PDF
			Document document = new Document(PageSize.A4);
			
			// Usar directorio de la app (no requiere permisos en Android 10+)
			// Intentar usar getExternalFilesDir primero (accesible desde el explorador de archivos)
			File pdfDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
			if (pdfDir == null) {
				// Si no está disponible, usar directorio interno de la app
				pdfDir = getFilesDir();
			}
			
			// Crear subdirectorio AppKardex
			File appKardexDir = new File(pdfDir, "AppKardex");
			if (!appKardexDir.exists()) {
				appKardexDir.mkdirs();
			}

			// Nombre del archivo con timestamp
			String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
			String fileName = "Kardex_" + (matriculaAlumno != null ? matriculaAlumno : "alumno") + "_" + timestamp + ".pdf";
			File pdfFile = new File(appKardexDir, fileName);

			PdfWriter.getInstance(document, new FileOutputStream(pdfFile));
			document.open();

			// Configurar fuentes
			Font titleFont = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD);
			Font headerFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
			Font normalFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);
			Font smallFont = new Font(Font.FontFamily.HELVETICA, 10, Font.ITALIC);

			// Título
			Paragraph title = new Paragraph("KARDEX ACADÉMICO", titleFont);
			title.setAlignment(Element.ALIGN_CENTER);
			title.setSpacingAfter(20);
			document.add(title);

			// Información del alumno
			document.add(new Paragraph("INFORMACIÓN DEL ALUMNO", headerFont));
			document.add(new Paragraph(" "));
			
			document.add(new Paragraph("Nombre: " + nombreAlumno, normalFont));
			document.add(new Paragraph("Matrícula: " + (matriculaAlumno != null ? matriculaAlumno : "N/A"), normalFont));
			document.add(new Paragraph("Carrera: " + (carreraAlumno != null ? carreraAlumno : "N/A"), normalFont));
			document.add(new Paragraph("Grupo: " + (grupoAlumno != null ? grupoAlumno : "N/A"), normalFont));
			
			// Obtener semestre y parcial
			String semestre = alumnoDoc.getString("semestre");
			String parcial = alumnoDoc.getString("parcial");
			if (semestre != null) {
				document.add(new Paragraph("Semestre: " + semestre, normalFont));
			}
			if (parcial != null) {
				document.add(new Paragraph("Parcial: " + parcial, normalFont));
			}

			document.add(new Paragraph(" "));
			document.add(new Paragraph(" "));

			// Calificaciones
			document.add(new Paragraph("CALIFICACIONES", headerFont));
			document.add(new Paragraph(" "));

			if (materias != null && !materias.isEmpty()) {
				// Crear tabla de calificaciones
				com.itextpdf.text.pdf.PdfPTable table = new com.itextpdf.text.pdf.PdfPTable(2);
				table.setWidthPercentage(100);
				table.setWidths(new float[]{5, 5});

				// Encabezados
				com.itextpdf.text.pdf.PdfPCell cell;
				cell = new com.itextpdf.text.pdf.PdfPCell(new Phrase("Materia", headerFont));
				cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
				cell.setPadding(8);
				table.addCell(cell);

				cell = new com.itextpdf.text.pdf.PdfPCell(new Phrase("Calificación", headerFont));
				cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
				cell.setPadding(8);
				table.addCell(cell);

				// Datos
				for (String materia : materias) {
					table.addCell(new com.itextpdf.text.pdf.PdfPCell(new Phrase(materia, normalFont)));
					
					// Obtener parciales
					Map<String, Long> parciales = calificacionesAlumno != null ? calificacionesAlumno.get(materia) : null;
					String califText;
					if (parciales != null) {
						Long p1 = parciales.get("parcial1");
						Long p2 = parciales.get("parcial2");
						Long p3 = parciales.get("parcial3");
						
						StringBuilder sb = new StringBuilder();
						sb.append("P1: ").append(p1 != null ? p1 : "-");
						sb.append(", P2: ").append(p2 != null ? p2 : "-");
						sb.append(", P3: ").append(p3 != null ? p3 : "-");
						
						// Calcular promedio
						int count = 0;
						double suma = 0.0;
						if (p1 != null) { suma += p1; count++; }
						if (p2 != null) { suma += p2; count++; }
						if (p3 != null) { suma += p3; count++; }
						if (count > 0) {
							double promedio = suma / count;
							sb.append("\nProm: ").append(String.format("%.1f", promedio));
						}
						califText = sb.toString();
					} else {
						califText = "Sin calificar";
					}
					table.addCell(new com.itextpdf.text.pdf.PdfPCell(new Phrase(califText, normalFont)));
				}

				document.add(table);
			} else {
				document.add(new Paragraph("No hay materias registradas", normalFont));
			}

			document.add(new Paragraph(" "));
			document.add(new Paragraph(" "));

			// Pie de página
			SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
			Paragraph footer = new Paragraph("Generado el: " + dateFormat.format(new Date()), smallFont);
			footer.setAlignment(Element.ALIGN_CENTER);
			document.add(footer);

			document.close();

			// Compartir el PDF usando FileProvider
			android.net.Uri pdfUri;
			try {
				// Intentar usar FileProvider (requiere configuración en AndroidManifest)
				pdfUri = androidx.core.content.FileProvider.getUriForFile(this,
					getPackageName() + ".fileprovider", pdfFile);
			} catch (Exception e) {
				// Si FileProvider no está configurado, usar método alternativo
				android.util.Log.w("AlumnoPanel", "FileProvider no configurado, usando método alternativo");
				
				// Crear URI usando content://
				android.content.ContentValues values = new android.content.ContentValues();
				values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName);
				values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
				
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
					values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/AppKardex");
					android.net.Uri uri = getContentResolver().insert(
						android.provider.MediaStore.Files.getContentUri("external"), values);
					
					if (uri != null) {
						try (java.io.OutputStream out = getContentResolver().openOutputStream(uri);
						     java.io.FileInputStream in = new java.io.FileInputStream(pdfFile)) {
							byte[] buffer = new byte[8192];
							int bytesRead;
							while ((bytesRead = in.read(buffer)) != -1) {
								if (out != null) {
									out.write(buffer, 0, bytesRead);
								}
							}
							pdfUri = uri;
						} catch (IOException ioException) {
							android.util.Log.e("AlumnoPanel", "Error al copiar PDF", ioException);
							pdfUri = null;
						}
					} else {
						pdfUri = null;
					}
				} else {
					pdfUri = null;
				}
			}
			
			if (pdfUri != null) {
				// Intentar abrir el PDF
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setDataAndType(pdfUri, "application/pdf");
				intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_GRANT_READ_URI_PERMISSION);
				
				try {
					startActivity(Intent.createChooser(intent, "Abrir PDF con..."));
					Toast.makeText(this, "PDF generado exitosamente", Toast.LENGTH_SHORT).show();
				} catch (Exception e) {
					// Si no se puede abrir, compartir el archivo
					Intent shareIntent = new Intent(Intent.ACTION_SEND);
					shareIntent.setType("application/pdf");
					shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri);
					shareIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
					startActivity(Intent.createChooser(shareIntent, "Compartir PDF"));
					Toast.makeText(this, "PDF generado. Compártelo desde el menú.", Toast.LENGTH_LONG).show();
				}
			} else {
				// Fallback: solo mostrar mensaje con la ubicación
				String ruta = pdfFile.getAbsolutePath();
				Toast.makeText(this, "PDF guardado exitosamente\n" + ruta, Toast.LENGTH_LONG).show();
				android.util.Log.d("AlumnoPanel", "PDF guardado en: " + ruta);
				
				// Intentar compartir el archivo directamente
				Intent shareIntent = new Intent(Intent.ACTION_SEND);
				shareIntent.setType("application/pdf");
				try {
					shareIntent.putExtra(Intent.EXTRA_STREAM, 
						androidx.core.content.FileProvider.getUriForFile(this,
							getPackageName() + ".fileprovider", pdfFile));
					shareIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
					startActivity(Intent.createChooser(shareIntent, "Compartir PDF"));
				} catch (Exception e) {
					android.util.Log.w("AlumnoPanel", "No se pudo compartir el PDF", e);
				}
			}

		} catch (DocumentException | IOException e) {
			Toast.makeText(this, "Error al generar PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
			android.util.Log.e("AlumnoPanel", "Error al generar PDF", e);
		}
	}

	private void verificarHabilitarReinscripcion(DocumentSnapshot doc) {
		// Verificar si ya tiene una solicitud pendiente
		String solicitudReinscripcion = doc.getString("solicitudReinscripcion");
		
		if ("pendiente".equals(solicitudReinscripcion)) {
			btnReinscripcion.setVisibility(View.VISIBLE);
			btnReinscripcion.setText("Solicitud de Reinscripción Enviada");
			btnReinscripcion.setEnabled(false);
			return;
		}
		
		if ("aprobada".equals(solicitudReinscripcion)) {
			// Ya fue aprobada, no mostrar botón
			btnReinscripcion.setVisibility(View.GONE);
			return;
		}
		
		// Verificar si está en el último semestre (semestre 4)
		if (semestreActual == null || Integer.parseInt(semestreActual) >= 4) {
			btnReinscripcion.setVisibility(View.GONE);
			return;
		}
		
		// Verificar si aprobó todas las materias
		if (verificarTodasMateriasAprobadas()) {
			btnReinscripcion.setVisibility(View.VISIBLE);
			btnReinscripcion.setEnabled(true);
		} else {
			btnReinscripcion.setVisibility(View.GONE);
		}
	}
	
	private boolean verificarTodasMateriasAprobadas() {
		if (materias == null || materias.isEmpty() || calificacionesAlumno == null) {
			return false;
		}
		
		// Verificar que todas las materias tengan calificaciones
		for (String materia : materias) {
			Map<String, Long> parciales = calificacionesAlumno.get(materia);
			if (parciales == null || parciales.isEmpty()) {
				return false;
			}
			
			// Obtener los 3 parciales
			Long p1 = parciales.get("parcial1");
			Long p2 = parciales.get("parcial2");
			Long p3 = parciales.get("parcial3");
			
			// Verificar que los 3 parciales estén completos (no null)
			if (p1 == null || p2 == null || p3 == null) {
				return false;
			}
			
			// Calcular promedio
			double promedio = (p1 + p2 + p3) / 3.0;
			
			// Si tiene menos de 6 de promedio, no aprobó
			if (promedio < 6.0) {
				return false;
			}
		}
		
		return true;
	}
	
	private void solicitarReinscripcion() {
		// Enviar solicitud a Firebase
		Map<String, Object> updates = new HashMap<>();
		updates.put("solicitudReinscripcion", "pendiente");
		updates.put("fechaSolicitudReinscripcion", com.google.firebase.firestore.FieldValue.serverTimestamp());
		
		db.collection("alumnos").document(user.getUid()).update(updates)
			.addOnCompleteListener(task -> {
				if (task.isSuccessful()) {
					Toast.makeText(this, "Solicitud de reinscripción enviada al administrador", Toast.LENGTH_LONG).show();
					btnReinscripcion.setText("Solicitud Enviada");
					btnReinscripcion.setEnabled(false);
				} else {
					Toast.makeText(this, "Error al enviar solicitud", Toast.LENGTH_SHORT).show();
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
