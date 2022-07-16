package io.github.haykam821.withersweeper.game.field;

import io.github.haykam821.withersweeper.Main;
import io.github.haykam821.withersweeper.game.phase.WithersweeperActivePhase;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import xyz.nucleoid.plasmid.game.stats.StatisticMap;

public class MineField extends Field {
	private static final BlockState STATE = Blocks.TNT.getDefaultState();
	private static final Text INFO_MESSAGE = Text.translatable("text.withersweeper.info.mine");

	@Override
	public boolean isCompleted() {
		return true;
	}

	@Override
	public void uncover(BlockPos pos, ServerPlayerEntity uncoverer, WithersweeperActivePhase phase) {
		super.uncover(pos, uncoverer, phase);

		StatisticMap statistics = phase.getStatisticsForPlayer(uncoverer);
		if (statistics != null) {
			statistics.increment(Main.MINES_REVEALED, 1);
		}

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