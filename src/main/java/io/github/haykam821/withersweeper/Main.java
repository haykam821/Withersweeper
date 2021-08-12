package io.github.haykam821.withersweeper;

import io.github.haykam821.withersweeper.game.WithersweeperConfig;
import io.github.haykam821.withersweeper.game.phase.WithersweeperWaitingPhase;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.game.GameType;
import xyz.nucleoid.plasmid.game.stats.StatisticKey;

public class Main implements ModInitializer {
	public static final String MOD_ID = "withersweeper";

	private static final Identifier WITHERSWEEPER_ID = new Identifier(MOD_ID, "withersweeper");
	public static final GameType<WithersweeperConfig> WITHERSWEEPER_TYPE = GameType.register(WITHERSWEEPER_ID, WithersweeperWaitingPhase::open, WithersweeperConfig.CODEC);

	private static final Identifier MINES_REVEALED_ID = new Identifier(MOD_ID, "mines_revealed");
	public static final StatisticKey<Integer> MINES_REVEALED = StatisticKey.intKey(MINES_REVEALED_ID, StatisticKey.StorageType.TOTAL);

	private static final Identifier FIELDS_UNCOVERED_ID = new Identifier(MOD_ID, "fields_uncovered");
	public static final StatisticKey<Integer> FIELDS_UNCOVERED = StatisticKey.intKey(FIELDS_UNCOVERED_ID, StatisticKey.StorageType.TOTAL);

	@Override
	public void onInitialize() {
		return;
	}
}