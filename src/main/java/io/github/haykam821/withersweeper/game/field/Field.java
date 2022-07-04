package io.github.haykam821.withersweeper.game.field;

import io.github.haykam821.withersweeper.Withersweeper;
import io.github.haykam821.withersweeper.game.phase.WithersweeperActivePhase;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayDeque;

public class Field {
	private static final int MAX_UNCOVER_DEPTH = 125000;

	private static final BlockState DEFAULT_STATE = Blocks.AIR.getDefaultState();
	private static final BlockState COVERED_STATE = Blocks.SOUL_SOIL.getDefaultState();
	private static final BlockState FLAGGED_STATE = Blocks.CRIMSON_NYLIUM.getDefaultState();
	private static final BlockState ALTERNATIVE_FLAGGED_STATE = Blocks.WARPED_NYLIUM.getDefaultState();

	private static final Text DEFAULT_INFO_MESSAGE = new TranslatableText("text.withersweeper.info.default");
	private static final Text COVERED_INFO_MESSAGE = new TranslatableText("text.withersweeper.info.covered");
	private static final Text FLAGGED_INFO_MESSAGE = new TranslatableText("text.withersweeper.info.flagged");

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
		for (int depth = 0; !stack.isEmpty() && depth <= MAX_UNCOVER_DEPTH; depth++) {
			var pos = stack.pop();
			var x = pos.getX();
			var z = pos.getZ();
			var currentField = board.getField(x, z);
			if (currentField.canUncoverRecursively()) {
				// find a west border
				var field = currentField;
				while (field != null && field.canUncoverRecursively()) {
					field = board.getField(--x, z);
				}

				// iterate until find an east border
				boolean eastBorder = false;
				boolean spanNorth = false;
				boolean spanSouth = false;
				while (true) {
					// uncover a field
					if (field != null && field.getVisibility() == FieldVisibility.COVERED) {
						field.setVisibility(FieldVisibility.UNCOVERED);
						fieldsUncovered++;
					}

					// check north
					field = board.getField(x, z - 1);
					if (field != null) {
						if (field.canUncoverRecursively()) {
							if (!spanNorth && field.getVisibility() == FieldVisibility.COVERED) {
								stack.add(new BlockPos(x, y, z - 1));
								spanNorth = true;
							}
						} else {
							if (field.getVisibility() == FieldVisibility.COVERED) {
								field.setVisibility(FieldVisibility.UNCOVERED);
								fieldsUncovered++;
							}
							if (spanNorth) spanNorth = false;
						}
					}

					// check south
					field = board.getField(x, z + 1);
					if (field != null) {
						if (field.canUncoverRecursively()) {
							if (!spanSouth && field.getVisibility() == FieldVisibility.COVERED) {
								stack.add(new BlockPos(x, y, z + 1));
								spanSouth = true;
							}
						} else {
							if (field.getVisibility() == FieldVisibility.COVERED) {
								field.setVisibility(FieldVisibility.UNCOVERED);
								fieldsUncovered++;
							}
							if (spanSouth) spanSouth = false;
						}
					}

					if (eastBorder) break;

					// move to a next field
					field = board.getField(++x, z);
					eastBorder = field == null || !field.canUncoverRecursively();
				}
			} else if (currentField.getVisibility() == FieldVisibility.COVERED) {
				currentField.setVisibility(FieldVisibility.UNCOVERED);
				fieldsUncovered++;
			}
		}

		var statistics = phase.getStatisticsForPlayer(uncoverer);
		if (statistics != null) {
			statistics.increment(Withersweeper.FIELDS_UNCOVERED, fieldsUncovered);
		}
	}

	public boolean canUncoverRecursively() {
		return false;
	}

	public BlockState getBlockState() {
		return DEFAULT_STATE;
	}

	public BlockState getCoveredBlockState() {
		return switch (this.visibility) {
			case COVERED -> COVERED_STATE;
			case FLAGGED -> FLAGGED_STATE;
			case ALTERNATIVE_FLAGGED -> ALTERNATIVE_FLAGGED_STATE;
			default -> this.getBlockState();
		};
	}

	public Text getInfoMessage() {
		return DEFAULT_INFO_MESSAGE;
	}

	public Text getCoveredInfoMessage() {
		return switch (this.visibility) {
			case COVERED -> COVERED_INFO_MESSAGE;
			case FLAGGED, ALTERNATIVE_FLAGGED -> FLAGGED_INFO_MESSAGE;
			default -> this.getInfoMessage();
		};
	}
}