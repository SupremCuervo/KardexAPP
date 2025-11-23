package com.mhrc.appkardex;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;

public class UserTypeSelectionActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_user_type_selection);

		Button btnAlumno = findViewById(R.id.btnAlumno);
		Button btnMaestro = findViewById(R.id.btnMaestro);
		Button btnAdmin = findViewById(R.id.btnAdmin);

		// Botón Alumno -> Registro de Alumno
		btnAlumno.setOnClickListener(v -> {
			Intent intent = new Intent(UserTypeSelectionActivity.this, SignInScreen.class);
			intent.putExtra("userType", "alumno");
			startActivity(intent);
		});

		// Botón Maestro -> Solicitar contraseña -> Registro de Maestro
		btnMaestro.setOnClickListener(v -> showPasswordDialog("maestro", "123456"));

		// Botón Admin -> Solicitar contraseña -> Registro de Admin
		btnAdmin.setOnClickListener(v -> showPasswordDialog("admin", "7890124"));
	}

	private void showPasswordDialog(String userType, String correctPassword) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.user_type_title));

		TextInputEditText input = new TextInputEditText(this);
		input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
		input.setHint("Contraseña");
		builder.setView(input);

		builder.setPositiveButton("Continuar", (dialog, which) -> {
			String password = input.getText().toString();
			if (password.equals(correctPassword)) {
				Intent intent = new Intent(UserTypeSelectionActivity.this, SignInScreen.class);
				intent.putExtra("userType", userType);
				startActivity(intent);
			} else {
				Toast.makeText(this, "Contraseña incorrecta", Toast.LENGTH_SHORT).show();
			}
		});

		builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

		builder.show();
	}
}

