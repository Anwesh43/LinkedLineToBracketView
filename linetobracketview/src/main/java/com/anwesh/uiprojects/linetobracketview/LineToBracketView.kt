package com.anwesh.uiprojects.linetobracketview

/**
 * Created by anweshmishra on 19/04/19.
 */

import android.view.View
import android.view.MotionEvent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.content.Context
import android.app.Activity

val nodes : Int = 5
val lines : Int = 2
val scGap : Float = 0.05f
val scDiv : Double = 0.51
val strokeFactor : Int = 90
val sizeFactor : Float = 2.9f
val foreColor : Int = Color.parseColor("#9C27B0")
val backColor : Int = Color.parseColor("#BDBDBD")
val lineF : Int = 4
val angleDeg : Float = 90f
val parts : Int = 2
val delay : Long = 20

fun Int.inverse() : Float = 1f / this
fun Float.maxScale(i : Int, n : Int) : Float = Math.max(0f, this - i * n.inverse())
fun Float.divideScale(i : Int, n : Int) : Float = Math.min(n.inverse(), maxScale(i, n)) * n
fun Float.scaleFactor() : Float = Math.floor(this / scDiv).toFloat()
fun Float.mirrorValue(a : Int, b : Int) : Float {
    val k : Float = scaleFactor()
    return (1 - k) * a.inverse() + k * b.inverse()
}
fun Float.updateValue(dir : Float, a : Int, b : Int) : Float = mirrorValue(a, b) * dir * scGap
fun Int.sjf() : Float = 1f - 2 * this

fun Canvas.drawBracket(size : Float, sc : Float, paint : Paint) {
    val lSize : Float = size / lineF
    val yOffset = size - lSize
    drawLine(0f, -yOffset, 0f, yOffset, paint)
    for (j in 0..(parts - 1)) {
        save()
        translate(0f, -yOffset * j.sjf())
        rotate(-angleDeg * sc.divideScale(j, parts) * j.sjf())
        drawLine(0f, 0f, 0f, -lSize * j.sjf(), paint)
        restore()
    }
}

fun Canvas.drawLineToBracket(size : Float, sc1 : Float, sc2 : Float, paint : Paint) {
    for (j in 0..(lines - 1)) {
        save()
        scale(j.sjf(), 1f)
        translate((size / 2) * sc1.divideScale(j, lines), 0f)
        drawBracket(size, sc2.divideScale(j, lines), paint)
        restore()
    }
}

fun Canvas.drawLTBNode(i : Int, scale : Float, paint : Paint) {
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    val gap : Float = h / (nodes + 1)
    val size : Float = gap / sizeFactor
    paint.color = foreColor
    paint.strokeWidth = Math.min(w, h) / strokeFactor
    paint.strokeCap = Paint.Cap.ROUND
    save()
    translate(w / 2, gap * (i + 1))
    drawLineToBracket(size, scale.divideScale(0, 2), scale.divideScale(1, 2), paint)
    restore()
}

class LineToBracketView(ctx : Context) : View(ctx) {

    private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val renderer : Renderer = Renderer(this)
    private var transformListener : TransformationListener? = null

    fun addTransformListener(lineToBracketCb: (Int) -> Unit, bracketToLineCb: (Int) -> Unit) {
        transformListener = TransformationListener(lineToBracketCb, bracketToLineCb)
    }

    fun transformFromLineToBracket(i : Int) {
        transformListener?.lineToBracketCb?.invoke(i)
    }

    fun transformFromBracketToLine(i : Int) {
        transformListener?.bracketToLineCb?.invoke(i)
    }

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas, paint)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                renderer.handleTap()
            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var dir : Float = 0f, var prevScale : Float = 0f) {

        fun update(cb : (Float) -> Unit) {
            scale += scale.updateValue(dir, lines, lines * parts)
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(delay)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class LTBNode(var i : Int, val state : State = State()) {

        private var next : LTBNode? = null
        private var prev : LTBNode? = null

        init {
            addNeighbor()
        }

        fun addNeighbor() {
            if (i < nodes - 1) {
                next = LTBNode(i + 1)
                next?.prev = this
            }
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawLTBNode(i, state.scale, paint)
            next?.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            state.update {
                cb(i, it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : LTBNode {
            var curr : LTBNode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class LineToBracket(var i : Int) {

        private val root : LTBNode = LTBNode(0)
        private var curr : LTBNode = root
        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            root.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            curr.update {i, scl ->
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                cb(i, scl)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer(var view : LineToBracketView) {

        private val animator : Animator = Animator(view)
        private val ltb : LineToBracket = LineToBracket(0)

        fun render(canvas : Canvas, paint : Paint) {
            canvas.drawColor(backColor)
            ltb.draw(canvas, paint)
            animator.animate {
                ltb.update {i, scl ->
                    animator.stop()
                    when(scl) {
                        0f -> view.transformFromBracketToLine(i)
                        1f -> view.transformFromLineToBracket(i)
                    }
                }
            }
        }

        fun handleTap() {
            ltb.startUpdating {
                animator.start()
            }
        }
    }

    companion object {

        fun create(activity : Activity) : LineToBracketView {
            val view : LineToBracketView = LineToBracketView(activity)
            activity.setContentView(view)
            return view
        }
    }
}

data class TransformationListener(var lineToBracketCb : (Int) -> Unit, var bracketToLineCb : (Int) -> Unit)