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

internal class Goat3DRenderer : GLSurfaceView.Renderer {
    private var program = 0
    private lateinit var sphere: SphereMesh

    private val projection = FloatArray(16)
    private val view = FloatArray(16)
    private val viewProjection = FloatArray(16)
    private val root = FloatArray(16)
    private val model = FloatArray(16)
    private val mvp = FloatArray(16)

    @Volatile
    private var motion: Goat3DMotion = Goat3DMotion.IDLE

    @Volatile
    private var motionStartedAtMillis: Long = SystemClock.uptimeMillis()

    fun play(
        next: Goat3DMotion,
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
            5.6f,
            0f,
            0.12f,
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

        // Compact torso with slightly darker rear haunches for depth.
        drawPart(
            parent = root,
            translateX = -0.38f,
            translateY = -0.18f,
            translateZ = -0.10f,
            scaleX = 0.42f,
            scaleY = 0.48f,
            scaleZ = 0.48f,
            color = FUR_WARM_GRAY,
        )
        drawPart(
            parent = root,
            translateX = 0.38f,
            translateY = -0.18f,
            translateZ = -0.10f,
            scaleX = 0.42f,
            scaleY = 0.48f,
            scaleZ = 0.48f,
            color = FUR_WARM_GRAY,
        )
        drawPart(
            parent = root,
            translateX = 0f,
            translateY = -0.02f,
            translateZ = 0f,
            scaleX = 0.70f,
            scaleY = 0.72f,
            scaleZ = 0.58f,
            color = FUR_IVORY,
        )
        drawPart(
            parent = root,
            translateX = 0f,
            translateY = -0.04f,
            translateZ = 0.49f,
            scaleX = 0.38f,
            scaleY = 0.43f,
            scaleZ = 0.12f,
            color = FUR_CREAM,
        )

        // Four legs with dark cloven-hoof style ends. No base/pedestal.
        drawLeg(root, -0.39f, -0.52f, 0.13f, -4f)
        drawLeg(root, 0.39f, -0.52f, 0.13f, 4f)
        drawLeg(root, -0.21f, -0.57f, 0.42f, -2f)
        drawLeg(root, 0.21f, -0.57f, 0.42f, 2f)

        // Green neck band marks the Green Ludo seat while the goat itself stays natural.
        drawPart(
            parent = root,
            translateX = 0f,
            translateY = 0.42f,
            translateZ = 0.02f,
            scaleX = 0.49f,
            scaleY = 0.095f,
            scaleZ = 0.45f,
            color = COLLAR_GREEN,
        )
        drawPart(
            parent = root,
            translateX = 0f,
            translateY = 0.33f,
            translateZ = 0.47f,
            scaleX = 0.095f,
            scaleY = 0.12f,
            scaleZ = 0.065f,
            color = TAG_GOLD,
        )

        // Neck and head.
        drawPart(
            parent = root,
            translateX = 0f,
            translateY = 0.51f,
            translateZ = -0.02f,
            scaleX = 0.42f,
            scaleY = 0.42f,
            scaleZ = 0.39f,
            color = FUR_IVORY,
        )
        drawPart(
            parent = root,
            translateX = 0f,
            translateY = 0.93f,
            translateZ = 0.02f,
            scaleX = 0.56f,
            scaleY = 0.52f,
            scaleZ = 0.50f,
            rotateZ = pose.headTiltDegrees,
            color = FUR_IVORY,
        )

        // Forehead patch and muzzle keep the face readable at board scale.
        drawPart(
            parent = root,
            translateX = 0f,
            translateY = 1.04f,
            translateZ = 0.40f,
            scaleX = 0.20f,
            scaleY = 0.26f,
            scaleZ = 0.10f,
            color = FUR_WARM_GRAY,
        )
        drawPart(
            parent = root,
            translateX = 0f,
            translateY = 0.79f,
            translateZ = 0.49f,
            scaleX = 0.36f,
            scaleY = 0.24f,
            scaleZ = 0.25f,
            color = MUZZLE_CREAM,
        )
        drawPart(
            parent = root,
            translateX = 0f,
            translateY = 0.79f,
            translateZ = 0.68f,
            scaleX = 0.13f,
            scaleY = 0.08f,
            scaleZ = 0.09f,
            color = NOSE_DARK,
        )

        drawEye(root, -0.20f)
        drawEye(root, 0.20f)

        // Side ears flick during idle and bounce during hops.
        drawPart(
            parent = root,
            translateX = -0.52f,
            translateY = 1.00f,
            translateZ = -0.01f,
            scaleX = 0.28f,
            scaleY = 0.18f,
            scaleZ = 0.15f,
            rotateZ = -16f - pose.earFlickDegrees,
            color = EAR_TAN,
        )
        drawPart(
            parent = root,
            translateX = 0.52f,
            translateY = 1.00f,
            translateZ = -0.01f,
            scaleX = 0.28f,
            scaleY = 0.18f,
            scaleZ = 0.15f,
            rotateZ = 16f + pose.earFlickDegrees,
            color = EAR_TAN,
        )

        // Two-segment backward horns for a clear goat silhouette.
        drawHorn(root, side = -1f)
        drawHorn(root, side = 1f)

        // Small beard under the chin swings independently.
        drawPart(
            parent = root,
            translateX = 0f,
            translateY = 0.55f,
            translateZ = 0.48f,
            scaleX = 0.13f,
            scaleY = 0.28f,
            scaleZ = 0.11f,
            rotateZ = pose.beardSwingDegrees,
            color = BEARD_GRAY,
        )

        // Short upright tail flicks to keep the character lively.
        drawPart(
            parent = root,
            translateX = 0.63f,
            translateY = 0.06f,
            translateZ = -0.37f,
            scaleX = 0.14f,
            scaleY = 0.34f,
            scaleZ = 0.14f,
            rotateZ = -34f + pose.tailFlickDegrees,
            color = FUR_IVORY,
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
            scaleX = 0.16f,
            scaleY = 0.34f,
            scaleZ = 0.16f,
            rotateZ = rotateZ,
            color = FUR_IVORY,
        )
        drawPart(
            parent = parent,
            translateX = x,
            translateY = y - 0.28f,
            translateZ = z + 0.03f,
            scaleX = 0.18f,
            scaleY = 0.105f,
            scaleZ = 0.22f,
            color = HOOF_DARK,
        )
    }

    private fun drawHorn(
        parent: FloatArray,
        side: Float,
    ) {
        drawPart(
            parent = parent,
            translateX = 0.27f * side,
            translateY = 1.35f,
            translateZ = -0.06f,
            scaleX = 0.10f,
            scaleY = 0.28f,
            scaleZ = 0.10f,
            rotateZ = 18f * side,
            rotateX = -12f,
            color = HORN_BEIGE,
        )
        drawPart(
            parent = parent,
            translateX = 0.37f * side,
            translateY = 1.56f,
            translateZ = -0.13f,
            scaleX = 0.085f,
            scaleY = 0.22f,
            scaleZ = 0.085f,
            rotateZ = 30f * side,
            rotateX = -18f,
            color = HORN_TIP,
        )
    }

    private fun drawEye(
        parent: FloatArray,
        x: Float,
    ) {
        drawPart(
            parent = parent,
            translateX = x,
            translateY = 1.02f,
            translateZ = 0.43f,
            scaleX = 0.105f,
            scaleY = 0.125f,
            scaleZ = 0.070f,
            color = EYE_DARK,
        )
        drawPart(
            parent = parent,
            translateX = x - 0.025f,
            translateY = 1.055f,
            translateZ = 0.492f,
            scaleX = 0.024f,
            scaleY = 0.030f,
            scaleZ = 0.018f,
            color = WHITE,
        )
    }

    private fun currentPose(): Goat3DPose {
        val now = SystemClock.uptimeMillis()
        val active = motion
        val duration = Goat3DMotionTimeline.durationMillis(active)
        val elapsed = (now - motionStartedAtMillis).coerceAtLeast(0L)

        if (active == Goat3DMotion.IDLE) {
            val loopProgress = (elapsed % duration).toFloat() / duration.toFloat()
            return Goat3DMotionTimeline.sample(Goat3DMotion.IDLE, loopProgress)
        }

        if (elapsed >= duration) {
            motion = Goat3DMotion.IDLE
            motionStartedAtMillis = now
            return Goat3DMotionTimeline.sample(Goat3DMotion.IDLE, 0f)
        }

        return Goat3DMotionTimeline.sample(
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
                "3D goat program link failed: ${GLES30.glGetProgramInfoLog(created)}"
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
                "3D goat shader compile failed: ${GLES30.glGetShaderInfoLog(shader)}"
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
            GLES30.glUniform3f(cameraLocation, 0f, 0.82f, 5.6f)

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
        val FUR_IVORY = floatArrayOf(0.88f, 0.86f, 0.78f, 1f)
        val FUR_CREAM = floatArrayOf(0.96f, 0.93f, 0.82f, 1f)
        val FUR_WARM_GRAY = floatArrayOf(0.58f, 0.55f, 0.49f, 1f)
        val MUZZLE_CREAM = floatArrayOf(0.92f, 0.88f, 0.75f, 1f)
        val EAR_TAN = floatArrayOf(0.66f, 0.58f, 0.47f, 1f)
        val BEARD_GRAY = floatArrayOf(0.70f, 0.68f, 0.62f, 1f)
        val HORN_BEIGE = floatArrayOf(0.76f, 0.69f, 0.54f, 1f)
        val HORN_TIP = floatArrayOf(0.55f, 0.49f, 0.38f, 1f)
        val HOOF_DARK = floatArrayOf(0.14f, 0.13f, 0.12f, 1f)
        val NOSE_DARK = floatArrayOf(0.08f, 0.07f, 0.07f, 1f)
        val EYE_DARK = floatArrayOf(0.025f, 0.030f, 0.025f, 1f)
        val COLLAR_GREEN = floatArrayOf(0.08f, 0.64f, 0.29f, 1f)
        val TAG_GOLD = floatArrayOf(0.95f, 0.72f, 0.12f, 1f)
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
                float specular = pow(max(dot(normal, halfDir), 0.0), 28.0) * 0.20;
                float rim = pow(1.0 - max(dot(normal, viewDir), 0.0), 2.2) * 0.11;
                vec3 rgb = uColor.rgb * (0.42 + diffuse * 0.58) + vec3(specular + rim);
                outColor = vec4(rgb, uColor.a);
            }
        """
    }
}
