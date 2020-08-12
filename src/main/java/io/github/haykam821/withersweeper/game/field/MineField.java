package io.github.haykam821.withersweeper.game.field;

import io.github.haykam821.withersweeper.game.phase.WithersweeperActivePhase;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;

public class MineField extends Field {
	private static final BlockState STATE = Blocks.TNT.getDefaultState();

	@Override
	public void uncover(PlayerEntity uncoverer, WithersweeperActivePhase phase) {
		super.uncover(uncoverer, phase);
		phase.mistakes += 1;
	}

	@Override
	public BlockState getBlockState() {
		return STATE;
	}
}