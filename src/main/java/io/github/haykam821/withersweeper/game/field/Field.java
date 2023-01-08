package io.github.haykam821.withersweeper.game.field;

import io.github.haykam821.withersweeper.Main;
import io.github.haykam821.withersweeper.game.phase.WithersweeperActivePhase;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayDeque;

public class Field {
	private static final BlockState DEFAULT_STATE = Blocks.AIR.getDefaultState();
	private static final BlockState COVERED_STATE = Blocks.SOUL_SOIL.getDefaultState();
	private static final BlockState FLAGGED_STATE = Blocks.CRIMSON_NYLIUM.getDefaultState();

	private static final Text DEFAULT_INFO_MESSAGE = Text.translatable("text.withersweeper.info.default");
	private static final Text COVERED_INFO_MESSAGE = Text.translatable("text.withersweeper.info.covered");
	private static final Text FLAGGED_INFO_MESSAGE = Text.translatable("text.withersweeper.info.flagged");

	private FieldVisibility visibility;

	public Field(FieldVisibility visibility) {
		this.visibility = visibility;
	}

	public Field() {
		this(FieldVisibility.COVERED);
	}

	public FieldVisibility getVisibility() {
		return this.visibility;
	}

	public void setVisibility(FieldVisibility visibility) {
		this.visibility = visibility;
	}

	public boolean isCompleted() {
		return this.getVisibility() == FieldVisibility.UNCOVERED;
	}

	public void uncover(BlockPos blockPos, ServerPlayerEntity uncoverer, WithersweeperActivePhase phase) {
		var y = blockPos.getY();
		var board = phase.getBoard();
		var stack = new ArrayDeque<BlockPos>();

		var fieldsUncovered = 0;
		stack.add(blockPos);
		while (!stack.isEmpty()) {
			var pos = stack.pop();
			var x = pos.getX();
			var z = pos.getZ();
			var currentField = board.getField(x, z);
			if (currentField.getVisibility() == FieldVisibility.COVERED) {
				currentField.setVisibility(FieldVisibility.UNCOVERED);
				fieldsUncovered++;
				if (currentField.canUncoverRecursively()) {
					for (var neighbor : BlockPos.iterate(x - 1, y, z - 1, x + 1, y, z + 1)) {
						var field = board.getField(neighbor.getX(), neighbor.getZ());
						if (field != null && field.getVisibility() == FieldVisibility.COVERED) {
							stack.add(new BlockPos(neighbor));
						}
					}
				}
			}
		}

		var statistics = phase.getStatisticsForPlayer(uncoverer);
		if (statistics != null) {
			statistics.increment(Main.FIELDS_UNCOVERED, fieldsUncovered);
		}
	}

	public boolean canUncoverRecursively() {
		return false;
	}

	public BlockState getBlockState() {
		return DEFAULT_STATE;
	}

	public BlockState getCoveredBlockState() {
		if (this.visibility == FieldVisibility.COVERED) {
			return COVERED_STATE;
		} else if (this.visibility == FieldVisibility.FLAGGED) {
			return FLAGGED_STATE;
		} else {
			return this.getBlockState();
		}
	}

	public Text getInfoMessage() {
		return DEFAULT_INFO_MESSAGE;
	}

	public Text getCoveredInfoMessage() {
		if (this.visibility == FieldVisibility.COVERED) {
			return COVERED_INFO_MESSAGE;
		} else if (this.visibility == FieldVisibility.FLAGGED) {
			return FLAGGED_INFO_MESSAGE;
		} else {
			return this.getInfoMessage();
		}
	}
}