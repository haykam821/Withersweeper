package io.github.haykam821.withersweeper.game.field;

import io.github.haykam821.withersweeper.Main;
import io.github.haykam821.withersweeper.game.phase.WithersweeperActivePhase;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class Field {
	private static final int MAX_UNCOVER_DEPTH = 25;

	private static final BlockState DEFAULT_STATE = Blocks.AIR.getDefaultState();
	private static final BlockState COVERED_STATE = Blocks.SOUL_SOIL.getDefaultState();
	private static final BlockState FLAGGED_STATE = Blocks.CRIMSON_NYLIUM.getDefaultState();

	private static final Text DEFAULT_INFO_MESSAGE = new LiteralText("This is a field");
	private static final Text COVERED_INFO_MESSAGE = new LiteralText("This field is covered");
	private static final Text FLAGGED_INFO_MESSAGE = new LiteralText("This field is flagged");

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

	public void uncover(BlockPos pos, ServerPlayerEntity uncoverer, WithersweeperActivePhase phase, LongSet uncoveredPositions, int depth) {
		uncoveredPositions.add(pos.asLong());

		this.setVisibility(FieldVisibility.UNCOVERED);
		phase.getStatisticsForPlayer(uncoverer).increment(Main.FIELDS_UNCOVERED, 1);

		if (this.canUncoverRecursively() && depth < MAX_UNCOVER_DEPTH) {
			for (BlockPos neighborPos : BlockPos.iterate(pos.getX() - 1, pos.getY(), pos.getZ() - 1, pos.getX() + 1, pos.getY(), pos.getZ() + 1)) {
				if (!uncoveredPositions.contains(neighborPos.asLong())) {
					Field field = phase.getBoard().getField(neighborPos.getX(), neighborPos.getZ());
					if (field != null && field.getVisibility() == FieldVisibility.COVERED) {
						field.uncover(neighborPos, uncoverer, phase, uncoveredPositions, depth + 1);
					}
				}
			}
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