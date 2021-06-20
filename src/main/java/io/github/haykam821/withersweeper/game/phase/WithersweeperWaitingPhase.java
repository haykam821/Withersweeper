package io.github.haykam821.withersweeper.game.phase;

import io.github.haykam821.withersweeper.game.WithersweeperConfig;
import io.github.haykam821.withersweeper.game.board.Board;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.world.GameMode;
import xyz.nucleoid.fantasy.BubbleWorldConfig;
import xyz.nucleoid.plasmid.game.GameOpenContext;
import xyz.nucleoid.plasmid.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.GameWaitingLobby;
import xyz.nucleoid.plasmid.game.StartResult;
import xyz.nucleoid.plasmid.game.config.PlayerConfig;
import xyz.nucleoid.plasmid.game.event.GameTickListener;
import xyz.nucleoid.plasmid.game.event.OfferPlayerListener;
import xyz.nucleoid.plasmid.game.event.PlayerAddListener;
import xyz.nucleoid.plasmid.game.event.PlayerDeathListener;
import xyz.nucleoid.plasmid.game.event.RequestStartListener;
import xyz.nucleoid.plasmid.game.player.JoinResult;
import xyz.nucleoid.plasmid.map.template.MapTemplate;
import xyz.nucleoid.plasmid.map.template.TemplateChunkGenerator;

public class WithersweeperWaitingPhase {
	private final ServerWorld world;
	public final GameSpace gameSpace;
	private final WithersweeperConfig config;
	private final Board board;

	public WithersweeperWaitingPhase(GameSpace gameSpace, WithersweeperConfig config, Board board) {
		this.world = gameSpace.getWorld();
		this.gameSpace = gameSpace;
		this.config = config;
		this.board = board;
	}

	public static GameOpenProcedure open(GameOpenContext<WithersweeperConfig> context) {
		Board board = new Board(context.getConfig().getBoardConfig());
		MapTemplate template = board.buildFromTemplate();

		BubbleWorldConfig worldConfig = new BubbleWorldConfig()
			.setGenerator(new TemplateChunkGenerator(context.getServer(), template))
			.setDefaultGameMode(GameMode.ADVENTURE);

		return context.createOpenProcedure(worldConfig, game -> {
			WithersweeperWaitingPhase phase = new WithersweeperWaitingPhase(game.getSpace(), context.getConfig(), board);
			GameWaitingLobby.applyTo(game, context.getConfig().getPlayerConfig());

			// Rules
			WithersweeperActivePhase.setRules(game);

			// Listeners
			game.on(GameTickListener.EVENT, phase::tick);
			game.on(OfferPlayerListener.EVENT, phase::offerPlayer);
			game.on(PlayerAddListener.EVENT, phase::addPlayer);
			game.on(PlayerDeathListener.EVENT, phase::onPlayerDeath);
			game.on(RequestStartListener.EVENT, phase::requestStart);
		});
	}

	private void tick() {
		for (ServerPlayerEntity player : this.gameSpace.getPlayers()) {
			if (player.getY() < 0) {
				this.spawn(player);
			}
		}
	}

	private boolean isFull() {
		return this.gameSpace.getPlayerCount() >= this.config.getPlayerConfig().getMaxPlayers();
	}

	private JoinResult offerPlayer(ServerPlayerEntity player) {
		return this.isFull() ? JoinResult.gameFull() : JoinResult.ok();
	}

	private StartResult requestStart() {
		PlayerConfig playerConfig = this.config.getPlayerConfig();
		if (this.gameSpace.getPlayerCount() < playerConfig.getMinPlayers()) {
			return StartResult.NOT_ENOUGH_PLAYERS;
		}

		WithersweeperActivePhase.open(this.gameSpace, this.config, this.board);
		return StartResult.OK;
	}

	private void addPlayer(ServerPlayerEntity player) {
		this.spawn(player);
	}

	private ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
		// Respawn player
		this.spawn(player);
		return ActionResult.FAIL;
	}

	private void spawn(ServerPlayerEntity player) {
		WithersweeperActivePhase.spawn(player, this.world, this.config);
	}
}