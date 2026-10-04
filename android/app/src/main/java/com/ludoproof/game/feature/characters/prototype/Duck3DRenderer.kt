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

internal class Duck3DRenderer : GLSurfaceView.Renderer {
    private var program = 0
    private lateinit var sphere: SphereMesh

    private val projection = FloatArray(16)
    private val view = FloatArray(16)
    private val viewProjection = FloatArray(16)
    private val root = FloatArray(16)
    private val model = FloatArray(16)
    private val mvp = FloatArray(16)

    @Volatile
    private var motion: Duck3DMotion = Duck3DMotion.IDLE

    @Volatile
    private var motionStartedAtMillis: Long = SystemClock.uptimeMillis()

    fun play(
        next: Duck3DMotion,
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
        val aspect =
            if (height == 0) 1f else width.toFloat() / height.toFloat()
        Matrix.perspectiveM(
            projection,
            0,
            34f,
            aspect,
            0.1f,
            100f,
        )
        Matrix.setLookAtM(
            view,
            0,
            0f,
            0.85f,
            5.4f,
            0f,
            0.15f,
            0f,
            0f,
            1f,
            0f,
        )
        Matrix.multiplyMM(
            viewProjection,
            0,
            projection,
            0,
            view,
            0,
        )
    }

    override fun onDrawFrame(
        gl: javax.microedition.khronos.opengles.GL10?,
    ) {
        GLES30.glClear(
            GLES30.GL_COLOR_BUFFER_BIT or
                GLES30.GL_DEPTH_BUFFER_BIT,
        )
        GLES30.glUseProgram(program)

        val pose = currentPose()

        Matrix.setIdentityM(root, 0)
        Matrix.translateM(root, 0, 0f, pose.liftY, 0f)
        Matrix.rotateM(root, 0, pose.bodyYawDegrees, 0f, 1f, 0f)

        // Feet are now the lowest visible part of the character.
        drawPart(
            parent = root,
            translateX = -0.25f,
            translateY = -0.61f,
            translateZ = 0.18f,
            scaleX = 0.23f,
            scaleY = 0.095f,
            scaleZ = 0.30f,
            rotateY = -8f,
            color = ORANGE,
        )
        drawPart(
            parent = root,
            translateX = 0.25f,
            translateY = -0.61f,
            translateZ = 0.18f,
            scaleX = 0.23f,
            scaleY = 0.095f,
            scaleZ = 0.30f,
            rotateY = 8f,
            color = ORANGE,
        )

        // Body.
        drawPart(
            parent = root,
            translateX = 0f,
            translateY = -0.04f,
            translateZ = 0f,
            scaleX = 0.68f,
            scaleY = 0.78f,
            scaleZ = 0.58f,
            color = DUCK_YELLOW,
        )
        drawPart(
            parent = root,
            translateX = 0f,
            translateY = -0.14f,
            translateZ = 0.47f,
            scaleX = 0.40f,
            scaleY = 0.48f,
            scaleZ = 0.12f,
            color = BELLY_YELLOW,
        )

        // Wings with independent flap angles.
        drawPart(
            parent = root,
            translateX = -0.61f,
            translateY = 0.03f,
            translateZ = -0.02f,
            scaleX = 0.22f,
            scaleY = 0.47f,
            scaleZ = 0.31f,
            rotateZ = -20f - pose.wingFlapDegrees,
            color = DUCK_YELLOW,
        )
        drawPart(
            parent = root,
            translateX = 0.61f,
            translateY = 0.03f,
            translateZ = -0.02f,
            scaleX = 0.22f,
            scaleY = 0.47f,
            scaleZ = 0.31f,
            rotateZ = 20f + pose.wingFlapDegrees,
            color = DUCK_YELLOW,
        )

        // Head and face.
        drawPart(
            parent = root,
            translateX = 0f,
            translateY = 0.87f,
            translateZ = 0.02f,
            scaleX = 0.54f,
            scaleY = 0.53f,
            scaleZ = 0.52f,
            rotateZ = pose.headTiltDegrees,
            color = DUCK_YELLOW,
        )
        drawPart(
            parent = root,
            translateX = 0f,
            translateY = 0.77f,
            translateZ = 0.50f,
            scaleX = 0.44f,
            scaleY = 0.15f,
            scaleZ = 0.31f,
            color = ORANGE,
        )

        drawEye(root, x = -0.20f)
        drawEye(root, x = 0.20f)

        // Small feather tuft keeps the silhouette character-like rather than icon-like.
        drawPart(
            parent = root,
            translateX = -0.10f,
            translateY = 1.37f,
            translateZ = -0.01f,
            scaleX = 0.10f,
            scaleY = 0.22f,
            scaleZ = 0.09f,
            rotateZ = -18f,
            color = DUCK_YELLOW,
        )
        drawPart(
            parent = root,
            translateX = 0.07f,
            translateY = 1.39f,
            translateZ = -0.02f,
            scaleX = 0.09f,
            scaleY = 0.20f,
            scaleZ = 0.08f,
            rotateZ = 15f,
            color = DUCK_YELLOW,
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
            translateZ = 0.46f,
            scaleX = 0.115f,
            scaleY = 0.145f,
            scaleZ = 0.075f,
            color = EYE_DARK,
        )
        drawPart(
            parent = parent,
            translateX = x - 0.027f,
            translateY = 1.047f,
            translateZ = 0.525f,
            scaleX = 0.027f,
            scaleY = 0.035f,
            scaleZ = 0.020f,
            color = WHITE,
        )
    }

    private fun currentPose(): Duck3DPose {
        val now = SystemClock.uptimeMillis()
        val active = motion
        val duration = Duck3DMotionTimeline.durationMillis(active)
        val elapsed = (now - motionStartedAtMillis).coerceAtLeast(0L)

        if (active == Duck3DMotion.IDLE) {
            val loopProgress =
                (elapsed % duration).toFloat() / duration.toFloat()
            return Duck3DMotionTimeline.sample(
                Duck3DMotion.IDLE,
                loopProgress,
            )
        }

        if (elapsed >= duration) {
            motion = Duck3DMotion.IDLE
            motionStartedAtMillis = now
            return Duck3DMotionTimeline.sample(Duck3DMotion.IDLE, 0f)
        }

        return Duck3DMotionTimeline.sample(
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
        if (rotateX != 0f) {
            Matrix.rotateM(model, 0, rotateX, 1f, 0f, 0f)
        }
        if (rotateY != 0f) {
            Matrix.rotateM(model, 0, rotateY, 0f, 1f, 0f)
        }
        if (rotateZ != 0f) {
            Matrix.rotateM(model, 0, rotateZ, 0f, 0f, 1f)
        }
        Matrix.scaleM(model, 0, scaleX, scaleY, scaleZ)
        Matrix.multiplyMM(mvp, 0, viewProjection, 0, model, 0)
        sphere.draw(
            program = program,
            model = model,
            mvp = mvp,
            color = color,
        )
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
                "3D duck program link failed: ${GLES30.glGetProgramInfoLog(created)}"
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
                "3D duck shader compile failed: ${GLES30.glGetShaderInfoLog(shader)}"
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
                    val phi =
                        2.0 * PI * lon.toDouble() / longitudeSegments.toDouble()
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
            GLES30.glUniform3f(cameraLocation, 0f, 0.85f, 5.4f)

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
        val DUCK_YELLOW = floatArrayOf(1.00f, 0.78f, 0.08f, 1f)
        val BELLY_YELLOW = floatArrayOf(1.00f, 0.88f, 0.34f, 1f)
        val ORANGE = floatArrayOf(1.00f, 0.42f, 0.03f, 1f)
        val EYE_DARK = floatArrayOf(0.025f, 0.035f, 0.045f, 1f)
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
