package com.mhrc.appkardex;

import android.content.Context;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper para gestionar materias por semestre y carrera
 */
public class MateriasHelper {
	
	// Estructura: carrera -> semestre -> lista de materias
	private static Map<String, Map<Integer, List<String>>> materiasPorCarrera = new HashMap<>();
	
	/**
	 * Inicializa las materias para cada carrera y semestre
	 */
	public static void inicializar(Context context) {
		// PROGRAMACIÓN
		Map<Integer, List<String>> programacion = new HashMap<>();
		
		// Semestre 1 - PROGRAMACIÓN
		List<String> progSem1 = new ArrayList<>();
		progSem1.add(context.getString(R.string.materia_programacion));
		progSem1.add(context.getString(R.string.materia_bases_datos));
		progSem1.add(context.getString(R.string.materia_estructuras));
		progSem1.add(context.getString(R.string.materia_disenno));
		
		// Semestre 2 - PROGRAMACIÓN
		List<String> progSem2 = new ArrayList<>();
		progSem2.add(context.getString(R.string.materia_poo));
		progSem2.add(context.getString(R.string.materia_calculo_avanzado));
		progSem2.add(context.getString(R.string.materia_estructuras_discretas));
		progSem2.add(context.getString(R.string.materia_fisica_general));
		progSem2.add(context.getString(R.string.materia_bases_datos_i));
		
		// Semestre 3 - PROGRAMACIÓN
		List<String> progSem3 = new ArrayList<>();
		progSem3.add(context.getString(R.string.materia_analisis_algoritmos));
		progSem3.add(context.getString(R.string.materia_bases_datos_ii));
		progSem3.add(context.getString(R.string.materia_calculo_iii));
		progSem3.add(context.getString(R.string.materia_arquitectura_computadoras));
		progSem3.add(context.getString(R.string.materia_ingenieria_software_i));
		
		// Semestre 4 - PROGRAMACIÓN
		List<String> progSem4 = new ArrayList<>();
		progSem4.add(context.getString(R.string.materia_sistemas_operativos));
		progSem4.add(context.getString(R.string.materia_redes_computadoras));
		progSem4.add(context.getString(R.string.materia_estructuras_datos_ii));
		progSem4.add(context.getString(R.string.materia_algebra_lineal_prog));
		progSem4.add(context.getString(R.string.materia_desarrollo_web));
		
		programacion.put(1, progSem1);
		programacion.put(2, progSem2);
		programacion.put(3, progSem3);
		programacion.put(4, progSem4);
		materiasPorCarrera.put("Programación", programacion);
		
		// INGENIERÍA CIVIL
		Map<Integer, List<String>> ingenieria = new HashMap<>();
		
		// Semestre 1 - INGENIERÍA CIVIL
		List<String> ingSem1 = new ArrayList<>();
		ingSem1.add(context.getString(R.string.materia_matematicas));
		ingSem1.add(context.getString(R.string.materia_estadistica));
		ingSem1.add(context.getString(R.string.materia_resistencia));
		ingSem1.add(context.getString(R.string.materia_topografia));
		
		// Semestre 2 - INGENIERÍA CIVIL
		List<String> ingSem2 = new ArrayList<>();
		ingSem2.add(context.getString(R.string.materia_calculo_vectorial));
		ingSem2.add(context.getString(R.string.materia_algebra_lineal_ing));
		ingSem2.add(context.getString(R.string.materia_estatica));
		ingSem2.add(context.getString(R.string.materia_topografia_2));
		ingSem2.add(context.getString(R.string.materia_quimica_general));
		
		// Semestre 3 - INGENIERÍA CIVIL
		List<String> ingSem3 = new ArrayList<>();
		ingSem3.add(context.getString(R.string.materia_cinematica_dinamica));
		ingSem3.add(context.getString(R.string.materia_mecanica_materiales_i));
		ingSem3.add(context.getString(R.string.materia_analisis_estructural_i));
		ingSem3.add(context.getString(R.string.materia_probabilidad_estadistica));
		ingSem3.add(context.getString(R.string.materia_geologia_aplicada));
		
		// Semestre 4 - INGENIERÍA CIVIL
		List<String> ingSem4 = new ArrayList<>();
		ingSem4.add(context.getString(R.string.materia_ecuaciones_diferenciales));
		ingSem4.add(context.getString(R.string.materia_hidraulica_basica));
		ingSem4.add(context.getString(R.string.materia_mecanica_materiales_ii));
		ingSem4.add(context.getString(R.string.materia_metodos_numericos));
		ingSem4.add(context.getString(R.string.materia_tecnologia_concreto));
		
		ingenieria.put(1, ingSem1);
		ingenieria.put(2, ingSem2);
		ingenieria.put(3, ingSem3);
		ingenieria.put(4, ingSem4);
		materiasPorCarrera.put("Ingeniería Civil", ingenieria);
		
		// ARQUITECTURA
		Map<Integer, List<String>> arquitectura = new HashMap<>();
		
		// Semestre 1 - ARQUITECTURA
		List<String> arqSem1 = new ArrayList<>();
		arqSem1.add(context.getString(R.string.materia_dibujo));
		arqSem1.add(context.getString(R.string.materia_historia));
		arqSem1.add(context.getString(R.string.materia_construccion));
		arqSem1.add(context.getString(R.string.materia_urbanismo));
		
		// Semestre 2 - ARQUITECTURA
		List<String> arqSem2 = new ArrayList<>();
		arqSem2.add(context.getString(R.string.materia_taller_diseno_ii));
		arqSem2.add(context.getString(R.string.materia_expresion_grafica_ii));
		arqSem2.add(context.getString(R.string.materia_geometria_descriptiva_ii));
		arqSem2.add(context.getString(R.string.materia_sistemas_estructurales));
		arqSem2.add(context.getString(R.string.materia_historia_universal_i));
		
		// Semestre 3 - ARQUITECTURA
		List<String> arqSem3 = new ArrayList<>();
		arqSem3.add(context.getString(R.string.materia_taller_diseno_iii));
		arqSem3.add(context.getString(R.string.materia_construccion_i));
		arqSem3.add(context.getString(R.string.materia_topografia_arq));
		arqSem3.add(context.getString(R.string.materia_historia_arquitectura_ii));
		arqSem3.add(context.getString(R.string.materia_teoria_arquitectura_i));
		
		// Semestre 4 - ARQUITECTURA
		List<String> arqSem4 = new ArrayList<>();
		arqSem4.add(context.getString(R.string.materia_taller_diseno_iv));
		arqSem4.add(context.getString(R.string.materia_resistencia_materiales_arq));
		arqSem4.add(context.getString(R.string.materia_instalaciones_i));
		arqSem4.add(context.getString(R.string.materia_historia_mexico));
		arqSem4.add(context.getString(R.string.materia_diseno_asistido));
		
		arquitectura.put(1, arqSem1);
		arquitectura.put(2, arqSem2);
		arquitectura.put(3, arqSem3);
		arquitectura.put(4, arqSem4);
		materiasPorCarrera.put("Arquitectura", arquitectura);
	}
	
	/**
	 * Obtiene las materias de un semestre específico para una carrera
	 */
	public static List<String> getMateriasPorSemestre(String carrera, int semestre) {
		if (materiasPorCarrera.containsKey(carrera)) {
			Map<Integer, List<String>> semestres = materiasPorCarrera.get(carrera);
			if (semestres.containsKey(semestre)) {
				return new ArrayList<>(semestres.get(semestre));
			}
		}
		return new ArrayList<>();
	}
	
	/**
	 * Valida que un alumno haya aprobado todas las materias del semestre anterior
	 * Retorna true si todas las materias tienen >= 6, false en caso contrario
	 */
	public static boolean validarAprobacionSemestreAnterior(
		String carrera, int semestreActual, Map<String, Map<String, Long>> calificaciones) {
		
		if (semestreActual <= 1) {
			// Si es semestre 1, no hay semestre anterior que validar
			return true;
		}
		
		int semestreAnterior = semestreActual - 1;
		List<String> materiasAnteriores = getMateriasPorSemestre(carrera, semestreAnterior);
		
		if (materiasAnteriores.isEmpty()) {
			// Si no hay materias del semestre anterior, permitir inscripción
			return true;
		}
		
		// Validar que todas las materias del semestre anterior tengan promedio >= 6
		for (String materia : materiasAnteriores) {
			Map<String, Long> parciales = calificaciones != null ? calificaciones.get(materia) : null;
			
			if (parciales == null || parciales.isEmpty()) {
				// Si no hay calificaciones, no puede avanzar
				return false;
			}
			
			// Calcular promedio de los 3 parciales
			Long parcial1 = parciales.get("parcial1");
			Long parcial2 = parciales.get("parcial2");
			Long parcial3 = parciales.get("parcial3");
			
			double promedio = 0.0;
			int count = 0;
			
			if (parcial1 != null) {
				promedio += parcial1;
				count++;
			}
			if (parcial2 != null) {
				promedio += parcial2;
				count++;
			}
			if (parcial3 != null) {
				promedio += parcial3;
				count++;
			}
			
			if (count > 0) {
				promedio = promedio / count;
			}
			
			// Si el promedio es menor a 6, no puede avanzar
			if (promedio < 6.0) {
				return false;
			}
		}
		
		return true;
	}
}

