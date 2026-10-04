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

internal class Dog3DRenderer : GLSurfaceView.Renderer {
    private var program = 0
    private lateinit var sphere: SphereMesh

    private val projection = FloatArray(16)
    private val view = FloatArray(16)
    private val viewProjection = FloatArray(16)
    private val root = FloatArray(16)
    private val model = FloatArray(16)
    private val mvp = FloatArray(16)

    @Volatile
    private var motion: Dog3DMotion = Dog3DMotion.IDLE

    @Volatile
    private var motionStartedAtMillis: Long = SystemClock.uptimeMillis()

    fun play(
        next: Dog3DMotion,
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
            0.78f,
            5.5f,
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

        // Rear haunches give the puppy a compact, believable 3D silhouette.
        drawPart(
            parent = root,
            translateX = -0.40f,
            translateY = -0.30f,
            translateZ = -0.10f,
            scaleX = 0.43f,
            scaleY = 0.52f,
            scaleZ = 0.50f,
            color = FUR_TAN,
        )
        drawPart(
            parent = root,
            translateX = 0.40f,
            translateY = -0.30f,
            translateZ = -0.10f,
            scaleX = 0.43f,
            scaleY = 0.52f,
            scaleZ = 0.50f,
            color = FUR_TAN,
        )

        // Main torso and cream chest.
        drawPart(
            parent = root,
            translateX = 0f,
            translateY = -0.08f,
            translateZ = 0f,
            scaleX = 0.72f,
            scaleY = 0.78f,
            scaleZ = 0.60f,
            color = FUR_TAN,
        )
        drawPart(
            parent = root,
            translateX = 0f,
            translateY = -0.13f,
            translateZ = 0.50f,
            scaleX = 0.38f,
            scaleY = 0.46f,
            scaleZ = 0.12f,
            color = FUR_CREAM,
        )

        // Four short puppy legs. The paws are the lowest visible geometry: no pedestal/base.
        drawLeg(root, -0.40f, -0.54f, 0.20f, -7f)
        drawLeg(root, 0.40f, -0.54f, 0.20f, 7f)
        drawLeg(root, -0.22f, -0.61f, 0.46f, -3f)
        drawLeg(root, 0.22f, -0.61f, 0.46f, 3f)

        // Red collar marks this as the Red Ludo character while keeping natural dog fur.
        drawPart(
            parent = root,
            translateX = 0f,
            translateY = 0.43f,
            translateZ = 0.02f,
            scaleX = 0.52f,
            scaleY = 0.10f,
            scaleZ = 0.48f,
            color = COLLAR_RED,
        )
        drawPart(
            parent = root,
            translateX = 0f,
            translateY = 0.34f,
            translateZ = 0.49f,
            scaleX = 0.10f,
            scaleY = 0.13f,
            scaleZ = 0.07f,
            color = TAG_GOLD,
        )

        // Head.
        drawPart(
            parent = root,
            translateX = 0f,
            translateY = 0.88f,
            translateZ = 0.02f,
            scaleX = 0.60f,
            scaleY = 0.56f,
            scaleZ = 0.54f,
            rotateZ = pose.headTiltDegrees,
            color = FUR_TAN,
        )

        // Cream cheeks/muzzle give the face a softer, realistic-cartoon look.
        drawPart(
            parent = root,
            translateX = -0.19f,
            translateY = 0.78f,
            translateZ = 0.46f,
            scaleX = 0.27f,
            scaleY = 0.23f,
            scaleZ = 0.22f,
            color = FUR_CREAM,
        )
        drawPart(
            parent = root,
            translateX = 0.19f,
            translateY = 0.78f,
            translateZ = 0.46f,
            scaleX = 0.27f,
            scaleY = 0.23f,
            scaleZ = 0.22f,
            color = FUR_CREAM,
        )
        drawPart(
            parent = root,
            translateX = 0f,
            translateY = 0.82f,
            translateZ = 0.63f,
            scaleX = 0.16f,
            scaleY = 0.12f,
            scaleZ = 0.13f,
            color = NOSE_DARK,
        )
        drawPart(
            parent = root,
            translateX = 0f,
            translateY = 0.63f,
            translateZ = 0.61f,
            scaleX = 0.11f,
            scaleY = 0.15f,
            scaleZ = 0.06f,
            color = TONGUE_PINK,
        )

        drawEye(root, -0.22f)
        drawEye(root, 0.22f)

        // Soft floppy ears, animated independently during hops/celebration.
        drawPart(
            parent = root,
            translateX = -0.50f,
            translateY = 0.98f,
            translateZ = -0.02f,
            scaleX = 0.25f,
            scaleY = 0.44f,
            scaleZ = 0.23f,
            rotateZ = -24f - pose.earBounceDegrees,
            color = EAR_BROWN,
        )
        drawPart(
            parent = root,
            translateX = 0.50f,
            translateY = 0.98f,
            translateZ = -0.02f,
            scaleX = 0.25f,
            scaleY = 0.44f,
            scaleZ = 0.23f,
            rotateZ = 24f + pose.earBounceDegrees,
            color = EAR_BROWN,
        )

        // Tail remains visible to one side and wags for a lively puppy personality.
        drawPart(
            parent = root,
            translateX = 0.69f,
            translateY = -0.10f,
            translateZ = -0.31f,
            scaleX = 0.17f,
            scaleY = 0.46f,
            scaleZ = 0.16f,
            rotateZ = -48f + pose.tailWagDegrees,
            color = FUR_TAN,
        )
        drawPart(
            parent = root,
            translateX = 0.93f,
            translateY = 0.16f,
            translateZ = -0.32f,
            scaleX = 0.13f,
            scaleY = 0.28f,
            scaleZ = 0.13f,
            rotateZ = -58f + pose.tailWagDegrees,
            color = FUR_CREAM,
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
            scaleX = 0.19f,
            scaleY = 0.30f,
            scaleZ = 0.19f,
            rotateZ = rotateZ,
            color = FUR_TAN,
        )
        drawPart(
            parent = parent,
            translateX = x,
            translateY = y - 0.25f,
            translateZ = z + 0.05f,
            scaleX = 0.23f,
            scaleY = 0.12f,
            scaleZ = 0.27f,
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
            translateY = 0.98f,
            translateZ = 0.47f,
            scaleX = 0.115f,
            scaleY = 0.145f,
            scaleZ = 0.075f,
            color = EYE_DARK,
        )
        drawPart(
            parent = parent,
            translateX = x - 0.028f,
            translateY = 1.025f,
            translateZ = 0.535f,
            scaleX = 0.026f,
            scaleY = 0.034f,
            scaleZ = 0.020f,
            color = WHITE,
        )
    }

    private fun currentPose(): Dog3DPose {
        val now = SystemClock.uptimeMillis()
        val active = motion
        val duration = Dog3DMotionTimeline.durationMillis(active)
        val elapsed = (now - motionStartedAtMillis).coerceAtLeast(0L)

        if (active == Dog3DMotion.IDLE) {
            val loopProgress = (elapsed % duration).toFloat() / duration.toFloat()
            return Dog3DMotionTimeline.sample(Dog3DMotion.IDLE, loopProgress)
        }

        if (elapsed >= duration) {
            motion = Dog3DMotion.IDLE
            motionStartedAtMillis = now
            return Dog3DMotionTimeline.sample(Dog3DMotion.IDLE, 0f)
        }

        return Dog3DMotionTimeline.sample(
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
                "3D dog program link failed: ${GLES30.glGetProgramInfoLog(created)}"
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
                "3D dog shader compile failed: ${GLES30.glGetShaderInfoLog(shader)}"
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
            GLES30.glUniformMatrix4fv(GLES30.glGetUniformLocation(program, "uModel"), 1, false, model, 0)
            GLES30.glUniformMatrix4fv(GLES30.glGetUniformLocation(program, "uMvp"), 1, false, mvp, 0)
            GLES30.glUniform4fv(GLES30.glGetUniformLocation(program, "uColor"), 1, color, 0)
            GLES30.glUniform3f(GLES30.glGetUniformLocation(program, "uLightDirection"), 0.40f, -1.0f, -0.65f)
            GLES30.glUniform3f(GLES30.glGetUniformLocation(program, "uCameraPosition"), 0f, 0.78f, 5.5f)

            vertexBuffer.position(0)
            GLES30.glEnableVertexAttribArray(0)
            GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 6 * Float.SIZE_BYTES, vertexBuffer)

            vertexBuffer.position(3)
            GLES30.glEnableVertexAttribArray(1)
            GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, 6 * Float.SIZE_BYTES, vertexBuffer)

            indexBuffer.position(0)
            GLES30.glDrawElements(GLES30.GL_TRIANGLES, indexCount, GLES30.GL_UNSIGNED_SHORT, indexBuffer)
            GLES30.glDisableVertexAttribArray(0)
            GLES30.glDisableVertexAttribArray(1)
        }
    }

    private companion object {
        val FUR_TAN = floatArrayOf(0.78f, 0.40f, 0.16f, 1f)
        val FUR_CREAM = floatArrayOf(0.98f, 0.84f, 0.65f, 1f)
        val EAR_BROWN = floatArrayOf(0.39f, 0.16f, 0.07f, 1f)
        val COLLAR_RED = floatArrayOf(0.88f, 0.08f, 0.09f, 1f)
        val TAG_GOLD = floatArrayOf(1.00f, 0.72f, 0.08f, 1f)
        val NOSE_DARK = floatArrayOf(0.035f, 0.030f, 0.028f, 1f)
        val EYE_DARK = floatArrayOf(0.025f, 0.025f, 0.030f, 1f)
        val TONGUE_PINK = floatArrayOf(1.00f, 0.35f, 0.46f, 1f)
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
                float rim = pow(1.0 - max(dot(normal, viewDir), 0.0), 2.2) * 0.10;
                vec3 rgb = uColor.rgb * (0.42 + diffuse * 0.58) + vec3(specular + rim);
                outColor = vec4(rgb, uColor.a);
            }
        """
    }
}
