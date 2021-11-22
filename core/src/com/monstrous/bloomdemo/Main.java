package com.monstrous.bloomdemo;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ScreenUtils;

public class Main extends ApplicationAdapter {
	SpriteBatch batch;
	OrthographicCamera camera;
	OrthographicCamera bufferCam;
	BitmapFont font;
	Texture img;
	int width, height;
	String text;
	float textX, textY;
	FrameBuffer fbo;
	FrameBuffer fbo2;
	FrameBuffer fbo3;
	ShaderProgram brightFilter;
	ShaderProgram blurHoriz;
	ShaderProgram blurVertical;
	ShaderProgram combineFilter;
	
	@Override
	public void create () {
		batch = new SpriteBatch();
		img = new Texture( Gdx.files.internal("badlogic.jpg"));

		width = Gdx.graphics.getWidth();
		height = Gdx.graphics.getHeight();
		camera = new OrthographicCamera(width, height);
		camera.position.set(width/2, height/2, 0);
		camera.update();
		bufferCam = new OrthographicCamera(width, height);

		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/ShortBaby-Mg2w.ttf"));
		FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
		parameter.size = 128; // font size
		font = generator.generateFont(parameter);
		generator.dispose(); // avoid memory leaks, important

		text = "Bloom";
		GlyphLayout layout = new GlyphLayout(font, text);
		float textWidth = layout.width;
		textX = (Gdx.graphics.getWidth() - layout.width)/2.0f;
		textY = (Gdx.graphics.getHeight() + 2f*layout.height)/2.0f;

		// full screen post processing shader
		brightFilter = new ShaderProgram(
				Gdx.files.internal("shaders\\brightfilter.vertex.glsl"),
				Gdx.files.internal("shaders\\brightfilter.fragment.glsl"));
		if (!brightFilter.isCompiled())
			throw new GdxRuntimeException(brightFilter.getLog());
		blurHoriz = new ShaderProgram(
				Gdx.files.internal("shaders\\blurhoriz.vertex.glsl"),
				Gdx.files.internal("shaders\\blurhoriz.fragment.glsl"));
		if (!blurHoriz.isCompiled())
			throw new GdxRuntimeException(blurHoriz.getLog());
		blurVertical = new ShaderProgram(
				Gdx.files.internal("shaders\\blurvertical.vertex.glsl"),
				Gdx.files.internal("shaders\\blurvertical.fragment.glsl"));
		if (!blurVertical.isCompiled())
			throw new GdxRuntimeException(blurVertical.getLog());
		combineFilter = new ShaderProgram(
				Gdx.files.internal("shaders\\combine.vertex.glsl"),
				Gdx.files.internal("shaders\\combine.fragment.glsl"));
		if (!combineFilter.isCompiled())
			throw new GdxRuntimeException(combineFilter.getLog());

		ShaderProgram.pedantic = false;

	}

	@Override
	public void resize(int width, int height) {
		super.resize(width, height);
		this.width = width;
		this.height = height;

		camera.viewportWidth = width;
		camera.viewportHeight = height;
		camera.position.set(width/2, height/2, 0);
		camera.update();

		fbo = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, false);

		// the buffers used for blurring have a lower resolution
		// this saves processing and gives a more blurry effect
		fbo2 = new FrameBuffer(Pixmap.Format.RGBA8888, width/8, height/8, false);
		fbo3 = new FrameBuffer(Pixmap.Format.RGBA8888, width/8, height/8, false);

	}



	private void renderScene() {
		ScreenUtils.clear(0, 0.1f, 0.2f, 1);
		batch.begin();
		batch.draw(img, 0,0);
		font.draw(batch, text, textX, textY);
		batch.end();
	}

	void renderBuffer(FrameBuffer fbo, int x, int y, int w, int h ) {
		Sprite s = new Sprite(fbo.getColorBufferTexture());
		s.flip(false, true); // coordinate system in buffer differs from screen

		batch.begin();
		batch.draw(s, x, y, w, h);    // draw frame buffer as screen filling texture
		batch.end();
	}

	void renderBuffer(FrameBuffer fbo, ShaderProgram shader, int x, int y, int w, int h ) {
		Sprite s = new Sprite(fbo.getColorBufferTexture());
		s.flip(false, true); // coordinate system in buffer differs from screen

		batch.begin();
		batch.setShader(shader);
		batch.draw(s, x, y, w, h);    // draw frame buffer as screen filling texture
		batch.setShader(null);
		batch.end();
	}

	void renderBufferToBuffer(FrameBuffer fboDest, FrameBuffer fbo, ShaderProgram shader ) {
		Sprite s = new Sprite(fbo.getColorBufferTexture());
		s.flip(false, true); // coordinate system in buffer differs from screen

		float width = fboDest.getWidth();
		float height = fboDest.getHeight();

		shader.bind();
		shader.setUniformf("u_resolution", new Vector2(width, height));

		// to adapt for fboDest resolution
		bufferCam.viewportWidth = width;
		bufferCam.viewportHeight = height;
		bufferCam.position.set(width/2, height/2, 0);
		bufferCam.update();
		batch.setProjectionMatrix(bufferCam.combined);

		fboDest.begin();
		batch.begin();
		batch.setShader(shader);
		batch.draw(s, 0, 0, width, height);
		batch.setShader(null);
		batch.end();
		fboDest.end();
	}

	void renderCombinedBuffers(FrameBuffer fbo, FrameBuffer fbo2, ShaderProgram shader, int x, int y, int w, int h ) {
		Sprite s = new Sprite(fbo.getColorBufferTexture());
		s.flip(false, true); // coordinate system in buffer differs from screen

		shader.bind();
		shader.setUniformi("u_highlightTexture", 1);
		Gdx.gl.glActiveTexture(Gdx.gl20.GL_TEXTURE1);		// bind fbo2 texture to texture unit 1
		fbo2.getColorBufferTexture().bind();
		Gdx.gl.glActiveTexture(Gdx.gl20.GL_TEXTURE0);

		batch.begin();
		batch.setShader(shader);
		batch.draw(s, x, y, w, h);
		batch.setShader(null);
		batch.end();
	}

	@Override
	public void render () {

		batch.setProjectionMatrix(camera.combined);

		// render scene to buffer
		fbo.begin();
		renderScene();
		fbo.end();

		// brightness filter fbo -> fbo2
		renderBufferToBuffer(fbo2, fbo, brightFilter);

		// alternate horizontal and vertical blurs
		for(int i = 0; i < 4; i++) {
			renderBufferToBuffer(fbo3, fbo2, blurHoriz);
			renderBufferToBuffer(fbo2, fbo3, blurVertical);
		}
		// latest blurred image is now in fbo2


		//batch.setProjectionMatrix(camera.combined);			// was clobbered by renderBufferToBuffer
		//renderScene();
		//renderBuffer(fbo2,0,0, width/2, height/2);
		//renderBuffer(fbo,width/2,0, width/2, height/2);

		batch.setProjectionMatrix(camera.combined);			// was clobbered by renderBufferToBuffer

		// combine original render with blurred highlights
		renderCombinedBuffers(fbo, fbo2, combineFilter, 0,0,width, height);
	}
	
	@Override
	public void dispose () {
		batch.dispose();
		font.dispose();
		fbo.dispose();
		fbo2.dispose();
		fbo3.dispose();
		blurHoriz.dispose();
		blurVertical.dispose();
		combineFilter.dispose();
		brightFilter.dispose();
	}
}
