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

public class MineField extends Field {
	private static final BlockState STATE = Blocks.TNT.getDefaultState();
	private static final Text INFO_MESSAGE = new LiteralText("This field is a mine");

	@Override
	public boolean isCompleted() {
		return true;
	}

	@Override
	public void uncover(BlockPos pos, ServerPlayerEntity uncoverer, WithersweeperActivePhase phase, LongSet uncoveredPositions, int depth) {
		super.uncover(pos, uncoverer, phase, uncoveredPositions, depth);

		phase.getStatisticsForPlayer(uncoverer).increment(Main.MINES_REVEALED, 1);
		phase.mistakes += 1;
	}

	@Override
	public BlockState getBlockState() {
		return STATE;
	}

	@Override
	public Text getInfoMessage() {
		return INFO_MESSAGE;
	}
}