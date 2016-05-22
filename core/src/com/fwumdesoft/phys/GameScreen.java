package com.fwumdesoft.phys;

import java.util.function.Consumer;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pools;
import com.badlogic.gdx.utils.viewport.FillViewport;
import com.fwumdesoft.phys.actors.AirMolecule;
import com.fwumdesoft.phys.actors.HitboxActor;
import com.fwumdesoft.phys.actors.Refractor;
import com.fwumdesoft.phys.actors.Transmitter;
import com.fwumdesoft.phys.actors.Wave;

public class GameScreen extends ScreenAdapter {
	private int nextLevel = 0;
	private Stage stage;
	private Array<HitboxActor> movableActors;
	private Level level;
	
	@Override
	public void show() {
		stage = new Stage(new FillViewport(1000f, 1000f * ((float)Gdx.graphics.getHeight() / Gdx.graphics.getWidth())));
		Gdx.input.setInputProcessor(stage);
		stage.addListener(new InputManager());
		
		generateAir(0.03f);
		
		Wave wave = new Wave();
		wave.debug();
		stage.addActor(wave);
		wave.propagate(100, 100, 10);
		
//		Vector2 v = Vector2.X.cpy().rotate(10).scl(100);
//		Reflector refl = new Reflector();
//		refl.setPosition(v.x + wave.getX(), v.y + wave.getY());
//		refl.setRotation(30);
//		stage.addActor(refl);
		
		Vector2 v = Vector2.X.cpy().rotate(10).scl(100);
		Refractor refr = new Refractor();
		refr.setPosition(v.x + wave.getX(), v.y + wave.getY());
		refr.setRotation(210);
		stage.addActor(refr);
		
//		loadNextLevel();
	}
	
	private void loadNextLevel() {
		level = Level.loadFromFile(Integer.toString(++nextLevel));
		level.setupStage(stage, false);
		movableActors = level.getNotFixedPositionActors();
		Consumer<HitboxActor> disable = actor -> actor.setVisible(false);
		Consumer<HitboxActor> addToStage = actor -> stage.addActor(actor);
		movableActors.forEach(disable);
		movableActors.forEach(addToStage);
	}
	
	private void resetLevel() {
		
	}
	
	/**
	 * Generates AirMolecule objects on the screen with a specified density and adds them
	 * to the stage.
	 * @param density Density of AirMolecules in molecules/unit^2
	 */
	private void generateAir(float density) {
		float worldWidth = stage.getWidth(), worldHeight = stage.getHeight();
		float area = worldWidth * worldHeight;
		int totalAir = MathUtils.ceil(area * density);
		int cellLength = MathUtils.ceil((float)Math.sqrt(1f / density));
		
		for(int minY = 0; minY < worldHeight; minY += cellLength) {
			for(int minX = 0; minX < worldWidth; minX += cellLength) {
				float randX = MathUtils.random(minX, minX + cellLength);
				float randY = MathUtils.random(minY, minY + cellLength);
				AirMolecule airMolecule = Pools.get(AirMolecule.class, totalAir).obtain();
				airMolecule.setPosition(randX, randY, Align.center);
				airMolecule.markPosition(new Vector2(airMolecule.getX(), airMolecule.getY())); //marks original position
				stage.addActor(airMolecule);
			}
		}
	}
	
	@Override
	public void resize(int width, int height) {
		stage.getViewport().update(width, height, true);
	}
	
	@Override
	public void render(float delta) {
		Gdx.gl.glClearColor(0.75f, 0.75f, 0.75f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		stage.act(delta);
		stage.draw();
		
//		***** check victory conditions *****
		int numWaves = 1;//level.getTransmitters().size;
		for(Actor actor : stage.getActors()) {
			if(actor instanceof Wave) {
				Wave wave = (Wave)actor;
				if(wave.wasSuccessful()) {
					numWaves--;
				} else if(!wave.isAlive()) {
					//TODO reset level since a wave has been lost before hitting a receiver
					Gdx.app.log("GameScreen", "Reset level");
				}
			}
		}
		if(numWaves == 0) {
			//TODO go to next level since all waves were successful
			Gdx.app.log("GameScreen", "Next Level");
		}
	}
	
	@Override
	public void hide() {
		dispose();
	}
	
	@Override
	public void dispose() {
		stage.dispose();
	}
	
	private class InputManager extends InputListener {
		private boolean transmitted;
		
		@Override
		public boolean keyDown(InputEvent event, int keycode) {
			switch(keycode) {
			case Keys.SPACE: //tells all transmitters to transmit a wave
				if(transmitted) break;
				for(int i = 0; i < stage.getActors().size; i++) {
					Actor actor = stage.getActors().get(i);
					if(actor instanceof Transmitter) {
						Transmitter trans = (Transmitter)actor;
						trans.transmit();
					}
				}
				transmitted = true;
				Gdx.app.log("GameScreen.InputManager", "Transmitters fired");
				return true;
			}
			return false;
		}
	}
}
