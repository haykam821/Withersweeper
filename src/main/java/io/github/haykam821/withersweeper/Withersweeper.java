package io.github.haykam821.withersweeper;

import io.github.haykam821.withersweeper.game.WithersweeperConfig;
import io.github.haykam821.withersweeper.game.phase.WithersweeperWaitingPhase;
import net.fabricmc.api.ModInitializer;
import net.minecraft.text.KeybindText;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.game.GameType;
import xyz.nucleoid.plasmid.game.stats.StatisticKey;

public class Withersweeper implements ModInitializer {
	public static final String MOD_ID = "withersweeper";

	private static final Identifier WITHERSWEEPER_ID = new Identifier(MOD_ID, "withersweeper");
	public static final GameType<WithersweeperConfig> WITHERSWEEPER_TYPE = GameType.register(WITHERSWEEPER_ID, WithersweeperConfig.CODEC, WithersweeperWaitingPhase::open);

	private static final Identifier MINES_REVEALED_ID = new Identifier(MOD_ID, "mines_revealed");
	public static final StatisticKey<Integer> MINES_REVEALED = StatisticKey.intKey(MINES_REVEALED_ID);

	private static final Identifier FIELDS_UNCOVERED_ID = new Identifier(MOD_ID, "fields_uncovered");
	public static final StatisticKey<Integer> FIELDS_UNCOVERED = StatisticKey.intKey(FIELDS_UNCOVERED_ID);

	public static final Text DESCRIPTION_TEXT = new LiteralText("").formatted(Formatting.GRAY).append("\n")
			.append(new TranslatableText("gameType.withersweeper.withersweeper").formatted(Formatting.BOLD).formatted(Formatting.WHITE)).append("\n")
			.append(new TranslatableText("game.withersweeper.desc")).append("\n\n")
			.append(new TranslatableText("game.withersweeper.desc.controls.uncover", new TranslatableText("%s", new KeybindText("key.attack")).formatted(Formatting.WHITE))).append("\n")
			.append(new TranslatableText("game.withersweeper.desc.controls.flag", new TranslatableText("%s", new KeybindText("key.use")).formatted(Formatting.WHITE))).append("\n")
			.append(new TranslatableText("game.withersweeper.desc.controls.info", new TranslatableText("%s", new KeybindText("key.drop")).formatted(Formatting.WHITE))).append("\n");

	@Override
	public void onInitialize() {}
}