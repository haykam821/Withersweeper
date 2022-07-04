package io.github.haykam821.withersweeper.game.phase;

import io.github.haykam821.withersweeper.Withersweeper;
import io.github.haykam821.withersweeper.game.WithersweeperConfig;
import io.github.haykam821.withersweeper.game.board.Board;
import io.github.haykam821.withersweeper.game.field.Field;
import io.github.haykam821.withersweeper.game.field.FieldVisibility;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.GameActivity;
import xyz.nucleoid.plasmid.game.GameCloseReason;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.game.player.PlayerOffer;
import xyz.nucleoid.plasmid.game.player.PlayerOfferResult;
import xyz.nucleoid.plasmid.game.rule.GameRuleType;
import xyz.nucleoid.plasmid.game.stats.GameStatisticBundle;
import xyz.nucleoid.plasmid.game.stats.StatisticKeys;
import xyz.nucleoid.plasmid.game.stats.StatisticMap;
import xyz.nucleoid.plasmid.util.PlayerRef;
import xyz.nucleoid.stimuli.event.block.BlockBreakEvent;
import xyz.nucleoid.stimuli.event.block.BlockPunchEvent;
import xyz.nucleoid.stimuli.event.block.BlockUseEvent;
import xyz.nucleoid.stimuli.event.item.ItemThrowEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

import java.util.HashSet;
import java.util.Set;

public class WithersweeperActivePhase {
	private final ServerWorld world;
	public final GameSpace gameSpace;
	private final WithersweeperConfig config;
	private final Board board;
	private final GameStatisticBundle statistics;
	private final Set<PlayerRef> participants = new HashSet<>();
	private int timeElapsed = 0;
	public int mistakes = 0;

	public WithersweeperActivePhase(GameSpace gameSpace, ServerWorld world, WithersweeperConfig config, Board board) {
		this.world = world;
		this.gameSpace = gameSpace;
		this.config = config;
		this.board = board;
		this.statistics = config.getStatisticBundle(gameSpace);
	}

	public static void open(GameSpace gameSpace, ServerWorld world, WithersweeperConfig config, Board board) {
		gameSpace.setActivity(activity -> {
			WithersweeperActivePhase phase = new WithersweeperActivePhase(gameSpace, world, config, board);

			// Rules
			WithersweeperActivePhase.setRules(activity);

			// Listeners
			activity.listen(GameActivityEvents.ENABLE, phase::enable);
			activity.listen(GameActivityEvents.TICK, phase::tick);
			activity.listen(GamePlayerEvents.OFFER, phase::onPlayerOffer);
			activity.listen(PlayerDeathEvent.EVENT, phase::onPlayerDeath);
			activity.listen(BlockUseEvent.EVENT, phase::onUseBlock);
			activity.listen(BlockPunchEvent.EVENT, phase::onPunchBlock);
			activity.listen(ItemThrowEvent.EVENT, phase::onThrowItem);
		});
	}

	protected static void setRules(GameActivity activity) {
		activity.deny(GameRuleType.BLOCK_DROPS);
		activity.deny(GameRuleType.CRAFTING);
		activity.deny(GameRuleType.FALL_DAMAGE);
		activity.deny(GameRuleType.HUNGER);
		activity.deny(GameRuleType.PORTALS);
		activity.deny(GameRuleType.PVP);
	}

	private void enable() {
		for (ServerPlayerEntity player : this.gameSpace.getPlayers()) {
			player.changeGameMode(GameMode.SURVIVAL);
		}
		this.updateFlagCounter();
	}

	private void tick() {
		this.timeElapsed += 1;

		for (ServerPlayerEntity player : this.gameSpace.getPlayers()) {
			if (player.getY() < 0) {
				this.spawn(player);
			}
		}
	}

	private Text getMistakeText(PlayerEntity causer) {
		Text displayName = causer.getDisplayName();

		if (this.config.getMaxMistakes() <= 1) {
			return new TranslatableText("text.withersweeper.reveal_mine", displayName).formatted(Formatting.RED);
		} else {
			return new TranslatableText("text.withersweeper.reveal_mine.max_mistakes", displayName, this.config.getMaxMistakes()).formatted(Formatting.RED);
		}
	}

	private void checkMistakes(PlayerEntity causer) {
		if (this.mistakes < this.config.getMaxMistakes()) return;

		Text text = this.getMistakeText(causer);
		for (PlayerEntity player : this.gameSpace.getPlayers()) {
			player.sendMessage(text, false);
		}

		if (this.statistics != null) {
			for (PlayerRef participant : this.participants) {
				this.statistics.forPlayer(participant).increment(StatisticKeys.GAMES_LOST, 1);
			}
		}

		this.gameSpace.close(GameCloseReason.FINISHED);
	}

	private void updateFlagCounter() {
		var remainingFlags = Math.max(this.board.getRemainingFlags(), 0);
		var experienceProgress = (float) remainingFlags / this.board.getMinesCount();
		for (ServerPlayerEntity player : this.gameSpace.getPlayers()) {
			player.experienceProgress = experienceProgress;
			player.setExperienceLevel(remainingFlags);
		}
	}

	private void addParticipant(ServerPlayerEntity player) {
		PlayerRef participant = PlayerRef.of(player);
		if (this.participants.add(participant) && this.statistics != null) {
			this.statistics.forPlayer(participant).increment(StatisticKeys.GAMES_PLAYED, 1);
		}
	}

	private ActionResult onUseBlock(ServerPlayerEntity uncoverer, Hand hand, BlockHitResult hitResult) {
		if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
		return this.onModifyField(uncoverer, hitResult.getBlockPos(), false);
	}

	private ActionResult onPunchBlock(ServerPlayerEntity uncoverer, Direction direction, BlockPos pos) {
		this.onModifyField(uncoverer, pos, true);
		return ActionResult.FAIL;
	}

	private ActionResult onModifyField(ServerPlayerEntity uncoverer, BlockPos pos, boolean uncover) {
		if (pos.getY() != 0) return ActionResult.PASS;
		if (!this.board.isValidPos(pos.getX(), pos.getZ())) return ActionResult.PASS;

		var field = this.board.getField(pos.getX(), pos.getZ());
		var visibility = field.getVisibility();
		if (visibility == FieldVisibility.UNCOVERED) return ActionResult.PASS;

		// uncovering
		if (uncover) {
			if (visibility == FieldVisibility.FLAGGED || visibility == FieldVisibility.ALTERNATIVE_FLAGGED) return ActionResult.PASS;
			this.board.placeMines(pos.getX(), pos.getZ(), this.world.getRandom());
			field.uncover(pos, uncoverer, this);
			this.world.playSound(null, pos, SoundEvents.BLOCK_SAND_BREAK, SoundCategory.BLOCKS, 0.5f, 1);
		}
		// flagging
		else switch (visibility) {
			case FLAGGED -> {
				field.setVisibility(FieldVisibility.ALTERNATIVE_FLAGGED);
				this.world.playSound(null, pos, SoundEvents.ENTITY_ITEM_FRAME_ROTATE_ITEM, SoundCategory.BLOCKS, 1, 1);
			}
			case ALTERNATIVE_FLAGGED -> {
				field.setVisibility(FieldVisibility.COVERED);
				this.world.playSound(null, pos, SoundEvents.ENTITY_ITEM_FRAME_REMOVE_ITEM, SoundCategory.BLOCKS, 1, 1);
			}
			default -> {
				field.setVisibility(FieldVisibility.FLAGGED);
				this.world.playSound(null, pos, SoundEvents.ENTITY_ITEM_FRAME_ADD_ITEM, SoundCategory.BLOCKS, 1, 1);
			}
		}

		this.addParticipant(uncoverer);
		this.checkMistakes(uncoverer);
		this.board.build(this.world);
		this.updateFlagCounter();

		if (this.board.isCompleted()) {
			var text = new TranslatableText("text.withersweeper.complete", this.timeElapsed / 20).formatted(Formatting.GOLD);
			for (var player : this.gameSpace.getPlayers()) {
				player.sendMessage(text, false);
			}
			if (this.statistics != null) {
				for (var participant : this.participants) {
					this.statistics.forPlayer(participant).increment(StatisticKeys.GAMES_WON, 1);
					this.statistics.forPlayer(participant).increment(StatisticKeys.QUICKEST_TIME, this.timeElapsed);
				}
			}
			this.gameSpace.close(GameCloseReason.FINISHED);
		}

		return ActionResult.SUCCESS;
	}

	private PlayerOfferResult onPlayerOffer(PlayerOffer offer) {
		return offer.accept(this.world, WithersweeperActivePhase.getSpawnPos(this.config)).and(() -> {
			offer.player().sendMessage(Withersweeper.DESCRIPTION_TEXT, false);
			offer.player().changeGameMode(GameMode.SURVIVAL);
			this.updateFlagCounter();
		});
	}

	private ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
		// Respawn player
		this.spawn(player);
		return ActionResult.FAIL;
	}

	private boolean attemptToSendInfoMessage(PlayerEntity player, BlockPos pos) {
		if (pos.getY() != 0) return false;
		if (!this.board.isValidPos(pos.getX(), pos.getZ())) return false;

		Field field = this.board.getField(pos.getX(), pos.getZ());

		Text message = field.getCoveredInfoMessage().shallowCopy().formatted(Formatting.DARK_PURPLE);
		player.sendMessage(message, true);

		return true;
	}

	private ActionResult onThrowItem(PlayerEntity player, int slot, ItemStack stack) {
		HitResult hit = player.raycast(8, 0, false);
		if (hit.getType() == HitResult.Type.BLOCK) {
			this.attemptToSendInfoMessage(player, ((BlockHitResult) hit).getBlockPos());
		}

		return ActionResult.FAIL;
	}

	public StatisticMap getStatisticsForPlayer(ServerPlayerEntity player) {
		if (this.statistics == null) {
			return null;
		}
		return this.statistics.forPlayer(player);
	}

	public Board getBoard() {
		return this.board;
	}

	private void spawn(ServerPlayerEntity player) {
		WithersweeperActivePhase.spawn(player, this.world, this.config);
	}

	protected static void spawn(ServerPlayerEntity player, ServerWorld world, WithersweeperConfig config) {
		Vec3d spawnPos = WithersweeperActivePhase.getSpawnPos(config);
		player.teleport(world, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), 0, 0);
	}

	protected static Vec3d getSpawnPos(WithersweeperConfig config) {
		return new Vec3d(config.getBoardConfig().x / 2d, 1, config.getBoardConfig().x / 2d);
	}
}