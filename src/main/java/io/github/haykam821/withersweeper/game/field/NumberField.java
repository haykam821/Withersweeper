package io.github.haykam821.withersweeper.game.field;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public class NumberField extends Field {
	private static final BlockState[] VALUES_TO_STATES = new BlockState[] {
		Blocks.WHITE_WOOL.getDefaultState(),
		Blocks.BLUE_WOOL.getDefaultState(),
		Blocks.GREEN_WOOL.getDefaultState(),
		Blocks.RED_WOOL.getDefaultState(),
		Blocks.LIGHT_BLUE_WOOL.getDefaultState(),
		Blocks.BROWN_WOOL.getDefaultState(),
		Blocks.CYAN_WOOL.getDefaultState(),
		Blocks.BLACK_WOOL.getDefaultState(),
		Blocks.LIGHT_GRAY_WOOL.getDefaultState()
	};

	private int value = 0;

	public NumberField(FieldVisibility visibility, int value) {
		super(visibility);

		if (value < 0 || value >= VALUES_TO_STATES.length) {
			throw new IllegalStateException("Value must be between 0 and 8 (inclusive)");
		}

		this.value = value;
	}

	public NumberField(int value) {
		this(FieldVisibility.COVERED, value);
	}

	@Override
	public boolean canUncoverRecursively() {
		return this.value == 0;
	}

	@Override
	public BlockState getBlockState() {
		return VALUES_TO_STATES[this.value];
	}

	@Override
	public Text getInfoMessage() {
		return new LiteralText("This field has " + this.value + " mine" + (this.value == 1 ? "" : "s") + " around it");
	}

	public NumberField increaseValue() {
		return new NumberField(this.getVisibility(), Math.min(this.value + 1, 8));
	}

	@Override
	public String toString() {
		return "NumberField{value=" + this.value + "}";
	}
}