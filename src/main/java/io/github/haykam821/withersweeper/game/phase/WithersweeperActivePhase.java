package io.github.haykam821.withersweeper.game.phase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.github.haykam821.withersweeper.game.WithersweeperConfig;
import io.github.haykam821.withersweeper.game.board.Board;
import io.github.haykam821.withersweeper.game.field.Field;
import io.github.haykam821.withersweeper.game.field.FieldVisibility;
import io.github.haykam821.withersweeper.game.field.NumberField;
import net.minecraft.block.Blocks;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.GameOpenContext;
import xyz.nucleoid.plasmid.game.GameWorld;
import xyz.nucleoid.plasmid.game.StartResult;
import xyz.nucleoid.plasmid.game.config.PlayerConfig;
import xyz.nucleoid.plasmid.game.event.GameTickListener;
import xyz.nucleoid.plasmid.game.event.OfferPlayerListener;
import xyz.nucleoid.plasmid.game.event.PlayerAddListener;
import xyz.nucleoid.plasmid.game.event.PlayerDeathListener;
import xyz.nucleoid.plasmid.game.event.RequestStartListener;
import xyz.nucleoid.plasmid.game.event.UseBlockListener;
import xyz.nucleoid.plasmid.game.map.template.MapTemplate;
import xyz.nucleoid.plasmid.game.map.template.TemplateChunkGenerator;
import xyz.nucleoid.plasmid.game.player.JoinResult;
import xyz.nucleoid.plasmid.game.rule.GameRule;
import xyz.nucleoid.plasmid.game.rule.RuleResult;
import xyz.nucleoid.plasmid.game.world.bubble.BubbleWorldConfig;
import xyz.nucleoid.plasmid.util.ItemStackBuilder;

public class WithersweeperActivePhase {
	private final ServerWorld world;
	public final GameWorld gameWorld;
	private final WithersweeperConfig config;
	private final Board board;
	private int timeElapsed = 0;
	public int mistakes = 0;
	private boolean started = false;

	public WithersweeperActivePhase(GameWorld gameWorld, WithersweeperConfig config, Board board) {
		this.world = gameWorld.getWorld();
		this.gameWorld = gameWorld;
		this.config = config;
		this.board = board;
	}

	public static CompletableFuture<Void> open(GameOpenContext<WithersweeperConfig> context) {
		MapTemplate template = MapTemplate.createEmpty();
		Board board = new Board(context.getConfig().getBoardConfig());
		board.build(template);

		return CompletableFuture.runAsync(() -> {
			BubbleWorldConfig worldConfig = new BubbleWorldConfig()
				.setGenerator(new TemplateChunkGenerator(context.getServer(), template, BlockPos.ORIGIN))
				.setDefaultGameMode(GameMode.ADVENTURE);
			GameWorld gameWorld = context.openWorld(worldConfig);

			WithersweeperActivePhase phase = new WithersweeperActivePhase(gameWorld, context.getConfig(), board);

			gameWorld.openGame(game -> {
				game.setRule(GameRule.BLOCK_DROPS, RuleResult.DENY);
				game.setRule(GameRule.CRAFTING, RuleResult.DENY);
				game.setRule(GameRule.FALL_DAMAGE, RuleResult.DENY);
				game.setRule(GameRule.HUNGER, RuleResult.DENY);
				game.setRule(GameRule.PORTALS, RuleResult.DENY);
				game.setRule(GameRule.PVP, RuleResult.DENY);
				game.setRule(GameRule.THROW_ITEMS, RuleResult.DENY);

				// Listeners
				game.on(GameTickListener.EVENT, phase::tick);
				game.on(OfferPlayerListener.EVENT, phase::offerPlayer);
				game.on(PlayerAddListener.EVENT, phase::addPlayer);
				game.on(PlayerDeathListener.EVENT, phase::onPlayerDeath);
				game.on(RequestStartListener.EVENT, phase::requestStart);
				game.on(UseBlockListener.EVENT, phase::useBlock);
			});
		}, Util.getMainWorkerExecutor());
	}

	private void tick() {
		this.timeElapsed += 1;
	}

	private Text getMistakeText(PlayerEntity causer) {
		MutableText displayName = causer.getDisplayName().copy();

		if (this.config.getMaxMistakes() <= 1) {
			return displayName.append(new LiteralText(" revealed a mine!")).formatted(Formatting.RED);
		} else {
			return displayName.append(new LiteralText(" revealed a mine! The maximum of " + this.config.getMaxMistakes() + " mistakes have been made.")).formatted(Formatting.RED);
		}
	}

	private void checkMistakes(PlayerEntity causer) {
		if (this.mistakes < this.config.getMaxMistakes()) return;

		Text text = this.getMistakeText(causer);
		for (PlayerEntity player : this.gameWorld.getPlayers()) {
			player.sendMessage(text, false);
		}

		this.gameWorld.close();
	}

	private boolean isFull() {
		return this.gameWorld.getPlayerCount() >= this.config.getPlayerConfig().getMaxPlayers();
	}

	private JoinResult offerPlayer(ServerPlayerEntity player) {
		return this.isFull() ? JoinResult.gameFull() : JoinResult.ok();
	}

	private StartResult requestStart() {
		PlayerConfig playerConfig = this.config.getPlayerConfig();
		if (this.gameWorld.getPlayerCount() < playerConfig.getMinPlayers()) {
			return StartResult.notEnoughPlayers();
		}

		this.started = true;
		this.updateFlagCount();
		return StartResult.ok();
	}

	private boolean isModifyingFlags(PlayerEntity player) {
		return player.inventory.selectedSlot == 8;
	}

	private void updateFlagCount() {
		ItemStack flagStack = ItemStackBuilder.of(this.config.getFlagStack())
			.addLore(new LiteralText("Right-click a covered field to").formatted(Formatting.GRAY))
			.addLore(new LiteralText("mark it as containing a mine.").formatted(Formatting.GRAY))
			.setCount(this.board.getRemainingFlags())
			.build();

		for (ServerPlayerEntity player : this.gameWorld.getPlayers()) {
			player.inventory.setStack(8, flagStack.copy());

			// Update inventory
			player.currentScreenHandler.sendContentUpdates();
			player.playerScreenHandler.onContentChanged(player.inventory);
			player.updateCursorStack();
		}
	}

	private ActionResult modifyField(ServerPlayerEntity uncoverer, BlockPos pos, Field field) {
		if (this.isModifyingFlags(uncoverer) && field.getVisibility() != FieldVisibility.UNCOVERED) {
			if (field.getVisibility() == FieldVisibility.FLAGGED) {
				field.setVisibility(FieldVisibility.COVERED);
				this.world.playSound(null, pos, SoundEvents.ENTITY_ITEM_FRAME_REMOVE_ITEM, SoundCategory.BLOCKS, 1, 1);
			} else {
				field.setVisibility(FieldVisibility.FLAGGED);
				this.world.playSound(null, pos, SoundEvents.ENTITY_ITEM_FRAME_ADD_ITEM, SoundCategory.BLOCKS, 1, 1);
			}

			return ActionResult.SUCCESS;
		} else if (field.getVisibility() == FieldVisibility.COVERED) {
			field.uncover(uncoverer, this);
			this.world.playSound(null, pos, SoundEvents.BLOCK_SAND_BREAK, SoundCategory.BLOCKS, 0.5f, 1);

			return ActionResult.SUCCESS;
		}
			
		return ActionResult.PASS;
	}

	private ActionResult useBlock(ServerPlayerEntity uncoverer, Hand hand, BlockHitResult hitResult) {
		if (!this.started) return ActionResult.PASS;
		if (hand != Hand.MAIN_HAND) return ActionResult.PASS;

		BlockPos pos = hitResult.getBlockPos();
		if (pos.getY() != 0) return ActionResult.PASS;
		if (!this.board.isValidPos(pos.getX(), pos.getZ())) return ActionResult.PASS;

		this.board.placeMines(pos.getX(), pos.getZ(), this.world.getRandom());

		Field field = this.board.getField(pos.getX(), pos.getZ());
		ActionResult result = this.modifyField(uncoverer, pos, field);

		if (result == ActionResult.SUCCESS) {
			this.uncoverNeighbors(uncoverer, pos, field);
			this.checkMistakes(uncoverer);
			this.board.build(this.world);
			this.updateFlagCount();

			if (this.board.isCompleted()) {
				Text text = new LiteralText("The board has been completed in " + this.timeElapsed / 20 + " seconds!").formatted(Formatting.GOLD);
				for (PlayerEntity player : this.gameWorld.getPlayers()) {
					player.sendMessage(text, false);
				}

				this.gameWorld.close();
			}
		}

		return result;
	}

	private void uncoverNeighbors(ServerPlayerEntity uncoverer, BlockPos clickedBlockPos, Field clickedField) {
		if (clickedField instanceof NumberField) return;
		if (((NumberField) clickedField).getValue() == 0) {
			List<BlockPos> uncheckedPositions = new ArrayList<>();
			List<BlockPos> checkedPositions = new ArrayList<>();
			uncheckedPositions.add(clickedBlockPos);

			while (!uncheckedPositions.isEmpty()) {
				BlockPos checkPos = uncheckedPositions.get(0);
				if (checkedPositions.contains(checkPos)) continue;

				for (int x = -1; x < 2; x++) {
					for (int z = -1; z < 2; z++) {
						BlockPos pos = checkPos.add(x, 0, z);
						Field field = this.board.getField(pos.getX(), pos.getZ());
						if (field == null) {
							checkedPositions.add(pos);
							continue;
						}

						if (field.getVisibility() == FieldVisibility.COVERED) {
							this.modifyField(uncoverer, pos, field);
						}
						if (field instanceof NumberField && ((NumberField)field).getValue() == 0) {
							uncheckedPositions.add(pos);
						}
					}
				}
				checkedPositions.add(checkPos);
				uncheckedPositions.remove(0);
			}
		}
	}

	private void addPlayer(ServerPlayerEntity player) {
		this.spawn(player);
	}

	private ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
		// Respawn player
		this.spawn(player);
		return ActionResult.SUCCESS;
	}

	private void spawn(ServerPlayerEntity player) {
		Vec3d center = new Vec3d(this.config.getBoardConfig().x / 2d, 1, this.config.getBoardConfig().x / 2d);
		player.teleport(this.world, center.getX(), center.getY(), center.getZ(), 0, 0);
	}
}