package com.ludoproof.game.feature.characters.prototype

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.SystemClock
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal class Cat3DRenderer : GLSurfaceView.Renderer {
    private var program = 0
    private lateinit var sphere: SphereMesh

    private val projection = FloatArray(16)
    private val view = FloatArray(16)
    private val viewProjection = FloatArray(16)
    private val root = FloatArray(16)
    private val model = FloatArray(16)
    private val mvp = FloatArray(16)

    @Volatile
    private var motion: Cat3DMotion = Cat3DMotion.IDLE

    @Volatile
    private var motionStartedAtMillis: Long = SystemClock.uptimeMillis()

    fun play(
        next: Cat3DMotion,
    ) {
        motion = next
        motionStartedAtMillis = SystemClock.uptimeMillis()
    }

    override fun onSurfaceCreated(
        gl: javax.microedition.khronos.opengles.GL10?,
        config: javax.microedition.khronos.egl.EGLConfig?,
    ) {
        GLES30.glClearColor(0.055f, 0.075f, 0.11f, 1f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        sphere = SphereMesh(latitudeSegments = 18, longitudeSegments = 28)
    }

    override fun onSurfaceChanged(
        gl: javax.microedition.khronos.opengles.GL10?,
        width: Int,
        height: Int,
    ) {
        GLES30.glViewport(0, 0, width, height)
        val aspect = if (height == 0) 1f else width.toFloat() / height.toFloat()
        Matrix.perspectiveM(projection, 0, 34f, aspect, 0.1f, 100f)
        Matrix.setLookAtM(
            view,
            0,
            0f,
            0.82f,
            5.5f,
            0f,
            0.14f,
            0f,
            0f,
            1f,
            0f,
        )
        Matrix.multiplyMM(viewProjection, 0, projection, 0, view, 0)
    }

    override fun onDrawFrame(
        gl: javax.microedition.khronos.opengles.GL10?,
    ) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        GLES30.glUseProgram(program)

        val pose = currentPose()
        Matrix.setIdentityM(root, 0)
        Matrix.translateM(root, 0, 0f, pose.liftY, 0f)
        Matrix.rotateM(root, 0, pose.bodyYawDegrees, 0f, 1f, 0f)

        // Compact feline body with slightly larger rear haunches.
        drawPart(
            parent = root,
            translateX = -0.34f,
            translateY = -0.28f,
            translateZ = -0.13f,
            scaleX = 0.40f,
            scaleY = 0.50f,
            scaleZ = 0.48f,
            color = FUR_SILVER,
        )
        drawPart(
            parent = root,
            translateX = 0.34f,
            translateY = -0.28f,
            translateZ = -0.13f,
            scaleX = 0.40f,
            scaleY = 0.50f,
            scaleZ = 0.48f,
            color = FUR_SILVER,
        )
        drawPart(
            parent = root,
            translateX = 0f,
            translateY = -0.02f,
            translateZ = 0f,
            scaleX = 0.68f,
            scaleY = 0.75f,
            scaleZ = 0.56f,
            color = FUR_SILVER,
        )
        drawPart(
            parent = root,
            translateX = 0f,
            translateY = -0.12f,
            translateZ = 0.48f,
            scaleX = 0.34f,
            scaleY = 0.44f,
            scaleZ = 0.11f,
            color = FUR_CREAM,
        )

        // Four legs/paws; the paws are the lowest visible geometry, with no base underneath.
        drawLeg(root, -0.36f, -0.53f, 0.19f, -5f)
        drawLeg(root, 0.36f, -0.53f, 0.19f, 5f)
        drawLeg(root, -0.20f, -0.60f, 0.44f, -2f)
        drawLeg(root, 0.20f, -0.60f, 0.44f, 2f)

        // Blue collar marks the Blue Ludo seat while preserving natural cat fur.
        drawPart(
            parent = root,
            translateX = 0f,
            translateY = 0.42f,
            translateZ = 0.02f,
            scaleX = 0.49f,
            scaleY = 0.09f,
            scaleZ = 0.46f,
            color = COLLAR_BLUE,
        )
        drawPart(
            parent = root,
            translateX = 0f,
            translateY = 0.34f,
            translateZ = 0.47f,
            scaleX = 0.085f,
            scaleY = 0.11f,
            scaleZ = 0.06f,
            color = TAG_GOLD,
        )

        // Head and cheeks.
        drawPart(
            parent = root,
            translateX = 0f,
            translateY = 0.90f,
            translateZ = 0.02f,
            scaleX = 0.58f,
            scaleY = 0.55f,
            scaleZ = 0.53f,
            rotateZ = pose.headTiltDegrees,
            color = FUR_SILVER,
        )
        drawPart(
            parent = root,
            translateX = -0.17f,
            translateY = 0.79f,
            translateZ = 0.47f,
            scaleX = 0.24f,
            scaleY = 0.20f,
            scaleZ = 0.19f,
            color = FUR_CREAM,
        )
        drawPart(
            parent = root,
            translateX = 0.17f,
            translateY = 0.79f,
            translateZ = 0.47f,
            scaleX = 0.24f,
            scaleY = 0.20f,
            scaleZ = 0.19f,
            color = FUR_CREAM,
        )

        // Pink nose and tiny chin.
        drawPart(
            parent = root,
            translateX = 0f,
            translateY = 0.80f,
            translateZ = 0.63f,
            scaleX = 0.11f,
            scaleY = 0.085f,
            scaleZ = 0.085f,
            color = NOSE_PINK,
        )
        drawPart(
            parent = root,
            translateX = 0f,
            translateY = 0.65f,
            translateZ = 0.58f,
            scaleX = 0.10f,
            scaleY = 0.08f,
            scaleZ = 0.06f,
            color = FUR_CREAM,
        )

        drawEye(root, -0.21f)
        drawEye(root, 0.21f)

        // Tall ears with darker outer fur and pink inner ear shapes.
        drawEar(root, -0.39f, -18f - pose.earTwitchDegrees)
        drawEar(root, 0.39f, 18f + pose.earTwitchDegrees)

        // Subtle forehead stripes keep the face feline without looking like a flat icon.
        drawPart(
            parent = root,
            translateX = -0.17f,
            translateY = 1.19f,
            translateZ = 0.43f,
            scaleX = 0.045f,
            scaleY = 0.18f,
            scaleZ = 0.035f,
            rotateZ = -13f,
            color = STRIPE_DARK,
        )
        drawPart(
            parent = root,
            translateX = 0f,
            translateY = 1.23f,
            translateZ = 0.45f,
            scaleX = 0.042f,
            scaleY = 0.18f,
            scaleZ = 0.035f,
            color = STRIPE_DARK,
        )
        drawPart(
            parent = root,
            translateX = 0.17f,
            translateY = 1.19f,
            translateZ = 0.43f,
            scaleX = 0.045f,
            scaleY = 0.18f,
            scaleZ = 0.035f,
            rotateZ = 13f,
            color = STRIPE_DARK,
        )

        // Whiskers are thin, elongated 3D pieces rather than a texture.
        drawWhiskers(root, side = -1f)
        drawWhiskers(root, side = 1f)

        // Curved-looking tail from two linked elongated parts; both follow the same sway cue.
        drawPart(
            parent = root,
            translateX = 0.67f,
            translateY = -0.12f,
            translateZ = -0.31f,
            scaleX = 0.15f,
            scaleY = 0.48f,
            scaleZ = 0.14f,
            rotateZ = -50f + pose.tailSwayDegrees,
            color = FUR_SILVER,
        )
        drawPart(
            parent = root,
            translateX = 0.94f,
            translateY = 0.18f,
            translateZ = -0.31f,
            scaleX = 0.12f,
            scaleY = 0.32f,
            scaleZ = 0.12f,
            rotateZ = -62f + pose.tailSwayDegrees,
            color = STRIPE_DARK,
        )
    }

    private fun drawLeg(
        parent: FloatArray,
        x: Float,
        y: Float,
        z: Float,
        rotateZ: Float,
    ) {
        drawPart(
            parent = parent,
            translateX = x,
            translateY = y,
            translateZ = z,
            scaleX = 0.17f,
            scaleY = 0.30f,
            scaleZ = 0.17f,
            rotateZ = rotateZ,
            color = FUR_SILVER,
        )
        drawPart(
            parent = parent,
            translateX = x,
            translateY = y - 0.25f,
            translateZ = z + 0.055f,
            scaleX = 0.21f,
            scaleY = 0.115f,
            scaleZ = 0.25f,
            color = FUR_CREAM,
        )
    }

    private fun drawEye(
        parent: FloatArray,
        x: Float,
    ) {
        drawPart(
            parent = parent,
            translateX = x,
            translateY = 1.00f,
            translateZ = 0.47f,
            scaleX = 0.12f,
            scaleY = 0.15f,
            scaleZ = 0.075f,
            color = EYE_GREEN,
        )
        drawPart(
            parent = parent,
            translateX = x,
            translateY = 1.00f,
            translateZ = 0.535f,
            scaleX = 0.036f,
            scaleY = 0.105f,
            scaleZ = 0.018f,
            color = PUPIL_DARK,
        )
        drawPart(
            parent = parent,
            translateX = x - 0.028f,
            translateY = 1.055f,
            translateZ = 0.55f,
            scaleX = 0.022f,
            scaleY = 0.026f,
            scaleZ = 0.015f,
            color = WHITE,
        )
    }

    private fun drawEar(
        parent: FloatArray,
        x: Float,
        rotateZ: Float,
    ) {
        drawPart(
            parent = parent,
            translateX = x,
            translateY = 1.29f,
            translateZ = -0.02f,
            scaleX = 0.22f,
            scaleY = 0.42f,
            scaleZ = 0.20f,
            rotateZ = rotateZ,
            color = FUR_SILVER,
        )
        drawPart(
            parent = parent,
            translateX = x * 1.01f,
            translateY = 1.30f,
            translateZ = 0.11f,
            scaleX = 0.11f,
            scaleY = 0.27f,
            scaleZ = 0.07f,
            rotateZ = rotateZ,
            color = EAR_PINK,
        )
    }

    private fun drawWhiskers(
        parent: FloatArray,
        side: Float,
    ) {
        val x = 0.34f * side
        val angle = 78f * side
        drawPart(
            parent = parent,
            translateX = x,
            translateY = 0.79f,
            translateZ = 0.57f,
            scaleX = 0.018f,
            scaleY = 0.34f,
            scaleZ = 0.018f,
            rotateZ = angle,
            color = WHISKER_LIGHT,
        )
        drawPart(
            parent = parent,
            translateX = x,
            translateY = 0.70f,
            translateZ = 0.56f,
            scaleX = 0.018f,
            scaleY = 0.31f,
            scaleZ = 0.018f,
            rotateZ = (66f * side),
            color = WHISKER_LIGHT,
        )
    }

    private fun currentPose(): Cat3DPose {
        val now = SystemClock.uptimeMillis()
        val active = motion
        val duration = Cat3DMotionTimeline.durationMillis(active)
        val elapsed = (now - motionStartedAtMillis).coerceAtLeast(0L)

        if (active == Cat3DMotion.IDLE) {
            val loopProgress = (elapsed % duration).toFloat() / duration.toFloat()
            return Cat3DMotionTimeline.sample(Cat3DMotion.IDLE, loopProgress)
        }

        if (elapsed >= duration) {
            motion = Cat3DMotion.IDLE
            motionStartedAtMillis = now
            return Cat3DMotionTimeline.sample(Cat3DMotion.IDLE, 0f)
        }

        return Cat3DMotionTimeline.sample(
            active,
            elapsed.toFloat() / duration.toFloat(),
        )
    }

    private fun drawPart(
        parent: FloatArray,
        translateX: Float,
        translateY: Float,
        translateZ: Float,
        scaleX: Float,
        scaleY: Float,
        scaleZ: Float,
        color: FloatArray,
        rotateX: Float = 0f,
        rotateY: Float = 0f,
        rotateZ: Float = 0f,
    ) {
        System.arraycopy(parent, 0, model, 0, 16)
        Matrix.translateM(model, 0, translateX, translateY, translateZ)
        if (rotateX != 0f) Matrix.rotateM(model, 0, rotateX, 1f, 0f, 0f)
        if (rotateY != 0f) Matrix.rotateM(model, 0, rotateY, 0f, 1f, 0f)
        if (rotateZ != 0f) Matrix.rotateM(model, 0, rotateZ, 0f, 0f, 1f)
        Matrix.scaleM(model, 0, scaleX, scaleY, scaleZ)
        Matrix.multiplyMM(mvp, 0, viewProjection, 0, model, 0)
        sphere.draw(program, model, mvp, color)
    }

    private fun createProgram(
        vertexSource: String,
        fragmentSource: String,
    ): Int {
        val vertex = compileShader(GLES30.GL_VERTEX_SHADER, vertexSource)
        val fragment = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentSource)
        return GLES30.glCreateProgram().also { created ->
            GLES30.glAttachShader(created, vertex)
            GLES30.glAttachShader(created, fragment)
            GLES30.glLinkProgram(created)
            val status = IntArray(1)
            GLES30.glGetProgramiv(created, GLES30.GL_LINK_STATUS, status, 0)
            check(status[0] == GLES30.GL_TRUE) {
                "3D cat program link failed: ${GLES30.glGetProgramInfoLog(created)}"
            }
            GLES30.glDeleteShader(vertex)
            GLES30.glDeleteShader(fragment)
        }
    }

    private fun compileShader(
        type: Int,
        source: String,
    ): Int =
        GLES30.glCreateShader(type).also { shader ->
            GLES30.glShaderSource(shader, source)
            GLES30.glCompileShader(shader)
            val status = IntArray(1)
            GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, status, 0)
            check(status[0] == GLES30.GL_TRUE) {
                "3D cat shader compile failed: ${GLES30.glGetShaderInfoLog(shader)}"
            }
        }

    private class SphereMesh(
        latitudeSegments: Int,
        longitudeSegments: Int,
    ) {
        private val vertexBuffer: FloatBuffer
        private val indexBuffer: ShortBuffer
        private val indexCount: Int

        init {
            val vertices = mutableListOf<Float>()
            val indices = mutableListOf<Short>()

            for (lat in 0..latitudeSegments) {
                val theta = PI * lat.toDouble() / latitudeSegments.toDouble()
                val sinTheta = sin(theta).toFloat()
                val cosTheta = cos(theta).toFloat()
                for (lon in 0..longitudeSegments) {
                    val phi = 2.0 * PI * lon.toDouble() / longitudeSegments.toDouble()
                    val x = sinTheta * cos(phi).toFloat()
                    val y = cosTheta
                    val z = sinTheta * sin(phi).toFloat()
                    vertices += x
                    vertices += y
                    vertices += z
                    vertices += x
                    vertices += y
                    vertices += z
                }
            }

            val row = longitudeSegments + 1
            for (lat in 0 until latitudeSegments) {
                for (lon in 0 until longitudeSegments) {
                    val first = lat * row + lon
                    val second = first + row
                    indices += first.toShort()
                    indices += second.toShort()
                    indices += (first + 1).toShort()
                    indices += second.toShort()
                    indices += (second + 1).toShort()
                    indices += (first + 1).toShort()
                }
            }

            vertexBuffer =
                ByteBuffer
                    .allocateDirect(vertices.size * Float.SIZE_BYTES)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer()
                    .apply {
                        vertices.forEach(::put)
                        position(0)
                    }
            indexBuffer =
                ByteBuffer
                    .allocateDirect(indices.size * Short.SIZE_BYTES)
                    .order(ByteOrder.nativeOrder())
                    .asShortBuffer()
                    .apply {
                        indices.forEach(::put)
                        position(0)
                    }
            indexCount = indices.size
        }

        fun draw(
            program: Int,
            model: FloatArray,
            mvp: FloatArray,
            color: FloatArray,
        ) {
            val modelLocation = GLES30.glGetUniformLocation(program, "uModel")
            val mvpLocation = GLES30.glGetUniformLocation(program, "uMvp")
            val colorLocation = GLES30.glGetUniformLocation(program, "uColor")
            val lightLocation = GLES30.glGetUniformLocation(program, "uLightDirection")
            val cameraLocation = GLES30.glGetUniformLocation(program, "uCameraPosition")

            GLES30.glUniformMatrix4fv(modelLocation, 1, false, model, 0)
            GLES30.glUniformMatrix4fv(mvpLocation, 1, false, mvp, 0)
            GLES30.glUniform4fv(colorLocation, 1, color, 0)
            GLES30.glUniform3f(lightLocation, 0.40f, -1.0f, -0.65f)
            GLES30.glUniform3f(cameraLocation, 0f, 0.82f, 5.5f)

            vertexBuffer.position(0)
            GLES30.glEnableVertexAttribArray(0)
            GLES30.glVertexAttribPointer(
                0,
                3,
                GLES30.GL_FLOAT,
                false,
                6 * Float.SIZE_BYTES,
                vertexBuffer,
            )

            vertexBuffer.position(3)
            GLES30.glEnableVertexAttribArray(1)
            GLES30.glVertexAttribPointer(
                1,
                3,
                GLES30.GL_FLOAT,
                false,
                6 * Float.SIZE_BYTES,
                vertexBuffer,
            )

            indexBuffer.position(0)
            GLES30.glDrawElements(
                GLES30.GL_TRIANGLES,
                indexCount,
                GLES30.GL_UNSIGNED_SHORT,
                indexBuffer,
            )

            GLES30.glDisableVertexAttribArray(0)
            GLES30.glDisableVertexAttribArray(1)
        }
    }

    private companion object {
        val FUR_SILVER = floatArrayOf(0.60f, 0.63f, 0.68f, 1f)
        val FUR_CREAM = floatArrayOf(0.90f, 0.87f, 0.80f, 1f)
        val STRIPE_DARK = floatArrayOf(0.25f, 0.28f, 0.33f, 1f)
        val COLLAR_BLUE = floatArrayOf(0.05f, 0.38f, 0.95f, 1f)
        val TAG_GOLD = floatArrayOf(0.95f, 0.72f, 0.15f, 1f)
        val NOSE_PINK = floatArrayOf(0.95f, 0.49f, 0.56f, 1f)
        val EAR_PINK = floatArrayOf(0.91f, 0.55f, 0.61f, 1f)
        val EYE_GREEN = floatArrayOf(0.45f, 0.78f, 0.48f, 1f)
        val PUPIL_DARK = floatArrayOf(0.02f, 0.025f, 0.03f, 1f)
        val WHISKER_LIGHT = floatArrayOf(0.92f, 0.92f, 0.92f, 1f)
        val WHITE = floatArrayOf(1f, 1f, 1f, 1f)

        const val VERTEX_SHADER = """
            #version 300 es
            layout(location = 0) in vec3 aPosition;
            layout(location = 1) in vec3 aNormal;
            uniform mat4 uModel;
            uniform mat4 uMvp;
            out vec3 vNormal;
            out vec3 vWorldPosition;
            void main() {
                vec4 world = uModel * vec4(aPosition, 1.0);
                vWorldPosition = world.xyz;
                vNormal = mat3(transpose(inverse(uModel))) * aNormal;
                gl_Position = uMvp * vec4(aPosition, 1.0);
            }
        """

        const val FRAGMENT_SHADER = """
            #version 300 es
            precision mediump float;
            uniform vec4 uColor;
            uniform vec3 uLightDirection;
            uniform vec3 uCameraPosition;
            in vec3 vNormal;
            in vec3 vWorldPosition;
            out vec4 outColor;
            void main() {
                vec3 normal = normalize(vNormal);
                vec3 light = normalize(-uLightDirection);
                vec3 viewDir = normalize(uCameraPosition - vWorldPosition);
                vec3 halfDir = normalize(light + viewDir);
                float diffuse = max(dot(normal, light), 0.0);
                float specular = pow(max(dot(normal, halfDir), 0.0), 28.0) * 0.22;
                float rim = pow(1.0 - max(dot(normal, viewDir), 0.0), 2.2) * 0.12;
                vec3 rgb = uColor.rgb * (0.42 + diffuse * 0.58) + vec3(specular + rim);
                outColor = vec4(rgb, uColor.a);
            }
        """
    }
}
