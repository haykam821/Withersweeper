package io.github.haykam821.withersweeper.game.phase;

import io.github.haykam821.withersweeper.game.WithersweeperConfig;
import io.github.haykam821.withersweeper.game.board.Board;
import io.github.haykam821.withersweeper.game.field.Field;
import io.github.haykam821.withersweeper.game.field.FieldVisibility;
import io.github.haykam821.withersweeper.game.field.MineField;
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
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import xyz.nucleoid.fantasy.BubbleWorldConfig;
import xyz.nucleoid.plasmid.game.GameOpenContext;
import xyz.nucleoid.plasmid.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.StartResult;
import xyz.nucleoid.plasmid.game.config.PlayerConfig;
import xyz.nucleoid.plasmid.game.event.GameTickListener;
import xyz.nucleoid.plasmid.game.event.OfferPlayerListener;
import xyz.nucleoid.plasmid.game.event.PlayerAddListener;
import xyz.nucleoid.plasmid.game.event.PlayerDeathListener;
import xyz.nucleoid.plasmid.game.event.RequestStartListener;
import xyz.nucleoid.plasmid.game.event.UseBlockListener;
import xyz.nucleoid.plasmid.game.player.JoinResult;
import xyz.nucleoid.plasmid.game.rule.GameRule;
import xyz.nucleoid.plasmid.game.rule.RuleResult;
import xyz.nucleoid.plasmid.map.template.MapTemplate;
import xyz.nucleoid.plasmid.map.template.TemplateChunkGenerator;
import xyz.nucleoid.plasmid.util.ItemStackBuilder;

public class WithersweeperActivePhase {
	private final ServerWorld world;
	public final GameSpace gameSpace;
	private final WithersweeperConfig config;
	private final Board board;
	private int timeElapsed = 0;
	public int mistakes = 0;
	private boolean started = false;
	private boolean firstReveal = true;

	public WithersweeperActivePhase(GameSpace gameSpace, WithersweeperConfig config, Board board) {
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
			WithersweeperActivePhase phase = new WithersweeperActivePhase(game.getSpace(), context.getConfig(), board);

			// Rules
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
		for (PlayerEntity player : this.gameSpace.getPlayers()) {
			player.sendMessage(text, false);
		}

		this.gameSpace.close();
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

		this.started = true;
		this.updateFlagCount();
		return StartResult.OK;
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

		for (ServerPlayerEntity player : this.gameSpace.getPlayers()) {
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

		if (this.firstReveal) {
			this.firstReveal = false;
			while (field.getBlockState() != Blocks.WHITE_WOOL.getDefaultState()) {
				this.board.generateBoard();
				this.board.placeMines(pos.getX(), pos.getZ(), this.world.getRandom());
				field = this.board.getField(pos.getX(), pos.getZ());
			}
		}

		ActionResult result = this.modifyField(uncoverer, pos, field);

		if (result == ActionResult.SUCCESS) {
			this.checkMistakes(uncoverer);
			this.board.build(this.world);
			this.updateFlagCount();

			if (this.board.isCompleted()) {
				Text text = new LiteralText("The board has been completed in " + this.timeElapsed / 20 + " seconds!").formatted(Formatting.GOLD);
				for (PlayerEntity player : this.gameSpace.getPlayers()) {
					player.sendMessage(text, false);
				}

				this.gameSpace.close();
			}
		}

		return result;
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