package com.example.bobgoods

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.content.ContextCompat
import java.util.*
import kotlin.math.max
import kotlin.math.min

class DrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var drawPath: Path = Path()
    private var drawPaint: Paint = Paint()
    private var canvasPaint: Paint = Paint(Paint.DITHER_FLAG)
    private var drawCanvas: Canvas? = null
    private var canvasBitmap: Bitmap? = null
    
    private var currentColor: Int = Color.BLACK // Padrão preto
    private val brushSize = 8f // Tamanho menor para desenhar linhas
    private var currentImageResource: Int = R.drawable.primeira_pintura
    private var paintMode: PaintMode = PaintMode.BUCKET_FILL

    // Variáveis para zoom e pan com gestos
    private var scaleFactor = 1.0f
    private var scaleGestureDetector: ScaleGestureDetector
    private var focusX = 0f
    private var focusY = 0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    private var isPanning = false
    private var matrix = Matrix()
    private var savedMatrix = Matrix()

    // Constantes de zoom
    private val MIN_ZOOM = 0.5f
    private val MAX_ZOOM = 5.0f

    // Enum para diferentes modos de pintura
    enum class PaintMode {
        BRUSH,
        BUCKET_FILL
    }

    init {
        setupDrawing()
        scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
    }

    private fun setupDrawing() {
        drawPaint.color = currentColor
        drawPaint.isAntiAlias = true
        drawPaint.strokeWidth = brushSize
        drawPaint.style = Paint.Style.STROKE
        drawPaint.strokeJoin = Paint.Join.ROUND
        drawPaint.strokeCap = Paint.Cap.ROUND
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        drawCanvas = Canvas(canvasBitmap!!)
        loadSelectedImage()
    }

    private fun loadSelectedImage() {
        drawCanvas?.drawColor(Color.WHITE)

        try {
            val drawable = ContextCompat.getDrawable(context, currentImageResource)
            drawable?.let {
                val drawableWidth = width - 40
                val drawableHeight = height - 40
                val left = 20
                val top = 20

                it.setBounds(left, top, left + drawableWidth, top + drawableHeight)
                it.draw(drawCanvas!!)
            }
        } catch (e: Exception) {
            drawBobbieSilhouette()
        }
    }

    private fun drawBobbieSilhouette() {
        val paint = Paint().apply {
            color = Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = 3f
            pathEffect = DashPathEffect(floatArrayOf(10f, 5f), 0f)
        }

        val centerX = width / 2f
        val centerY = height / 2f

        // Desenha uma forma simples do fazendeiro
        drawCanvas?.apply {
            // Cabeça
            drawCircle(centerX, centerY - 100, 40f, paint)
            // Corpo
            drawRect(centerX - 30, centerY - 60, centerX + 30, centerY + 60, paint)
            // Braços
            drawLine(centerX - 30, centerY - 20, centerX - 60, centerY + 10, paint)
            drawLine(centerX + 30, centerY - 20, centerX + 60, centerY + 10, paint)
            // Pernas
            drawLine(centerX - 15, centerY + 60, centerX - 15, centerY + 120, paint)
            drawLine(centerX + 15, centerY + 60, centerX + 15, centerY + 120, paint)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.save()
        canvas.concat(matrix)

        canvasBitmap?.let { canvas.drawBitmap(it, 0f, 0f, canvasPaint) }
        canvas.drawPath(drawPath, drawPaint)

        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Primeiro processa gestos de escala
        scaleGestureDetector.onTouchEvent(event)

        // Se está fazendo zoom, não processa pintura
        if (scaleGestureDetector.isInProgress) {
            return true
        }

        // Converte coordenadas da tela para coordenadas do canvas
        val inverse = Matrix()
        matrix.invert(inverse)
        val touchPoint = floatArrayOf(event.x, event.y)
        inverse.mapPoints(touchPoint)
        val touchX = touchPoint[0]
        val touchY = touchPoint[1]

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activePointerId = event.getPointerId(0)
                lastTouchX = event.x
                lastTouchY = event.y

                if (paintMode == PaintMode.BUCKET_FILL) {
                    performBucketFill(touchX.toInt(), touchY.toInt())
                } else {
                    drawPath.moveTo(touchX, touchY)
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val pointerIndex = event.findPointerIndex(activePointerId)
                if (pointerIndex == -1) return true

                val x = event.getX(pointerIndex)
                val y = event.getY(pointerIndex)

                // Verifica se é um movimento de pan (arrastar para mover a imagem)
                if (event.pointerCount == 1 && isPanning) {
                    val dx = x - lastTouchX
                    val dy = y - lastTouchY
                    matrix.postTranslate(dx, dy)
                    lastTouchX = x
                    lastTouchY = y
                    invalidate()
                    return true
                }

                // Se não está fazendo pan e está no modo pincel, desenha
                if (!isPanning && paintMode == PaintMode.BRUSH) {
                    drawPath.lineTo(touchX, touchY)
                    invalidate()
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (paintMode == PaintMode.BRUSH && !isPanning) {
                    drawCanvas?.drawPath(drawPath, drawPaint)
                    drawPath.reset()
                }
                activePointerId = MotionEvent.INVALID_POINTER_ID
                isPanning = false
                return true
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                // Segundo dedo tocou - ativa modo pan
                isPanning = true
                return true
            }

            MotionEvent.ACTION_POINTER_UP -> {
                // Um dedo foi levantado
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)

                if (pointerId == activePointerId) {
                    // O dedo principal foi levantado, escolhe outro
                    val newPointerIndex = if (pointerIndex == 0) 1 else 0
                    lastTouchX = event.getX(newPointerIndex)
                    lastTouchY = event.getY(newPointerIndex)
                    activePointerId = event.getPointerId(newPointerIndex)
                }

                // Se só resta um dedo, desativa pan
                if (event.pointerCount == 2) {
                    isPanning = false
                }
                return true
            }
        }

        invalidate()
        return true
    }

    private fun performBucketFill(x: Int, y: Int) {
        canvasBitmap?.let { bitmap ->
            if (x >= 0 && x < bitmap.width && y >= 0 && y < bitmap.height) {
                val targetColor = bitmap.getPixel(x, y)

                // Não pinta se já é da mesma cor
                if (targetColor == currentColor) return

                // Usa algoritmo melhorado de flood fill
                floodFillOptimized(bitmap, x, y, targetColor, currentColor)
                invalidate()
            }
        }
    }

    private fun floodFillOptimized(bitmap: Bitmap, startX: Int, startY: Int, targetColor: Int, fillColor: Int) {
        if (targetColor == fillColor) return

        val width = bitmap.width
        val height = bitmap.height
        val stack = ArrayDeque<Point>()
        val visited = BooleanArray(width * height)

        stack.push(Point(startX, startY))

        while (stack.isNotEmpty()) {
            val point = stack.pop()
            val x = point.x
            val y = point.y

            // Verifica limites
            if (x < 0 || x >= width || y < 0 || y >= height) continue

            val index = y * width + x
            if (visited[index]) continue

            val currentPixelColor = bitmap.getPixel(x, y)

            // Verifica se é a cor alvo com tolerância para anti-aliasing
            if (!isColorMatch(currentPixelColor, targetColor)) continue

            // Marca como visitado e pinta
            visited[index] = true
            bitmap.setPixel(x, y, fillColor)

            // Adiciona pixels adjacentes (4-conectividade)
            stack.push(Point(x + 1, y))
            stack.push(Point(x - 1, y))
            stack.push(Point(x, y + 1))
            stack.push(Point(x, y - 1))
        }
    }

    private fun isColorMatch(color1: Int, color2: Int): Boolean {
        // Verifica se as cores são exatamente iguais
        if (color1 == color2) return true

        // Para lidar com anti-aliasing, verifica similaridade de cores
        val threshold = 30

        val r1 = Color.red(color1)
        val g1 = Color.green(color1)
        val b1 = Color.blue(color1)
        val a1 = Color.alpha(color1)

        val r2 = Color.red(color2)
        val g2 = Color.green(color2)
        val b2 = Color.blue(color2)
        val a2 = Color.alpha(color2)

        return (kotlin.math.abs(r1 - r2) <= threshold &&
                kotlin.math.abs(g1 - g2) <= threshold &&
                kotlin.math.abs(b1 - b2) <= threshold &&
                kotlin.math.abs(a1 - a2) <= threshold)
    }

    fun loadImage(imageResource: Int) {
        currentImageResource = imageResource
        if (width > 0 && height > 0) {
            loadSelectedImage()
            invalidate()
        }
    }

    fun setCurrentColor(color: Int) {
        currentColor = color
        drawPaint.color = color
    }

    fun setPaintMode(mode: PaintMode) {
        paintMode = mode
        // Ajusta o tamanho do pincel baseado no modo
        drawPaint.strokeWidth = when (mode) {
            PaintMode.BRUSH -> brushSize
            PaintMode.BUCKET_FILL -> brushSize / 2 // Mais fino para desenhar linhas
        }
    }

    fun clearCanvas() {
        canvasBitmap?.let {
            drawCanvas?.drawColor(Color.WHITE)
            loadSelectedImage()
        }
        drawPath.reset()
        invalidate()
    }

    fun getBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        draw(canvas)
        return bitmap
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            scaleFactor = max(MIN_ZOOM, min(scaleFactor, MAX_ZOOM))

            focusX = detector.focusX
            focusY = detector.focusY

            matrix.postScale(detector.scaleFactor, detector.scaleFactor, focusX, focusY)
            invalidate()
            return true
        }

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            return true
        }
    }

    fun resetZoomAndPan() {
        matrix.reset()
        scaleFactor = 1.0f
        invalidate()
    }

    fun getCurrentZoom(): Float = scaleFactor
}
