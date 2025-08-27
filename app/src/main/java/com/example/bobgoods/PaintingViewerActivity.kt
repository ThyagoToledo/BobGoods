package com.example.bobgoods

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class PaintingViewerActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var textInfo: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_painting_viewer)

        setupViews()
        loadPainting()
    }

    private fun setupViews() {
        imageView = findViewById(R.id.imageViewFullPainting)
        textInfo = findViewById(R.id.textPaintingInfo)

        findViewById<ImageView>(R.id.btnBackFromViewer).setOnClickListener {
            finish()
        }
    }

    private fun loadPainting() {
        val paintingPath = intent.getStringExtra("painting_path")

        if (paintingPath != null) {
            val file = File(paintingPath)

            if (file.exists()) {
                try {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    imageView.setImageBitmap(bitmap)

                    // Mostra informações do arquivo
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.getDefault())
                    val fileSize = String.format("%.1f KB", file.length() / 1024.0)
                    textInfo.text = "Criada em ${dateFormat.format(Date(file.lastModified()))}\nTamanho: $fileSize"

                } catch (e: Exception) {
                    // Removida notificação de erro - fecha silenciosamente se houver problema
                    finish()
                }
            } else {
                // Removida notificação "Arquivo não encontrado" - fecha silenciosamente
                finish()
            }
        } else {
            // Removida notificação "Caminho inválido" - fecha silenciosamente
            finish()
        }
    }
}
