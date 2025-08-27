package com.example.bobgoods

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var drawingView: DrawingView
    private lateinit var btnClear: Button
    private lateinit var btnSave: Button
    private lateinit var btnToggleMode: Button
    private lateinit var btnGallery: Button
    private lateinit var btnResetZoom: Button
    private lateinit var colorButtons: List<Button>
    private lateinit var imageCard1: CardView
    private lateinit var imageCard2: CardView

    private var currentMode = DrawingView.PaintMode.BUCKET_FILL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupImageGallery()
        setupColorButtons()
        setupControlButtons()

        // Define modo balde de tinta por padrão
        drawingView.setPaintMode(currentMode)
        updateModeButtonText()
    }

    private fun initializeViews() {
        drawingView = findViewById(R.id.drawingView)
        btnClear = findViewById(R.id.btnClear)
        btnSave = findViewById(R.id.btnSave)
        btnToggleMode = findViewById(R.id.btnToggleMode)
        btnGallery = findViewById(R.id.btnGallery)
        btnResetZoom = findViewById(R.id.btnResetZoom)
        imageCard1 = findViewById(R.id.imageCard1)
        imageCard2 = findViewById(R.id.imageCard2)

        colorButtons = listOf(
            findViewById(R.id.colorBlack),
            findViewById(R.id.colorRed),
            findViewById(R.id.colorBlue),
            findViewById(R.id.colorYellow),
            findViewById(R.id.colorGreen),
            findViewById(R.id.colorPurple),
            findViewById(R.id.colorBrown)
        )
    }

    private fun setupImageGallery() {
        // Configura o clique na primeira imagem (foto real) - removidas notificações excessivas
        imageCard1.setOnClickListener {
            drawingView.loadImage(R.drawable.primeira_pintura)
            // Destaque visual da imagem selecionada
            resetImageSelection()
            imageCard1.cardElevation = 8f
            imageCard1.scaleX = 1.05f
            imageCard1.scaleY = 1.05f
        }

        // Configura o clique na segunda imagem (desenho vetorial) - removidas notificações excessivas
        imageCard2.setOnClickListener {
            drawingView.loadImage(R.drawable.bobbie_farmer)
            // Destaque visual da imagem selecionada
            resetImageSelection()
            imageCard2.cardElevation = 8f
            imageCard2.scaleX = 1.05f
            imageCard2.scaleY = 1.05f
        }

        // Carrega a primeira imagem por padrão
        drawingView.loadImage(R.drawable.primeira_pintura)
        imageCard1.cardElevation = 8f
        imageCard1.scaleX = 1.05f
        imageCard1.scaleY = 1.05f
    }

    private fun resetImageSelection() {
        // Remove destaque de todos os cards
        imageCard1.cardElevation = 4f
        imageCard1.scaleX = 1.0f
        imageCard1.scaleY = 1.0f

        imageCard2.cardElevation = 4f
        imageCard2.scaleX = 1.0f
        imageCard2.scaleY = 1.0f
    }

    private fun setupColorButtons() {
        val colors = listOf(
            Color.BLACK,      // Nova cor preta
            Color.RED,
            Color.BLUE,
            Color.YELLOW,
            Color.GREEN,
            Color.parseColor("#800080"), // Purple
            Color.parseColor("#8B4513")  // Brown
        )

        colorButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                drawingView.setCurrentColor(colors[index])
                // Removidas notificações excessivas
                updateColorButtonSelection(button)
            }
        }

        // Seleciona a primeira cor (preta) por padrão
        updateColorButtonSelection(colorButtons[0])
        drawingView.setCurrentColor(colors[0])
    }

    private fun updateColorButtonSelection(selectedButton: Button) {
        colorButtons.forEach { button ->
            button.alpha = if (button == selectedButton) 1.0f else 0.7f
            button.scaleX = if (button == selectedButton) 1.1f else 1.0f
            button.scaleY = if (button == selectedButton) 1.1f else 1.0f
        }
    }

    private fun setupControlButtons() {
        btnToggleMode.setOnClickListener {
            currentMode = when (currentMode) {
                DrawingView.PaintMode.BUCKET_FILL -> DrawingView.PaintMode.BRUSH
                DrawingView.PaintMode.BRUSH -> DrawingView.PaintMode.BUCKET_FILL
            }
            drawingView.setPaintMode(currentMode)
            updateModeButtonText()
            // Removida notificação desnecessária
        }

        btnClear.setOnClickListener {
            drawingView.clearCanvas()
            // Removida notificação desnecessária
        }

        btnSave.setOnClickListener {
            saveDrawing()
        }

        btnGallery.setOnClickListener {
            val intent = Intent(this, SavedPaintingsActivity::class.java)
            startActivity(intent)
        }

        btnResetZoom.setOnClickListener {
            drawingView.resetZoomAndPan()
            // Removida notificação "Zoom resetado" - ação é visualmente clara
        }
    }

    private fun updateModeButtonText() {
        btnToggleMode.text = when (currentMode) {
            DrawingView.PaintMode.BUCKET_FILL -> "Modo Balde"
            DrawingView.PaintMode.BRUSH -> "Modo Pincel"
        }
    }

    private fun saveDrawing() {
        val bitmap = drawingView.getBitmap()

        try {
            val fileName = "bobbie_painting_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.jpg"
            val file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), fileName)

            val fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
            fos.flush()
            fos.close()

            Toast.makeText(this, "Desenho salvo com sucesso!", Toast.LENGTH_SHORT).show()

            // Direciona automaticamente para a galeria após salvar
            val intent = Intent(this, SavedPaintingsActivity::class.java)
            startActivity(intent)

        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao salvar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}