package com.monstrous.bloomdemo;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.monstrous.bloomdemo.filters.Filter;

public class Main extends ApplicationAdapter {
	SpriteBatch batch;
	OrthographicCamera camera;
	BitmapFont font;
	Texture img;
	int width, height;
	String text;
	float textX, textY;
	FrameBuffer fbo;
	PostProcessor postProcessor;

	
	@Override
	public void create () {
		batch = new SpriteBatch();
		img = new Texture( Gdx.files.internal("badlogic.jpg"));

		width = Gdx.graphics.getWidth();
		height = Gdx.graphics.getHeight();
		camera = new OrthographicCamera(width, height);
		camera.position.set(width/2, height/2, 0);
		camera.update();

		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/ShortBaby-Mg2w.ttf"));
		FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
		parameter.size = 128; // font size
		font = generator.generateFont(parameter);
		generator.dispose(); // avoid memory leaks, important
		font.setColor(0.7f,1.0f, 1.0f, 1.0f);		// use a bright colour

		text = "Bloom";
		GlyphLayout layout = new GlyphLayout(font, text);
		float textWidth = layout.width;
		textX = (Gdx.graphics.getWidth() - layout.width)/2.0f;
		textY = (Gdx.graphics.getHeight() + 2f*layout.height)/2.0f;

		postProcessor = new PostProcessor();
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
		postProcessor.resize(width, height);
	}



	private void renderScene() {
		ScreenUtils.clear(0, 0.1f, 0.2f, 1);
		batch.begin();
		batch.draw(img, 0,0);

		font.draw(batch, text, textX, textY);
		batch.end();
	}



	@Override
	public void render () {

		// render scene to buffer
		fbo.begin();
		renderScene();
		fbo.end();

		postProcessor.render(fbo, 0, 0, width, height);
	}
	
	@Override
	public void dispose () {
		batch.dispose();
		font.dispose();
		fbo.dispose();
		postProcessor.dispose();
	}
}
