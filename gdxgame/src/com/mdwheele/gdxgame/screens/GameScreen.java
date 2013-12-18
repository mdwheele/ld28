package com.mdwheele.gdxgame.screens;

import box2dLight.RayHandler;

import com.artemis.managers.GroupManager;
import com.artemis.managers.TagManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.World;
import com.mdwheele.gdxgame.GameLevel;
import com.mdwheele.gdxgame.GdxGame;
import com.mdwheele.gdxgame.events.InputActionEvent;
import com.mdwheele.gdxgame.input.InputAction;
import com.mdwheele.gdxgame.systems.AntiGravitySystem;
import com.mdwheele.gdxgame.systems.CameraSystem;
import com.mdwheele.gdxgame.systems.CollisionSystem;
import com.mdwheele.gdxgame.systems.FlameThrowerSystem;
import com.mdwheele.gdxgame.systems.LifetimeSystem;
import com.mdwheele.gdxgame.systems.LightSystem;
import com.mdwheele.gdxgame.systems.PlayerInputSystem;
import com.mdwheele.gdxgame.systems.RenderSystem;
import com.mdwheele.gdxgame.systems.ScriptSystem;

public class GameScreen extends AbstractScreen {
	
	TiledMap map;
	OrthogonalTiledMapRenderer mapRenderer;
	com.artemis.World entityWorld;
	World physicsWorld;	
	Box2DDebugRenderer box2dRenderer;
	BitmapFont debugFont;
	ShapeRenderer debugRenderer;
	RayHandler rayHandler;
	
	GameLevel level;
	
	private float fboScaler = 1.5f;
	private FrameBuffer fbo = null;
	private TextureRegion fboRegion = null;
	
	public GameScreen(final GdxGame game, String levelPath) {
		super(game);
		/**
		 * Create Box2d World
		 */
		physicsWorld = new World(new Vector2(0, -9.8f), true);
		
		/**
		 * Create ray handler
		 */
		rayHandler = new RayHandler(physicsWorld);
		
		/**
		 * Create Artemis World
		 */
		entityWorld = new com.artemis.World();	
		entityWorld.setManager(new TagManager());	
		entityWorld.setManager(new GroupManager());

		entityWorld.setSystem(new CameraSystem(this.camera));
		PlayerInputSystem playerInputSystem = new PlayerInputSystem(eventManager, physicsWorld);
		entityWorld.setSystem(playerInputSystem);
		entityWorld.setSystem(new ScriptSystem());
		entityWorld.setSystem(new RenderSystem(this.batch, this.camera));
		
		/**
		 * Behavioral
		 */
		entityWorld.setSystem(new LifetimeSystem());
		entityWorld.setSystem(new FlameThrowerSystem());
		entityWorld.setSystem(new AntiGravitySystem(physicsWorld));
		entityWorld.setSystem(new LightSystem(physicsWorld, rayHandler));
		
		CollisionSystem collisionSystem = new CollisionSystem(eventManager, physicsWorld);
		entityWorld.setSystem(collisionSystem);
		physicsWorld.setContactListener(collisionSystem);
		physicsWorld.setContactListener(playerInputSystem);
		
		entityWorld.initialize();
		
		/**
		 * Load Level
		 */
		level = new GameLevel(physicsWorld, entityWorld, 1f);
		map = level.loadFromFile("maps/debug-map.tmx");		

		mapRenderer = new OrthogonalTiledMapRenderer(map, 1f, this.batch);
				
		/**
		 * Create debug renderer for Box2d
		 */
		box2dRenderer = new Box2DDebugRenderer();	
		debugFont = new BitmapFont();
		debugRenderer = new ShapeRenderer();

		fbo = new FrameBuffer(Format.RGBA8888, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false);
		
		this.start();
	}
	
	public void start() {
		/**
		 * Subscribe this class to the event manager.
		 */
		eventManager.subscribe(InputActionEvent.class, this);		
	}
	
	@Override
	public void render(float delta) {
		super.render(delta);
		
		// simulate physics.
		physicsWorld.step(1/60f, 6, 2);	
		
		// render map
		mapRenderer.setView(this.camera);
		mapRenderer.render();
		
		batch.begin();			
		
			// update entity system
			entityWorld.setDelta(delta);
			entityWorld.process();
			
		batch.end();		

        fbo.begin();
    		Gdx.gl.glActiveTexture(GL10.GL_TEXTURE1);
        	Gdx.gl.glClearColor(0, 0, 0, 1);
        	Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
        	
			rayHandler.setCombinedMatrix(this.camera.combined);
			rayHandler.updateAndRender();
		fbo.end();

		// debug
		box2dRenderer.render(physicsWorld, camera.combined);
				
	}
		
	public void handleEvent(InputActionEvent event) {
		if(event.action() == InputAction.QUIT)
			Gdx.app.exit();
	}

	@Override
	public void resize(int width, int height) {
		super.resize(width, height);
		
		camera.setToOrtho(false, game.width, game.height);
	}

	@Override
	public void show() {
	}

	@Override
	public void hide() {
	}

	@Override
	public void pause() {
	}

	@Override
	public void resume() {	
	}

	@Override
	public void dispose() {
		map.dispose();
		rayHandler.dispose();
	}
}
