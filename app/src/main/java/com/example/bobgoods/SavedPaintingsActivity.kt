package com.example.bobgoods

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class SavedPaintingsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SavedPaintingsAdapter
    private lateinit var emptyView: TextView
    private val savedPaintings = mutableListOf<File>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_paintings)

        setupViews()
        loadSavedPaintings()
    }

    private fun setupViews() {
        recyclerView = findViewById(R.id.recyclerViewPaintings)
        emptyView = findViewById(R.id.textEmptyGallery)

        // Configura o RecyclerView com grid de 2 colunas
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        adapter = SavedPaintingsAdapter(savedPaintings) { file ->
            openPaintingViewer(file)
        }
        recyclerView.adapter = adapter

        // Botão de voltar
        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    private fun loadSavedPaintings() {
        savedPaintings.clear()

        try {
            val picturesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            picturesDir?.let { dir ->
                val files = dir.listFiles { file ->
                    file.isFile && file.name.startsWith("bobbie_painting_") &&
                    (file.name.endsWith(".jpg") || file.name.endsWith(".png"))
                }

                files?.let {
                    // Ordena por data de modificação (mais recente primeiro)
                    savedPaintings.addAll(it.sortedByDescending { file -> file.lastModified() })
                }
            }
        } catch (e: Exception) {
            // Removida notificação de erro - falha silenciosa, lista fica vazia
        }

        // Atualiza a interface
        if (savedPaintings.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
        }

        adapter.notifyDataSetChanged()
    }

    private fun openPaintingViewer(file: File) {
        val intent = Intent(this, PaintingViewerActivity::class.java)
        intent.putExtra("painting_path", file.absolutePath)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        loadSavedPaintings() // Recarrega quando voltar à tela
    }
}

class SavedPaintingsAdapter(
    private val paintings: List<File>,
    private val onItemClick: (File) -> Unit
) : RecyclerView.Adapter<SavedPaintingsAdapter.PaintingViewHolder>() {

    class PaintingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageViewPainting)
        val textDate: TextView = itemView.findViewById(R.id.textDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaintingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_saved_painting, parent, false)
        return PaintingViewHolder(view)
    }

    override fun onBindViewHolder(holder: PaintingViewHolder, position: Int) {
        val file = paintings[position]

        // Carrega a imagem
        try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            holder.imageView.setImageBitmap(bitmap)
        } catch (e: Exception) {
            holder.imageView.setImageResource(R.drawable.ic_broken_image)
        }

        // Formata a data
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        holder.textDate.text = dateFormat.format(Date(file.lastModified()))

        // Configura o clique
        holder.itemView.setOnClickListener {
            onItemClick(file)
        }
    }

    override fun getItemCount(): Int = paintings.size
}
