package io.github.haykam821.withersweeper.game.phase;

import io.github.haykam821.withersweeper.game.WithersweeperConfig;
import io.github.haykam821.withersweeper.game.board.Board;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameMode;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.plasmid.api.game.GameOpenContext;
import xyz.nucleoid.plasmid.api.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.api.game.GameResult;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.GameWaitingLobby;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;
import xyz.nucleoid.plasmid.api.game.world.generator.TemplateChunkGenerator;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

public class WithersweeperWaitingPhase {
	private final ServerWorld world;
	public final GameSpace gameSpace;
	private final WithersweeperConfig config;
	private final Board board;

	public WithersweeperWaitingPhase(GameSpace gameSpace, ServerWorld world, WithersweeperConfig config, Board board) {
		this.world = world;
		this.gameSpace = gameSpace;
		this.config = config;
		this.board = board;
	}

	public static GameOpenProcedure open(GameOpenContext<WithersweeperConfig> context) {
		Board board = new Board(context.config().getBoardConfig());
		MapTemplate template = board.buildFromTemplate();

		RuntimeWorldConfig worldConfig = new RuntimeWorldConfig()
			.setGenerator(new TemplateChunkGenerator(context.server(), template));

		return context.openWithWorld(worldConfig, (activity, world) -> {
			WithersweeperWaitingPhase phase = new WithersweeperWaitingPhase(activity.getGameSpace(), world, context.config(), board);
			GameWaitingLobby.addTo(activity, context.config().getPlayerConfig());

			// Rules
			WithersweeperActivePhase.setRules(activity);

			// Listeners
			activity.listen(GameActivityEvents.TICK, phase::tick);
			activity.listen(GamePlayerEvents.ACCEPT, phase::onAcceptPlayers);
			activity.listen(GamePlayerEvents.OFFER, JoinOffer::accept);
			activity.listen(GamePlayerEvents.ADD, phase::addPlayer);
			activity.listen(PlayerDeathEvent.EVENT, phase::onPlayerDeath);
			activity.listen(GameActivityEvents.REQUEST_START, phase::requestStart);
		});
	}

	private void tick() {
		for (ServerPlayerEntity player : this.gameSpace.getPlayers()) {
			if (player.getY() < 0) {
				this.spawn(player);
			}
		}
	}

	private JoinAcceptorResult onAcceptPlayers(JoinAcceptor acceptor) {
		return acceptor.teleport(this.world, WithersweeperActivePhase.getSpawnPos(this.config)).thenRunForEach(player -> {
			player.changeGameMode(GameMode.ADVENTURE);
		});
	}

	private GameResult requestStart() {
		WithersweeperActivePhase.open(this.gameSpace, this.world, this.config, this.board);
		return GameResult.ok();
	}

	private void addPlayer(ServerPlayerEntity player) {
		this.spawn(player);
	}

	private EventResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
		// Respawn player
		this.spawn(player);
		return EventResult.DENY;
	}

	private void spawn(ServerPlayerEntity player) {
		WithersweeperActivePhase.spawn(player, this.world, this.config);
	}
}