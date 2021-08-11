package io.github.haykam821.withersweeper.game.field;

import io.github.haykam821.withersweeper.game.phase.WithersweeperActivePhase;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public class Field {
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

	public void uncover(PlayerEntity uncoverer, WithersweeperActivePhase phase) {
		this.setVisibility(FieldVisibility.UNCOVERED);
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